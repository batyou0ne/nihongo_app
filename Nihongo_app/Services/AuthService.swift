import Foundation
import Security
import CryptoKit
import UIKit
import AuthenticationServices
import FirebaseAuth
import FirebaseCore
import GoogleSignIn

/// Firebase Auth işlemlerini yöneten servis.
///
/// Uygulama ilk açılışta sessizce anonim oturum açar; kullanıcı daha sonra
/// Apple ile giriş yaptığında anonim hesap Apple kimliğine "link" edilir.
/// Böylece uid değişmez ve ilerleme verisi kaybolmaz.
@Observable
final class AuthService {
    static let shared = AuthService()

    /// O anki Firebase kullanıcısı (anonim veya hesaba bağlı).
    private(set) var currentUser: FirebaseAuth.User?

    /// Apple ile giriş akışı için üretilen nonce (replay saldırılarını önler).
    private var currentNonce: String?

    /// Auth durumu değişikliklerini dinleyen handle (yaşam süresi boyunca tutulur).
    private var authStateListener: AuthStateDidChangeListenerHandle?

    private init() {
        currentUser = Auth.auth().currentUser
        authStateListener = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            self?.currentUser = user
        }
    }

    /// Kullanıcı Apple kimliğiyle hesap bağlamış mı?
    var isLinkedToApple: Bool {
        currentUser?.providerData.contains { $0.providerID == "apple.com" } ?? false
    }

    /// Kullanıcı hâlâ anonim mi (henüz hesap bağlamamış mı)?
    var isAnonymous: Bool {
        currentUser?.isAnonymous ?? true
    }

    /// Kullanıcının bağlı bir hesabı var mı (Apple, Google veya email — fark etmez)?
    var hasAccount: Bool {
        !(currentUser?.isAnonymous ?? true)
    }

    /// Hesap ekranında gösterilecek email/isim.
    var accountDescription: String {
        guard let user = currentUser else { return "" }
        return user.email
            ?? user.providerData.compactMap(\.email).first
            ?? user.displayName
            ?? "Bağlı hesap"
    }

    /// Bağlı giriş yöntemlerinin okunabilir adları (ör. ["Google", "Email/Şifre"]).
    var providerNames: [String] {
        (currentUser?.providerData ?? []).map { provider in
            switch provider.providerID {
            case "apple.com": return "Apple"
            case "google.com": return "Google"
            case "password": return "Email/Şifre"
            default: return provider.providerID
            }
        }
    }

    /// Oturumu kapatır ve misafir moduna (yeni anonim oturum) döner.
    func signOut() async {
        try? Auth.auth().signOut()
        currentUser = nil
        await signInAnonymouslyIfNeeded()
    }

    // MARK: - Anonim oturum

    /// Uygulama açılışında çağrılır: mevcut oturum yoksa anonim oturum açar.
    /// İnternet yoksa sessizce geçer; bir sonraki açılışta tekrar denenir.
    func signInAnonymouslyIfNeeded() async {
        guard Auth.auth().currentUser == nil else { return }
        do {
            let result = try await Auth.auth().signInAnonymously()
            currentUser = result.user
        } catch {
            print("Anonim oturum açılamadı: \(error.localizedDescription)")
        }
    }

    // MARK: - Sign in with Apple

    /// `SignInWithAppleButton`'ın `onRequest` closure'ında çağrılır.
    func prepareAppleRequest(_ request: ASAuthorizationAppleIDRequest) {
        let nonce = randomNonceString()
        currentNonce = nonce
        request.requestedScopes = [.fullName, .email]
        request.nonce = sha256(nonce)
    }

    /// `SignInWithAppleButton`'ın `onCompletion` sonucunu işler.
    ///
    /// Anonim kullanıcıyı Apple kimliğine bağlar; bu Apple kimliği daha önce
    /// başka bir hesaba bağlanmışsa o hesaba giriş yapar.
    /// - Returns: Kullanıcı akışı kendi iptal ettiyse `false`, giriş başarılıysa `true`.
    @discardableResult
    func handleAppleCompletion(_ result: Result<ASAuthorization, Error>) async throws -> Bool {
        switch result {
        case .failure(let error):
            // Kullanıcının kendi iptali hata sayılmaz.
            if let authError = error as? ASAuthorizationError, authError.code == .canceled {
                return false
            }
            throw error

        case .success(let authorization):
            guard
                let appleIDCredential = authorization.credential as? ASAuthorizationAppleIDCredential,
                let nonce = currentNonce,
                let tokenData = appleIDCredential.identityToken,
                let idToken = String(data: tokenData, encoding: .utf8)
            else {
                throw AuthError.invalidCredential
            }

            let credential = OAuthProvider.appleCredential(
                withIDToken: idToken,
                rawNonce: nonce,
                fullName: appleIDCredential.fullName
            )
            try await linkOrSignIn(with: credential)
            return true
        }
    }

    // MARK: - Google ile giriş

    /// Google giriş akışını başlatır ve sonucu Firebase hesabına bağlar.
    /// - Returns: Kullanıcı akışı kendi iptal ettiyse `false`, giriş başarılıysa `true`.
    @discardableResult
    func signInWithGoogle() async throws -> Bool {
        guard let clientID = FirebaseApp.app()?.options.clientID else {
            throw AuthError.missingGoogleConfig
        }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)

        guard let rootViewController else {
            throw AuthError.missingGoogleConfig
        }

        let result: GIDSignInResult
        do {
            result = try await GIDSignIn.sharedInstance.signIn(withPresenting: rootViewController)
        } catch let error as GIDSignInError where error.code == .canceled {
            return false // Kullanıcının kendi iptali hata sayılmaz.
        }

        guard let idToken = result.user.idToken?.tokenString else {
            throw AuthError.invalidCredential
        }
        let credential = GoogleAuthProvider.credential(
            withIDToken: idToken,
            accessToken: result.user.accessToken.tokenString
        )
        try await linkOrSignIn(with: credential)
        return true
    }

    /// Google giriş sayfasını sunmak için o anki kök view controller.
    private var rootViewController: UIViewController? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }?
            .rootViewController
    }

    // MARK: - Ortak bağlama akışı

    /// Verilen kimliği mevcut (anonim) hesaba bağlar → uid korunur. Kimlik zaten
    /// başka bir hesaba aitse o hesaba giriş yapar.
    private func linkOrSignIn(with credential: AuthCredential) async throws {
        if let user = Auth.auth().currentUser {
            do {
                let result = try await user.link(with: credential)
                currentUser = result.user
            } catch let error as NSError where error.code == AuthErrorCode.credentialAlreadyInUse.rawValue {
                let existingCredential =
                    (error.userInfo[AuthErrorUserInfoUpdatedCredentialKey] as? AuthCredential) ?? credential
                let result = try await Auth.auth().signIn(with: existingCredential)
                currentUser = result.user
            }
        } else {
            let result = try await Auth.auth().signIn(with: credential)
            currentUser = result.user
        }
    }

    // MARK: - Email/şifre

    /// Email/şifre ile kayıt oluşturur. Mevcut anonim hesap bu kimliğe "link" edilir,
    /// böylece uid değişmez ve o ana kadarki ilerleme korunur.
    func signUp(email: String, password: String) async throws {
        let credential = EmailAuthProvider.credential(withEmail: email, password: password)
        if let user = Auth.auth().currentUser {
            let result = try await user.link(with: credential)
            currentUser = result.user
        } else {
            let result = try await Auth.auth().createUser(withEmail: email, password: password)
            currentUser = result.user
        }
    }

    /// Var olan bir email/şifre hesabına giriş yapar.
    func signIn(email: String, password: String) async throws {
        let result = try await Auth.auth().signIn(withEmail: email, password: password)
        currentUser = result.user
    }

    /// Şifre sıfırlama maili gönderir.
    func sendPasswordReset(email: String) async throws {
        try await Auth.auth().sendPasswordReset(withEmail: email)
    }

    /// Firebase Auth hatalarını kullanıcıya gösterilebilir Türkçe mesajlara çevirir.
    static func friendlyMessage(for error: Error) -> String {
        if let authError = error as? AuthError {
            return authError.errorDescription ?? "Bir hata oluştu."
        }
        let code = AuthErrorCode(rawValue: (error as NSError).code)
        switch code {
        case .invalidEmail:
            return "Geçersiz email adresi."
        case .emailAlreadyInUse, .credentialAlreadyInUse:
            return "Bu email zaten kayıtlı. Giriş yapmayı dene."
        case .weakPassword:
            return "Şifre en az 6 karakter olmalı."
        case .wrongPassword, .invalidCredential:
            return "Email veya şifre hatalı."
        case .userNotFound:
            return "Bu email ile bir hesap bulunamadı. Önce kayıt ol."
        case .networkError:
            return "İnternet bağlantını kontrol edip tekrar dene."
        case .tooManyRequests:
            return "Çok fazla deneme yapıldı. Biraz bekleyip tekrar dene."
        case .operationNotAllowed:
            return "Bu giriş yöntemi Firebase Console'da etkin değil (Sign-in method sekmesinden açılmalı)."
        default:
            return error.localizedDescription
        }
    }

    enum AuthError: LocalizedError {
        case invalidCredential
        case missingGoogleConfig

        var errorDescription: String? {
            switch self {
            case .invalidCredential:
                return "Kimlik bilgileri doğrulanamadı. Lütfen tekrar deneyin."
            case .missingGoogleConfig:
                return "Google giriş yapılandırması bulunamadı."
            }
        }
    }

    // MARK: - Nonce yardımcıları

    /// Kriptografik olarak güvenli rastgele bir nonce üretir.
    private func randomNonceString(length: Int = 32) -> String {
        precondition(length > 0)
        var randomBytes = [UInt8](repeating: 0, count: length)
        let status = SecRandomCopyBytes(kSecRandomDefault, randomBytes.count, &randomBytes)
        precondition(status == errSecSuccess, "Nonce üretilemedi: \(status)")
        let charset: [Character] = Array("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz-._")
        return String(randomBytes.map { charset[Int($0) % charset.count] })
    }

    /// Apple'a gönderilecek nonce'un SHA256 özeti (ham nonce Firebase'e gider).
    private func sha256(_ input: String) -> String {
        let hash = SHA256.hash(data: Data(input.utf8))
        return hash.map { String(format: "%02x", $0) }.joined()
    }
}
