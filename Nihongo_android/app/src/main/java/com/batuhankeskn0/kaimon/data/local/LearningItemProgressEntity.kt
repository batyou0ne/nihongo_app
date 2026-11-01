package com.batuhankeskn0.kaimon.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tek bir öğrenilebilir öğenin (karakter/kanji/kelime/gramer) spaced-repetition durumu.
 * `itemID` alanı `JapaneseCharacter.id`, `Kanji.id`, `VocabularyWord.id` ya da `GrammarPoint.id` ile eşleşir.
 * Zamanlama mantığı `SpacedRepetitionService` içinde hesaplanır; bu model sadece
 * hesaplanan sonucu (yeni interval, yeni tekrar tarihi) saklar.
 */
@Entity(tableName = "learning_item_progress")
data class LearningItemProgressEntity(
    @PrimaryKey
    val itemID: String,
    var itemKind: LearnableItemKind,

    /** SM-2 / WaniKani algoritmasının temel değişkenleri. */
    var easeFactor: Double = 2.5,
    var intervalDays: Int = 0,
    var repetitionCount: Int = 0,

    var dueDate: Long = System.currentTimeMillis(),
    var lastReviewedDate: Long? = null,
    var isLearned: Boolean = false,

    /**
     * Yanlış cevap verildiğinde işaretlenir ve kullanıcı "Tekrar Çalış" ekranında
     * öğrendiğini onaylayana kadar işaretli kalır.
     */
    var needsReview: Boolean = false
)
