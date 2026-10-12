import Foundation
import Observation

@Observable
final class ChatViewModel {
    var messages: [ChatMessage] = []
    var isTyping = false
    var lastFailedText: String? = nil
    
    init() {
        // İlk mesajı (karşılama) otomatik ekleyelim
        messages.append(ChatMessage(text: "こんにちは！今日はどんな練習をしましょうか？ (Merhaba! Bugün ne tür bir pratik yapalım?)", isUser: false))
    }
    
    func sendMessage(_ text: String) {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        
        let userMsg = ChatMessage(text: trimmed, isUser: true)
        messages.append(userMsg)
        
        executeAIMessage(userText: trimmed)
    }
    
    func retryLastMessage() {
        guard let text = lastFailedText else { return }
        if let last = messages.last, last.isError {
            messages.removeLast()
        }
        executeAIMessage(userText: text)
    }
    
    private func executeAIMessage(userText: String) {
        lastFailedText = nil
        isTyping = true
        
        Task {
            do {
                let aiResponse = try await AIService.shared.sendMessage(userText)
                await MainActor.run {
                    self.isTyping = false
                    self.messages.append(ChatMessage(text: aiResponse, isUser: false))
                }
            } catch {
                await MainActor.run {
                    self.isTyping = false
                    self.lastFailedText = userText
                    self.messages.append(ChatMessage(text: error.localizedDescription, isUser: false, isError: true))
                }
            }
        }
    }
    
    func clearMessages() {
        AIService.shared.resetConversation()
        messages = [
            ChatMessage(text: "こんにちは！今日はどんな練習をしましょうか？ (Merhaba! Bugün ne tür bir pratik yapalım?)", isUser: false)
        ]
        lastFailedText = nil
    }
}
