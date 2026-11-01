package com.batuhankeskn0.kaimon.domain.engine

/**
 * Japonca fiil gruplarını (Godan alt türleri, Ichidan ve Düzensiz fiiller) temsil eder.
 */
enum class VerbType(val rawValue: String) {
    GODAN_U("う"),
    GODAN_KU("く"),
    GODAN_GU("ぐ"),
    GODAN_SU("す"),
    GODAN_TSU("つ"),
    GODAN_NU("ぬ"),
    GODAN_BU("ぶ"),
    GODAN_MU("む"),
    GODAN_RU("る (Godan)"),
    ICHIDAN("る (Ichidan)"),
    SURU("する (Düzensiz)"),
    KURU("くる (Düzensiz)"),
    ARU("ある (Düzensiz)"),
    UNKNOWN("Bilinmiyor");

    val displayName: String
        get() = rawValue

    val isGodan: Boolean
        get() = when (this) {
            GODAN_U, GODAN_KU, GODAN_GU, GODAN_SU, GODAN_TSU,
            GODAN_NU, GODAN_BU, GODAN_MU, GODAN_RU -> true
            else -> false
        }

    companion object {
        val godanU: VerbType = GODAN_U
        val godanKu: VerbType = GODAN_KU
        val godanGu: VerbType = GODAN_GU
        val godanSu: VerbType = GODAN_SU
        val godanTsu: VerbType = GODAN_TSU
        val godanNu: VerbType = GODAN_NU
        val godanBu: VerbType = GODAN_BU
        val godanMu: VerbType = GODAN_MU
        val godanRu: VerbType = GODAN_RU
        val ichidan: VerbType = ICHIDAN
        val suru: VerbType = SURU
        val kuru: VerbType = KURU
        val aru: VerbType = ARU
        val unknown: VerbType = UNKNOWN
    }
}

/**
 * Tek bir fiil çekim formunu (örn. ます, て-biçimi, ない-biçimi) temsil eder.
 */
data class ConjugationForm(
    val id: String,
    val label: String,        // Display label in Turkish
    val japanese: String,     // The conjugated form (hiragana/kanji)
    val romaji: String,
    val explanation: String   // Brief Turkish explanation
)

/**
 * Bir fiilin sınıflandırma ve tüm çekim formlarını barındıran sonuç modeli.
 */
data class VerbConjugationResult(
    val dictionaryForm: String,    // e.g. たべる
    val kanji: String?,            // e.g. 食べる (optional)
    val verbType: VerbType,
    val forms: List<ConjugationForm>
)

/**
 * N5 seviyesinde kullanılan temel Japonca fiil çekimlerini saf kurallarla hesaplayan motor.
 * Sözlük formundaki hiragana fiil dizgesini girdi olarak alır (`VerbConjugationEngine.swift` 1:1 portu).
 */
object VerbConjugationEngine {

    private val godanRuOnly: Set<String> = setOf(
        "ある", "きる", "しる", "はいる", "はしる", "かえる",
        "ちる", "まいる", "まじる", "せびる"
    )

    private val iRowKana: Set<String> = setOf(
        "い", "き", "し", "ち", "に", "ひ", "み", "り", "ぎ", "じ", "び", "ぴ"
    )

    private val eRowKana: Set<String> = setOf(
        "え", "け", "せ", "て", "ね", "へ", "め", "れ", "げ", "ぜ", "で", "べ", "ぺ"
    )

