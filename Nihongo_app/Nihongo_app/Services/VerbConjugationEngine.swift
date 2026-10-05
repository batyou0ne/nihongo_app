import Foundation

// MARK: - Verb Type

enum VerbType: String {
    case godanU   = "う"
    case godanKu  = "く"
    case godanGu  = "ぐ"
    case godanSu  = "す"
    case godanTsu = "つ"
    case godanNu  = "ぬ"
    case godanBu  = "ぶ"
    case godanMu  = "む"
    case godanRu  = "る (Godan)"
    case ichidan  = "る (Ichidan)"
    case suru     = "する (Düzensiz)"
    case kuru     = "くる (Düzensiz)"
    case aru      = "ある (Düzensiz)"
    case unknown  = "Bilinmiyor"

    var displayName: String { rawValue }

    var isGodan: Bool {
        switch self {
        case .godanU, .godanKu, .godanGu, .godanSu, .godanTsu,
             .godanNu, .godanBu, .godanMu, .godanRu: return true
        default: return false
        }
    }
}

// MARK: - Conjugation Form

struct ConjugationForm: Identifiable {
    let id: String
    let label: String        // Display label in Turkish
    let japanese: String     // The conjugated form (hiragana/kanji)
    let romaji: String
    let explanation: String  // Brief Turkish explanation
}

// MARK: - Conjugation Result

struct VerbConjugationResult {
    let dictionaryForm: String    // e.g. たべる
    let kanji: String?            // e.g. 食べる (optional)
    let verbType: VerbType
    let forms: [ConjugationForm]
}

// MARK: - Engine

/// Pure-logic engine that computes the main Japanese verb conjugations
/// used at N5 level. Accepts a dictionary-form hiragana string.
enum VerbConjugationEngine {

    // MARK: - Public Entry Point

    static func conjugate(hiragana: String, kanji: String? = nil) -> VerbConjugationResult {
        let type = classify(hiragana)
        let forms = buildForms(hiragana: hiragana, type: type, kanji: kanji)
        return VerbConjugationResult(
            dictionaryForm: hiragana,
            kanji: kanji?.isEmpty == true ? nil : kanji,
            verbType: type,
            forms: forms
        )
    }

    // MARK: - Classification

    /// Classifies a verb written in hiragana (dictionary form).
    static func classify(_ hiragana: String) -> VerbType {
        guard !hiragana.isEmpty else { return .unknown }

        // --- Irregular verbs ---
        if hiragana == "する" || hiragana.hasSuffix("する") { return .suru }
        if hiragana == "くる" || hiragana == "来る" { return .kuru }
        if hiragana == "ある" { return .aru }

        let last = String(hiragana.last!)

        // --- Verbs not ending in る → definitely Godan ---
        switch last {
        case "う": return .godanU
        case "く": return .godanKu
        case "ぐ": return .godanGu
        case "す": return .godanSu
        case "つ": return .godanTsu
        case "ぬ": return .godanNu
        case "ぶ": return .godanBu
        case "む": return .godanMu
        default: break
        }

        // --- る ending: check if Ichidan or Godan ---
        if last == "る" {
            // Known Ichidan verbs (ends in い/え sound before る)
            if isIchidan(hiragana) { return .ichidan }
            return .godanRu
        }

        return .unknown
    }

    // MARK: - Ichidan Detection

    /// Returns true if the verb is Ichidan (Group 2) based on the vowel
    /// before the final る. Ichidan verbs end in い-段 or え-段 + る.
    private static func isIchidan(_ hiragana: String) -> Bool {
        guard hiragana.count >= 2 else { return false }

        // Hardcoded Godan-る exceptions (commonly tested at N5)
        let godanRuExceptions: Set<String> = [
            "ある", "いる", "おる", "かえる", "きる", "しる",
            "はいる", "はしる", "はなす", "ちる", "ねる" // ねる IS ichidan; keep for reference
        ]
        // Actually re-filter: exceptions that ARE godan despite ending in る
        let godanRuOnly: Set<String> = [
            "ある", "きる", "しる", "はいる", "はしる", "かえる",
            "ちる", "まいる", "まじる", "せびる",
        ]
        if godanRuOnly.contains(hiragana) { return false }

        // Check second-to-last character's vowel row
        let chars = Array(hiragana)
        let preRu = String(chars[chars.count - 2])

        let iRowKana: Set<String> = ["い","き","し","ち","に","ひ","み","り","ぎ","じ","び","ぴ"]
        let eRowKana: Set<String> = ["え","け","せ","て","ね","へ","め","れ","げ","ぜ","で","べ","ぺ"]

        return iRowKana.contains(preRu) || eRowKana.contains(preRu)
    }

