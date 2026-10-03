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
    }
    
    /// Yanlış cevap verildiğinde çalınacak hata geri bildirimi.
    /// Daha ağır/tok bir titreşim (.error) ve boğuk bir hata sesi çalar.
    func playError() {
        // Haptic
        let generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(.error)
    }
    
    /// Sadece butona basma hissiyatı vermek istendiğinde kullanılacak hafif geri bildirim.
    func playSelection() {
        let generator = UISelectionFeedbackGenerator()
        generator.selectionChanged()
    }
}
