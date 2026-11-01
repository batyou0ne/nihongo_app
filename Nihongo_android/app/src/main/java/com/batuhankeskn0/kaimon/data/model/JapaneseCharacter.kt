package com.batuhankeskn0.kaimon.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Bir öğrenme modülünün türü. HomeView'daki kart renklerini ve
 * LearningViewModel'in hangi JSON dosyasını yükleyeceğini belirler.
 */
@Serializable
enum class CharacterType(val rawValue: String) {
    @SerialName("hiragana")
    HIRAGANA("hiragana"),

    @SerialName("katakana")
    KATAKANA("katakana");

    val displayName: String
        get() = when (this) {
            HIRAGANA -> "Hiragana"
            KATAKANA -> "Katakana"
        }

    companion object {
        val hiragana: CharacterType = HIRAGANA
        val katakana: CharacterType = KATAKANA
        val allCases: List<CharacterType>
            get() = entries

        fun fromRawValue(rawValue: String): CharacterType? =
            entries.firstOrNull { it.rawValue == rawValue }
    }
}

/**
 * Bir karakterin (hece) geçtiği örnek bir kelime. Kart çevrildiğinde gösterilir.
 */
@Serializable
data class ExampleWord(
    val hiragana: String,
    val romaji: String,
    val turkishMeaning: String
)

/**
 * HiraganaData.json / KatakanaData.json içindeki tek bir karakter kaydı.
 * Statik içerik olduğu için Room entity değil, sade bir Serializable data class'tır —
 * öğrenme ilerlemesi ayrı olarak `LearningItemProgress` ile takip edilir.
 */
@Serializable
data class JapaneseCharacter(
    val character: String,
    val romaji: String,
    val turkishPronunciation: String,
    val row: String,
    val strokeCount: Int,
    /**
     * JSON'da tutulmadığı için `@Transient` olarak işaretlenir ve yükleme sırasında
     * dosya kaynağına (`HiraganaData.json` / `KatakanaData.json`) göre atanır.
     */
    @Transient
    var type: CharacterType = CharacterType.HIRAGANA,
    /**
     * Şu an sadece HiraganaData.json'da dolu; KatakanaData.json'da anahtar yoksa boş dizi kalır.
     */
    override val exampleWords: List<ExampleWord> = emptyList()
) : FlashcardItem {
    override val id: String
        get() = character

    override val prompt: String
        get() = character

    override val correctAnswer: String
        get() = romaji

    override val flipRecap: String
        get() = "$character = $romaji · $turkishPronunciation"
}
