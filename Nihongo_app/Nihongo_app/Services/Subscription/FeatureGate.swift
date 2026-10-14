import Foundation
import Observation

/// Pro'ya özel özellikler. UI katmanı yalnızca bu enum üzerinden kilit sorar.
enum ProFeature {
    case unlimitedAIChat
    case lockedContent      // Ücretsiz sınırın ötesindeki ünite/bölümler
    case unlimitedExams
    case adFree
}

/// Ücretsiz kullanıcı sınırları — tek yerden ayarlanır.
enum FreeTierLimits {
    static let dailyAIMessages = 3
    /// Kelime/Kanji/Dilbilgisi'nde ücretsiz açık bölüm sayısı.
    static let freeUnitsPerModule = 2
    static let dailyExams = 1
}

/// Günlük kota sayacı (cihaz yerel, gün değişince sıfırlanır).
struct DailyQuota {
    let key: String
    let limit: Int
    private let defaults = UserDefaults.standard

    private var dayKey: String { "quota.\(key).day" }
    private var countKey: String { "quota.\(key).count" }
    private var bonusKey: String { "quota.\(key).bonus" }

    private func rolloverIfNeeded() {
        let today = Calendar.current.startOfDay(for: Date()).timeIntervalSince1970
        if defaults.double(forKey: dayKey) != today {
            defaults.set(today, forKey: dayKey)
            defaults.set(0, forKey: countKey)
            defaults.set(0, forKey: bonusKey)
        }
    }

    var used: Int { rolloverIfNeeded(); return defaults.integer(forKey: countKey) }
    var bonus: Int { rolloverIfNeeded(); return defaults.integer(forKey: bonusKey) }
    var remaining: Int { max(0, limit + bonus - used) }

    func consume() {
        rolloverIfNeeded()
        defaults.set(used + 1, forKey: countKey)
    }

    /// Ödüllü reklam vb. ile ek hak ekler (reklam aşamasında kullanılacak).
    func grantBonus(_ amount: Int) {
        rolloverIfNeeded()
        defaults.set(bonus + amount, forKey: bonusKey)
    }
}

/// Özellik erişim kararlarını veren merkezi servis.
@Observable
@MainActor
final class FeatureGate {
    static let shared = FeatureGate()

    /// Kota değişimlerinde SwiftUI'nin yenilenmesi için tetikleyici.
    private(set) var revision = 0

    private let aiQuota = DailyQuota(key: "ai", limit: FreeTierLimits.dailyAIMessages)
    private let examQuota = DailyQuota(key: "exam", limit: FreeTierLimits.dailyExams)

    private var subscription: SubscriptionManager { .shared }

    func isUnlocked(_ feature: ProFeature) -> Bool {
        subscription.isPro
    }

    /// Bölüm indeksi (0'dan) ücretsiz mi?
    func isUnitAvailable(index: Int) -> Bool {
        subscription.isPro || index < FreeTierLimits.freeUnitsPerModule
    }

    // MARK: AI sohbet kotası

    var remainingAIMessages: Int? {
        _ = revision
        return subscription.isPro ? nil : aiQuota.remaining
    }

    var canSendAIMessage: Bool {
        subscription.isPro || aiQuota.remaining > 0
    }

    func recordAIMessage() {
        guard !subscription.isPro else { return }
        aiQuota.consume()
        revision += 1
    }

    func grantAIBonus(_ amount: Int) {
        aiQuota.grantBonus(amount)
        revision += 1
    }

    // MARK: Sınav kotası

    var canStartExam: Bool {
        subscription.isPro || examQuota.remaining > 0
    }

    func recordExam() {
        guard !subscription.isPro else { return }
        examQuota.consume()
        revision += 1
    }
}
