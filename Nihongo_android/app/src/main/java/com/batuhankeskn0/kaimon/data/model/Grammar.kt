package com.batuhankeskn0.kaimon.data.model

import com.batuhankeskn0.kaimon.data.service.L10n
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Gramer konusunun ait olduğu kategori. GrammarPartSelectionView bu alana göre
 * bölümler ("Edatlar", "Fiiller"...) oluşturur; içerik olarak jlptsensei ve
 * mlcjapanese listeleri temel alındı.
 */
@Serializable
enum class GrammarCategory(val rawValue: String) {
    @SerialName("particle")
    PARTICLE("particle"),       // edatlar: は, を, で, に, も, の...

    @SerialName("verb")
    VERB("verb"),               // fiil çekimleri: ます, てください, ています, たい...

    @SerialName("adjective")
    ADJECTIVE("adjective"),     // い / な sıfatları

    @SerialName("conjunction")
    CONJUNCTION("conjunction"), // bağlaçlar: から, ので, が, けど...

    @SerialName("expression")
    EXPRESSION("expression");   // kalıp ifadeler: があります/います, ほうがいい...

    val displayName: String
        get() = L10n.grammarCategoryName(rawValue)

    /** Bölümlerin öğrenme ekranında görünme sırası (kolaydan zora). */
    val order: Int
        get() = when (this) {
            PARTICLE -> 0
            EXPRESSION -> 1
            ADJECTIVE -> 2
            VERB -> 3
            CONJUNCTION -> 4
        }

    companion object {
        val particle: GrammarCategory = PARTICLE
        val verb: GrammarCategory = VERB
        val adjective: GrammarCategory = ADJECTIVE
        val conjunction: GrammarCategory = CONJUNCTION
        val expression: GrammarCategory = EXPRESSION
        val allCases: List<GrammarCategory>
            get() = entries

        fun fromRawValue(rawValue: String): GrammarCategory? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

/**
 * Bir gramer konusuna ait örnek cümle. Ders ekranında dört satır olarak gösterilir:
 * Japonca (büyük) · hiragana okunuşu (küçük/gri altyazı) · romaji · Türkçe.
 * `hiragana` yalnızca kanji içeren cümlelerde doludur (saf kana cümlelerde gereksiz).
 */
@Serializable
data class GrammarExample(
    val japanese: String,
    val hiragana: String? = null,
    val romaji: String,
    val turkish: String
)

/**
 * Pratik sorusunun tipi. Frontend her tipi farklı render eder (bkz. GrammarPracticeView).
 */
@Serializable
enum class GrammarQuestionKind(val rawValue: String) {
    @SerialName("fillBlank")
    FILL_BLANK("fillBlank"),         // cümledeki ＿＿ yerine doğru şıkkı seç (özellikle edatlar)

    @SerialName("wordOrder")
    WORD_ORDER("wordOrder"),         // karışık kelime kartlarını doğru sıraya diz

    @SerialName("multipleChoice")
    MULTIPLE_CHOICE("multipleChoice"); // anlam / formül / kullanım sorusu, 4 şık

    companion object {
        val fillBlank: GrammarQuestionKind = FILL_BLANK
        val wordOrder: GrammarQuestionKind = WORD_ORDER
        val multipleChoice: GrammarQuestionKind = MULTIPLE_CHOICE

        fun fromRawValue(rawValue: String): GrammarQuestionKind? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

/**
 * Bir gramer konusunun tek bir pratik sorusu. Alanların hangisinin dolu olduğu
 * `kind`'a göre değişir:
 * - fillBlank / multipleChoice: `choices` (şıklar) + `answer` (doğru şık)
 * - wordOrder: `choices` (karışık parçalar) + `answerTokens` (doğru sıra) + `answer` (birleştirilmiş doğru cümle, geri bildirimde gösterilir)
 */
@Serializable
data class GrammarQuestion(
    val kind: GrammarQuestionKind,
    /**
     * fillBlank'te ＿＿ içeren cümle; wordOrder'da hedefi tarif eden metin;
     * multipleChoice'ta soru metni.
     */
    val prompt: String,
    /** İsteğe bağlı ipucu / Türkçe çeviri. */
    val hint: String? = null,
    /**
     * `prompt` içindeki Japonca cümlenin hiragana okunuşu (kanji içeren fillBlank ve
     * alıntılı multipleChoice sorularında dolu). Soru metninin altında küçük/gri gösterilir.
     */
    val promptReading: String? = null,
    val choices: List<String> = emptyList(),
    val answer: String,
    val answerTokens: List<String>? = null
)

/**
 * N5–N1 GrammarData.json içindeki tek bir gramer konusu. Statik içerik olduğu için
 * Room entity değil sade bir Serializable data class'tır — spaced-repetition ilerlemesi
 * diğer modüllerle aynı `LearningItemProgress` tablosunda `itemKind == grammar`
 * olarak, `itemID == GrammarPoint.id` ile takip edilir.
 */
@Serializable
data class GrammarPoint(
    val key: String,              // benzersiz, sabit anahtar — ör. "wa_b_desu"
    val pattern: String,          // ～は～です
    val romaji: String,           // "~ wa ~ desu"
    val title: String,            // kısa başlık — ör. "A wa B desu"
    val explanation: String,      // 1-2 cümlelik Türkçe açıklama
    val formula: String,          // "[İsim A] + は + [İsim B] + です"
    val category: GrammarCategory,
    val difficulty: Int,          // 1-3, bölüm içi sıralama
    val examples: List<GrammarExample> = emptyList(),
    val questions: List<GrammarQuestion> = emptyList()
) {
    val id: String
        get() = key
}

/**
 * N5–N1 GrammarSyllabus.json içindeki ünite yapısını temsil eder.
 */
@Serializable
data class GrammarUnit(
    val id: Int,
    val title: String,
    val description: String,
    val grammarKeys: List<String> = emptyList()
)
