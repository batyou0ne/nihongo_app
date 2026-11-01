package com.batuhankeskn0.kaimon

import com.batuhankeskn0.kaimon.data.local.ActivityDao
import com.batuhankeskn0.kaimon.data.local.DailyActivityEntity
import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.local.LearningItemProgressEntity
import com.batuhankeskn0.kaimon.data.local.LevelProgressDao
import com.batuhankeskn0.kaimon.data.local.ProgressDao
import com.batuhankeskn0.kaimon.data.local.UserLevelProgressEntity
import com.batuhankeskn0.kaimon.data.local.UserProgressEntity
import com.batuhankeskn0.kaimon.data.model.GrammarCategory
import com.batuhankeskn0.kaimon.data.model.GrammarExample
import com.batuhankeskn0.kaimon.data.model.GrammarPoint
import com.batuhankeskn0.kaimon.data.model.GrammarQuestion
import com.batuhankeskn0.kaimon.data.model.GrammarQuestionKind
import com.batuhankeskn0.kaimon.data.model.StoryType
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.AppLanguage
import com.batuhankeskn0.kaimon.data.service.FeatureGate
import com.batuhankeskn0.kaimon.data.service.InMemoryQuotaStorage
import com.batuhankeskn0.kaimon.data.service.LanguageManager
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import com.batuhankeskn0.kaimon.domain.engine.SpacedRepetitionService
import com.batuhankeskn0.kaimon.domain.engine.VerbConjugationEngine
import com.batuhankeskn0.kaimon.domain.engine.VerbType
import com.batuhankeskn0.kaimon.domain.engine.XPManager
import com.batuhankeskn0.kaimon.ui.screens.dictionary.DictionaryViewModel
import com.batuhankeskn0.kaimon.ui.screens.grammar.GrammarPracticeController
import com.batuhankeskn0.kaimon.ui.screens.grammar.buildGrammarUnitStatuses
import com.batuhankeskn0.kaimon.ui.screens.grammar.isGrammarUnitFinished
import com.batuhankeskn0.kaimon.ui.screens.grammar.markGrammarUnitCompleted
import com.batuhankeskn0.kaimon.ui.screens.grammar.recordGrammarPointCompletion
import com.batuhankeskn0.kaimon.ui.screens.grammar.selectGrammarSessionPoints
import com.batuhankeskn0.kaimon.ui.screens.grammar.syncGrammarUnitsCompletion
import com.batuhankeskn0.kaimon.ui.screens.reading.extractUniqueStoryVocabulary
import com.batuhankeskn0.kaimon.ui.screens.reading.groupStoriesByType
import com.batuhankeskn0.kaimon.ui.screens.reading.recordStoryCompleted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Faz 8 — Gramer Modülü (`GrammarMainScreen`, `GrammarPartSelectionScreen`, `GrammarSessionScreen`,
 * `GrammarLessonScreen`, `GrammarPracticeScreen`), Fiil Çekimleyici (`VerbConjugationScreen`),
 * Referans Kütüphanesi (`GrammarReferenceScreen`, `GrammarCategoryListScreen`, `VerbCategoryListScreen`)
 * ve Okuma / Hikaye Modülü (`ReadingMainScreen`, `StoryReaderScreen`) Birim Testleri.
 */
class Phase8GrammarReadingReferenceTest {

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

    private class FakeProgressDao : ProgressDao {
        private var userProgress: UserProgressEntity? = null
        private val items = LinkedHashMap<String, LearningItemProgressEntity>()

        override fun observeAllItemProgress(): Flow<List<LearningItemProgressEntity>> =
            MutableStateFlow(items.values.toList())

        override suspend fun getAllItemProgress(): List<LearningItemProgressEntity> =
            items.values.toList()

        override fun observeItemProgress(itemID: String): Flow<LearningItemProgressEntity?> =
            MutableStateFlow(items[itemID])

        override suspend fun getItemProgress(itemID: String): LearningItemProgressEntity? =
            items[itemID]

        override fun observeProgressByKind(kind: LearnableItemKind): Flow<List<LearningItemProgressEntity>> =
            MutableStateFlow(items.values.filter { it.itemKind == kind })

        override suspend fun getProgressByKind(kind: LearnableItemKind): List<LearningItemProgressEntity> =
            items.values.filter { it.itemKind == kind }

        override fun observeReviewItems(nowMillis: Long): Flow<List<LearningItemProgressEntity>> =
            MutableStateFlow(getReviewItemsSync(nowMillis))

