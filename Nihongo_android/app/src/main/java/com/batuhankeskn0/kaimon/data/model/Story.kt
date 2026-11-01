package com.batuhankeskn0.kaimon.data.model

import com.batuhankeskn0.kaimon.data.service.L10n
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Okuma hikayelerinin içerdiği alfabe/yazı sistemi türü.
 */
@Serializable
enum class StoryType(val rawValue: String) {
    @SerialName("hiragana")
    HIRAGANA("hiragana"),

    @SerialName("hiraganaKatakana")
    HIRAGANA_KATAKANA("hiraganaKatakana"),

    @SerialName("hiraganaKanji")
    HIRAGANA_KANJI("hiraganaKanji"),

    @SerialName("all")
    ALL("all");

    val displayName: String
        get() = L10n.storyTypeName(rawValue)

    companion object {
        val hiragana: StoryType = HIRAGANA
        val hiraganaKatakana: StoryType = HIRAGANA_KATAKANA
        val hiraganaKanji: StoryType = HIRAGANA_KANJI
        val all: StoryType = ALL
        val allCases: List<StoryType>
            get() = entries

        fun fromRawValue(rawValue: String): StoryType? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

/**
 * Bir hikaye içindeki tek bir cümle, okunuşu, çevirisi ve anahtar kelimeleri.
 */
@Serializable
data class StorySentence(
    val id: String,
    val japaneseText: String,
    val romaji: String,
    val turkishTranslation: String,
    val vocabulary: List<String> = emptyList() // Format: "あさ (Sabah)"
)

/**
 * JLPT okuma pratiği için hikaye veya metinleri temsil eder.
 */
@Serializable
data class Story(
    val id: String,
    val type: StoryType,
    val title: String,
    val sentences: List<StorySentence> = emptyList()
)
