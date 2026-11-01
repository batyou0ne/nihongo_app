package com.batuhankeskn0.kaimon

import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.local.LevelProgressDao
import com.batuhankeskn0.kaimon.data.local.UserLevelProgressEntity
import com.batuhankeskn0.kaimon.data.model.ExamMode
import com.batuhankeskn0.kaimon.data.model.ExamQuestionType
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.domain.engine.ExamGenerator
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Faz 3 — `LevelProgressionService`, `JLPTLevel`, `UserLevelProgressEntity` ve `ExamGenerator` birim testleri.
 */
class LevelProgressionTest {

    private val service = LevelProgressionService()

    @Test
    fun `JLPTLevel hierarchy, thresholds and targetCounts match iOS specification`() {
        assertEquals(listOf(JLPTLevel.N5, JLPTLevel.N4, JLPTLevel.N3, JLPTLevel.N2, JLPTLevel.N1), JLPTLevel.allCases)

        assertTrue(JLPTLevel.N5 < JLPTLevel.N4)
        assertTrue(JLPTLevel.N4 < JLPTLevel.N3)
        assertTrue(JLPTLevel.N3 < JLPTLevel.N2)
        assertTrue(JLPTLevel.N2 < JLPTLevel.N1)

        assertNull(JLPTLevel.N5.previousLevel)
        assertEquals(JLPTLevel.N4, JLPTLevel.N5.nextLevel)
        assertEquals(JLPTLevel.N5, JLPTLevel.N4.previousLevel)
        assertNull(JLPTLevel.N1.nextLevel)

        for (level in JLPTLevel.allCases) {
            assertEquals(0.85, level.completionThreshold, 0.0001)
        }

        assertEquals(80, JLPTLevel.N5.targetCounts.kanji)
        assertEquals(675, JLPTLevel.N5.targetCounts.vocab)
        assertEquals(85, JLPTLevel.N5.targetCounts.grammar)
    }

    @Test
    fun `ensureInitialProgress unlocks N5 and locks N4 to N1`() = runTest {
        val dao = FakeLevelProgressDao()
        val initial = service.ensureInitialProgress(dao)

        assertEquals(5, initial.size)
        assertTrue(service.isLevelUnlocked(JLPTLevel.N5, dao))
        assertFalse(service.isLevelUnlocked(JLPTLevel.N4, dao))
        assertFalse(service.isLevelUnlocked(JLPTLevel.N3, dao))
        assertFalse(service.isLevelUnlocked(JLPTLevel.N2, dao))
        assertFalse(service.isLevelUnlocked(JLPTLevel.N1, dao))
    }

    @Test
    fun `intra-level part locking requires previous part completion`() = runTest {
        val dao = FakeLevelProgressDao()
        service.ensureInitialProgress(dao)

        // N5 açık olduğu için Part 0 açık, Part 1 kilitli başlamalı
        assertTrue(service.isPartUnlocked(module = "kanji", level = JLPTLevel.N5, partIndex = 0, dao = dao))
        assertFalse(service.isPartUnlocked(module = "kanji", level = JLPTLevel.N5, partIndex = 1, dao = dao))

        // N4 kilitli olduğu için Part 0 dahil tüm parçalar kilitli olmalı
        assertFalse(service.isPartUnlocked(module = "kanji", level = JLPTLevel.N4, partIndex = 0, dao = dao))

        // N5 Kanji Part 0 tamamlandığında Part 1 açılmalı, Part 2 hâlâ kilitli kalmalı
        service.markPartCompleted(module = "kanji", level = JLPTLevel.N5, partIndex = 0, dao = dao)
        assertTrue(service.isPartUnlocked(module = "kanji", level = JLPTLevel.N5, partIndex = 1, dao = dao))
        assertFalse(service.isPartUnlocked(module = "kanji", level = JLPTLevel.N5, partIndex = 2, dao = dao))
    }

