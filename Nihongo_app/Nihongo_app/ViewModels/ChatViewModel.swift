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
        
        isTyping = true
        
        Task {
            do {
                let aiResponse = try await AIService.shared.sendMessage(text)
                await MainActor.run {
                    self.isTyping = false
                    self.messages.append(ChatMessage(text: aiResponse, isUser: false))
                }
            } catch {
                await MainActor.run {
                    self.isTyping = false
                    self.messages.append(ChatMessage(text: "Hata oluştu: \(error.localizedDescription)", isUser: false))
                }
            }
        }
    }
}