        override suspend fun getReviewItems(nowMillis: Long): List<LearningItemProgressEntity> =
            getReviewItemsSync(nowMillis)

        private fun getReviewItemsSync(nowMillis: Long): List<LearningItemProgressEntity> =
            items.values.filter { it.needsReview || (it.isLearned && it.dueDate <= nowMillis) }

        override fun observeLearnedCount(kind: LearnableItemKind): Flow<Int> =
            MutableStateFlow(items.values.count { it.itemKind == kind && it.repetitionCount >= 1 })

        override suspend fun getLearnedCount(kind: LearnableItemKind): Int =
            items.values.count { it.itemKind == kind && it.repetitionCount >= 1 }

        override suspend fun upsertItemProgress(progress: LearningItemProgressEntity) {
            items[progress.itemID] = progress
        }

        override suspend fun upsertAllItemProgress(items: List<LearningItemProgressEntity>) {
            items.forEach { this.items[it.itemID] = it }
        }

        override suspend fun deleteItemProgress(progress: LearningItemProgressEntity) {
            items.remove(progress.itemID)
        }

        override suspend fun clearAllItemProgress() {
            items.clear()
        }

        override fun observeUserProgress(): Flow<UserProgressEntity?> =
            MutableStateFlow(userProgress)

        override suspend fun getUserProgress(): UserProgressEntity? = userProgress

        override suspend fun upsertUserProgress(userProgress: UserProgressEntity) {
            this.userProgress = userProgress
        }

        override suspend fun clearUserProgress() {
            userProgress = null
        }
    }

    private class FakeActivityDao : ActivityDao {
        private val activities = LinkedHashMap<String, DailyActivityEntity>()

        override fun observeAllActivities(): Flow<List<DailyActivityEntity>> =
            MutableStateFlow(activities.values.toList())

        override suspend fun getAllActivities(): List<DailyActivityEntity> =
            activities.values.toList()

        override fun observeActivityByDate(dateString: String): Flow<DailyActivityEntity?> =
            MutableStateFlow(activities[dateString])

        override suspend fun getActivityByDate(dateString: String): DailyActivityEntity? =
            activities[dateString]

        override fun observeRecentActivities(limit: Int): Flow<List<DailyActivityEntity>> =
            MutableStateFlow(activities.values.take(limit))

        override suspend fun getRecentActivities(limit: Int): List<DailyActivityEntity> =
            activities.values.take(limit)

        override suspend fun upsertActivity(activity: DailyActivityEntity) {
            activities[activity.dateString] = activity
        }

        override suspend fun upsertAllActivities(activities: List<DailyActivityEntity>) {
            activities.forEach { this.activities[it.dateString] = it }
        }

        override suspend fun clearAllActivities() {
            activities.clear()
        }
    }

    @Before
    fun setUp() {
        LanguageManager.current = AppLanguage.TURKISH
    }

    // MARK: - 1. Gramer Ünite Akışı, Kilitler ve Oturum Seçimi

