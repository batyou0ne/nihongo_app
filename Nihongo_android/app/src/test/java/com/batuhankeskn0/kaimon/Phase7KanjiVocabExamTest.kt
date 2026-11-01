package com.batuhankeskn0.kaimon

import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.local.LearningItemProgressEntity
import com.batuhankeskn0.kaimon.data.local.LevelProgressDao
import com.batuhankeskn0.kaimon.data.local.UserLevelProgressEntity
import com.batuhankeskn0.kaimon.data.model.ExamMode
import com.batuhankeskn0.kaimon.data.model.ExamQuestionType
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.AppLanguage
import com.batuhankeskn0.kaimon.data.service.DailyQuota
import com.batuhankeskn0.kaimon.data.service.FeatureGate
import com.batuhankeskn0.kaimon.data.service.FreeTierLimits
import com.batuhankeskn0.kaimon.data.service.InMemoryQuotaStorage
import com.batuhankeskn0.kaimon.data.service.LanguageManager
import com.batuhankeskn0.kaimon.data.service.ProFeature
import com.batuhankeskn0.kaimon.domain.engine.ExamGenerator
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import com.batuhankeskn0.kaimon.ui.screens.kanji.buildKanjiPartStatuses
import com.batuhankeskn0.kaimon.ui.screens.kanji.computeKanjiPartLearnedRatio
import com.batuhankeskn0.kaimon.ui.screens.kanji.markKanjiPartCompleted
import com.batuhankeskn0.kaimon.ui.screens.quiz.GatewayExamController
import com.batuhankeskn0.kaimon.ui.screens.vocabulary.buildVocabPartStatuses
import com.batuhankeskn0.kaimon.ui.screens.vocabulary.computeVocabPartLearnedRatio
import com.batuhankeskn0.kaimon.ui.screens.vocabulary.findCurrentVocabPartIndex
import com.batuhankeskn0.kaimon.ui.screens.vocabulary.markVocabPartCompleted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.TimeZone

/**
 * Faz 7 — Özellik Kapısı (`FeatureGate`), Kanji Modülü (`KanjiLevelSelectionScreen` / `KanjiPartSelectionScreen`),
 * Kelime Modülü (`VocabularyMainScreen` / `VocabularyPartSelectionScreen`) ve
 * Seviye Kapı / Test-Out Sınavları (`GatewayExamScreen` / `GatewayExamController`) Birim Testleri.
 */
class Phase7KanjiVocabExamTest {

    private class FakeLevelProgressDao : LevelProgressDao {
        private val records = LinkedHashMap<String, UserLevelProgressEntity>()
        private val flow = MutableStateFlow<List<UserLevelProgressEntity>>(emptyList())

        override fun observeAllLevelProgress(): Flow<List<UserLevelProgressEntity>> = flow

        override suspend fun getAllLevelProgress(): List<UserLevelProgressEntity> =
            records.values.map { it.copy() }

        override fun observeLevelProgress(levelRaw: String): Flow<UserLevelProgressEntity?> =
            flow.map { list -> list.firstOrNull { it.levelRaw == levelRaw } }

        override suspend fun getLevelProgress(levelRaw: String): UserLevelProgressEntity? =
            records[levelRaw]?.copy()

        override suspend fun upsertLevelProgress(progress: UserLevelProgressEntity) {
            records[progress.levelRaw] = progress.copy()
            flow.value = getAllLevelProgress()
        }

        override suspend fun upsertAllLevelProgress(items: List<UserLevelProgressEntity>) {
            for (item in items) {
                records[item.levelRaw] = item.copy()
            }
            flow.value = getAllLevelProgress()
        }

        override suspend fun clearAllLevelProgress() {
            records.clear()
            flow.value = emptyList()
        }
    }

    @Before
    fun setUp() {
        LanguageManager.current = AppLanguage.TURKISH
    }

    // MARK: - 1. FeatureGate & DailyQuota Testleri

