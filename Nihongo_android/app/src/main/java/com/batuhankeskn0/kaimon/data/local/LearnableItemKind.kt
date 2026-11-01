package com.batuhankeskn0.kaimon.data.local

import com.batuhankeskn0.kaimon.data.service.L10n
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Bir `LearningItemProgressEntity` kaydının hangi içerik türüne ait olduğunu belirtir.
 * Hem karakterler hem kelimeler aynı spaced-repetition modelini paylaşır,
 * bu yüzden tek bir tabloda tutulup türe göre ayrıştırılır.
 */
@Serializable
enum class LearnableItemKind(val rawValue: String) {
    @SerialName("hiraganaCharacter")
    HIRAGANA_CHARACTER("hiraganaCharacter"),

    @SerialName("katakanaCharacter")
    KATAKANA_CHARACTER("katakanaCharacter"),

    @SerialName("vocabularyWord")
    VOCABULARY_WORD("vocabularyWord"), // N5 kelime modülü (VocabularyWord) tarafından kullanılır

    @SerialName("kanji")
    KANJI("kanji"),

    @SerialName("grammar")
    GRAMMAR("grammar"); // N5 gramer modülü (GrammarPoint) tarafından kullanılır

    val displayName: String
        get() = when (this) {
            HIRAGANA_CHARACTER -> "Hiragana"
            KATAKANA_CHARACTER -> "Katakana"
            VOCABULARY_WORD -> L10n.itemKindName(rawValue)
            KANJI -> "Kanji"
            GRAMMAR -> L10n.itemKindName(rawValue)
        }

    /**
     * Modüldeki toplam öğe sayısı (Resources JSON içerikleriyle eşleşir).
     * HomeView ve ProgressOverviewView aynı kaynağı kullansın diye burada tutulur.
     */
    val totalCount: Int
        get() = when (this) {
            HIRAGANA_CHARACTER, KATAKANA_CHARACTER -> 46
            KANJI -> 80
            VOCABULARY_WORD -> 675
            GRAMMAR -> 85
        }

    /** Ana ekranda ikon yerine gösterilen Japonca karakter. */
    val symbol: String
        get() = when (this) {
            HIRAGANA_CHARACTER -> "あ"
            KATAKANA_CHARACTER -> "ア"
            KANJI -> "漢"
            VOCABULARY_WORD -> "語"
            GRAMMAR -> "文"
        }

    companion object {
        val hiraganaCharacter: LearnableItemKind = HIRAGANA_CHARACTER
        val katakanaCharacter: LearnableItemKind = KATAKANA_CHARACTER
        val vocabularyWord: LearnableItemKind = VOCABULARY_WORD
        val kanji: LearnableItemKind = KANJI
        val grammar: LearnableItemKind = GRAMMAR

        val allCases: List<LearnableItemKind>
            get() = entries

        fun fromRawValue(rawValue: String): LearnableItemKind? =
            entries.firstOrNull {
                it.rawValue == rawValue || it.name.equals(rawValue, ignoreCase = true)
            }
    }
}
