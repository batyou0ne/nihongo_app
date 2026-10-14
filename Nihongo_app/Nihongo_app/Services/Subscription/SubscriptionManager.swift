import Foundation
import StoreKit
import Observation

enum PurchaseOutcome {
    case success
    case pending
    case cancelled
}

/// StoreKit 2 tabanlı abonelik yöneticisi. Tek doğruluk kaynağı: `isPro`.
/// - Ürünleri yükler, satın alma / geri yükleme işlemlerini yürütür
/// - `Transaction.updates` ile yenileme, iptal ve iadeleri canlı dinler
/// - Mevcut hakları (`currentEntitlements`) App Store imzalarıyla doğrular
@Observable
@MainActor
final class SubscriptionManager {
    static let shared = SubscriptionManager()

    private(set) var products: [Product] = []
    private(set) var entitlement: ProEntitlement = .free
    private(set) var isLoadingProducts = false
    private(set) var isPurchasing = false
    private(set) var lastError: String?

    #if DEBUG
    /// Geliştirme sırasında Pro'yu StoreKit olmadan açıp kapatmak için.
    var debugForcePro: Bool = UserDefaults.standard.bool(forKey: "debugForcePro") {
        didSet { UserDefaults.standard.set(debugForcePro, forKey: "debugForcePro") }
    }
    #endif

    var isPro: Bool {
        #if DEBUG
        if debugForcePro { return true }
        #endif
        return entitlement.isPro
    }

    @ObservationIgnored private var updatesTask: Task<Void, Never>?

    private init() {
        updatesTask = listenForTransactions()
        Task { await refresh() }
    }

    deinit { updatesTask?.cancel() }

    // MARK: - Ürünler

    func loadProducts() async {
        guard products.isEmpty else { return }
        isLoadingProducts = true
        defer { isLoadingProducts = false }
        do {
            let fetched = try await Product.products(for: SubscriptionPlan.allProductIDs)
            products = fetched.sorted {
                (SubscriptionPlan(rawValue: $0.id)?.sortOrder ?? 99) < (SubscriptionPlan(rawValue: $1.id)?.sortOrder ?? 99)
            }
            if products.isEmpty {
                lastError = "Ürünler şu an yüklenemedi."
            }
        } catch {
            lastError = "Ürünler yüklenemedi: \(error.localizedDescription)"
        }
    }

    func product(for plan: SubscriptionPlan) -> Product? {
        products.first { $0.id == plan.rawValue }
    }

    // MARK: - Satın alma

    @discardableResult
    func purchase(_ product: Product) async -> PurchaseOutcome {
        isPurchasing = true
        lastError = nil
        defer { isPurchasing = false }
        do {
            let result = try await product.purchase()
            switch result {
            case .success(let verification):
                let transaction = try checkVerified(verification)
                await transaction.finish()
                await refresh()
                return .success
            case .pending:
                return .pending
            case .userCancelled:
                return .cancelled
            @unknown default:
                return .cancelled
            }
        } catch {
            lastError = "Satın alma başarısız: \(error.localizedDescription)"
            return .cancelled
        }
    }

    /// "Satın Alımları Geri Yükle" (App Store zorunluluğu).
    func restorePurchases() async {
        lastError = nil
        do {
            try await AppStore.sync()
        } catch {
            lastError = "Geri yükleme başarısız: \(error.localizedDescription)"
        }
        await refresh()
    }

    // MARK: - Hak durumu

    /// Mevcut geçerli işlemlerden entitlement'ı yeniden hesaplar.
    func refresh() async {
        var best: ProEntitlement = .free

        for await result in Transaction.currentEntitlements {
            guard let transaction = try? checkVerified(result),
                  let plan = SubscriptionPlan(rawValue: transaction.productID),
                  transaction.revocationDate == nil else { continue }

            if let expiry = transaction.expirationDate, expiry < Date() { continue }

            // Lifetime her zaman en güçlü haktır.
            if plan == .lifetime {
                best = .pro(plan: plan, expiresAt: nil)
                break
            }
            best = .pro(plan: plan, expiresAt: transaction.expirationDate)
        }
        entitlement = best
    }

    // MARK: - Dahili

    private func listenForTransactions() -> Task<Void, Never> {
        Task { [weak self] in
            for await result in Transaction.updates {
                guard let self else { return }
                if let transaction = try? self.checkVerified(result) {
                    await transaction.finish()
                }
                await self.refresh()
            }
        }
    }

    private nonisolated func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .unverified: throw StoreKitError.unknown
        case .verified(let safe): return safe
        }
    }
}