    @Test
    fun `buildGrammarUnitStatuses computes learned counts intra-level locks and Pro locks accurately`() = runBlocking {
        val levelDao = FakeLevelProgressDao()
        val progressDao = FakeProgressDao()
        val levelService = LevelProgressionService.shared
        var isPro = false
        val gate = FeatureGate(
            storage = InMemoryQuotaStorage(),
            isProProvider = { isPro }
        )

        val records = levelService.ensureInitialProgress(dao = levelDao)
        val n5Record = records.first { it.levelRaw == "N5" }
        val syllabus = ContentStore.loadGrammarSyllabus("N5")
        val allPoints = ContentStore.loadGrammar("N5")
        val pointsByKey = allPoints.associateBy { it.key }

        assertTrue(syllabus.isNotEmpty())
        assertTrue(allPoints.isNotEmpty())

        // İlk ünitedeki 1 konuyu öğrenilmiş işaretle
        val unit0Topic0 = syllabus[0].grammarKeys.first()
        val statusesInitial = buildGrammarUnitStatuses(
            level = JLPTLevel.N5,
            syllabus = syllabus,
            pointsByKey = pointsByKey,
            levelProgress = n5Record,
            learnedIDs = setOf(unit0Topic0),
            featureGate = gate,
            progressionService = levelService
        )

        assertEquals(syllabus.size, statusesInitial.size)
        // Unit 0: açık, Pro kilidi yok, 1 konu öğrenilmiş
        assertTrue(statusesInitial[0].isUnlocked)
        assertFalse(statusesInitial[0].isProLocked)
        assertEquals(1, statusesInitial[0].learnedCount)
        assertFalse(statusesInitial[0].isCompleted)

        // Unit 1: Unit 0 tamamlanmadığı için kilitli, index 1 olduğu için Pro kilidi yok
        assertFalse(statusesInitial[1].isUnlocked)
        assertFalse(statusesInitial[1].isProLocked)

        // Unit 2+: Free tier için Pro kilitli
        if (statusesInitial.size > 2) {
            assertFalse(statusesInitial[2].isUnlocked)
            assertTrue(statusesInitial[2].isProLocked)
        }

        // Unit 0 içindeki tüm konuları öğrenilmiş yap ve syncGrammarUnitsCompletion çalıştır
        val unit0LearnedIDs = syllabus[0].grammarKeys.toSet()
        for (id in unit0LearnedIDs) {
            progressDao.upsertItemProgress(
                LearningItemProgressEntity(
                    itemID = id,
                    itemKind = LearnableItemKind.GRAMMAR,
                    repetitionCount = 1,
                    isLearned = true
                )
            )
        }

        assertTrue(isGrammarUnitFinished(syllabus[0], pointsByKey, unit0LearnedIDs))

        val completedSet = syncGrammarUnitsCompletion(
            level = JLPTLevel.N5,
            syllabus = syllabus,
            pointsByKey = pointsByKey,
            progressDao = progressDao,
            levelProgressDao = levelDao,
            progressionService = levelService
        )
        assertTrue(completedSet.contains(0))

        val n5AfterUnit0 = levelDao.getLevelProgress("N5")
        val statusesAfterUnit0 = buildGrammarUnitStatuses(
            level = JLPTLevel.N5,
            syllabus = syllabus,
            pointsByKey = pointsByKey,
            levelProgress = n5AfterUnit0,
            learnedIDs = unit0LearnedIDs,
            featureGate = gate,
            progressionService = levelService
        )

        assertTrue(statusesAfterUnit0[0].isCompleted)
        assertTrue(statusesAfterUnit0[1].isUnlocked)
        assertFalse(statusesAfterUnit0[1].isProLocked)

        // Unit 1 de tamamlandığında Pro açıksa Unit 2 açılır
        isPro = true
        val n5AfterUnit1 = markGrammarUnitCompleted(
            level = JLPTLevel.N5,
            unitIndex = 1,
            progressionService = levelService,
            dao = levelDao
        )
        val statusesPro = buildGrammarUnitStatuses(
            level = JLPTLevel.N5,
            syllabus = syllabus,
            pointsByKey = pointsByKey,
            levelProgress = n5AfterUnit1,
            learnedIDs = unit0LearnedIDs,
            featureGate = gate,
            progressionService = levelService
        )
        if (statusesPro.size > 2) {
            assertTrue(statusesPro[2].isUnlocked)
            assertFalse(statusesPro[2].isProLocked)
        }
    }

    @Test
    fun `selectGrammarSessionPoints returns up to 3 unlearned grammar points`() {
        val allPoints = ContentStore.loadGrammar("N5")

        // Hiçbiri öğrenilmemişken en fazla 3 konu döner
        val initialBatch = selectGrammarSessionPoints(
            allPoints = allPoints,
            learnedIDs = emptySet(),
            maxCount = 3
        )
        assertEquals(3, initialBatch.size)

        // İlk konuyu öğrenilmiş işaretleyince listeden çıkar
        val firstId = initialBatch.first().id
        val nextBatch = selectGrammarSessionPoints(
            allPoints = allPoints,
            learnedIDs = setOf(firstId),
            maxCount = 3
        )
        assertEquals(3, nextBatch.size)
        assertFalse(nextBatch.any { it.id == firstId })
    }

    // MARK: - 2. Gramer Pratik Soruları (fillBlank, wordOrder, multipleChoice) ve SRS + XP Kaydı