    @Test
    fun `FreeTierLimits matches iOS specification exactly`() {
        assertEquals(3, FreeTierLimits.dailyAIMessages)
        assertEquals(2, FreeTierLimits.freeUnitsPerModule)
        assertEquals(1, FreeTierLimits.dailyExams)
    }

    @Test
    fun `DailyQuota tracks usage grants bonus and automatically resets on day rollover`() {
        val storage = InMemoryQuotaStorage()
        // 2026-11-01 14:00:00 UTC
        var currentMillis = 1_793_541_600_000L
        val utc = TimeZone.getTimeZone("UTC")

        val quota = DailyQuota(
            key = "ai",
            limit = FreeTierLimits.dailyAIMessages,
            storage = storage,
            nowMillisProvider = { currentMillis },
            timeZoneProvider = { utc }
        )

        assertEquals(0, quota.used)
        assertEquals(0, quota.bonus)
        assertEquals(3, quota.remaining)

        quota.consume()
        quota.consume()
        assertEquals(2, quota.used)
        assertEquals(1, quota.remaining)

        // Ödüllü reklam bonusu (+2)
        quota.grantBonus(2)
        assertEquals(2, quota.bonus)
        assertEquals(3, quota.remaining) // 3 + 2 - 2 = 3

        // Ertesi güne geçiş (+24 saat) -> otomatik sıfırlama
        currentMillis += 24L * 60L * 60L * 1000L
        assertEquals(0, quota.used)
        assertEquals(0, quota.bonus)
        assertEquals(3, quota.remaining)
    }

    @Test
    fun `FeatureGate enforces unit availability AI quota and exam quota for free vs Pro users`() {
        val storage = InMemoryQuotaStorage()
        var isPro = false
        val gate = FeatureGate(
            storage = storage,
            isProProvider = { isPro }
        )

        // Free tier: ilk 2 ünite (index 0, 1) açık, index 2+ Pro kilitli
        assertFalse(gate.isUnlocked(ProFeature.LOCKED_CONTENT))
        assertTrue(gate.isUnitAvailable(0))
        assertTrue(gate.isUnitAvailable(1))
        assertFalse(gate.isUnitAvailable(2))
        assertFalse(gate.isUnitAvailable(3))

        // AI mesaj kotası (günlük 3)
        assertEquals(3, gate.remainingAIMessages)
        assertTrue(gate.canSendAIMessage)
        gate.recordAIMessage()
        gate.recordAIMessage()
        gate.recordAIMessage()
        assertEquals(0, gate.remainingAIMessages)
        assertFalse(gate.canSendAIMessage)

        gate.grantAIBonus(1)
        assertEquals(1, gate.remainingAIMessages)
        assertTrue(gate.canSendAIMessage)

        // Sınav kotası (günlük 1)
        assertEquals(1, gate.remainingExams)
        assertTrue(gate.canStartExam)
        gate.recordExam()
        assertEquals(0, gate.remainingExams)
        assertFalse(gate.canStartExam)

        // Pro'ya yükseltildiğinde tüm kilitler ve kotalar kalkar
        isPro = true
        assertTrue(gate.isUnlocked(ProFeature.UNLIMITED_AI_CHAT))
        assertTrue(gate.isUnlocked(ProFeature.LOCKED_CONTENT))
        assertTrue(gate.isUnlocked(ProFeature.UNLIMITED_EXAMS))
        assertTrue(gate.isUnlocked(ProFeature.AD_FREE))
        assertTrue(gate.isUnitAvailable(2))
        assertTrue(gate.isUnitAvailable(10))
        assertNull(gate.remainingAIMessages)
        assertTrue(gate.canSendAIMessage)
        assertNull(gate.remainingExams)
        assertTrue(gate.canStartExam)
    }

    // MARK: - 2. Kanji Modülü Testleri