    private val romajiTableSorted: List<Pair<String, String>> = listOf(
        "あ" to "a", "い" to "i", "う" to "u", "え" to "e", "お" to "o",
        "か" to "ka", "き" to "ki", "く" to "ku", "け" to "ke", "こ" to "ko",
        "が" to "ga", "ぎ" to "gi", "ぐ" to "gu", "げ" to "ge", "ご" to "go",
        "さ" to "sa", "し" to "shi", "す" to "su", "せ" to "se", "そ" to "so",
        "ざ" to "za", "じ" to "ji", "ず" to "zu", "ぜ" to "ze", "ぞ" to "zo",
        "た" to "ta", "ち" to "chi", "つ" to "tsu", "て" to "te", "と" to "to",
        "だ" to "da", "ぢ" to "di", "づ" to "du", "で" to "de", "ど" to "do",
        "な" to "na", "に" to "ni", "ぬ" to "nu", "ね" to "ne", "の" to "no",
        "は" to "ha", "ひ" to "hi", "ふ" to "fu", "へ" to "he", "ほ" to "ho",
        "ば" to "ba", "び" to "bi", "ぶ" to "bu", "べ" to "be", "ぼ" to "bo",
        "ぱ" to "pa", "ぴ" to "pi", "ぷ" to "pu", "ぺ" to "pe", "ぽ" to "po",
        "ま" to "ma", "み" to "mi", "む" to "mu", "め" to "me", "も" to "mo",
        "や" to "ya", "ゆ" to "yu", "よ" to "yo",
        "ら" to "ra", "り" to "ri", "る" to "ru", "れ" to "re", "ろ" to "ro",
        "わ" to "wa", "を" to "wo", "ん" to "n",
        "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo",
        "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho",
        "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
        "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo",
        "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
        "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo",
        "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
        "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
        "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
        "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo",
        "っ" to "tt"
    ).sortedByDescending { it.first.length }

    // MARK: - Public Entry Point

    fun conjugate(hiragana: String, kanji: String? = null): VerbConjugationResult {
        val normalizedKanji = if (kanji.isNullOrEmpty()) null else kanji
        val type = classify(hiragana)
        val forms = buildForms(hiragana = hiragana, type = type, kanji = normalizedKanji)
        return VerbConjugationResult(
            dictionaryForm = hiragana,
            kanji = normalizedKanji,
            verbType = type,
            forms = forms
        )
    }

    /**
     * Temel 10 N5 çekim formuna ek olarak İstek (たい), İrade (よう/おう), Emir (命令形) ve Şart (ば)
     * formlarını da içeren kapsamlı çekim tablosunu döner (`VerbConjugationScreen` için).
     */
    fun conjugateExtended(hiragana: String, kanji: String? = null): VerbConjugationResult {
        val base = conjugate(hiragana = hiragana, kanji = kanji)
        val extra = buildExtendedForms(hiragana = hiragana, type = base.verbType, kanji = base.kanji)
        return base.copy(forms = base.forms + extra)
    }

    // MARK: - Classification

    /**
     * Hiragana (sözlük formu) olarak yazılmış bir fiili sınıflandırır.
     */
    fun classify(hiragana: String): VerbType {
        if (hiragana.isEmpty()) return VerbType.UNKNOWN

        // --- Irregular verbs ---
        if (hiragana == "する" || hiragana.endsWith("する")) return VerbType.SURU
        if (hiragana == "くる" || hiragana == "来る") return VerbType.KURU
        if (hiragana == "ある") return VerbType.ARU

        val last = hiragana.last().toString()

        // --- Verbs not ending in る → definitely Godan ---
        when (last) {
            "う" -> return VerbType.GODAN_U
            "く" -> return VerbType.GODAN_KU
            "ぐ" -> return VerbType.GODAN_GU
            "す" -> return VerbType.GODAN_SU
            "つ" -> return VerbType.GODAN_TSU
            "ぬ" -> return VerbType.GODAN_NU
            "ぶ" -> return VerbType.GODAN_BU
            "む" -> return VerbType.GODAN_MU
        }

        // --- る ending: check if Ichidan or Godan ---
        if (last == "る") {
            if (isIchidan(hiragana)) return VerbType.ICHIDAN
            return VerbType.GODAN_RU
        }

        return VerbType.UNKNOWN
    }

    // MARK: - Ichidan Detection

    /**
     * Sondaki る karakterinden önceki ünlü sırasına (い-段 veya え-段) ve
     * Godan-る istisnalarına bakarak fiilin Ichidan (Grup 2) olup olmadığını döner.
     */
    private fun isIchidan(hiragana: String): Boolean {
        if (hiragana.length < 2) return false
        if (godanRuOnly.contains(hiragana)) return false

        val preRu = hiragana[hiragana.length - 2].toString()
        return iRowKana.contains(preRu) || eRowKana.contains(preRu)
    }

    // MARK: - Forms Builder

