import Foundation
import AVFoundation

/// Metin okuma (Text-to-Speech) işlemlerini yöneten servis.
/// Özellikle Japonca kelime ve cümlelerin doğru telaffuzu için kullanılır.
class TTSService: NSObject, ObservableObject, AVSpeechSynthesizerDelegate {
    static let shared = TTSService()
    
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
    func speak(text: String, languageCode: String = "ja-JP") {
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
        
        let utterance = AVSpeechUtterance(string: text)
        
        if let voice = AVSpeechSynthesisVoice(language: languageCode) {
            utterance.voice = voice
        }
        
        // Kullanıcıların daha net anlaması için konuşma hızını çok hafif düşürebiliriz
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.95
        
        synthesizer.speak(utterance)
    }
    
    /// Devam eden okumayı durdurur.
    func stop() {
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
    }
    
    // MARK: - AVSpeechSynthesizerDelegate
    
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didStart utterance: AVSpeechUtterance) {
        DispatchQueue.main.async {
            self.isSpeaking = true
        }
    }
    
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        DispatchQueue.main.async {
            self.isSpeaking = false
        }
    }
    
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didCancel utterance: AVSpeechUtterance) {
        DispatchQueue.main.async {
            self.isSpeaking = false
        }
    }
}
