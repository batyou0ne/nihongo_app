import Foundation

/// JLPT seviyesine göre gruplanan kelime verisi (şu an sadece N5 dolu — bkz.
/// N5VocabularyData.json, nihongoichiban.com N5 listesinden alınıp Türkçe'ye çevrildi).
/// Kartta kanji büyük gösterilir, üstünde küçük/gri hiragana okunuşu yer alır;
/// kanjisi olmayan kelimelerde (ör. ああ) hiragana tek başına büyük gösterilir.
struct VocabularyWord: Codable, Identifiable, Hashable {
    let kanji: String
    let hiragana: String
    let romaji: String
    let turkishMeaning: String

    /// Aynı okunuşa sahip farklı kelimeler olabildiği için (ör. はし = köprü / çubuklar)
    /// kimlik, okunuş + anlam birleşiminden üretilir.
    var id: String { "\(hiragana)|\(turkishMeaning)" }

    /// Kartta büyük gösterilen metin.
    var displayText: String { kanji.isEmpty ? hiragana : kanji }
}

extension VocabularyWord: FlashcardItem {
    var prompt: String { displayText }
    var correctAnswer: String { turkishMeaning }

    var flipRecap: String {
        kanji.isEmpty ? "\(hiragana) · \(romaji)" : "\(kanji) — \(hiragana) · \(romaji)"
    }

    var exampleWords: [ExampleWord] { [] }

    /// Kanjinin üstünde gösterilen hiragana okunuşu (kanji yoksa gerek yok).
    var promptReading: String? { kanji.isEmpty ? nil : hiragana }

    /// TTS kanjiyi yanlış okuyabilir; her zaman hiragana okunur.
    var speechText: String { hiragana }
}