    @Test
    fun `GrammarPracticeController handles fillBlank multipleChoice and wordOrder questions accurately`() {
        val samplePoint = GrammarPoint(
            key = "test_wa_desu",
            pattern = "〜は〜です",
            romaji = "~ wa ~ desu",
            title = "A wa B desu",
            explanation = "Konu edatı ve bildirme eki",
            formula = "[İsim A] + は + [İsim B] + です",
            category = GrammarCategory.PARTICLE,
            difficulty = 1,
            examples = listOf(
                GrammarExample(
                    japanese = "わたしは学生です。",
                    hiragana = "わたしはがくせいです。",
                    romaji = "Watashi wa gakusei desu.",
                    turkish = "Ben öğrenciyim."
                )
            ),
            questions = listOf(
                GrammarQuestion(
                    kind = GrammarQuestionKind.FILL_BLANK,
                    prompt = "わたし＿＿学生です。",
                    hint = "Ben öğrenciyim.",
                    choices = listOf("は", "を", "に", "で"),
                    answer = "は"
                ),
                GrammarQuestion(
                    kind = GrammarQuestionKind.WORD_ORDER,
                    prompt = "Ben öğretmenim.",
                    hint = "Kelimeleri sıralayın",
                    choices = listOf("です", "先生", "は", "わたし"),
                    answer = "わたしは先生です",
                    answerTokens = listOf("わたし", "は", "先生", "です")
                ),
                GrammarQuestion(
                    kind = GrammarQuestionKind.MULTIPLE_CHOICE,
                    prompt = "「です」ne işe yarar?",
                    choices = listOf("Kibar bildirme eki", "Soru eki", "Geçmiş zaman", "Olumsuzluk"),
                    answer = "Kibar bildirme eki"
                )
            )
        )

        val controller = GrammarPracticeController(
            points = listOf(samplePoint),
            shuffleWordOrderChoices = false
        )
        assertEquals(3, controller.totalQuestions)
        assertEquals(0, controller.questionIndex)
        assertFalse(controller.isFinished)

        // Soru 1: FILL_BLANK -> Doğru cevap ("は")
        assertEquals(GrammarQuestionKind.FILL_BLANK, controller.currentQuestion?.kind)
        assertTrue(controller.selectChoice("は"))
        assertTrue(controller.hasAnswered)
        assertEquals(1, controller.totalAnswered)
        assertEquals(0, controller.totalWrong)
        assertTrue(controller.wrongSummary.isEmpty())

        controller.advance() // Soru 2'ye geç
        assertEquals(1, controller.questionIndex)

        // Soru 2: WORD_ORDER -> Kelime ekleme, geri çıkarma ve doğru sıralama kontrolü
        assertEquals(GrammarQuestionKind.WORD_ORDER, controller.currentQuestion?.kind)
        assertEquals(4, controller.orderPool.size)

        val firstToken = controller.orderPool.first() // "です"
        controller.selectPoolToken(firstToken.id)
        assertEquals(1, controller.orderAssembled.size)
        assertEquals(3, controller.orderPool.size)
        controller.removeAssembledToken(firstToken.id)
        assertTrue(controller.orderAssembled.isEmpty())
        assertEquals(4, controller.orderPool.size)

        // Doğru sıra: "わたし", "は", "先生", "です"
        for (targetText in listOf("わたし", "は", "先生", "です")) {
            val token = controller.orderPool.first { it.text == targetText }
            controller.selectPoolToken(token.id)
        }
        assertTrue(controller.canCheckOrder)
        assertTrue(controller.checkOrder())
        assertTrue(controller.hasAnswered)
        assertTrue(controller.isOrderCorrect)
        assertEquals(2, controller.totalAnswered)
        assertEquals(0, controller.totalWrong)

        controller.advance() // Soru 3'e geç
        assertEquals(2, controller.questionIndex)

        // Soru 3: MULTIPLE_CHOICE -> Yanlış şık seç ("Soru eki")
        assertEquals(GrammarQuestionKind.MULTIPLE_CHOICE, controller.currentQuestion?.kind)
        assertFalse(controller.selectChoice("Soru eki"))
        assertTrue(controller.hasAnswered)
        assertEquals(3, controller.totalAnswered)
        assertEquals(1, controller.totalWrong)
        assertEquals(1, controller.wrongSummary.size)
        assertEquals("Kibar bildirme eki", controller.wrongSummary.first().answer)

        // Son sorudan sonra advance() -> isFinished = true
        controller.advance()
        assertTrue(controller.isFinished)
    }

