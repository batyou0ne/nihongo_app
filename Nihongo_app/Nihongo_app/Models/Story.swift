import Foundation

enum StoryType: String, Codable, CaseIterable {
    case hiragana = "hiragana"
    case hiraganaKatakana = "hiraganaKatakana"
    case hiraganaKanji = "hiraganaKanji"
    case all = "all"
    
    var displayName: String {
        switch self {
        case .hiragana: return "Sadece Hiragana"
        case .hiraganaKatakana: return "Hiragana + Katakana"
        case .hiraganaKanji: return "Hiragana + Kanji"
        case .all: return "Hiragana + Katakana + Kanji"
        }
    }
}

struct StorySentence: Codable, Identifiable, Hashable {
    let id: String
    let japaneseText: String
    let romaji: String
    let turkishTranslation: String
    let vocabulary: [String] // Format: "あさ (Sabah)"
}

/// N5 okuma pratiği için hikaye veya metinleri temsil eder.
struct Story: Codable, Identifiable, Hashable {
    let id: String
    let type: StoryType
    let title: String
    let sentences: [StorySentence]
}
