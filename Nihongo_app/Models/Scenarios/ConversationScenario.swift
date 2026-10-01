import Foundation

struct ConversationScenario: Identifiable, Codable {
    let id: String
    let title: String
    let description: String
    let level: String
    let steps: [ScenarioStep]
}

struct ScenarioStep: Identifiable, Codable {
    let id: String
    let botMessage: ChatMessageData
    let options: [ScenarioOption]
}

struct ChatMessageData: Codable {
    let japanese: String
    let romaji: String
    let turkish: String
}

struct ScenarioOption: Identifiable, Codable {
    let id: String
    let japanese: String
    let romaji: String
    let turkish: String
    let isCorrect: Bool
}
