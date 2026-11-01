package com.batuhankeskn0.kaimon.data.model

import kotlinx.serialization.Serializable

/**
 * JLPT seviyesine göre gruplanan kelime verisi (N5–N1 VocabularyData.json).
 * Kartta kanji büyük gösterilir, üstünde küçük/gri hiragana okunuşu yer alır;
 * kanjisi olmayan kelimelerde (ör. ああ) hiragana tek başına büyük gösterilir.
 */
@Serializable
data class VocabularyWord(
    val kanji: String,
    val hiragana: String,
    val romaji: String,
    val turkishMeaning: String,
    val exampleSentence: String? = null,
    val exampleTranslation: String? = null
) : FlashcardItem {

    /**
     * Aynı okunuşa sahip farklı kelimeler olabildiği için (ör. はし = köprü / çubuklar)
     * kimlik, okunuş + anlam birleşiminden üretilir.
     */
    override val id: String
        get() = "$hiragana|$turkishMeaning"

    /** Kartta büyük gösterilen metin. */
    val displayText: String
        get() = kanji.ifEmpty { hiragana }

    override val prompt: String
        get() = displayText

    override val correctAnswer: String
        get() = turkishMeaning

    override val alternatePrompt: String?
        get() {
            val sentence = exampleSentence ?: return null
            val target = kanji.ifEmpty { hiragana }
            if (target.isNotEmpty() && sentence.contains(target)) {
                return sentence.replace(target, "____")
            }
            return null
        }

    override val alternateAnswer: String?
        get() = kanji.ifEmpty { hiragana }

    override val flipRecap: String
        get() = if (kanji.isEmpty()) {
            "$hiragana · $romaji"
        } else {
            "$kanji — $hiragana · $romaji"
        }

    override val exampleWords: List<ExampleWord>
        get() = emptyList()

    /** Kanjinin üstünde gösterilen hiragana okunuşu (kanji yoksa gerek yok). */
    override val promptReading: String?
        get() = if (kanji.isEmpty()) null else hiragana

    /** TTS kanjiyi yanlış okuyabilir; her zaman hiragana okunur. */
    override val speechText: String
        get() = hiragana

    override val contextHint: ContextHint?
        get() {
            val sentence = exampleSentence ?: return null
            val translation = exampleTranslation ?: return null
            return ContextHint(sentence = sentence, translation = translation)
        }
}
