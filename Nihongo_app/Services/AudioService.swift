import AVFoundation

/// Karakter ve kelime telaffuzlarını çalar. MVP'de ses dosyası kaydetmek/indirmek yerine
/// iOS'un yerleşik Japonca TTS sesini kullanıyoruz — hem 3rd-party bağımlılığı sıfırda
/// tutar hem de 46+46+100 ayrı ses dosyası üretme/indirme maliyetinden kurtarır.
/// İleride gerçek seslendirme eklenecekse bu servisin arayüzü değişmeden AVAudioPlayer'a geçilebilir.
@MainActor
final class AudioService: NSObject {
    static let shared = AudioService()

    private let synthesizer = AVSpeechSynthesizer()

    private override init() {
        super.init()
    }

    /// `text` Japonca yazılmalı (örn. "あ" ya da "みず") — romaji değil, doğru telaffuz için.
    func speak(_ text: String, rate: Float = 0.42) {
        synthesizer.stopSpeaking(at: .immediate)

        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: "ja-JP")
        utterance.rate = rate
        synthesizer.speak(utterance)
    }

    func stop() {
        synthesizer.stopSpeaking(at: .immediate)
    }
}
