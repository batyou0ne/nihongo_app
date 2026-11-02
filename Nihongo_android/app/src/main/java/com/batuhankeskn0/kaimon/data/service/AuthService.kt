package com.batuhankeskn0.kaimon.data.service

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Oturum açmış (veya anonim) Firebase kullanıcısının UI'a taşınan değiştirilemez özeti.
 * iOS `AuthService.currentUser` (FirebaseAuth.User) karşılığıdır.
 */
data class AuthUserInfo(
    val uid: String,
    val isAnonymous: Boolean,
    val email: String? = null,
    val displayName: String? = null,
    val providerIds: List<String> = emptyList()
) {
    /** Kullanıcının bağlı bir hesabı var mı (Google veya email — fark etmez)? */
    val hasAccount: Boolean
        get() = !isAnonymous

    val isLinkedToGoogle: Boolean
        get() = providerIds.contains(PROVIDER_GOOGLE)

    /** Hesap ekranında gösterilecek email/isim. */
    val accountDescription: String
        get() = email?.takeIf { it.isNotBlank() }
            ?: displayName?.takeIf { it.isNotBlank() }
            ?: ""

    /** Bağlı giriş yöntemlerinin okunabilir adları (ör. `["Google", "Email/Şifre"]`). */
    val providerNames: List<String>
        get() = providerIds.mapNotNull { id ->
            when (id) {
                PROVIDER_GOOGLE -> "Google"
                PROVIDER_PASSWORD -> "Email/Şifre"
                PROVIDER_FIREBASE -> null // `firebase` sağlayıcısı anonim/özel token oturumudur, gösterilmez.
                else -> id
            }
        }

    companion object {
        const val PROVIDER_GOOGLE = "google.com"
        const val PROVIDER_PASSWORD = "password"
        const val PROVIDER_FIREBASE = "firebase"
    }
}

/**
 * `AuthService` tarafından fırlatılan, UI'da yerelleştirilmiş mesaja çevrilebilen alan hataları.
 */
class AuthServiceException(
    val kind: Kind,
    cause: Throwable? = null
) : Exception(kind.name, cause) {
    enum class Kind {
        FIREBASE_UNAVAILABLE,
        INVALID_CREDENTIAL,
        MISSING_GOOGLE_CONFIG,
        NO_GOOGLE_ACCOUNT,
        RECENT_LOGIN_REQUIRED,
        NO_SIGNED_IN_USER
    }
}

/**
 * Firebase Auth işlemlerini yöneten servis. iOS `AuthService.swift` karşılığıdır.
 *
 * - İlk açılışta sessizce anonim oturum açar (`signInAnonymouslyIfNeeded`).
 * - Google (Android `CredentialManager` + `GoogleIdTokenCredential`) ve Email/Şifre ile giriş yapar.
 * - **Hesap bağlama:** Anonim kullanıcı Google/Email bağladığında `linkWithCredential` ile `uid` korunur.
 *   Kimlik başka bir hesaba aitse (`FirebaseAuthUserCollisionException`) `signInWithCredential` ile o hesaba geçilir.
 * - Her kimlik değişiminde `UserDatabaseManager` yeni `uid` veritabanına geçer; anonim `progress_anonymous.db`,
 *   hedef `progress_<uid>.db` henüz yoksa `.db/-shm/-wal` dosyalarıyla veri kaybı olmadan taşınır.
 * - `google-services.json` yoksa `FirebaseApp` başlatılmaz; servis çökmeden yerel (offline) modda kalır.
 */
class AuthService internal constructor() {

    @Volatile
    private var appContext: Context? = null

    private var auth: FirebaseAuth? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentUser = MutableStateFlow<AuthUserInfo?>(null)

    /** O anki Firebase kullanıcısı (anonim veya hesaba bağlı); Firebase yapılandırılmamışsa `null`. */
    val currentUser: StateFlow<AuthUserInfo?> = _currentUser.asStateFlow()

    /** Firebase yapılandırılmış ve kullanılabilir mi (`google-services.json` mevcut mu)? */
    @Volatile
    var isFirebaseAvailable: Boolean = false
        private set

    val isAnonymous: Boolean
        get() = _currentUser.value?.isAnonymous ?: true

    val hasAccount: Boolean
        get() = _currentUser.value?.hasAccount ?: false

