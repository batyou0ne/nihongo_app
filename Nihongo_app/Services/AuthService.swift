import Foundation
import Security
import CryptoKit
import AuthenticationServices
import FirebaseAuth

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

            if let user = Auth.auth().currentUser {
                do {
                    // Anonim hesabı Apple kimliğine bağla → uid korunur.
                    let result = try await user.link(with: credential)
                    currentUser = result.user
                } catch let error as NSError where error.code == AuthErrorCode.credentialAlreadyInUse.rawValue {
                    // Bu Apple kimliği zaten başka bir hesaba ait: o hesaba giriş yap.
                    let existingCredential =
                        (error.userInfo[AuthErrorUserInfoUpdatedCredentialKey] as? AuthCredential) ?? credential
                    let result = try await Auth.auth().signIn(with: existingCredential)
                    currentUser = result.user
                }
            } else {
                let result = try await Auth.auth().signIn(with: credential)
                currentUser = result.user
            }
            return true
        }
    }

    enum AuthError: LocalizedError {
        case invalidCredential

        var errorDescription: String? {
            "Apple kimlik bilgileri doğrulanamadı. Lütfen tekrar deneyin."
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