    // MARK: - Forms Builder

    private static func buildForms(hiragana: String, type: VerbType, kanji: String?) -> [ConjugationForm] {
        switch type {
        case .ichidan:
            return ichidanForms(hiragana, kanji: kanji)
        case .godanU, .godanKu, .godanGu, .godanSu, .godanTsu,
             .godanNu, .godanBu, .godanMu, .godanRu:
            return godanForms(hiragana, type: type, kanji: kanji)
        case .suru:
            return suruForms(hiragana, kanji: kanji)
        case .kuru:
            return kuruForms()
        case .aru:
            return aruForms()
        case .unknown:
            return []
        }
    }

    // MARK: - Ichidan Conjugations

    private static func ichidanForms(_ h: String, kanji: String?) -> [ConjugationForm] {
        // Stem = drop final る
        let stem = String(h.dropLast())
        let kanjiStem = kanji.map { String($0.dropLast()) }

        func j(_ s: String) -> String {
            if let ks = kanjiStem { return ks + s } else { return stem + s }
        }

        return [
            ConjugationForm(id: "dict", label: "Sözlük (Plain)",
                            japanese: kanji ?? h, romaji: toRomaji(h),
                            explanation: "Temel form. Günlük konuşmada, arkadaşlar arasında."),
            ConjugationForm(id: "masu", label: "Kibar (ます)",
                            japanese: j("ます"), romaji: toRomaji(stem + "masu"),
                            explanation: "Kibar, olumlu, geniş/gelecek zaman."),
            ConjugationForm(id: "masen", label: "Kibar Olumsuz (ません)",
                            japanese: j("ません"), romaji: toRomaji(stem + "masen"),
                            explanation: "Kibar, olumsuz, geniş/gelecek zaman."),
            ConjugationForm(id: "mashita", label: "Kibar Geçmiş (ました)",
                            japanese: j("ました"), romaji: toRomaji(stem + "mashita"),
                            explanation: "Kibar, olumlu, geçmiş zaman."),
            ConjugationForm(id: "masen_deshita", label: "Kibar Geçmiş Olumsuz",
                            japanese: j("ませんでした"), romaji: toRomaji(stem + "masen deshita"),
                            explanation: "Kibar, olumsuz, geçmiş zaman."),
            ConjugationForm(id: "te", label: "て-Biçimi",
                            japanese: j("て"), romaji: toRomaji(stem + "te"),
                            explanation: "Eylem bağlacı. ～てください, ～ています gibi yapılarda kullanılır."),
            ConjugationForm(id: "ta", label: "た-Biçimi (Plain Geçmiş)",
                            japanese: j("た"), romaji: toRomaji(stem + "ta"),
                            explanation: "Kısa/içten geçmiş zaman."),
            ConjugationForm(id: "nai", label: "Olumsuz Plain (ない)",
                            japanese: j("ない"), romaji: toRomaji(stem + "nai"),
                            explanation: "Kısa, olumsuz form."),
            ConjugationForm(id: "potential", label: "Yapabilme (られる)",
                            japanese: j("られる"), romaji: toRomaji(stem + "rareru"),
                            explanation: "\"...yapabilmek\" anlamında yetenek/olanak ifadesi."),
            ConjugationForm(id: "mashou", label: "Öneri (ましょう)",
                            japanese: j("ましょう"), romaji: toRomaji(stem + "mashou"),
                            explanation: "\"...yapalım\" — Öneri veya davet."),
        ]
    }

    // MARK: - Godan Conjugations

