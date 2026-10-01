import Foundation

/// Uygulamanın arayüz dilini yöneten servis. Kullanıcı profil ekranından
/// Türkçe veya İngilizce seçebilir; tercih UserDefaults'ta saklanır.
@Observable
final class LanguageManager {
    static let shared = LanguageManager()

    enum AppLanguage: String, CaseIterable, Identifiable {
        case turkish = "tr"
        case english = "en"

        var id: String { rawValue }

        var displayName: String {
            switch self {
            case .turkish: return "Türkçe"
            case .english: return "English"
            }
        }

        var flag: String {
            switch self {
            case .turkish: return "🇹🇷"
            case .english: return "🇬🇧"
            }
        }
    }

    private static let storageKey = "app_language"

    var current: AppLanguage {
        didSet {
            UserDefaults.standard.set(current.rawValue, forKey: Self.storageKey)
        }
    }

    private init() {
        if let raw = UserDefaults.standard.string(forKey: Self.storageKey),
           let lang = AppLanguage(rawValue: raw) {
            current = lang
        } else {
            current = .turkish
        }
    }
}
