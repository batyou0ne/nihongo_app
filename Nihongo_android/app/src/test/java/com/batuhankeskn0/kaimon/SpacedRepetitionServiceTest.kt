package com.batuhankeskn0.kaimon

import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.local.LearningItemProgressEntity
import com.batuhankeskn0.kaimon.domain.engine.SpacedRepetitionService
import com.batuhankeskn0.kaimon.domain.engine.SpacedRepetitionService.SRSLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

/**
 * Faz 3 — `SpacedRepetitionService` (WaniKani 7 kademeli SRS motoru) birim testleri.
 */
class SpacedRepetitionServiceTest {

    private val srs = SpacedRepetitionService.shared
    private val fixedZone: ZoneId = ZoneId.of("UTC")
    private val fixedNowMillis: Long = 1_700_000_000_000L // 2023-11-14T22:13:20Z

    @Test
    fun `SRSLevel intervals and titles match WaniKani 7-stage specification`() {
        assertEquals(7, SRSLevel.allCases.size)

        assertEquals(4, SRSLevel.apprentice1.nextIntervalHours)
        assertEquals(24, SRSLevel.apprentice2.nextIntervalHours)
        assertEquals(72, SRSLevel.apprentice3.nextIntervalHours)
        assertEquals(168, SRSLevel.guru.nextIntervalHours)
        assertEquals(336, SRSLevel.master.nextIntervalHours)
        assertEquals(720, SRSLevel.enlightened.nextIntervalHours)
        assertEquals(0, SRSLevel.burned.nextIntervalHours)

        assertEquals("Çırak", SRSLevel.apprentice1.title)
        assertEquals("Çırak", SRSLevel.apprentice2.title)
        assertEquals("Çırak", SRSLevel.apprentice3.title)
        assertEquals("Kalfa", SRSLevel.guru.title)
        assertEquals("Usta", SRSLevel.master.title)
        assertEquals("Aydınlanmış", SRSLevel.enlightened.title)
        assertEquals("Kalıcı (Burned)", SRSLevel.burned.title)
    }

    @Test
    fun `correct answers advance item through all 7 SRS levels up to burned`() {
        val progress = LearningItemProgressEntity(
            itemID = "あ",
            itemKind = LearnableItemKind.hiraganaCharacter
        )

        val expectedHours = listOf(4, 24, 72, 168, 336, 720)
        for ((idx, hours) in expectedHours.withIndex()) {
            val expectedLevel = idx + 1
            srs.updateProgress(
                progress = progress,
                correct = true,
                nowMillis = fixedNowMillis,
                zoneId = fixedZone
            )

            assertEquals(expectedLevel, progress.repetitionCount)
            assertTrue(progress.isLearned)
            assertFalse(progress.needsReview)
            assertEquals(fixedNowMillis, progress.lastReviewedDate)
            assertEquals(hours / 24, progress.intervalDays)
            assertEquals(fixedNowMillis + hours * 3_600_000L, progress.dueDate)
        }

        // 7. doğru cevap -> Burned (Seviye 7, 10 yıl sonrası)
        srs.updateProgress(
            progress = progress,
            correct = true,
            nowMillis = fixedNowMillis,
            zoneId = fixedZone
        )
        assertEquals(7, progress.repetitionCount)
        assertEquals(0, progress.intervalDays)
        val expectedBurnedDue = Instant.ofEpochMilli(fixedNowMillis)
            .atZone(fixedZone)
            .plusYears(10)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedBurnedDue, progress.dueDate)

        // 8. doğru cevap -> Seviye 7'yi aşmamalı (tavan = 7)
        srs.updateProgress(
            progress = progress,
            correct = true,
            nowMillis = fixedNowMillis,
            zoneId = fixedZone
        )
        assertEquals(7, progress.repetitionCount)
    }

    @Test
    fun `wrong answer on apprentice or guru drops 1 level with floor at 1 and sets needsReview`() {
        // Seviye 0'da yanlış cevap -> Seviye 1 (en düşük seviye 1), needsReview = true, isLearned = false
        val newItem = LearningItemProgressEntity(
            itemID = "日",
            itemKind = LearnableItemKind.kanji
        )
        srs.updateProgress(newItem, correct = false, nowMillis = fixedNowMillis, zoneId = fixedZone)
        assertEquals(1, newItem.repetitionCount)
        assertTrue(newItem.needsReview)
        assertFalse(newItem.isLearned)
        assertEquals(fixedNowMillis + 4 * 3_600_000L, newItem.dueDate)

        // Seviye 4 (Guru) yanlış cevap -> Seviye 3 (1 seviye düşer)
        val guruItem = LearningItemProgressEntity(
            itemID = "月",
            itemKind = LearnableItemKind.kanji,
            repetitionCount = 4,
            isLearned = true
        )
        srs.updateProgress(guruItem, correct = false, nowMillis = fixedNowMillis, zoneId = fixedZone)
        assertEquals(3, guruItem.repetitionCount)
        assertTrue(guruItem.needsReview)
        assertEquals(3, guruItem.intervalDays)
        assertEquals(fixedNowMillis + 72 * 3_600_000L, guruItem.dueDate)
    }

    @Test
    fun `wrong answer above guru (level greater than 4) drops 2 levels`() {
        // Seviye 5 (Master) -> Seviye 3
        val masterItem = LearningItemProgressEntity(
            itemID = "水",
            itemKind = LearnableItemKind.kanji,
            repetitionCount = 5,
            isLearned = true
        )
        srs.updateProgress(masterItem, correct = false, nowMillis = fixedNowMillis, zoneId = fixedZone)
        assertEquals(3, masterItem.repetitionCount)
        assertTrue(masterItem.needsReview)

        // Seviye 6 (Enlightened) -> Seviye 4
        val enlightenedItem = LearningItemProgressEntity(
            itemID = "火",
            itemKind = LearnableItemKind.kanji,
            repetitionCount = 6,
            isLearned = true
        )
        srs.updateProgress(enlightenedItem, correct = false, nowMillis = fixedNowMillis, zoneId = fixedZone)
        assertEquals(4, enlightenedItem.repetitionCount)
        assertEquals(7, enlightenedItem.intervalDays)

        // Seviye 7 (Burned) -> Seviye 5
        val burnedItem = LearningItemProgressEntity(
            itemID = "木",
            itemKind = LearnableItemKind.kanji,
            repetitionCount = 7,
            isLearned = true
        )
        srs.updateProgress(burnedItem, correct = false, nowMillis = fixedNowMillis, zoneId = fixedZone)
        assertEquals(5, burnedItem.repetitionCount)
        assertEquals(14, burnedItem.intervalDays)
        assertTrue(burnedItem.needsReview)
    }
}