    private static func godanForms(_ h: String, type: VerbType, kanji: String?) -> [ConjugationForm] {
        let stem = String(h.dropLast()) // e.g. か from かく

        // Build conjugated hiragana tails + romaji based on ending type
        // Returns (て-form, た-form, ます-stem, ない-stem, potential-stem)
        let (teEnding, taEnding, masuStem, naiStem, potentialStem) = godanEndings(type, stem: stem)

        let displayStem = kanji.map { String($0.dropLast()) } ?? stem

        func jTe() -> String { displayStem + teEnding }
        func jTa() -> String { displayStem + taEnding }
        func jMasu() -> String { displayStem + masuStem + "ます" }
        func jMasen() -> String { displayStem + masuStem + "ません" }
        func jMashita() -> String { displayStem + masuStem + "ました" }
        func jMasenDeshita() -> String { displayStem + masuStem + "ませんでした" }
        func jNai() -> String { displayStem + naiStem + "ない" }
        func jPotential() -> String { displayStem + potentialStem + "る" }
        func jMashou() -> String { displayStem + masuStem + "ましょう" }

        return [
            ConjugationForm(id: "dict", label: "Sözlük (Plain)",
                            japanese: kanji ?? h, romaji: toRomaji(h),
                            explanation: "Temel form. Günlük konuşmada, arkadaşlar arasında."),
            ConjugationForm(id: "masu", label: "Kibar (ます)",
                            japanese: jMasu(), romaji: toRomaji(stem + masuStem + "masu"),
                            explanation: "Kibar, olumlu, geniş/gelecek zaman."),
            ConjugationForm(id: "masen", label: "Kibar Olumsuz (ません)",
                            japanese: jMasen(), romaji: toRomaji(stem + masuStem + "masen"),
                            explanation: "Kibar, olumsuz, geniş/gelecek zaman."),
            ConjugationForm(id: "mashita", label: "Kibar Geçmiş (ました)",
                            japanese: jMashita(), romaji: toRomaji(stem + masuStem + "mashita"),
                            explanation: "Kibar, olumlu, geçmiş zaman."),
            ConjugationForm(id: "masen_deshita", label: "Kibar Geçmiş Olumsuz",
                            japanese: jMasenDeshita(), romaji: toRomaji(stem + masuStem + "masen deshita"),
                            explanation: "Kibar, olumsuz, geçmiş zaman."),
            ConjugationForm(id: "te", label: "て-Biçimi",
                            japanese: jTe(), romaji: toRomaji(stem + teEnding),
                            explanation: "Eylem bağlacı. ～てください, ～ています gibi yapılarda kullanılır."),
            ConjugationForm(id: "ta", label: "た-Biçimi (Plain Geçmiş)",
                            japanese: jTa(), romaji: toRomaji(stem + taEnding),
                            explanation: "Kısa/içten geçmiş zaman."),
            ConjugationForm(id: "nai", label: "Olumsuz Plain (ない)",
                            japanese: jNai(), romaji: toRomaji(stem + naiStem + "nai"),
                            explanation: "Kısa, olumsuz form."),
            ConjugationForm(id: "potential", label: "Yapabilme",
                            japanese: jPotential(), romaji: toRomaji(stem + potentialStem + "ru"),
                            explanation: "\"...yapabilmek\" anlamında yetenek/olanak ifadesi."),
            ConjugationForm(id: "mashou", label: "Öneri (ましょう)",
                            japanese: jMashou(), romaji: toRomaji(stem + masuStem + "mashou"),
                            explanation: "\"...yapalım\" — Öneri veya davet."),
        ]
    }

    /// Returns (te-ending, ta-ending, masu-stem, nai-stem, potential-stem)
    /// All values are hiragana suffixes to be appended after the verb stem.
    private static func godanEndings(_ type: VerbType, stem: String) -> (String, String, String, String, String) {
        switch type {
        case .godanU:   return ("って", "った", "い", "わ", "え")
        case .godanKu:  return ("いて", "いた", "き", "か", "け")
        case .godanGu:  return ("いで", "いだ", "ぎ", "が", "げ")
        case .godanSu:  return ("して", "した", "し", "さ", "せ")
        case .godanTsu: return ("って", "った", "ち", "た", "て")
        case .godanNu:  return ("んで", "んだ", "に", "な", "ね")
        case .godanBu:  return ("んで", "んだ", "び", "ば", "べ")
        case .godanMu:  return ("んで", "んだ", "み", "ま", "め")
        case .godanRu:  return ("って", "った", "り", "ら", "れ")
        default:        return ("", "", "", "", "")
        }
    }

    // MARK: - Irregular: する

