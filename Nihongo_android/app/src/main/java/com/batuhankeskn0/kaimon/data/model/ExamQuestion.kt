package com.batuhankeskn0.kaimon.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Sınav türü: Müfredatı bitirenler için "Kapı Sınavı" veya önceden bilenler için "Test-Out Challenge".
 */
@Serializable
enum class ExamMode(val rawValue: String) {
    @SerialName("gateway")
    GATEWAY("gateway"), // Standart Seviye Kapı Sınavı (%80 başarı)

    @SerialName("testOut")
    TEST_OUT("testOut"); // Seviye Atlama Sınavı (3 can hakkı)

    val id: String
        get() = rawValue

    val title: String
        get() = when (this) {
            GATEWAY -> "Seviye Kapı Sınavı"
            TEST_OUT -> "Seviye Atlama (Test-Out) Sınavı"
        }

    companion object {
        val gateway: ExamMode = GATEWAY
        val testOut: ExamMode = TEST_OUT
    }
}

/**
 * Sınav sorusunun ait olduğu dil alanı.
 */
@Serializable
enum class ExamQuestionType(val rawValue: String) {
    @SerialName("Kanji")
    KANJI("Kanji"),

    @SerialName("Kelime")
    VOCABULARY("Kelime"),

    @SerialName("Gramer")
    GRAMMAR("Gramer");

    val badgeIcon: String
        get() = when (this) {
            KANJI -> "character.book.closed.fill"
            VOCABULARY -> "character.bubble.fill"
            GRAMMAR -> "text.alignleft"
        }

    companion object {
        val kanji: ExamQuestionType = KANJI
        val vocabulary: ExamQuestionType = VOCABULARY
        val grammar: ExamQuestionType = GRAMMAR
        val allCases: List<ExamQuestionType>
            get() = entries
    }
}

/**
 * Sınavda gösterilen tek bir soru.
 */
@Serializable
data class ExamQuestion(
    val id: String,
    val type: ExamQuestionType,
    val prompt: String,              // Japonca ana metin veya cümle
    val subPrompt: String? = null,   // Okunuş, ipucu veya soru açıklaması
    val options: List<String>,       // 4 seçenek
    val correctAnswer: String,       // Doğru şık
    val explanation: String? = null  // Çözüm / çeviri açıklaması
)