    @Test
    fun `recordGrammarPointCompletion updates SRS progress level unit completion and XP`() = runBlocking {
        val levelDao = FakeLevelProgressDao()
        val progressDao = FakeProgressDao()
        val activityDao = FakeActivityDao()

        val levelService = LevelProgressionService.shared
        val srsService = SpacedRepetitionService.shared
        val xpManager = XPManager.shared

        levelService.ensureInitialProgress(dao = levelDao)
        val syllabus = ContentStore.loadGrammarSyllabus("N5")
        val allPoints = ContentStore.loadGrammar("N5")
        val pointsByKey = allPoints.associateBy { it.key }
        val unit0 = syllabus.first()
        val unit0Points = unit0.grammarKeys.mapNotNull { pointsByKey[it] }
        assertTrue(unit0Points.isNotEmpty())

        // Ünitedeki tüm konuları hatasız (allQuestionsCorrect = true) tamamla
        var totalAwardedXp = 0
        for (point in unit0Points) {
            totalAwardedXp += recordGrammarPointCompletion(
                point = point,
                allQuestionsCorrect = true,
                progressDao = progressDao,
                activityDao = activityDao,
                srsService = srsService,
                xpManager = xpManager
            )
        }

        // Tüm konuların LearnableItemKind.GRAMMAR olarak SRS'e kaydedildiğini doğrula
        val learnedGrammar = progressDao.getProgressByKind(LearnableItemKind.GRAMMAR)
        assertEquals(unit0Points.size, learnedGrammar.size)
        assertTrue(learnedGrammar.all { it.isLearned && it.repetitionCount >= 1 })

        // syncGrammarUnitsCompletion ile 0. ünitenin tamamlandığını doğrula
        val completedIndices = syncGrammarUnitsCompletion(
            level = JLPTLevel.N5,
            syllabus = syllabus,
            pointsByKey = pointsByKey,
            progressDao = progressDao,
            levelProgressDao = levelDao,
            progressionService = levelService
        )
        assertTrue(completedIndices.contains(0))

        val n5Record = levelDao.getLevelProgress("N5")
        assertTrue(levelService.isPartUnlocked("grammar", JLPTLevel.N5, 1, n5Record))

        // XPManager üzerinde GRAMMAR_COMPLETED (20 XP) * konu sayısı eklendiğini doğrula
        val userProgress = progressDao.getUserProgress()
        assertNotNull(userProgress)
        val expectedXp = unit0Points.size * XPManager.XPAction.GRAMMAR_COMPLETED.points
        assertEquals(expectedXp, totalAwardedXp)
        assertEquals(expectedXp, userProgress?.totalXP)
    }

    // MARK: - 3. Fiil Çekimleyici (`VerbConjugationEngine.conjugateExtended`) & Referans Kütüphanesi

    @Test
    fun `VerbConjugationEngine conjugateExtended generates all 14 forms including Tai Volitional Imperative and Conditional`() {
        // 1. Ichidan: たべる / 食べる
        val taberu = VerbConjugationEngine.conjugateExtended(hiragana = "たべる", kanji = "食べる")
        assertEquals(VerbType.ICHIDAN, taberu.verbType)
        assertEquals(14, taberu.forms.size)

        val taberuMap = taberu.forms.associateBy { it.id }
        assertEquals("食べます", taberuMap["masu"]?.japanese)
        assertEquals("食べて", taberuMap["te"]?.japanese)
        assertEquals("食べない", taberuMap["nai"]?.japanese)
        assertEquals("食べた", taberuMap["ta"]?.japanese)
        assertEquals("食べられる", taberuMap["potential"]?.japanese)
        assertEquals("食べよう", taberuMap["volitional"]?.japanese)
        assertEquals("食べたい", taberuMap["tai"]?.japanese)
        assertEquals("食べろ", taberuMap["imperative"]?.japanese)
        assertEquals("食べれば", taberuMap["conditional"]?.japanese)

        // 2. Godan: かく / 書く (Godan -ku kuralları)
        val kaku = VerbConjugationEngine.conjugateExtended(hiragana = "かく", kanji = "書く")
        assertTrue(kaku.verbType.isGodan)
        val kakuMap = kaku.forms.associateBy { it.id }
        assertEquals("書きます", kakuMap["masu"]?.japanese)
        assertEquals("書いて", kakuMap["te"]?.japanese)
        assertEquals("書かない", kakuMap["nai"]?.japanese)
        assertEquals("書ける", kakuMap["potential"]?.japanese)
        assertEquals("書こう", kakuMap["volitional"]?.japanese)
        assertEquals("書きたい", kakuMap["tai"]?.japanese)
        assertEquals("書け", kakuMap["imperative"]?.japanese)
        assertEquals("書けば", kakuMap["conditional"]?.japanese)

        // 3. Düzensiz Suru: べんきょうする / 勉強する
        val benkyou = VerbConjugationEngine.conjugateExtended(hiragana = "べんきょうする", kanji = "勉強する")
        assertEquals(VerbType.SURU, benkyou.verbType)
        val benkyouMap = benkyou.forms.associateBy { it.id }
        assertEquals("勉強します", benkyouMap["masu"]?.japanese)
        assertEquals("勉強できる", benkyouMap["potential"]?.japanese)
        assertEquals("勉強しよう", benkyouMap["volitional"]?.japanese)
        assertEquals("勉強したい", benkyouMap["tai"]?.japanese)
        assertEquals("勉強しろ", benkyouMap["imperative"]?.japanese)
        assertEquals("勉強すれば", benkyouMap["conditional"]?.japanese)

        // 4. Düzensiz Kuru: くる / 来る
        val kuru = VerbConjugationEngine.conjugateExtended(hiragana = "くる", kanji = "来る")
        assertEquals(VerbType.KURU, kuru.verbType)
        val kuruMap = kuru.forms.associateBy { it.id }
        assertEquals("きます", kuruMap["masu"]?.japanese)
        assertEquals("こない", kuruMap["nai"]?.japanese)
        assertEquals("こられる", kuruMap["potential"]?.japanese)
        assertEquals("こよう", kuruMap["volitional"]?.japanese)
        assertEquals("きたい", kuruMap["tai"]?.japanese)
        assertEquals("こい", kuruMap["imperative"]?.japanese)
        assertEquals("くれば", kuruMap["conditional"]?.japanese)
    }

