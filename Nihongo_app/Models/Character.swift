import Foundation

/// Bir öğrenme modülünün türü. HomeView'daki kart renklerini ve
/// LearningViewModel'in hangi JSON dosyasını yükleyeceğini belirler.
enum CharacterType: String, Codable, CaseIterable {
    case hiragana
    case katakana

    var displayName: String {
        switch self {
        case .hiragana: return "Hiragana"
        case .katakana: return "Katakana"
        }
    }
}

/// Bir karakterin (hece) geçtiği örnek bir kelime. Kart çevrildiğinde gösterilir.
struct ExampleWord: Codable, Hashable {
    let hiragana: String
    let romaji: String
    let turkishMeaning: String
}

/// HiraganaData.json / KatakanaData.json içindeki tek bir karakter kaydı.
/// Statik içerik olduğu için SwiftData modeli değil, sade bir Codable struct'tır —
/// öğrenme ilerlemesi ayrı olarak `LearningItemProgress` (bkz. UserProgress.swift) ile takip edilir.
struct JapaneseCharacter: Codable, Identifiable, Hashable {
    var id: String { character }

    let character: String
    let romaji: String
    let turkishPronunciation: String
    let row: String
    let strokeCount: Int

    /// JSON'da tutulmadığı için yükleme sırasında dosya kaynağına göre atanır.
    var type: CharacterType = .hiragana

    /// Şu an sadece HiraganaData.json'da dolu; KatakanaData.json'da anahtar yoksa boş dizi kalır.
    var exampleWords: [ExampleWord] = []

    /// `type` kasıtlı olarak dışarıda bırakıldı: JSON dosyalarında bu alan yok,
    /// yükleme sonrası LearningViewModel tarafından atanıyor.
    private enum CodingKeys: String, CodingKey {
        case character, romaji, turkishPronunciation, row, strokeCount, exampleWords
    }

    init(character: String, romaji: String, turkishPronunciation: String, row: String, strokeCount: Int, type: CharacterType = .hiragana, exampleWords: [ExampleWord] = []) {
        self.character = character
        self.romaji = romaji
        self.turkishPronunciation = turkishPronunciation
        self.row = row
        self.strokeCount = strokeCount
        self.type = type
        self.exampleWords = exampleWords
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        character = try container.decode(String.self, forKey: .character)
        romaji = try container.decode(String.self, forKey: .romaji)
        turkishPronunciation = try container.decode(String.self, forKey: .turkishPronunciation)
        row = try container.decode(String.self, forKey: .row)
        strokeCount = try container.decode(Int.self, forKey: .strokeCount)
        exampleWords = try container.decodeIfPresent([ExampleWord].self, forKey: .exampleWords) ?? []
    }
}
