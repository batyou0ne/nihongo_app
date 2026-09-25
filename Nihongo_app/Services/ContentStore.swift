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

    static func loadGrammar(level: String = "N5") -> [GrammarPoint] {
        load("\(level)GrammarData") ?? []
    }

    static func loadGrammarSyllabus(level: String = "N5") -> [GrammarUnit] {
        load("\(level)GrammarSyllabus") ?? []
    }

    static func loadStories(level: String = "N5") -> [Story] {
        load("\(level)StoriesData") ?? []
    }

    // MARK: - Bölümler (part)

    /// Kanji seviyesini eşit 4 parçaya böler. Hem KanjiPartSelectionView hem de ana
    /// ekrandaki "Kaldığın yer" kartı bunu kullanır — bölümleme mantığı tek yerde
    /// kalsın ki iki taraf farklı kartlara işaret etmesin.
    static func kanjiParts(level: String = "N5", partCount: Int = 4) -> [[Kanji]] {
        chunks(of: loadKanji(level: level), partCount: partCount)
    }

    /// Kelime seviyesini 25'erlik parçalara böler (aynı gerekçe: bkz. kanjiParts).
    static func vocabularyParts(level: String = "N5", partSize: Int = 25) -> [[VocabularyWord]] {
        chunks(of: loadVocabulary(level: level), size: partSize)
    }

    private static func chunks<T>(of items: [T], partCount: Int) -> [[T]] {
        guard !items.isEmpty, partCount > 0 else { return [] }
        let size = Int(ceil(Double(items.count) / Double(partCount)))
        return chunks(of: items, size: size)
    }

    private static func chunks<T>(of items: [T], size: Int) -> [[T]] {
        guard !items.isEmpty, size > 0 else { return [] }
        return stride(from: 0, to: items.count, by: size).map {
            Array(items[$0..<min($0 + size, items.count)])
        }
    }

    private static func load<T: Decodable>(_ fileName: String) -> T? {
        guard let url = Bundle.main.url(forResource: fileName, withExtension: "json"),
              let data = try? Data(contentsOf: url) else {
            return nil
        }
        return try? JSONDecoder().decode(T.self, from: data)
    }
}