    @Test
    fun `Reference Library filters grammar by category and extracts verbs without level locks`() {
        val allGrammar = JLPTLevel.allCases.flatMap { ContentStore.loadGrammar(it.rawValue) }
        assertTrue(allGrammar.isNotEmpty())

        for (category in GrammarCategory.allCases) {
            val filtered = DictionaryViewModel.filterGrammar(
                points = allGrammar,
                query = "",
                category = category
            )
            assertTrue("Category ${category.rawValue} should have items", filtered.isNotEmpty())
            assertTrue(filtered.all { it.category == category })
        }

        val allVocab = JLPTLevel.allCases.flatMap { ContentStore.loadVocabulary(it.rawValue) }
        val verbs = DictionaryViewModel.extractVerbs(allVocab)
        assertTrue(verbs.isNotEmpty())

        val ichidanVerbs = verbs.filter { it.verbType == VerbType.ICHIDAN }
        val godanVerbs = verbs.filter { it.verbType.isGodan }
        assertTrue(ichidanVerbs.isNotEmpty())
        assertTrue(godanVerbs.isNotEmpty())

        val searchedVerbs = DictionaryViewModel.filterVerbs(
            verbs = verbs,
            query = ichidanVerbs.first().hiragana
        )
        assertTrue(searchedVerbs.isNotEmpty())
    }

    // MARK: - 4. Okuma & Hikaye Modülü (`ReadingMainScreen` & `StoryReaderScreen`)

    @Test
    fun `groupStoriesByType groups N5 stories by StoryType and extractUniqueStoryVocabulary deduplicates words`() {
        val n5Stories = ContentStore.loadStories("N5")
        assertTrue(n5Stories.isNotEmpty())

        val grouped = groupStoriesByType(n5Stories)
        assertTrue(grouped.isNotEmpty())
        val totalGroupedCount = grouped.sumOf { it.second.size }
        assertEquals(n5Stories.size, totalGroupedCount)

        // Yazı tipi sıralaması StoryType.allCases sırasına uygun olmalı
        val typesOrder = grouped.map { it.first }
        assertEquals(typesOrder.sortedBy { StoryType.allCases.indexOf(it) }, typesOrder)

        // İlk hikayenin cümlelerini ve tekil kelime listesini doğrula
        val firstStory = n5Stories.first()
        assertTrue(firstStory.sentences.isNotEmpty())
        val uniqueVocab = extractUniqueStoryVocabulary(firstStory)
        assertEquals(uniqueVocab.distinct().size, uniqueVocab.size)
    }

    @Test
    fun `recordStoryCompleted awards STORY_COMPLETED XP and updates daily study streak`() = runBlocking {
        val progressDao = FakeProgressDao()
        val activityDao = FakeActivityDao()

        val (newStreak, increased) = recordStoryCompleted(
            progressDao = progressDao,
            activityDao = activityDao,
            xpManager = XPManager.shared
        )

        val userProgress = progressDao.getUserProgress()
        assertNotNull(userProgress)
        assertEquals(XPManager.XPAction.STORY_COMPLETED.points, userProgress?.totalXP)
        assertEquals(1, newStreak)
        assertTrue(increased)
        assertEquals(1, userProgress?.currentStreak)
    }
}
