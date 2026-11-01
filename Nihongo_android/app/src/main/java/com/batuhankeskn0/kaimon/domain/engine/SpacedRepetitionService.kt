package com.batuhankeskn0.kaimon.domain.engine

import com.batuhankeskn0.kaimon.data.local.LearningItemProgressEntity
import com.batuhankeskn0.kaimon.data.local.ProgressDao
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Anki / WaniKani tarzı Spaced Repetition (Aralıklı Tekrar) algoritmasını yürüten servis.
 * SuperMemo-2 (SM-2) ve WaniKani 7 kademeli SRS sistemi temel alınmıştır.
 */
@Singleton
class SpacedRepetitionService @Inject constructor() {

    enum class SRSLevel(val rawValue: Int) {
        APPRENTICE_1(1), // 4 saat
        APPRENTICE_2(2), // 1 gün
        APPRENTICE_3(3), // 3 gün
        GURU(4),         // 1 hafta
        MASTER(5),       // 2 hafta
        ENLIGHTENED(6),  // 1 ay
        BURNED(7);       // Kalıcı öğrenilmiş (10 yıl)

        val title: String
            get() = when (this) {
                APPRENTICE_1, APPRENTICE_2, APPRENTICE_3 -> "Çırak"
                GURU -> "Kalfa"
                MASTER -> "Usta"
                ENLIGHTENED -> "Aydınlanmış"
                BURNED -> "Kalıcı (Burned)"
            }

        val nextIntervalHours: Int
            get() = when (this) {
                APPRENTICE_1 -> 4
                APPRENTICE_2 -> 24  // 1 gün
                APPRENTICE_3 -> 72  // 3 gün
                GURU -> 168         // 1 hafta
                MASTER -> 336       // 2 hafta
                ENLIGHTENED -> 720  // ~1 ay
                BURNED -> 0
            }

        companion object {
            val apprentice1: SRSLevel = APPRENTICE_1
            val apprentice2: SRSLevel = APPRENTICE_2
            val apprentice3: SRSLevel = APPRENTICE_3
            val guru: SRSLevel = GURU
            val master: SRSLevel = MASTER
            val enlightened: SRSLevel = ENLIGHTENED
            val burned: SRSLevel = BURNED

            val allCases: List<SRSLevel>
                get() = entries

            fun fromRawValue(rawValue: Int): SRSLevel? =
                entries.firstOrNull { it.rawValue == rawValue }
        }
    }

    /**
     * Verilen öğrenme kaydını SRS (WaniKani stili) algoritmasına göre günceller.
     * `repetitionCount` alanı SRS Level (`1..7`) olarak görev yapar.
     */
    fun updateProgress(
        progress: LearningItemProgressEntity,
        correct: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): LearningItemProgressEntity {
        var currentLevel = progress.repetitionCount

        if (correct) {
            currentLevel += 1
            if (currentLevel > 7) currentLevel = 7

            progress.needsReview = false
            progress.isLearned = true // Bir kere bile doğru bilindiyse öğrenildi sayılır.
        } else {
            // Yanlış cevap verildiğinde ceza olarak 1 veya daha fazla seviye düşer.
            // Kalfa Üstü (> 4) ise 2 seviye, Çırak/Kalfa (<= 4) ise 1 seviye düşer.
            if (currentLevel > 4) {
                currentLevel -= 2
            } else {
                currentLevel -= 1
            }

            if (currentLevel < 1) currentLevel = 1 // En düşük seviye 1 (Apprentice 1)
            progress.needsReview = true
        }

        progress.repetitionCount = currentLevel
        progress.lastReviewedDate = nowMillis

        val nowZoned = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        val level = SRSLevel.fromRawValue(currentLevel)

        if (level != null) {
            progress.intervalDays = level.nextIntervalHours / 24 // İstatistik ve geriye dönük uyumluluk için

            progress.dueDate = if (level == SRSLevel.BURNED) {
                // Kalıcı öğrenilenler için bir daha review'a düşmemesi adına 10 yıl sonraya atıyoruz
                nowZoned.plusYears(10).toInstant().toEpochMilli()
            } else {
                nowZoned.plusHours(level.nextIntervalHours.toLong()).toInstant().toEpochMilli()
            }
        } else {
            // Fallback
            progress.repetitionCount = 1
            progress.intervalDays = 0
            progress.dueDate = nowZoned.plusHours(4).toInstant().toEpochMilli()
        }

        return progress
    }

    /**
     * İlerleme kaydını günceller ve doğrudan Room `ProgressDao` üzerine yazar.
     */
    suspend fun updateAndSaveProgress(
        progress: LearningItemProgressEntity,
        correct: Boolean,
        progressDao: ProgressDao,
        nowMillis: Long = System.currentTimeMillis()
    ): LearningItemProgressEntity {
        val updated = updateProgress(progress = progress, correct = correct, nowMillis = nowMillis)
        progressDao.upsertItemProgress(updated)
        return updated
    }

    companion object {
        val shared: SpacedRepetitionService = SpacedRepetitionService()

        fun updateProgress(
            progress: LearningItemProgressEntity,
            correct: Boolean,
            nowMillis: Long = System.currentTimeMillis()
        ): LearningItemProgressEntity =
            shared.updateProgress(progress = progress, correct = correct, nowMillis = nowMillis)
    }
}
