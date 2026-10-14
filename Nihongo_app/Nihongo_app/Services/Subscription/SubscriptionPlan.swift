import Foundation

/// Kaimon Pro'nun satın alınabilir planları. Ürün kimlikleri App Store Connect'te
/// (ve yerel test için `Kaimon.storekit` dosyasında) birebir aynı olmalıdır.
enum SubscriptionPlan: String, CaseIterable, Identifiable {
    case monthly  = "com.batuhankeskn0.kaimon.pro.monthly"
    case yearly   = "com.batuhankeskn0.kaimon.pro.yearly"
    case lifetime = "com.batuhankeskn0.kaimon.pro.lifetime"

    var id: String { rawValue }

    /// Abonelik grubu kimliği (App Store Connect'teki grup ile eşleşmeli).
    static let groupID = "kaimon_pro"

    static var allProductIDs: [String] { allCases.map(\.rawValue) }

    var isAutoRenewing: Bool { self != .lifetime }

    /// Paywall'da ürünlerin sıralanması için.
    var sortOrder: Int {
        switch self {
        case .yearly: return 0
        case .monthly: return 1
        case .lifetime: return 2
        }
    }
}

/// Kullanıcının mevcut erişim durumu.
enum ProEntitlement: Equatable {
    case free
    case pro(plan: SubscriptionPlan, expiresAt: Date?)

    var isPro: Bool {
        if case .pro = self { return true }
        return false
    }
}
