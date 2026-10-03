import Foundation
import AVFoundation
import SwiftUI
import Combine

/// Metin okuma (Text-to-Speech) işlemlerini yöneten servis.
/// Özellikle Japonca kelime ve cümlelerin doğru telaffuzu için kullanılır.
///
/// **Ses Öncelik Sırası:**
/// 1. Önceden üretilmiş ses dosyası (Google Cloud TTS Neural2 — yüksek kalite)
/// 2. Apple TTS fallback (cihaz üzerinde sentez)
///
/// AVAudioSession yapılandırması sayesinde cihaz sessizde olsa bile sesi çalar.
@MainActor
class AudioService: NSObject, ObservableObject, AVSpeechSynthesizerDelegate {
    static let shared = AudioService()
    
    private let synthesizer = AVSpeechSynthesizer()
    private var audioPlayer: AVAudioPlayer?
    private var effectPlayer: AVAudioPlayer?
    @Published var isSpeaking: Bool = false
    
    /// Japonca metin → ses dosyası adı eşlemesi (audio_manifest.json'dan yüklenir)
    private var audioManifest: [String: String] = [:]
    
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
        
        // Ses manifest dosyasını yükle
        loadAudioManifest()
    }
    
    // MARK: - Manifest Yükleme
    
    /// `audio_manifest.json` dosyasından metin→dosya adı eşlemesini yükler.
    private func loadAudioManifest() {
        guard let url = Bundle.main.url(forResource: "audio_manifest", withExtension: "json", subdirectory: "Audio") else {
            // Alt dizinde değilse düz Resources'ta ara
            guard let flatUrl = Bundle.main.url(forResource: "audio_manifest", withExtension: "json") else {
                print("⚠️ audio_manifest.json bulunamadı — Apple TTS kullanılacak")
                return
            }
            loadManifest(from: flatUrl)
            return
        }
        loadManifest(from: url)
    }
    
    private func loadManifest(from url: URL) {
        do {
            let data = try Data(contentsOf: url)
            audioManifest = try JSONDecoder().decode([String: String].self, from: data)
            print("✅ Ses manifest yüklendi: \(audioManifest.count) giriş")
        } catch {
            print("❌ Ses manifest yüklenemedi: \(error.localizedDescription)")
        }
    }
    
    // MARK: - Ses Çalma
    
    /// Verilen metni okur. Önce önceden üretilmiş ses dosyasını arar,
    /// bulamazsa Apple TTS'e düşer.
    /// - Parameters:
    ///   - text: Okunacak metin.
    ///   - languageCode: Dil kodu. Varsayılan olarak Japonca ("ja-JP").
    ///   - rate: Konuşma hızı (sadece Apple TTS fallback için geçerli).
    func speak(_ text: String, languageCode: String = "ja-JP", rate: Float = 0.42) {
        // Devam eden okumayı/çalmayı durdur
        stopAll()
        
        // 1) Önceden üretilmiş ses dosyasını dene
        if playPrerecordedAudio(for: text) {
            return
        }
        
        // 2) Fallback: Apple TTS
        speakWithTTS(text, languageCode: languageCode, rate: rate)
    }
    
    /// Önceden üretilmiş ses dosyasını çalar.
    /// - Returns: Ses dosyası bulunup çalındıysa `true`.
    private func playPrerecordedAudio(for text: String) -> Bool {
        // Manifest'ten dosya adını al
        guard let filename = audioManifest[text] else { return false }
        
        // Dosya adından uzantıyı ayır
        let name = (filename as NSString).deletingPathExtension
        let ext = (filename as NSString).pathExtension
        
        // Bundle'da Audio/ altında ara
        guard let url = Bundle.main.url(forResource: name, withExtension: ext, subdirectory: "Audio")
            ?? Bundle.main.url(forResource: name, withExtension: ext) else {
            return false
        }
        
        do {
            audioPlayer = try AVAudioPlayer(contentsOf: url)
            audioPlayer?.delegate = self
            audioPlayer?.play()
            isSpeaking = true
            return true
        } catch {
            print("Ses dosyası çalınamadı: \(error.localizedDescription)")
            return false
        }
    }
    
    /// Apple TTS ile metni okur (fallback).
    private func speakWithTTS(_ text: String, languageCode: String, rate: Float) {
        let utterance = AVSpeechUtterance(string: text)
        
        if let voice = AVSpeechSynthesisVoice(language: languageCode) {
            utterance.voice = voice
        }
        
        utterance.rate = rate
        
        synthesizer.speak(utterance)
    }
    
    /// Devam eden tüm okumaları/çalmaları durdurur.
    func stop() {
        stopAll()
    }
    
    private func stopAll() {
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
        if let player = audioPlayer, player.isPlaying {
            player.stop()
            isSpeaking = false
        }
    }
    
    // MARK: - Sound Effects
    
    /// Doğru cevap sesini çalar.
    func playCorrectSound() {
        playSoundEffect(name: "correct-audio", ext: "wav")
    }
    
    /// Yanlış cevap sesini çalar.
    func playWrongSound() {
        playSoundEffect(name: "wrong-audio", ext: "wav")
    }
    
    private func playSoundEffect(name: String, ext: String) {
        guard let url = Bundle.main.url(forResource: name, withExtension: ext, subdirectory: "Audio")
            ?? Bundle.main.url(forResource: name, withExtension: ext) else {
            print("⚠️ Ses efekti bulunamadı: \(name).\(ext)")
            return
        }
        
        do {
            effectPlayer = try AVAudioPlayer(contentsOf: url)
            effectPlayer?.volume = 0.6 // Ses seviyesini ayarlayabilirsiniz
            effectPlayer?.play()
        } catch {
            print("Ses efekti çalınamadı: \(error.localizedDescription)")
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

// MARK: - AVAudioPlayerDelegate

extension AudioService: AVAudioPlayerDelegate {
    nonisolated func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        Task { @MainActor in
            self.isSpeaking = false
        }
    }
    
    nonisolated func audioPlayerDecodeErrorDidOccur(_ player: AVAudioPlayer, error: Error?) {
        Task { @MainActor in
            self.isSpeaking = false
            if let error = error {
                print("Ses decode hatası: \(error.localizedDescription)")
            }
        }
    }
}
