import UIKit
import AudioToolbox

/// Dokunsal (Haptic) ve işitsel (Sound) geri bildirimleri yöneten merkezi servis.
@MainActor
final class FeedbackManager {
    static let shared = FeedbackManager()
    
    private init() {}
    
    /// Doğru cevap verildiğinde çalınacak başarı geri bildirimi.
    /// İnce bir titreşim (.medium) ve tiz bir "tık/zil" sesi çalar.
    func playSuccess() {
        // Haptic
        let generator = UIImpactFeedbackGenerator(style: .medium)
        generator.prepare()
        generator.impactOccurred()
        
        // Sound: 1057 (Tink) or 1322 (Success)
        // 1322 is a nice positive tink sound in iOS.
        AudioServicesPlaySystemSound(1322)
    }
    
    /// Yanlış cevap verildiğinde çalınacak hata geri bildirimi.
    /// Daha ağır/tok bir titreşim (.error) ve boğuk bir hata sesi çalar.
    func playError() {
        // Haptic
        let generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(.error)
        
        // Sound: 1053 (Beep) or maybe just standard error vibration
        AudioServicesPlaySystemSound(1053)
    }
    
    /// Sadece butona basma hissiyatı vermek istendiğinde kullanılacak hafif geri bildirim.
    func playSelection() {
        let generator = UISelectionFeedbackGenerator()
        generator.selectionChanged()
    }
}