    @Test
    fun `Kanji parts calculate learned percentage accurately and enforce part and Pro locks`() = runBlocking {
        val dao = FakeLevelProgressDao()
        val progression = LevelProgressionService()
        progression.ensureInitialProgress(dao)

        val freeGate = FeatureGate(storage = InMemoryQuotaStorage(), isProProvider = { false })
        val proGate = FeatureGate(storage = InMemoryQuotaStorage(), isProProvider = { true })

        val parts = ContentStore.kanjiParts("N5", partCount = 4)
        assertEquals(4, parts.size)

        val part0 = parts[0]
        // Part 0 içindeki kanjilerin yarısını öğrenilmiş işaretle
        val halfCount = part0.size / 2
        val progressMap = part0.take(halfCount).associate { kanji ->
            kanji.id to LearningItemProgressEntity(
                itemID = kanji.id,
                itemKind = LearnableItemKind.KANJI,
                repetitionCount = 1,
                isLearned = true
            )
        }

        val expectedRatio = halfCount.toDouble() / part0.size.toDouble()
        assertEquals(expectedRatio, computeKanjiPartLearnedRatio(part0, progressMap, isPartCompleted = false), 0.001)

        val initialLevelProgress = progression.getProgress(JLPTLevel.N5, dao)
        val statusesFree = buildKanjiPartStatuses(
            level = JLPTLevel.N5,
            parts = parts,
            levelProgress = initialLevelProgress,
            progressByID = progressMap,
            featureGate = freeGate,
            progressionService = progression
        )

        // Başlangıçta sadece Part 0 açık; Part 1 önceki bölüm bitmediği için kilitli; Part 2 ve 3 Pro kilitli
        assertTrue(statusesFree[0].isUnlocked)
        assertFalse(statusesFree[0].isProLocked)
        assertEquals(halfCount, statusesFree[0].learnedCount)

        assertFalse(statusesFree[1].isUnlocked)
        assertFalse(statusesFree[1].isProLocked)

        assertTrue(statusesFree[2].isProLocked)
        assertTrue(statusesFree[3].isProLocked)

        // Part 0 tamamlandığında `markKanjiPartCompleted` -> Part 1 kilidi açılır!
        val updatedProgress = markKanjiPartCompleted(
            level = JLPTLevel.N5,
            partIndex = 0,
            progressionService = progression,
            dao = dao
        )
        assertTrue(updatedProgress.isPartCompleted("kanji", 0))

        val statusesAfterPart0 = buildKanjiPartStatuses(
            level = JLPTLevel.N5,
            parts = parts,
            levelProgress = updatedProgress,
            progressByID = progressMap,
            featureGate = proGate,
            progressionService = progression
        )
        assertTrue(statusesAfterPart0[0].isCompleted)
        assertEquals(1.0, statusesAfterPart0[0].learnedRatio, 0.001)
        assertTrue(statusesAfterPart0[1].isUnlocked)
        assertFalse(statusesAfterPart0[2].isProLocked)
    }

    // MARK: - 3. Kelime (Vocabulary) Modülü Testleri

    @Test
    fun `Vocabulary parts list 25-word groups track currentPartIndex and mark vocab part completed`() = runBlocking {
        val dao = FakeLevelProgressDao()
        val progression = LevelProgressionService()
        progression.ensureInitialProgress(dao)

        val parts = ContentStore.vocabularyParts("N5", partSize = 25)
        assertTrue(parts.isNotEmpty())
        assertEquals(25, parts.first().size)

        var levelProgress = progression.getProgress(JLPTLevel.N5, dao)
        assertEquals(0, findCurrentVocabPartIndex(parts.size, levelProgress))

        // İlk parçadaki 10 kelimeyi öğrenilmiş işaretle
        val part0 = parts[0]
        val vocabProgressMap = part0.take(10).associate { word ->
            word.id to LearningItemProgressEntity(
                itemID = word.id,
                itemKind = LearnableItemKind.VOCABULARY_WORD,
                repetitionCount = 2,
                isLearned = true
            )
        }
        assertEquals(10.0 / 25.0, computeVocabPartLearnedRatio(part0, vocabProgressMap, isPartCompleted = false), 0.001)

        // Part 0 tamamlandığında `markVocabPartCompleted` -> `currentPartIndex` 1 olur ve Part 1 açılır
        levelProgress = markVocabPartCompleted(
            level = JLPTLevel.N5,
            partIndex = 0,
            progressionService = progression,
            dao = dao
        )
        assertEquals(1, findCurrentVocabPartIndex(parts.size, levelProgress))

        val proGate = FeatureGate(storage = InMemoryQuotaStorage(), isProProvider = { true })
        val statuses = buildVocabPartStatuses(
            level = JLPTLevel.N5,
            parts = parts,
            levelProgress = levelProgress,
            progressByID = vocabProgressMap,
            featureGate = proGate,
            progressionService = progression
        )

        assertTrue(statuses[0].isCompleted)
        assertEquals(100, statuses[0].learnedPercent)
        assertTrue(statuses[1].isUnlocked)
        assertFalse(statuses[2].isUnlocked)
    }