    private fun buildForms(
        hiragana: String,
        type: VerbType,
        kanji: String?
    ): List<ConjugationForm> = when (type) {
        VerbType.ICHIDAN -> ichidanForms(hiragana, kanji = kanji)
        VerbType.GODAN_U,
        VerbType.GODAN_KU,
        VerbType.GODAN_GU,
        VerbType.GODAN_SU,
        VerbType.GODAN_TSU,
        VerbType.GODAN_NU,
        VerbType.GODAN_BU,
        VerbType.GODAN_MU,
        VerbType.GODAN_RU -> godanForms(hiragana, type = type, kanji = kanji)
        VerbType.SURU -> suruForms(hiragana, kanji = kanji)
        VerbType.KURU -> kuruForms()
        VerbType.ARU -> aruForms()
        VerbType.UNKNOWN -> emptyList()
    }

    // MARK: - Ichidan Conjugations

    private fun ichidanForms(h: String, kanji: String?): List<ConjugationForm> {
        val stem = h.dropLast(1)
        val kanjiStem = kanji?.dropLast(1)

        fun j(suffix: String): String = (kanjiStem ?: stem) + suffix

        return listOf(
            ConjugationForm(
                id = "dict",
                label = "Sözlük (Plain)",
                japanese = kanji ?: h,
                romaji = toRomaji(h),
                explanation = "Temel form. Günlük konuşmada, arkadaşlar arasında."
            ),
            ConjugationForm(
                id = "masu",
                label = "Kibar (ます)",
                japanese = j("ます"),
                romaji = toRomaji(stem + "masu"),
                explanation = "Kibar, olumlu, geniş/gelecek zaman."
            ),
            ConjugationForm(
                id = "masen",
                label = "Kibar Olumsuz (ません)",
                japanese = j("ません"),
                romaji = toRomaji(stem + "masen"),
                explanation = "Kibar, olumsuz, geniş/gelecek zaman."
            ),
            ConjugationForm(
                id = "mashita",
                label = "Kibar Geçmiş (ました)",
                japanese = j("ました"),
                romaji = toRomaji(stem + "mashita"),
                explanation = "Kibar, olumlu, geçmiş zaman."
            ),
            ConjugationForm(
                id = "masen_deshita",
                label = "Kibar Geçmiş Olumsuz",
                japanese = j("ませんでした"),
                romaji = toRomaji(stem + "masen deshita"),
                explanation = "Kibar, olumsuz, geçmiş zaman."
            ),
            ConjugationForm(
                id = "te",
                label = "て-Biçimi",
                japanese = j("て"),
                romaji = toRomaji(stem + "te"),
                explanation = "Eylem bağlacı. ～てください, ～ています gibi yapılarda kullanılır."
            ),
            ConjugationForm(
                id = "ta",
                label = "た-Biçimi (Plain Geçmiş)",
                japanese = j("た"),
                romaji = toRomaji(stem + "ta"),
                explanation = "Kısa/içten geçmiş zaman."
            ),
            ConjugationForm(
                id = "nai",
                label = "Olumsuz Plain (ない)",
                japanese = j("ない"),
                romaji = toRomaji(stem + "nai"),
                explanation = "Kısa, olumsuz form."
            ),
            ConjugationForm(
                id = "potential",
                label = "Yapabilme (られる)",
                japanese = j("られる"),
                romaji = toRomaji(stem + "rareru"),
                explanation = "\"...yapabilmek\" anlamında yetenek/olanak ifadesi."
            ),
            ConjugationForm(
                id = "mashou",
                label = "Öneri (ましょう)",
                japanese = j("ましょう"),
                romaji = toRomaji(stem + "mashou"),
                explanation = "\"...yapalım\" — Öneri veya davet."
            )
        )
    }

    // MARK: - Godan Conjugations

    private data class GodanEndings(
        val teEnding: String,
        val taEnding: String,
        val masuStem: String,
        val naiStem: String,
        val potentialStem: String
    )

