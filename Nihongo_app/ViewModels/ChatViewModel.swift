import Foundation
import Observation

@Observable
final class ChatViewModel {
    var messages: [ChatMessage] = []
    var isTyping = false
    
    init() {
        // İlk mesajı (karşılama) otomatik ekleyelim
        messages.append(ChatMessage(text: "こんにちは！今日はどんな練習をしましょうか？ (Merhaba! Bugün ne tür bir pratik yapalım?)", isUser: false))
    }
    
    func sendMessage(_ text: String) {
        guard !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return }
        
        let userMsg = ChatMessage(text: text, isUser: true)
        messages.append(userMsg)
        
        // AI yanıtını taklit etme (Mock)
        isTyping = true
        
        Task {
            try? await Task.sleep(for: .seconds(1.5))
            await MainActor.run {
                self.isTyping = false
                let aiResponse = self.mockResponse(for: text)
                self.messages.append(ChatMessage(text: aiResponse, isUser: false))
            }
        }
    }
    
    private func mockResponse(for userText: String) -> String {
        let lowerText = userText.lowercased()
        if lowerText.contains("kahve") || lowerText.contains("coffee") || lowerText.contains("コーヒー") {
            return "かしこまりました。コーヒーですね。ホットですか、アイスですか？ (Anlaşıldı. Kahve değil? Sıcak mı olsun, soğuk mu?)"
        } else if lowerText.contains("sıcak") || lowerText.contains("hot") || lowerText.contains("あつい") {
            return "ホットコーヒーですね。サイズはどうなさいますか？ (Sıcak kahve. Boyutu nasıl olsun?)"
        } else if lowerText.contains("teşekkür") || lowerText.contains("arigatou") || lowerText.contains("ありがとう") {
            return "どういたしまして！またお待ちしております。(Rica ederim! Yine bekleriz.)"
        }
        return "なるほど！それは面白いですね。続けてください。(Anlıyorum! Bu çok ilginç. Devam et.)"
    }
}