    // MARK: - 4. Seviye Kapı Sınavı (Gateway Exam) & Test-Out Challenge Testleri

    @Test
    fun `ExamGenerator generates 30 mixed questions (8 Kanji + 12 Vocab + 10 Grammar) for N5`() {
        val questions = ExamGenerator.generateExam(level = JLPTLevel.N5, mode = ExamMode.GATEWAY)
        assertEquals(30, questions.size)

        val kanjiCount = questions.count { it.type == ExamQuestionType.KANJI }
        val vocabCount = questions.count { it.type == ExamQuestionType.VOCABULARY }
        val grammarCount = questions.count { it.type == ExamQuestionType.GRAMMAR }

        assertEquals(8, kanjiCount)
        assertEquals(12, vocabCount)
        assertEquals(10, grammarCount)

        questions.forEach { q ->
            assertEquals(4, q.options.size)
            assertTrue(q.options.contains(q.correctAnswer))
        }
    }

    @Test
    fun `GatewayExamController in GATEWAY mode passes at 80 percent and unlocks next JLPT level`() = runBlocking {
        val dao = FakeLevelProgressDao()
        val progression = LevelProgressionService()
        progression.ensureInitialProgress(dao)

        val questions = ExamGenerator.generateExam(level = JLPTLevel.N5, mode = ExamMode.GATEWAY)
        assertEquals(30, questions.size)

        var unlockedNext = false
        var awardedXp = 0

        val controller = GatewayExamController(
            level = JLPTLevel.N5,
            mode = ExamMode.GATEWAY,
            initialQuestions = questions,
            triggerFeedback = false,
            onExamPassed = { completedLevel, scoreRatio, isTestOut, xp ->
                runBlocking {
                    progression.unlockNextLevel(
                        afterCompleting = completedLevel,
                        score = scoreRatio,
                        isTestOut = isTestOut,
                        dao = dao
                    )
                    unlockedNext = progression.isLevelUnlocked(JLPTLevel.N4, dao)
                    awardedXp = xp
                }
            }
        )

        // 30 sorunun 24'ünü doğru (%80), 6'sını yanlış cevapla
        for (i in questions.indices) {
            val q = controller.currentQuestion!!
            if (i < 24) {
                assertTrue(controller.submitAnswer(q.correctAnswer))
            } else {
                val wrongOption = q.options.first { it != q.correctAnswer }
                assertFalse(controller.submitAnswer(wrongOption))
            }
            controller.advanceAfterAnswer()
        }

        assertTrue(controller.isPassed)
        assertFalse(controller.isFailed)
        assertTrue(controller.showLessonComplete)
        assertEquals(0.80, controller.scoreRatio, 0.001)
        assertEquals(250, awardedXp)
        assertTrue(unlockedNext)
    }

    @Test
    fun `GatewayExamController in GATEWAY mode fails when score is below 80 percent`() {
        val questions = ExamGenerator.generateExam(level = JLPTLevel.N5, mode = ExamMode.GATEWAY)
        val controller = GatewayExamController(
            level = JLPTLevel.N5,
            mode = ExamMode.GATEWAY,
            initialQuestions = questions,
            triggerFeedback = false
        )

        // 23 doğru / 30 soru (%76.6 < %80)
        for (i in questions.indices) {
            val q = controller.currentQuestion!!
            if (i < 23) {
                controller.submitAnswer(q.correctAnswer)
            } else {
                val wrongOption = q.options.first { it != q.correctAnswer }
                controller.submitAnswer(wrongOption)
            }
            controller.advanceAfterAnswer()
        }

        assertTrue(controller.isFailed)
        assertFalse(controller.isPassed)
        assertFalse(controller.showLessonComplete)
    }

