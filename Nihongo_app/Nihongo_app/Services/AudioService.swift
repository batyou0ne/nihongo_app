import Foundation
import AVFoundation
import SwiftUI
import Combine

/// Metin okuma (Text-to-Speech) işlemlerini yöneten servis.
/// Özellikle Japonca kelime ve cümlelerin doğru telaffuzu için kullanılır.
/// AVAudioSession yapılandırması sayesinde cihaz sessizde olsa bile sesi çalar.
@MainActor
class AudioService: NSObject, ObservableObject, AVSpeechSynthesizerDelegate {
    static let shared = AudioService()
    
    private let synthesizer = AVSpeechSynthesizer()
    @Published var isSpeaking: Bool = false
    
    override private init() {
        super.init()
        synthesizer.delegate = self
        
        // Sessizdeyken bile sesin çıkmasını sağlamak için ses oturumunu (AudioSession) yapılandırıyoruz
        do {
            try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: .duckOthers)
            try AVAudioSession.sharedInstance().setActive(true)
        } catch {
            print("Audio session setup failed: \(error.localizedDescription)")
        }
    }
    
    /// Verilen metni okur.
    /// - Parameters:
    ///   - text: Okunacak metin.
    ///   - languageCode: Dil kodu. Varsayılan olarak Japonca ("ja-JP").
    ///   - rate: Konuşma hızı.
    func speak(_ text: String, languageCode: String = "ja-JP", rate: Float = 0.42) {
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
        
        let utterance = AVSpeechUtterance(string: text)
        
        if let voice = AVSpeechSynthesisVoice(language: languageCode) {
            utterance.voice = voice
        }
        
        utterance.rate = rate
        
        synthesizer.speak(utterance)
    }
    
    /// Devam eden okumayı durdurur.
    func stop() {
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
    }
    
    // MARK: - AVSpeechSynthesizerDelegate
    
    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didStart utterance: AVSpeechUtterance) {
        Task { @MainActor in
            self.isSpeaking = true
        }
    }
    
    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        Task { @MainActor in
            self.isSpeaking = false
        }
    }
    
    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
        Task { @MainActor in
            self.isSpeaking = false
        }
    }
}
