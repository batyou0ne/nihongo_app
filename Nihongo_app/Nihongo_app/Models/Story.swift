import Foundation

enum StoryType: String, Codable, CaseIterable {
    case hiragana = "hiragana"
    case hiraganaKatakana = "hiraganaKatakana"
    case hiraganaKanji = "hiraganaKanji"
    case all = "all"
    
    var displayName: String {
        L10n.storyTypeName(rawValue)
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