    @Test
    fun `GatewayExamController in TEST_OUT mode ends immediately on 3rd wrong answer (0 hearts)`() {
        val questions = ExamGenerator.generateExam(level = JLPTLevel.N5, mode = ExamMode.TEST_OUT)
        val controller = GatewayExamController(
            level = JLPTLevel.N5,
            mode = ExamMode.TEST_OUT,
            initialQuestions = questions,
            triggerFeedback = false
        )

        assertEquals(3, controller.hearts)

        // 1. yanlış -> 2 can kalır
        val q0 = controller.currentQuestion!!
        controller.submitAnswer(q0.options.first { it != q0.correctAnswer })
        assertEquals(2, controller.hearts)
        controller.advanceAfterAnswer()
        assertFalse(controller.isFailed)
        assertEquals(1, controller.currentIndex)

        // 2. yanlış -> 1 can kalır
        val q1 = controller.currentQuestion!!
        controller.submitAnswer(q1.options.first { it != q1.correctAnswer })
        assertEquals(1, controller.hearts)
        controller.advanceAfterAnswer()
        assertFalse(controller.isFailed)
        assertEquals(2, controller.currentIndex)

        // 3. yanlış -> 0 can kalır ve sınav 3. soruda derhal biter!
        val q2 = controller.currentQuestion!!
        controller.submitAnswer(q2.options.first { it != q2.correctAnswer })
        assertEquals(0, controller.hearts)
        controller.advanceAfterAnswer()

        assertTrue(controller.isFailed)
        assertFalse(controller.isPassed)
        assertEquals(2, controller.currentIndex) // 4. soruya geçmeden durdu
    }

    @Test
    fun `GatewayExamController in TEST_OUT mode passes when completing exam with at least 1 heart left and unlocks next level`() = runBlocking {
        val dao = FakeLevelProgressDao()
        val progression = LevelProgressionService()
        progression.ensureInitialProgress(dao)

        val questions = ExamGenerator.generateExam(level = JLPTLevel.N5, mode = ExamMode.TEST_OUT)
        var awardedXp = 0

        val controller = GatewayExamController(
            level = JLPTLevel.N5,
            mode = ExamMode.TEST_OUT,
            initialQuestions = questions,
            triggerFeedback = false,
            onExamPassed = { completedLevel, scoreRatio, isTestOut, xp ->
                runBlocking {
                    progression.unlockNextLevel(
                        afterCompleting = completedLevel,
                        score = scoreRatio,
                        isTestOut = isTestOut,
                        dao = dao
                    )
                    awardedXp = xp
                }
            }
        )

        // İlk 2 soruyu yanlış yap (1 can kalır), kalan 28 soruyu doğru yap
        for (i in questions.indices) {
            val q = controller.currentQuestion!!
            if (i < 2) {
                controller.submitAnswer(q.options.first { it != q.correctAnswer })
            } else {
                controller.submitAnswer(q.correctAnswer)
            }
            controller.advanceAfterAnswer()
        }

        assertEquals(1, controller.hearts)
        assertTrue(controller.isPassed)
        assertFalse(controller.isFailed)
        assertTrue(controller.showLessonComplete)
        assertEquals(350, awardedXp)

        val n5Record = progression.getProgress(JLPTLevel.N5, dao)
        assertTrue(n5Record.isCompleted)
        assertTrue(n5Record.hasPassedGatewayExam)
        assertTrue(n5Record.isSkippedViaPlacement)
        assertTrue(progression.isLevelUnlocked(JLPTLevel.N4, dao))
        assertNotNull(n5Record.gatewayExamDate)
    }
}