    private fun godanForms(h: String, type: VerbType, kanji: String?): List<ConjugationForm> {
        val stem = h.dropLast(1)
        val (teEnding, taEnding, masuStem, naiStem, potentialStem) = godanEndings(type)
        val displayStem = kanji?.dropLast(1) ?: stem

        fun jTe(): String = displayStem + teEnding
        fun jTa(): String = displayStem + taEnding
        fun jMasu(): String = displayStem + masuStem + "ます"
        fun jMasen(): String = displayStem + masuStem + "ません"
        fun jMashita(): String = displayStem + masuStem + "ました"
        fun jMasenDeshita(): String = displayStem + masuStem + "ませんでした"
        fun jNai(): String = displayStem + naiStem + "ない"
        fun jPotential(): String = displayStem + potentialStem + "る"
        fun jMashou(): String = displayStem + masuStem + "ましょう"

        return listOf(
            ConjugationForm(
                id = "dict",
                label = "Sözlük (Plain)",
                japanese = kanji ?: h,
                romaji = toRomaji(h),
                explanation = "Temel form. Günlük konuşmada, arkadaşlar arasında."
            ),
            ConjugationForm(
                id = "masu",
                label = "Kibar (ます)",
                japanese = jMasu(),
                romaji = toRomaji(stem + masuStem + "masu"),
                explanation = "Kibar, olumlu, geniş/gelecek zaman."
            ),
            ConjugationForm(
                id = "masen",
                label = "Kibar Olumsuz (ません)",
                japanese = jMasen(),
                romaji = toRomaji(stem + masuStem + "masen"),
                explanation = "Kibar, olumsuz, geniş/gelecek zaman."
            ),
            ConjugationForm(
                id = "mashita",
                label = "Kibar Geçmiş (ました)",
                japanese = jMashita(),
                romaji = toRomaji(stem + masuStem + "mashita"),
                explanation = "Kibar, olumlu, geçmiş zaman."
            ),
            ConjugationForm(
                id = "masen_deshita",
                label = "Kibar Geçmiş Olumsuz",
                japanese = jMasenDeshita(),
                romaji = toRomaji(stem + masuStem + "masen deshita"),
                explanation = "Kibar, olumsuz, geçmiş zaman."
            ),
            ConjugationForm(
                id = "te",
                label = "て-Biçimi",
                japanese = jTe(),
                romaji = toRomaji(stem + teEnding),
                explanation = "Eylem bağlacı. ～てください, ～ています gibi yapılarda kullanılır."
            ),
            ConjugationForm(
                id = "ta",
                label = "た-Biçimi (Plain Geçmiş)",
                japanese = jTa(),
                romaji = toRomaji(stem + taEnding),
                explanation = "Kısa/içten geçmiş zaman."
            ),
            ConjugationForm(
                id = "nai",
                label = "Olumsuz Plain (ない)",
                japanese = jNai(),
                romaji = toRomaji(stem + naiStem + "nai"),
                explanation = "Kısa, olumsuz form."
            ),
            ConjugationForm(
                id = "potential",
                label = "Yapabilme",
                japanese = jPotential(),
                romaji = toRomaji(stem + potentialStem + "ru"),
                explanation = "\"...yapabilmek\" anlamında yetenek/olanak ifadesi."
            ),
            ConjugationForm(
                id = "mashou",
                label = "Öneri (ましょう)",
                japanese = jMashou(),
                romaji = toRomaji(stem + masuStem + "mashou"),
                explanation = "\"...yapalım\" — Öneri veya davet."
            )
        )
    }

    private fun godanEndings(type: VerbType): GodanEndings = when (type) {
        VerbType.GODAN_U -> GodanEndings("って", "った", "い", "わ", "え")
        VerbType.GODAN_KU -> GodanEndings("いて", "いた", "き", "か", "け")
        VerbType.GODAN_GU -> GodanEndings("いで", "いだ", "ぎ", "が", "げ")
        VerbType.GODAN_SU -> GodanEndings("して", "した", "し", "さ", "せ")
        VerbType.GODAN_TSU -> GodanEndings("って", "った", "ち", "た", "て")
        VerbType.GODAN_NU -> GodanEndings("んで", "んだ", "に", "な", "ね")
        VerbType.GODAN_BU -> GodanEndings("んで", "んだ", "び", "ば", "べ")
        VerbType.GODAN_MU -> GodanEndings("んで", "んだ", "み", "ま", "め")
        VerbType.GODAN_RU -> GodanEndings("って", "った", "り", "ら", "れ")
        else -> GodanEndings("", "", "", "", "")
    }

    // MARK: - Irregular: する

