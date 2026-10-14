import Foundation

/// Reklam birim kimlikleri.
/// DEBUG'da Google'ın herkese açık TEST kimlikleri kullanılır (gerçek reklam gösterilmez,
/// hesap gerekmez). Yayından önce AdMob hesabı açıp RELEASE kimliklerini doldurun ve
/// `Nihongo-app-Info.plist` içindeki `GADApplicationIdentifier` değerini gerçek uygulama
/// kimliğiyle değiştirin. RELEASE kimlikleri boşken hiçbir reklam yüklenmez.
enum AdConfig {
    #if DEBUG
    static let rewardedUnitID = "ca-app-pub-3940256099942544/1712485313"
    static let interstitialUnitID = "ca-app-pub-3940256099942544/4411468910"
    #else
    static let rewardedUnitID = ""
    static let interstitialUnitID = ""
    #endif

    /// Ödüllü reklam başına kazanılan AI mesaj hakkı.
    static let rewardedAIMessages = 2
    /// Geçiş reklamı: her N tamamlanan dersten sonra...
    static let lessonsPerInterstitial = 3
    /// ...ve en az bu kadar saniye geçtiyse göster.
    static let minInterstitialInterval: TimeInterval = 180
    /// Uygulamayı ilk kurduktan sonra ilk geçiş reklamına kadar bu kadar ders ücretsiz.
    static let graceLessons = 3
}
