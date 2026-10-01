import Foundation

/// JLPT seviyesine göre gruplanan kanji verisi (şu an sadece N5 dolu — bkz. N5KanjiData.json).
/// Quiz'de karakter gösterilip doğru Türkçe anlam 4 şıktan seçilir; kart çevrilince
/// okunuşlar (on'yomi/kun'yomi) ve örnek bir kelime gösterilir.
struct Kanji: Codable, Identifiable, Hashable {
    var id: String { character }

    let character: String
    let onyomi: String
    let kunyomi: String
    let meaning: String
    let strokeCount: Int
    var exampleWords: [ExampleWord] = []
}