    /**
     * `KaimonApplication.onCreate` içinde çağrılır: Firebase'in başlatılıp başlatılmadığını denetler,
     * dinleyiciyi bağlar, yerel veritabanını mevcut kullanıcıya göre seçer ve anonim oturumu garantiler.
     */
    internal fun attachContext(context: Context) {
        val app = context.applicationContext
        appContext = app
        val firebaseReady = runCatching { FirebaseApp.getApps(app).isNotEmpty() }.getOrDefault(false)
        isFirebaseAvailable = firebaseReady
        if (!firebaseReady) {
            Log.i(TAG, "Firebase yapılandırılmamış (google-services.json yok); yerel mod etkin.")
            return
        }

        val firebaseAuth = FirebaseAuth.getInstance()
        auth = firebaseAuth
        publishUser(firebaseAuth.currentUser, switchDatabaseBlocking = true)

        authStateListener?.let { firebaseAuth.removeAuthStateListener(it) }
        val listener = FirebaseAuth.AuthStateListener { fa ->
            scope.launch { applyUserChange(fa.currentUser) }
        }
        authStateListener = listener
        firebaseAuth.addAuthStateListener(listener)

        scope.launch { signInAnonymouslyIfNeeded() }
    }

    // MARK: - Anonim oturum

    /**
     * Uygulama açılışında çağrılır: mevcut oturum yoksa anonim oturum açar.
     * İnternet yoksa sessizce geçer; bir sonraki açılışta tekrar denenir.
     */
    suspend fun signInAnonymouslyIfNeeded() {
        val firebaseAuth = auth ?: return
        if (firebaseAuth.currentUser != null) return
        try {
            firebaseAuth.signInAnonymously().await()
            applyUserChange(firebaseAuth.currentUser)
        } catch (error: Exception) {
            Log.w(TAG, "Anonim oturum açılamadı: ${error.javaClass.simpleName}")
        }
    }

    // MARK: - Google ile giriş

