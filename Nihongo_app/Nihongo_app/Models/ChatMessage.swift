import Foundation

/// Chat ekranındaki tek bir mesajı temsil eder.
struct ChatMessage: Identifiable, Equatable {
    let id: UUID
    let text: String
    let isUser: Bool
    let isError: Bool
    let timestamp: Date
    
    init(id: UUID = UUID(), text: String, isUser: Bool, isError: Bool = false, timestamp: Date = Date()) {
        self.id = id
        self.text = text
        self.isUser = isUser
        self.isError = isError
        self.timestamp = timestamp
    }
}
