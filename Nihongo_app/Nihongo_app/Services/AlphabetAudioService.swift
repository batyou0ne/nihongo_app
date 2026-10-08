import Foundation
import UIKit
import Observation

/// Hiragana ve Katakana alfabe tablosundaki karakterlerin sesli telaffuzunu yöneten servis.
/// Dokunulduğunda hafif dokunsal geri bildirim (haptic) üretir ve `AudioService` üzerinden
/// stüdyo kaydı veya yüksek kaliteli Japonca TTS ile karakterin sesini çalar.
@Observable
final class AlphabetAudioService {
    static let shared = AlphabetAudioService()
    
    /// O an çalınmakta olan karakterin kimliği (görsel vurgulama ve animasyon için)
    private(set) var currentCharacter: String? = nil
    
    private var resetTask: Task<Void, Never>?
    
    private init() {}
    
    /// Verilen karakterin sesini çalar ve görsel geri bildirim için durumu günceller.
    @MainActor
    func play(character: String) {
        // Dokunsal geri bildirim (Haptic)
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        
        // Ses çal (önce stüdyo mp3 kaydı, yoksa yerel Japonca TTS)
        AudioService.shared.speak(character)
        
        // Görsel vurgu için durumu ayarla
        currentCharacter = character
        
        resetTask?.cancel()
        resetTask = Task {
            try? await Task.sleep(for: .milliseconds(700))
            guard !Task.isCancelled else { return }
            currentCharacter = nil
        }
    }
    
    /// Ses çalmayı durdurur
    @MainActor
    func stop() {
        AudioService.shared.stop()
        currentCharacter = nil
        resetTask?.cancel()
    }
}