    /**
     * Android `CredentialManager` ile Google hesabı seçtirir ve sonucu Firebase hesabına bağlar.
     * @param activityContext Credential Manager arayüzünü sunmak için gereken `Activity` bağlamı.
     * @return Kullanıcı akışı kendi iptal ettiyse `false`, giriş başarılıysa `true`.
     */
    suspend fun signInWithGoogle(activityContext: Context): Boolean {
        requireAuth()
        val serverClientId = googleWebClientId() ?: throw AuthServiceException(
            AuthServiceException.Kind.MISSING_GOOGLE_CONFIG
        )

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val response = try {
            CredentialManager.create(activityContext).getCredential(activityContext, request)
        } catch (_: GetCredentialCancellationException) {
            return false // Kullanıcının kendi iptali hata sayılmaz.
        } catch (error: NoCredentialException) {
            throw AuthServiceException(AuthServiceException.Kind.NO_GOOGLE_ACCOUNT, error)
        } catch (error: GetCredentialException) {
            throw AuthServiceException(AuthServiceException.Kind.INVALID_CREDENTIAL, error)
        }

        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val idToken = try {
                GoogleIdTokenCredential.createFrom(credential.data).idToken
            } catch (error: GoogleIdTokenParsingException) {
                throw AuthServiceException(AuthServiceException.Kind.INVALID_CREDENTIAL, error)
            }
            linkOrSignIn(GoogleAuthProvider.getCredential(idToken, null))
            return true
        }
        throw AuthServiceException(AuthServiceException.Kind.INVALID_CREDENTIAL)
    }

    /**
     * `google-services` eklentisinin ürettiği `default_web_client_id` string kaynağını çalışma zamanında okur.
     * Eklenti etkin değilse (`google-services.json` yok) `null` döner ve derleme kırılmaz.
     */
    @Suppress("DiscouragedApi")
    private fun googleWebClientId(): String? {
        val context = appContext ?: return null
        val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (resId == 0) return null
        return context.getString(resId).takeIf { it.isNotBlank() }
    }

    // MARK: - Ortak bağlama akışı

    /**
     * Verilen kimliği mevcut (anonim) hesaba bağlar → `uid` korunur. Kimlik zaten başka bir hesaba aitse
     * o hesaba giriş yapar; bu durumda anonim ilerleme, hedef veritabanı yoksa yeni `uid` veritabanına taşınır.
     */
    private suspend fun linkOrSignIn(credential: AuthCredential) {
        val firebaseAuth = requireAuth()
        val user = firebaseAuth.currentUser
        if (user != null) {
            try {
                user.linkWithCredential(credential).await()
            } catch (collision: FirebaseAuthUserCollisionException) {
                val existingCredential = collision.updatedCredential ?: credential
                firebaseAuth.signInWithCredential(existingCredential).await()
            }
        } else {
            firebaseAuth.signInWithCredential(credential).await()
        }
        applyUserChange(firebaseAuth.currentUser)
    }

    // MARK: - Email/şifre

    /**
     * Email/şifre ile kayıt oluşturur. Mevcut anonim hesap bu kimliğe `link` edilir,
     * böylece `uid` değişmez ve o ana kadarki ilerleme korunur.
     */
    suspend fun signUp(email: String, password: String, displayName: String? = null) {
        val firebaseAuth = requireAuth()
        val user = firebaseAuth.currentUser
        if (user != null) {
            user.linkWithCredential(EmailAuthProvider.getCredential(email, password)).await()
        } else {
            firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        }

        val current = firebaseAuth.currentUser
        if (!displayName.isNullOrBlank() && current != null) {
            runCatching {
                current.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(displayName).build()
                ).await()
            }
        }
        applyUserChange(firebaseAuth.currentUser)
    }

    /** Var olan bir email/şifre hesabına giriş yapar. */
    suspend fun signIn(email: String, password: String) {
        val firebaseAuth = requireAuth()
        firebaseAuth.signInWithEmailAndPassword(email, password).await()
        applyUserChange(firebaseAuth.currentUser)
    }

    /** Şifre sıfırlama maili gönderir. */
    suspend fun sendPasswordReset(email: String) {
        requireAuth().sendPasswordResetEmail(email).await()
    }

    // MARK: - Çıkış & hesap silme

    /** Oturumu kapatır ve misafir moduna (yeni anonim oturum) döner. */
    suspend fun signOut() {
        val firebaseAuth = auth
        firebaseAuth?.signOut()
        clearGoogleCredentialState()
        applyUserChange(null)
        UserProfileService.shared.clear()
        signInAnonymouslyIfNeeded()
    }

    /**
     * Hesabı kalıcı olarak siler: Firestore profili, Firebase kullanıcısı ve bu cihazdaki `progress_<uid>.db`.
     * Firebase "yakın zamanda giriş" şartı karşılanmıyorsa [AuthServiceException.Kind.RECENT_LOGIN_REQUIRED] fırlatır
     * ve hiçbir veri değiştirilmez.
     */
    suspend fun deleteAccount() {
        val firebaseAuth = requireAuth()
        val user = firebaseAuth.currentUser
            ?: throw AuthServiceException(AuthServiceException.Kind.NO_SIGNED_IN_USER)
        val uid = user.uid
        val profileService = UserProfileService.shared
        val profileBackup = profileService.profile.value

        profileService.deleteProfile(uid)
        try {
            user.delete().await()
        } catch (error: FirebaseAuthRecentLoginRequiredException) {
            profileBackup?.let { profileService.save(it) }
            throw AuthServiceException(AuthServiceException.Kind.RECENT_LOGIN_REQUIRED, error)
        } catch (error: Exception) {
            profileBackup?.let { profileService.save(it) }
            throw error
        }

        withContext(Dispatchers.IO) {
            val databaseManager = UserDatabaseManager.shared
            // Önce anonim veritabanına geç (aktif bağlantıyı bırak), sonra eski kullanıcının dosyalarını sil.
            databaseManager.switchUser(UserDatabaseManager.ANONYMOUS_UID)
            databaseManager.deleteUserDatabase(UserDatabaseManager.normalizeUid(uid))
        }
        clearGoogleCredentialState()
        firebaseAuth.signOut()
        applyUserChange(null)
        profileService.clear()
        signInAnonymouslyIfNeeded()
    }

    // MARK: - Dahili

    private fun requireAuth(): FirebaseAuth =
        auth ?: throw AuthServiceException(AuthServiceException.Kind.FIREBASE_UNAVAILABLE)

    private suspend fun clearGoogleCredentialState() {
        val context = appContext ?: return
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }

    /** Kullanıcı değişimini yayınlar; yerel veritabanını ve profil önbelleğini yeni kimliğe göre ayarlar. */
    private suspend fun applyUserChange(user: FirebaseUser?) {
        withContext(Dispatchers.IO) {
            publishUser(user, switchDatabaseBlocking = true)
        }
        UserProfileService.shared.onUserChanged(user?.uid?.takeIf { !user.isAnonymous })
    }

    private fun publishUser(user: FirebaseUser?, switchDatabaseBlocking: Boolean) {
        val info = user?.let {
            AuthUserInfo(
                uid = it.uid,
                isAnonymous = it.isAnonymous,
                email = it.email ?: it.providerData.firstNotNullOfOrNull { provider -> provider.email },
                displayName = it.displayName,
                providerIds = it.providerData.map { provider -> provider.providerId }
            )
        }
        _currentUser.value = info

        if (switchDatabaseBlocking) {
            val targetUid = UserDatabaseManager.resolveDatabaseUid(info?.uid, info?.isAnonymous ?: true)
            runCatching { UserDatabaseManager.shared.switchUser(targetUid) }
                .onFailure { Log.e(TAG, "Kullanıcı veritabanına geçilemedi: ${it.javaClass.simpleName}") }
        }
    }

    companion object {
        private const val TAG = "AuthService"

        @Volatile
        private var sharedInstance: AuthService = AuthService()

        val shared: AuthService
            get() = sharedInstance

        fun init(context: Context) {
            sharedInstance.attachContext(context)
        }

        /** Firebase Auth hatalarını kullanıcıya gösterilebilir (TR/EN) mesajlara çevirir. */
        fun friendlyMessage(error: Throwable): String {
            if (error is AuthServiceException) return friendlyMessageForKind(error.kind)
            if (error is FirebaseNetworkException) return L10n.authNetworkError
            if (error is FirebaseTooManyRequestsException) return L10n.authTooManyRequests
            if (error is FirebaseAuthUserCollisionException) {
                return friendlyMessageForCode(error.errorCode) ?: L10n.authEmailInUse
            }
            if (error is FirebaseAuthException) {
                return friendlyMessageForCode(error.errorCode) ?: (error.localizedMessage ?: L10n.authGenericError)
            }
            return error.localizedMessage?.takeIf { it.isNotBlank() } ?: L10n.authGenericError
        }

        fun friendlyMessageForKind(kind: AuthServiceException.Kind): String =
            when (kind) {
                AuthServiceException.Kind.FIREBASE_UNAVAILABLE -> L10n.authFirebaseUnavailable
                AuthServiceException.Kind.INVALID_CREDENTIAL -> L10n.authInvalidCredential
                AuthServiceException.Kind.MISSING_GOOGLE_CONFIG -> L10n.authMissingGoogleConfig
                AuthServiceException.Kind.NO_GOOGLE_ACCOUNT -> L10n.authNoGoogleAccount
                AuthServiceException.Kind.RECENT_LOGIN_REQUIRED -> L10n.authRecentLoginRequired
                AuthServiceException.Kind.NO_SIGNED_IN_USER -> L10n.authGenericError
            }

        /** `FirebaseAuthException.errorCode` değerini yerelleştirilmiş mesaja eşler; bilinmiyorsa `null`. */
        fun friendlyMessageForCode(code: String?): String? =
            when (code) {
                "ERROR_INVALID_EMAIL" -> L10n.authInvalidEmail
                "ERROR_EMAIL_ALREADY_IN_USE",
                "ERROR_CREDENTIAL_ALREADY_IN_USE" -> L10n.authEmailInUse
                "ERROR_WEAK_PASSWORD" -> L10n.authWeakPassword
                "ERROR_WRONG_PASSWORD",
                "ERROR_INVALID_CREDENTIAL",
                "ERROR_INVALID_LOGIN_CREDENTIALS" -> L10n.authWrongCredentials
                "ERROR_USER_NOT_FOUND" -> L10n.authUserNotFound
                "ERROR_TOO_MANY_REQUESTS" -> L10n.authTooManyRequests
                "ERROR_OPERATION_NOT_ALLOWED" -> L10n.authOperationNotAllowed
                "ERROR_REQUIRES_RECENT_LOGIN" -> L10n.authRecentLoginRequired
                else -> null
            }
    }
}