    private fun suruForms(h: String, kanji: String?): List<ConjugationForm> {
        val prefix = if (h.endsWith("する")) h.dropLast(2) else ""
        val kPrefix = if (kanji?.endsWith("する") == true) kanji.dropLast(2) else (kanji ?: prefix)

        fun j(suffix: String): String = kPrefix + suffix
        fun r(suffix: String): String = prefix + suffix

        return listOf(
            ConjugationForm(id = "dict", label = "Sözlük (Plain)", japanese = kanji ?: h, romaji = toRomaji(h), explanation = "Temel form."),
            ConjugationForm(id = "masu", label = "Kibar (ます)", japanese = j("します"), romaji = r("shimasu"), explanation = "Kibar, olumlu."),
            ConjugationForm(id = "masen", label = "Kibar Olumsuz", japanese = j("しません"), romaji = r("shimasen"), explanation = "Kibar, olumsuz."),
            ConjugationForm(id = "mashita", label = "Kibar Geçmiş", japanese = j("しました"), romaji = r("shimashita"), explanation = "Kibar, geçmiş."),
            ConjugationForm(id = "masen_deshita", label = "Kibar Geçmiş Olumsuz", japanese = j("しませんでした"), romaji = r("shimasen deshita"), explanation = "Kibar, geçmiş, olumsuz."),
            ConjugationForm(id = "te", label = "て-Biçimi", japanese = j("して"), romaji = r("shite"), explanation = "Bağlaç formu."),
            ConjugationForm(id = "ta", label = "た-Biçimi", japanese = j("した"), romaji = r("shita"), explanation = "Plain geçmiş."),
            ConjugationForm(id = "nai", label = "Olumsuz Plain", japanese = j("しない"), romaji = r("shinai"), explanation = "Kısa olumsuz."),
            ConjugationForm(id = "potential", label = "Yapabilme", japanese = j("できる"), romaji = r("dekiru"), explanation = "\"...yapabilmek\"."),
            ConjugationForm(id = "mashou", label = "Öneri (ましょう)", japanese = j("しましょう"), romaji = r("shimashou"), explanation = "\"...yapalım\".")
        )
    }

    // MARK: - Irregular: くる

    private fun kuruForms(): List<ConjugationForm> = listOf(
        ConjugationForm(id = "dict", label = "Sözlük (Plain)", japanese = "くる", romaji = "kuru", explanation = "Temel form."),
        ConjugationForm(id = "masu", label = "Kibar (ます)", japanese = "きます", romaji = "kimasu", explanation = "Kibar, olumlu."),
        ConjugationForm(id = "masen", label = "Kibar Olumsuz", japanese = "きません", romaji = "kimasen", explanation = "Kibar, olumsuz."),
        ConjugationForm(id = "mashita", label = "Kibar Geçmiş", japanese = "きました", romaji = "kimashita", explanation = "Kibar, geçmiş."),
        ConjugationForm(id = "masen_deshita", label = "Kibar Geçmiş Olumsuz", japanese = "きませんでした", romaji = "kimasen deshita", explanation = "Kibar, geçmiş, olumsuz."),
        ConjugationForm(id = "te", label = "て-Biçimi", japanese = "きて", romaji = "kite", explanation = "Bağlaç formu."),
        ConjugationForm(id = "ta", label = "た-Biçimi", japanese = "きた", romaji = "kita", explanation = "Plain geçmiş."),
        ConjugationForm(id = "nai", label = "Olumsuz Plain", japanese = "こない", romaji = "konai", explanation = "Kısa olumsuz."),
        ConjugationForm(id = "potential", label = "Yapabilme", japanese = "こられる", romaji = "korareru", explanation = "\"...gelebilmek\"."),
        ConjugationForm(id = "mashou", label = "Öneri (ましょう)", japanese = "きましょう", romaji = "kimashou", explanation = "\"...gidelim\".")
    )

    // MARK: - Irregular: ある