    @Test
    fun `calculateLevelProgress and canTakeGatewayExam enforce 85 percent threshold`() = runTest {
        val dao = FakeLevelProgressDao()
        service.ensureInitialProgress(dao)

        val kanjiPartsCount = ContentStore.kanjiParts("N5").size
        val vocabPartsCount = ContentStore.vocabularyParts("N5").size
        val grammarUnitsCount = ContentStore.loadGrammarSyllabus("N5").size

        // Başlangıçta ilerleme %0 -> Kapı Sınavına giremez
        val initialProgress = service.calculateLevelProgress(JLPTLevel.N5, dao)
        assertEquals(0.0, initialProgress.overall, 0.0001)
        assertFalse(service.canTakeGatewayExam(JLPTLevel.N5, dao))

        // Tüm parçaları tamamla (%100 >= %85)
        repeat(kanjiPartsCount) { idx ->
            service.markPartCompleted("kanji", JLPTLevel.N5, idx, dao)
        }
        repeat(vocabPartsCount) { idx ->
            service.markPartCompleted("vocab", JLPTLevel.N5, idx, dao)
        }
        repeat(grammarUnitsCount) { idx ->
            service.markPartCompleted("grammar", JLPTLevel.N5, idx, dao)
        }

        val fullProgress = service.calculateLevelProgress(JLPTLevel.N5, dao)
        assertEquals(1.0, fullProgress.kanji, 0.0001)
        assertEquals(1.0, fullProgress.vocab, 0.0001)
        assertEquals(1.0, fullProgress.grammar, 0.0001)
        assertEquals(1.0, fullProgress.overall, 0.0001)
        assertTrue(service.canTakeGatewayExam(JLPTLevel.N5, dao))
    }

    @Test
    fun `unlockNextLevel marks current level completed and unlocks next level`() = runTest {
        val dao = FakeLevelProgressDao()
        service.ensureInitialProgress(dao)

        service.unlockNextLevel(
            afterCompleting = JLPTLevel.N5,
            score = 0.90,
            isTestOut = true,
            dao = dao,
            nowMillis = 1_700_000_000_000L
        )

        val n5 = service.getProgress(JLPTLevel.N5, dao)
        assertTrue(n5.isCompleted)
        assertTrue(n5.hasPassedGatewayExam)
        assertEquals(0.90, n5.gatewayExamScore ?: 0.0, 0.0001)
        assertNotNull(n5.gatewayExamDate)
        assertTrue(n5.isSkippedViaPlacement)

        assertTrue("N5 geçilince N4 kilidi açılmalı", service.isLevelUnlocked(JLPTLevel.N4, dao))
        assertFalse("N3 hâlâ kilitli olmalı", service.isLevelUnlocked(JLPTLevel.N3, dao))
    }

    @Test
    fun `ExamGenerator builds 30 mixed questions with 8 Kanji, 12 Vocab and 10 Grammar`() {
        val questions = ExamGenerator.generateExam(level = JLPTLevel.N5, mode = ExamMode.GATEWAY)

        assertEquals(30, questions.size)
        assertEquals(8, questions.count { it.type == ExamQuestionType.KANJI })
        assertEquals(12, questions.count { it.type == ExamQuestionType.VOCABULARY })
        assertEquals(10, questions.count { it.type == ExamQuestionType.GRAMMAR })

        questions.forEach { q ->
            assertEquals("Her sınav sorusunda 4 şık bulunmalı", 4, q.options.size)
            assertTrue("Doğru cevap şıklar arasında yer almalı", q.options.contains(q.correctAnswer))
        }
    }
}

private class FakeLevelProgressDao : LevelProgressDao {
    private val map = mutableMapOf<String, UserLevelProgressEntity>()

    override fun observeAllLevelProgress(): Flow<List<UserLevelProgressEntity>> =
        MutableStateFlow(map.values.toList())

    override suspend fun getAllLevelProgress(): List<UserLevelProgressEntity> =
        map.values.toList()

    override fun observeLevelProgress(levelRaw: String): Flow<UserLevelProgressEntity?> =
        MutableStateFlow(map[levelRaw])

    override suspend fun getLevelProgress(levelRaw: String): UserLevelProgressEntity? =
        map[levelRaw]

    override suspend fun upsertLevelProgress(progress: UserLevelProgressEntity) {
        map[progress.levelRaw] = progress
    }

    override suspend fun upsertAllLevelProgress(items: List<UserLevelProgressEntity>) {
        items.forEach { map[it.levelRaw] = it }
    }

    override suspend fun clearAllLevelProgress() {
        map.clear()
    }
}
