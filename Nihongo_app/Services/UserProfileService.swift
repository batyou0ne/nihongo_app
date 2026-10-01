import Foundation
import FirebaseAuth
import FirebaseFirestore

/// Firestore'daki kullanıcı profili (users/{uid} dokümanı).
struct UserProfile: Codable {
    var firstName: String
    var lastName: String
    var age: Int?

    var fullName: String {
        "\(firstName) \(lastName)".trimmingCharacters(in: .whitespaces)
    }
}

/// Kayıt sırasında girilen kişisel bilgileri Firestore'a yazan ve hesap
/// ekranı için okuyan servis. Doküman uid'ye bağlı olduğu için kullanıcı
/// başka cihazdan giriş yaptığında da profili gelir.
@Observable
final class UserProfileService {
    static let shared = UserProfileService()

    private(set) var profile: UserProfile?
    private var loadedForUID: String?

    private init() {}

    /// Profili Firestore'a kaydeder (offline'da kuyruğa alınır, bağlantı gelince yazılır).
    func save(_ profile: UserProfile) throws {
        guard let uid = Auth.auth().currentUser?.uid else { return }
        try Firestore.firestore().collection("users").document(uid).setData(from: profile, merge: true)
        self.profile = profile
        loadedForUID = uid
    }

    /// O anki kullanıcının profilini (henüz yüklenmediyse) Firestore'dan çeker.
    func loadIfNeeded() async {
        guard let uid = Auth.auth().currentUser?.uid, loadedForUID != uid else { return }
        do {
            let snapshot = try await Firestore.firestore().collection("users").document(uid).getDocument()
            profile = try? snapshot.data(as: UserProfile.self)
            loadedForUID = uid
        } catch {
            print("Profil yüklenemedi: \(error.localizedDescription)")
        }
    }

    /// Çıkış yapıldığında önbelleği temizler.
    func clear() {
        profile = nil
        loadedForUID = nil
    }
}