    private fun aruForms(): List<ConjugationForm> = listOf(
        ConjugationForm(id = "dict", label = "Sözlük (Plain)", japanese = "ある", romaji = "aru", explanation = "Var olmak (cansız)."),
        ConjugationForm(id = "masu", label = "Kibar (ます)", japanese = "あります", romaji = "arimasu", explanation = "Kibar, olumlu."),
        ConjugationForm(id = "masen", label = "Kibar Olumsuz", japanese = "ありません", romaji = "arimasen", explanation = "Kibar, olumsuz."),
        ConjugationForm(id = "mashita", label = "Kibar Geçmiş", japanese = "ありました", romaji = "arimashita", explanation = "Kibar, geçmiş."),
        ConjugationForm(id = "masen_deshita", label = "Kibar Geçmiş Olumsuz", japanese = "ありませんでした", romaji = "arimasen deshita", explanation = "Kibar, geçmiş, olumsuz."),
        ConjugationForm(id = "te", label = "て-Biçimi", japanese = "あって", romaji = "atte", explanation = "Bağlaç formu."),
        ConjugationForm(id = "ta", label = "た-Biçimi", japanese = "あった", romaji = "atta", explanation = "Plain geçmiş."),
        ConjugationForm(id = "nai", label = "Olumsuz Plain", japanese = "ない", romaji = "nai", explanation = "ある fiilinin kısa olumsuzu özel biçimdir."),
        ConjugationForm(id = "potential", label = "Yapabilme", japanese = "ありえる", romaji = "ariereru", explanation = "Mümkün olmak."),
        ConjugationForm(id = "mashou", label = "Öneri (ましょう)", japanese = "ありましょう", romaji = "arimashou", explanation = "Nadiren kullanılır.")
    )

    // MARK: - Extended Forms (İstek, İrade, Emir, Şart)

    private fun godanORowStem(type: VerbType): String = when (type) {
        VerbType.GODAN_U -> "お"
        VerbType.GODAN_KU -> "こ"
        VerbType.GODAN_GU -> "ご"
        VerbType.GODAN_SU -> "そ"
        VerbType.GODAN_TSU -> "と"
        VerbType.GODAN_NU -> "の"
        VerbType.GODAN_BU -> "ぼ"
        VerbType.GODAN_MU -> "も"
        VerbType.GODAN_RU -> "ろ"
        else -> ""
    }

