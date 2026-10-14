import Foundation
import UIKit
import Observation
import AppTrackingTransparency
import GoogleMobileAds
import UserMessagingPlatform

/// AdMob yöneticisi: UMP (GDPR/KVKK) onayı → Apple ATT izni → SDK başlatma →
/// ödüllü ve geçiş reklamlarını önceden yükleme / gösterme.
/// Pro kullanıcıya hiçbir reklam yüklenmez ya da gösterilmez.
@Observable
@MainActor
final class AdManager: NSObject {
    static let shared = AdManager()

    private(set) var isRewardedReady = false
    private(set) var isShowingAd = false

    @ObservationIgnored private var hasStartedSDK = false
    @ObservationIgnored private var rewardedAd: RewardedAd?
    @ObservationIgnored private var interstitialAd: InterstitialAd?
    @ObservationIgnored private var pendingReward: (() -> Void)?
    @ObservationIgnored private var didEarnReward = false

    private let defaults = UserDefaults.standard
    private let lessonCountKey = "ads.lessonsCompleted"
    private let lastInterstitialKey = "ads.lastInterstitialAt"

    private var isPro: Bool { SubscriptionManager.shared.isPro }

    // MARK: - Başlatma

    /// Uygulama açılışında bir kez çağrılır (aktif sahnede).
    func start() async {
        guard !hasStartedSDK, !isPro else { return }

        await requestConsentIfNeeded()
        await requestTrackingIfNeeded()

        guard ConsentInformation.shared.canRequestAds else { return }
        hasStartedSDK = true
        await MobileAds.shared.start()
        loadRewarded()
        loadInterstitial()
    }

    private func requestConsentIfNeeded() async {
        do {
            try await ConsentInformation.shared.requestConsentInfoUpdate(with: RequestParameters())
            if let root = Self.topViewController() {
                try await ConsentForm.loadAndPresentIfRequired(from: root)
            }
        } catch {
            // Onay akışı başarısız olsa bile daha önce alınmış onayla devam edilebilir.
            print("UMP hatası: \(error.localizedDescription)")
        }
    }

    private func requestTrackingIfNeeded() async {
        guard ATTrackingManager.trackingAuthorizationStatus == .notDetermined else { return }
        _ = await ATTrackingManager.requestTrackingAuthorization()
    }

    // MARK: - Ödüllü reklam

    private func loadRewarded() {
        guard !isPro, !AdConfig.rewardedUnitID.isEmpty, rewardedAd == nil else { return }
        Task {
            do {
                let ad = try await RewardedAd.load(with: AdConfig.rewardedUnitID, request: Request())
                ad.fullScreenContentDelegate = self
                rewardedAd = ad
                isRewardedReady = true
            } catch {
                isRewardedReady = false
                print("Ödüllü reklam yüklenemedi: \(error.localizedDescription)")
            }
        }
    }

    /// Reklamı gösterir; kullanıcı ödülü kazanırsa `onReward` çağrılır.
    /// Reklam hazır değilse false döner.
    @discardableResult
    func showRewarded(onReward: @escaping () -> Void) -> Bool {
        guard !isPro, let ad = rewardedAd, let root = Self.topViewController() else {
            loadRewarded()
            return false
        }
        pendingReward = onReward
        didEarnReward = false
        isShowingAd = true
        ad.present(from: root) { [weak self] in
            self?.didEarnReward = true
        }
        return true
    }

    // MARK: - Geçiş reklamı

    private func loadInterstitial() {
        guard !isPro, !AdConfig.interstitialUnitID.isEmpty, interstitialAd == nil else { return }
        Task {
            do {
                let ad = try await InterstitialAd.load(with: AdConfig.interstitialUnitID, request: Request())
                ad.fullScreenContentDelegate = self
                interstitialAd = ad
            } catch {
                print("Geçiş reklamı yüklenemedi: \(error.localizedDescription)")
            }
        }
    }

    /// Ders tamamlanma ekranında "Devam" sonrası çağrılır. Sıklık sınırı uygular.
    func lessonCompleted() {
        guard !isPro else { return }
        let count = defaults.integer(forKey: lessonCountKey) + 1
        defaults.set(count, forKey: lessonCountKey)

        guard count > AdConfig.graceLessons,
              (count - AdConfig.graceLessons) % AdConfig.lessonsPerInterstitial == 0 else { return }

        let last = defaults.double(forKey: lastInterstitialKey)
        guard Date().timeIntervalSince1970 - last >= AdConfig.minInterstitialInterval else { return }

        guard !isShowingAd, let ad = interstitialAd, let root = Self.topViewController() else {
            loadInterstitial()
            return
        }
        defaults.set(Date().timeIntervalSince1970, forKey: lastInterstitialKey)
        isShowingAd = true
        ad.present(from: root)
    }

    // MARK: - Yardımcı

    static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        var top = scene?.windows.first(where: \.isKeyWindow)?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}

// MARK: - FullScreenContentDelegate

extension AdManager: FullScreenContentDelegate {
    nonisolated func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
        Task { @MainActor in
            isShowingAd = false
            if ad is RewardedAd {
                rewardedAd = nil
                isRewardedReady = false
                if didEarnReward { pendingReward?() }
                pendingReward = nil
                didEarnReward = false
                loadRewarded()
            } else if ad is InterstitialAd {
                interstitialAd = nil
                loadInterstitial()
            }
        }
    }

    nonisolated func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
        Task { @MainActor in
            isShowingAd = false
            pendingReward = nil
            if ad is RewardedAd {
                rewardedAd = nil
                isRewardedReady = false
                loadRewarded()
            } else if ad is InterstitialAd {
                interstitialAd = nil
                loadInterstitial()
            }
        }
    }
}
