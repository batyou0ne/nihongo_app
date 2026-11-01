package com.batuhankeskn0.kaimon.data.model

import kotlinx.serialization.Serializable

/**
 * JLPT seviyesine göre gruplanan kanji verisi (N5–N1 KanjiData.json).
 * Quiz'de karakter gösterilip doğru Türkçe anlam 4 şıktan seçilir; kart çevrilince
 * okunuşlar (on'yomi/kun'yomi) ve örnek bir kelime gösterilir.
 */
@Serializable
data class Kanji(
    val character: String,
    val onyomi: String,
    val kunyomi: String,
    val meaning: String,
    val strokeCount: Int,
    override val exampleWords: List<ExampleWord> = emptyList()
) : FlashcardItem {
    override val id: String
        get() = character

    override val prompt: String
        get() = character

    override val correctAnswer: String
        get() = meaning

    override val flipRecap: String
        get() {
            val reading = when {
                kunyomi.isEmpty() -> onyomi
                onyomi.isEmpty() -> kunyomi
                else -> "$kunyomi / $onyomi"
            }
            return "$character — $reading"
        }
}
