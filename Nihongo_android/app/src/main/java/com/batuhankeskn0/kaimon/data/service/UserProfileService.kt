package com.batuhankeskn0.kaimon.data.service

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore'daki kullanıcı profili (`users/{uid}` dokümanı). iOS `UserProfile` karşılığıdır.
 *
 * Doküman alanları `toFirestoreMap`/`fromFirestoreMap` ile elle eşlenir; böylece Firestore'un reflection
 * tabanlı POJO eşlemesine (ve R8 kurallarına) bağımlı kalınmaz.
 */
data class UserProfile(
    val firstName: String = "",
    val lastName: String = "",
    val age: Int? = null
) {
    val fullName: String
        get() = "$firstName $lastName".trim()

    fun toFirestoreMap(): Map<String, Any?> =
        mapOf(
            FIELD_FIRST_NAME to firstName,
            FIELD_LAST_NAME to lastName,
            FIELD_AGE to age
        )

    companion object {
        const val COLLECTION = "users"
        const val FIELD_FIRST_NAME = "firstName"
        const val FIELD_LAST_NAME = "lastName"
        const val FIELD_AGE = "age"

        /** Firestore doküman verisinden profil üretir; alanlar eksik/yanlış tipteyse güvenli varsayılanlar kullanır. */
        fun fromFirestoreMap(data: Map<String, Any?>?): UserProfile? {
            if (data == null) return null
            val age = (data[FIELD_AGE] as? Number)?.toInt()
            return UserProfile(
                firstName = data[FIELD_FIRST_NAME] as? String ?: "",
                lastName = data[FIELD_LAST_NAME] as? String ?: "",
                age = age
            )
        }
    }
}

/**
 * Kayıt sırasında girilen kişisel bilgileri Firestore'a yazan ve hesap ekranı için okuyan servis.
 * Doküman `uid`'ye bağlı olduğundan kullanıcı başka cihazdan giriş yaptığında da profili gelir.
 * iOS `UserProfileService.swift` karşılığıdır. Firebase yapılandırılmamışsa tüm işlemler sessizce atlanır.
 */
class UserProfileService internal constructor() {

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    @Volatile
    private var loadedForUid: String? = null

    private val isAvailable: Boolean
        get() = AuthService.shared.isFirebaseAvailable

    private fun firestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    private fun currentUid(): String? =
        if (isAvailable) FirebaseAuth.getInstance().currentUser?.uid else null

    /** Profili Firestore'a kaydeder (offline'da Firestore kuyruğa alır, bağlantı gelince yazılır). */
    fun save(profile: UserProfile) {
        val uid = currentUid() ?: return
        firestore().collection(UserProfile.COLLECTION).document(uid)
            .set(profile.toFirestoreMap(), SetOptions.merge())
            .addOnFailureListener { Log.w(TAG, "Profil kaydedilemedi: ${it.javaClass.simpleName}") }
        _profile.value = profile
        loadedForUid = uid
    }

    /** O anki kullanıcının profilini (henüz yüklenmediyse) Firestore'dan çeker. */
    suspend fun loadIfNeeded() {
        val uid = currentUid() ?: return
        if (loadedForUid == uid) return
        try {
            val snapshot = firestore().collection(UserProfile.COLLECTION).document(uid).get().await()
            _profile.value = if (snapshot.exists()) UserProfile.fromFirestoreMap(snapshot.data) else null
            loadedForUid = uid
        } catch (error: Exception) {
            Log.w(TAG, "Profil yüklenemedi: ${error.javaClass.simpleName}")
        }
    }

    /** Hesap silme sırasında kullanıcının Firestore dokümanını siler (en iyi çaba). */
    suspend fun deleteProfile(uid: String) {
        if (!isAvailable) return
        runCatching {
            firestore().collection(UserProfile.COLLECTION).document(uid).delete().await()
        }.onFailure { Log.w(TAG, "Profil silinemedi: ${it.javaClass.simpleName}") }
    }

    /**
     * Kimlik değiştiğinde önbelleği geçersiz kılar. Bağlı (anonim olmayan) yeni kullanıcı varsa profilini yükler.
     */
    suspend fun onUserChanged(linkedUid: String?) {
        if (linkedUid == null) {
            clear()
            return
        }
        if (loadedForUid != linkedUid) {
            _profile.value = null
            loadedForUid = null
        }
        loadIfNeeded()
    }

    /** Çıkış yapıldığında önbelleği temizler. */
    fun clear() {
        _profile.value = null
        loadedForUid = null
    }

    companion object {
        private const val TAG = "UserProfileService"

        @Volatile
        private var sharedInstance: UserProfileService = UserProfileService()

        val shared: UserProfileService
            get() = sharedInstance
    }
}
