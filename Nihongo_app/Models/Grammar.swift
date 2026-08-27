import Foundation

/// N5 gramer konusunun ait olduğu kategori. GrammarPartSelectionView bu alana göre
/// bölümler ("Edatlar", "Fiiller"...) oluşturur; içerik olarak jlptsensei ve
/// mlcjapanese N5 listeleri temel alındı.
enum GrammarCategory: String, Codable, CaseIterable {
    case particle       // edatlar: は, を, で, に, も, の...
    case verb           // fiil çekimleri: ます, てください, ています, たい...
    case adjective      // い / な sıfatları
    case conjunction    // bağlaçlar: から, ので, が, けど...
    case expression     // kalıp ifadeler: があります/います, ほうがいい...

    var displayName: String {
        switch self {
        case .particle: return "Edatlar"
        case .verb: return "Fiiller"
        case .adjective: return "Sıfatlar"
        case .conjunction: return "Bağlaçlar"
        case .expression: return "İfadeler"
        }
    }

    /// Bölümlerin öğrenme ekranında görünme sırası (kolaydan zora).
    var order: Int {
        switch self {
        case .particle: return 0
        case .expression: return 1
        case .adjective: return 2
        case .verb: return 3
        case .conjunction: return 4
        }
    }
}

/// Bir gramer konusuna ait örnek cümle. Ders ekranında dört satır olarak gösterilir:
/// Japonca (büyük) · hiragana okunuşu (küçük/gri altyazı) · romaji · Türkçe.
/// `hiragana` yalnızca kanji içeren cümlelerde doludur (saf kana cümlelerde gereksiz).
struct GrammarExample: Codable, Hashable {
    let japanese: String
    let hiragana: String?
    let romaji: String
    let turkish: String
}

/// Pratik sorusunun tipi. Frontend her tipi farklı render eder (bkz. GrammarPracticeView).
enum GrammarQuestionKind: String, Codable {
    case fillBlank          // cümledeki ＿＿ yerine doğru şıkkı seç (özellikle edatlar)
    case wordOrder          // karışık kelime kartlarını doğru sıraya diz
    case multipleChoice     // anlam / formül / kullanım sorusu, 4 şık
}

/// Bir gramer konusunun tek bir pratik sorusu. Alanların hangisinin dolu olduğu
/// `kind`'a göre değişir:
/// - fillBlank / multipleChoice: `choices` (şıklar) + `answer` (doğru şık)
/// - wordOrder: `choices` (karışık parçalar) + `answerTokens` (doğru sıra) + `answer` (birleştirilmiş doğru cümle, geri bildirimde gösterilir)
struct GrammarQuestion: Codable, Hashable {
    let kind: GrammarQuestionKind

    /// fillBlank'te ＿＿ içeren cümle; wordOrder'da hedefi tarif eden metin;
    /// multipleChoice'ta soru metni.
    let prompt: String

    /// İsteğe bağlı ipucu / Türkçe çeviri.
    let hint: String?

    /// `prompt` içindeki Japonca cümlenin hiragana okunuşu (kanji içeren fillBlank ve
    /// alıntılı multipleChoice sorularında dolu). Soru metninin altında küçük/gri gösterilir.
    let promptReading: String?

    let choices: [String]
    let answer: String
    let answerTokens: [String]?

    private enum CodingKeys: String, CodingKey {
        case kind, prompt, hint, promptReading, choices, answer, answerTokens
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        kind = try c.decode(GrammarQuestionKind.self, forKey: .kind)
        prompt = try c.decode(String.self, forKey: .prompt)
        hint = try c.decodeIfPresent(String.self, forKey: .hint)
        promptReading = try c.decodeIfPresent(String.self, forKey: .promptReading)
        choices = try c.decodeIfPresent([String].self, forKey: .choices) ?? []
        answer = try c.decode(String.self, forKey: .answer)
        answerTokens = try c.decodeIfPresent([String].self, forKey: .answerTokens)
    }

    init(kind: GrammarQuestionKind, prompt: String, hint: String? = nil, promptReading: String? = nil, choices: [String] = [], answer: String, answerTokens: [String]? = nil) {
        self.kind = kind
        self.prompt = prompt
        self.hint = hint
        self.promptReading = promptReading
        self.choices = choices
        self.answer = answer
        self.answerTokens = answerTokens
    }
}

/// N5GrammarData.json içindeki tek bir gramer konusu. Statik içerik olduğu için
/// SwiftData modeli değil sade bir Codable struct'tır — spaced-repetition ilerlemesi
/// diğer modüllerle aynı `LearningItemProgress` tablosunda `itemKind == .grammar`
/// olarak, `itemID == GrammarPoint.id` ile takip edilir.
struct GrammarPoint: Codable, Identifiable, Hashable {
    var id: String { key }

    let key: String              // benzersiz, sabit anahtar — ör. "wa_b_desu"
    let pattern: String          // ～は～です
    let romaji: String           // "~ wa ~ desu"
    let title: String            // kısa başlık — ör. "A wa B desu"
    let explanation: String      // 1-2 cümlelik Türkçe açıklama
    let formula: String          // "[İsim A] + は + [İsim B] + です"
    let category: GrammarCategory
    let difficulty: Int          // 1-3, bölüm içi sıralama
    let examples: [GrammarExample]
    let questions: [GrammarQuestion]
}