    private static func suruForms(_ h: String, kanji: String?) -> [ConjugationForm] {
        // Handle compound verbs like べんきょうする
        let prefix = h.hasSuffix("する") ? String(h.dropLast(2)) : ""
        let kPrefix = kanji?.hasSuffix("する") == true ? String(kanji!.dropLast(2)) : (kanji ?? prefix)

        func j(_ s: String) -> String { kPrefix + s }
        func r(_ s: String) -> String { prefix + s }

        return [
            ConjugationForm(id: "dict", label: "Sözlük (Plain)", japanese: kanji ?? h, romaji: toRomaji(h), explanation: "Temel form."),
            ConjugationForm(id: "masu", label: "Kibar (ます)", japanese: j("します"), romaji: r("shimasu"), explanation: "Kibar, olumlu."),
            ConjugationForm(id: "masen", label: "Kibar Olumsuz", japanese: j("しません"), romaji: r("shimasen"), explanation: "Kibar, olumsuz."),
            ConjugationForm(id: "mashita", label: "Kibar Geçmiş", japanese: j("しました"), romaji: r("shimashita"), explanation: "Kibar, geçmiş."),
            ConjugationForm(id: "masen_deshita", label: "Kibar Geçmiş Olumsuz", japanese: j("しませんでした"), romaji: r("shimasen deshita"), explanation: "Kibar, geçmiş, olumsuz."),
            ConjugationForm(id: "te", label: "て-Biçimi", japanese: j("して"), romaji: r("shite"), explanation: "Bağlaç formu."),
            ConjugationForm(id: "ta", label: "た-Biçimi", japanese: j("した"), romaji: r("shita"), explanation: "Plain geçmiş."),
            ConjugationForm(id: "nai", label: "Olumsuz Plain", japanese: j("しない"), romaji: r("shinai"), explanation: "Kısa olumsuz."),
            ConjugationForm(id: "potential", label: "Yapabilme", japanese: j("できる"), romaji: r("dekiru"), explanation: "\"...yapabilmek\"."),
            ConjugationForm(id: "mashou", label: "Öneri (ましょう)", japanese: j("しましょう"), romaji: r("shimashou"), explanation: "\"...yapalım\"."),
        ]
    }

    // MARK: - Irregular: くる

    private static func kuruForms() -> [ConjugationForm] {
        return [
            ConjugationForm(id: "dict", label: "Sözlük (Plain)", japanese: "くる", romaji: "kuru", explanation: "Temel form."),
            ConjugationForm(id: "masu", label: "Kibar (ます)", japanese: "きます", romaji: "kimasu", explanation: "Kibar, olumlu."),
            ConjugationForm(id: "masen", label: "Kibar Olumsuz", japanese: "きません", romaji: "kimasen", explanation: "Kibar, olumsuz."),
            ConjugationForm(id: "mashita", label: "Kibar Geçmiş", japanese: "きました", romaji: "kimashita", explanation: "Kibar, geçmiş."),
            ConjugationForm(id: "masen_deshita", label: "Kibar Geçmiş Olumsuz", japanese: "きませんでした", romaji: "kimasen deshita", explanation: "Kibar, geçmiş, olumsuz."),
            ConjugationForm(id: "te", label: "て-Biçimi", japanese: "きて", romaji: "kite", explanation: "Bağlaç formu."),
            ConjugationForm(id: "ta", label: "た-Biçimi", japanese: "きた", romaji: "kita", explanation: "Plain geçmiş."),
            ConjugationForm(id: "nai", label: "Olumsuz Plain", japanese: "こない", romaji: "konai", explanation: "Kısa olumsuz."),
            ConjugationForm(id: "potential", label: "Yapabilme", japanese: "こられる", romaji: "korareru", explanation: "\"...gelebilmek\"."),
            ConjugationForm(id: "mashou", label: "Öneri (ましょう)", japanese: "きましょう", romaji: "kimashou", explanation: "\"...gidelim\"."),
        ]
    }

    // MARK: - Irregular: ある