    private fun buildExtendedForms(
        hiragana: String,
        type: VerbType,
        kanji: String?
    ): List<ConjugationForm> = when (type) {
        VerbType.ICHIDAN -> {
            val stem = hiragana.dropLast(1)
            val displayStem = kanji?.dropLast(1) ?: stem
            listOf(
                ConjugationForm(
                    id = "tai",
                    label = "İstek (たい)",
                    japanese = displayStem + "たい",
                    romaji = toRomaji(stem + "tai"),
                    explanation = "\"...yapmak istemek\" anlamında istek ifadesi."
                ),
                ConjugationForm(
                    id = "volitional",
                    label = "İrade / İstek Plain (よう)",
                    japanese = displayStem + "よう",
                    romaji = toRomaji(stem + "you"),
                    explanation = "\"...yapalım / yapayım\" — Günlük konuşma öneri ve niyet formu."
                ),
                ConjugationForm(
                    id = "imperative",
                    label = "Emir (ろ)",
                    japanese = displayStem + "ろ",
                    romaji = toRomaji(stem + "ro"),
                    explanation = "Doğrudan emir kipi (\"...yap!\")."
                ),
                ConjugationForm(
                    id = "conditional",
                    label = "Şart (れば)",
                    japanese = displayStem + "れば",
                    romaji = toRomaji(stem + "reba"),
                    explanation = "\"Eğer ... yaparsa\" şart kipi."
                )
            )
        }
        VerbType.GODAN_U,
        VerbType.GODAN_KU,
        VerbType.GODAN_GU,
        VerbType.GODAN_SU,
        VerbType.GODAN_TSU,
        VerbType.GODAN_NU,
        VerbType.GODAN_BU,
        VerbType.GODAN_MU,
        VerbType.GODAN_RU -> {
            val stem = hiragana.dropLast(1)
            val displayStem = kanji?.dropLast(1) ?: stem
            val (_, _, masuStem, _, potentialStem) = godanEndings(type)
            val oRow = godanORowStem(type)
            listOf(
                ConjugationForm(
                    id = "tai",
                    label = "İstek (たい)",
                    japanese = displayStem + masuStem + "たい",
                    romaji = toRomaji(stem + masuStem + "tai"),
                    explanation = "\"...yapmak istemek\" anlamında istek ifadesi."
                ),
                ConjugationForm(
                    id = "volitional",
                    label = "İrade / İstek Plain (おう)",
                    japanese = displayStem + oRow + "う",
                    romaji = toRomaji(stem + oRow + "u"),
                    explanation = "\"...yapalım / yapayım\" — Günlük konuşma öneri ve niyet formu."
                ),
                ConjugationForm(
                    id = "imperative",
                    label = "Emir",
                    japanese = displayStem + potentialStem,
                    romaji = toRomaji(stem + potentialStem),
                    explanation = "Doğrudan emir kipi (\"...yap!\")."
                ),
                ConjugationForm(
                    id = "conditional",
                    label = "Şart (ば)",
                    japanese = displayStem + potentialStem + "ば",
                    romaji = toRomaji(stem + potentialStem + "ba"),
                    explanation = "\"Eğer ... yaparsa\" şart kipi."
                )
            )
        }
        VerbType.SURU -> {
            val prefix = if (hiragana.endsWith("する")) hiragana.dropLast(2) else ""
            val kPrefix = if (kanji?.endsWith("する") == true) kanji.dropLast(2) else (kanji ?: prefix)
            listOf(
                ConjugationForm(
                    id = "tai",
                    label = "İstek (たい)",
                    japanese = kPrefix + "したい",
                    romaji = toRomaji(prefix + "shitai"),
                    explanation = "\"...yapmak istemek\"."
                ),
                ConjugationForm(
                    id = "volitional",
                    label = "İrade / İstek Plain (しよう)",
                    japanese = kPrefix + "しよう",
                    romaji = toRomaji(prefix + "shiyou"),
                    explanation = "\"...yapalım\" (plain)."
                ),
                ConjugationForm(
                    id = "imperative",
                    label = "Emir (しろ)",
                    japanese = kPrefix + "しろ",
                    romaji = toRomaji(prefix + "shiro"),
                    explanation = "Doğrudan emir kipi."
                ),
                ConjugationForm(
                    id = "conditional",
                    label = "Şart (すれば)",
                    japanese = kPrefix + "すれば",
                    romaji = toRomaji(prefix + "sureba"),
                    explanation = "\"Eğer ... yaparsa\"."
                )
            )
        }
        VerbType.KURU -> listOf(
            ConjugationForm(id = "tai", label = "İstek (たい)", japanese = "きたい", romaji = "kitai", explanation = "\"...gelmek istemek\"."),
            ConjugationForm(id = "volitional", label = "İrade / İstek Plain", japanese = "こよう", romaji = "koyou", explanation = "\"...gelelim\" (plain)."),
            ConjugationForm(id = "imperative", label = "Emir", japanese = "こい", romaji = "koi", explanation = "\"Gel!\" (doğrudan emir)."),
            ConjugationForm(id = "conditional", label = "Şart (くれば)", japanese = "くれば", romaji = "kureba", explanation = "\"Eğer gelirse\".")
        )
        VerbType.ARU -> listOf(
            ConjugationForm(id = "tai", label = "İstek (たい)", japanese = "ありたい", romaji = "aritai", explanation = "Olmasını istemek."),
            ConjugationForm(id = "volitional", label = "İrade / Tahmin", japanese = "あろう", romaji = "arou", explanation = "Muhtemelen vardır."),
            ConjugationForm(id = "imperative", label = "Emir", japanese = "あれ", romaji = "are", explanation = "Emir formu."),
            ConjugationForm(id = "conditional", label = "Şart (あれば)", japanese = "あれば", romaji = "areba", explanation = "\"Eğer varsa\".")
        )
        VerbType.UNKNOWN -> emptyList()
    }

    // MARK: - Romaji Helper

    /**
     * Temel hiragana -> romaji dönüştürücü (`VerbConjugationEngine.swift` ile birebir).
     */
    fun toRomaji(text: String): String {
        if (text.all { it.code < 128 }) return text

        var result = text
        for ((kana, romaji) in romajiTableSorted) {
            result = result.replace(kana, romaji)
        }

        val filtered = StringBuilder(result.length)
        var i = 0
        while (i < result.length) {
            val cp = result.codePointAt(i)
            if (cp < 128 || (cp in 0x3040..0x30FF)) {
                filtered.appendCodePoint(cp)
            }
            i += Character.charCount(cp)
        }
        return filtered.toString()
    }
}
