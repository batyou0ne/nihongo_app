import Foundation

/// Statik içerik JSON'larını (hiragana/katakana/kanji/kelime) yükleyen ortak yardımcı.
/// ReviewListView gibi birden fazla modülün verisine aynı anda ihtiyaç duyan ekranlar
/// için tek noktadan erişim sağlar.
enum ContentStore {
    static func loadCharacters(_ type: CharacterType) -> [JapaneseCharacter] {
        let fileName = type == .hiragana ? "HiraganaData" : "KatakanaData"
        guard var loaded: [JapaneseCharacter] = load(fileName) else { return [] }
        for index in loaded.indices { loaded[index].type = type }
        return loaded
    }

    static func loadKanji(level: String = "N5") -> [Kanji] {
        load("\(level)KanjiData") ?? []
    }

    static func loadVocabulary(level: String = "N5") -> [VocabularyWord] {
        load("\(level)VocabularyData") ?? []
    }

    private static func load<T: Decodable>(_ fileName: String) -> T? {
        guard let url = Bundle.main.url(forResource: fileName, withExtension: "json"),
              let data = try? Data(contentsOf: url) else {
            return nil
        }
        return try? JSONDecoder().decode(T.self, from: data)
    }
}