    private static func aruForms() -> [ConjugationForm] {
        return [
            ConjugationForm(id: "dict", label: "Sözlük (Plain)", japanese: "ある", romaji: "aru", explanation: "Var olmak (cansız)."),
            ConjugationForm(id: "masu", label: "Kibar (ます)", japanese: "あります", romaji: "arimasu", explanation: "Kibar, olumlu."),
            ConjugationForm(id: "masen", label: "Kibar Olumsuz", japanese: "ありません", romaji: "arimasen", explanation: "Kibar, olumsuz."),
            ConjugationForm(id: "mashita", label: "Kibar Geçmiş", japanese: "ありました", romaji: "arimashita", explanation: "Kibar, geçmiş."),
            ConjugationForm(id: "masen_deshita", label: "Kibar Geçmiş Olumsuz", japanese: "ありませんでした", romaji: "arimasen deshita", explanation: "Kibar, geçmiş, olumsuz."),
            ConjugationForm(id: "te", label: "て-Biçimi", japanese: "あって", romaji: "atte", explanation: "Bağlaç formu."),
            ConjugationForm(id: "ta", label: "た-Biçimi", japanese: "あった", romaji: "atta", explanation: "Plain geçmiş."),
            ConjugationForm(id: "nai", label: "Olumsuz Plain", japanese: "ない", romaji: "nai", explanation: "ある fiilinin kısa olumsuzu özel biçimdir."),
            ConjugationForm(id: "potential", label: "Yapabilme", japanese: "ありえる", romaji: "ariereru", explanation: "Mümkün olmak."),
            ConjugationForm(id: "mashou", label: "Öneri (ましょう)", japanese: "ありましょう", romaji: "arimashou", explanation: "Nadiren kullanılır."),
        ]
    }

    // MARK: - Romaji Helper (simplified pass-through)

    /// Very light romaji helper — primarily a passthrough since romaji is stored
    /// in vocabulary data. For auto-generated forms we do basic kana→romaji.
    static func toRomaji(_ text: String) -> String {
        // If the text is already in romaji (ASCII), return as-is
        if text.unicodeScalars.allSatisfy({ $0.value < 128 }) { return text }

        // Basic hiragana → romaji table
        let table: [(String, String)] = [
            ("あ","a"),("い","i"),("う","u"),("え","e"),("お","o"),
            ("か","ka"),("き","ki"),("く","ku"),("け","ke"),("こ","ko"),
            ("が","ga"),("ぎ","gi"),("ぐ","gu"),("げ","ge"),("ご","go"),
            ("さ","sa"),("し","shi"),("す","su"),("せ","se"),("そ","so"),
            ("ざ","za"),("じ","ji"),("ず","zu"),("ぜ","ze"),("ぞ","zo"),
            ("た","ta"),("ち","chi"),("つ","tsu"),("て","te"),("と","to"),
            ("だ","da"),("ぢ","di"),("づ","du"),("で","de"),("ど","do"),
            ("な","na"),("に","ni"),("ぬ","nu"),("ね","ne"),("の","no"),
            ("は","ha"),("ひ","hi"),("ふ","fu"),("へ","he"),("ほ","ho"),
            ("ば","ba"),("び","bi"),("ぶ","bu"),("べ","be"),("ぼ","bo"),
            ("ぱ","pa"),("ぴ","pi"),("ぷ","pu"),("ぺ","pe"),("ぽ","po"),
            ("ま","ma"),("み","mi"),("む","mu"),("め","me"),("も","mo"),
            ("や","ya"),("ゆ","yu"),("よ","yo"),
            ("ら","ra"),("り","ri"),("る","ru"),("れ","re"),("ろ","ro"),
            ("わ","wa"),("を","wo"),("ん","n"),
            ("きゃ","kya"),("きゅ","kyu"),("きょ","kyo"),
            ("しゃ","sha"),("しゅ","shu"),("しょ","sho"),
            ("ちゃ","cha"),("ちゅ","chu"),("ちょ","cho"),
            ("にゃ","nya"),("にゅ","nyu"),("にょ","nyo"),
            ("ひゃ","hya"),("ひゅ","hyu"),("ひょ","hyo"),
            ("みゃ","mya"),("みゅ","myu"),("みょ","myo"),
            ("りゃ","rya"),("りゅ","ryu"),("りょ","ryo"),
            ("ぎゃ","gya"),("ぎゅ","gyu"),("ぎょ","gyo"),
            ("じゃ","ja"),("じゅ","ju"),("じょ","jo"),
            ("びゃ","bya"),("びゅ","byu"),("びょ","byo"),
            ("ぴゃ","pya"),("ぴゅ","pyu"),("ぴょ","pyo"),
            ("っ","tt"),
        ]

        var result = text
        // Sort by length descending to match digraphs first
        for (kana, romaji) in table.sorted(by: { $0.0.count > $1.0.count }) {
            result = result.replacingOccurrences(of: kana, with: romaji)
        }
        // Strip kanji that remain
        result = result.unicodeScalars
            .filter { $0.value < 128 || ($0.value >= 0x3040 && $0.value <= 0x30FF) }
            .map { String($0) }
            .joined()
        return result
    }
}
