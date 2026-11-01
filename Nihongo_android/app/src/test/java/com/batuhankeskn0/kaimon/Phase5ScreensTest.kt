package com.batuhankeskn0.kaimon

import com.batuhankeskn0.kaimon.data.local.DailyActivityEntity
import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.local.LearningItemProgressEntity
import com.batuhankeskn0.kaimon.data.local.LearningSessionStateEntity
import com.batuhankeskn0.kaimon.data.local.UserLevelProgressEntity
import com.batuhankeskn0.kaimon.data.local.UserProgressEntity
import com.batuhankeskn0.kaimon.data.model.CharacterType
import com.batuhankeskn0.kaimon.data.model.GrammarCategory
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.AppLanguage
import com.batuhankeskn0.kaimon.data.service.LanguageManager
import com.batuhankeskn0.kaimon.ui.screens.dictionary.DictionaryViewModel
import com.batuhankeskn0.kaimon.ui.screens.home.HomeViewModel
import com.batuhankeskn0.kaimon.ui.screens.home.ResumeTarget
import com.batuhankeskn0.kaimon.ui.screens.progress.ProgressViewModel
import com.batuhankeskn0.kaimon.ui.screens.progress.SrsStageGroup
import com.batuhankeskn0.kaimon.ui.screens.review.ReviewViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Faz 5 — Ana Ekran (`HomeScreen`), İlerleme (`ProgressScreen`), Tekrar (`ReviewListScreen`)
 * ve Sözlük (`DictionaryScreen`) Birim Testleri.
 */
class Phase5ScreensTest {

    private val fixedZone = ZoneId.of("UTC")
    private val fixedNowMillis = 1_730_000_000_000L // 2024-10-27T03:33:20Z

    @Before
    fun setUp() {
        LanguageManager.current = AppLanguage.TURKISH
    }

    // MARK: - 1. HomeScreen & ResumeTarget Testleri

    @Test
    fun `ResumeTarget parses hiragana katakana kanji and vocab session keys accurately`() {
        assertEquals("N5" to 0, ResumeTarget.parsePart("kanji_N5_part1", "kanji_"))
        assertEquals("N5" to 2, ResumeTarget.parsePart("vocab_N5_part3", "vocab_"))
        assertNull(ResumeTarget.parsePart("vocab_N5_part0", "vocab_"))
        assertNull(ResumeTarget.parsePart("review_kanji", "kanji_"))

        val hiraganaSession = LearningSessionStateEntity(
            moduleType = "hiragana",
            remainingItemIDs = List(30) { "h_$it" },
            wrongItemIDs = List(6) { "w_$it" },
            isCompleted = false,
            lastOpenedAt = 1000L
        )
        val hiraTarget = ResumeTarget.fromSession(hiraganaSession)
        assertNotNull(hiraTarget)
        assertEquals(ResumeTarget.Module.Hiragana, hiraTarget?.module)
        assertEquals("Hiragana", hiraTarget?.title)
        assertEquals(46, hiraTarget?.total)
        assertEquals(10, hiraTarget?.done) // 46 - (30 + 6) = 10

        val kanjiSession = LearningSessionStateEntity(
            moduleType = "kanji_N5_part2",
            remainingItemIDs = List(8) { "k_$it" },
            wrongItemIDs = emptyList(),
            isCompleted = false,
            lastOpenedAt = 2000L
        )
        val kanjiTarget = ResumeTarget.fromSession(kanjiSession)
        assertNotNull(kanjiTarget)
        assertEquals(ResumeTarget.Module.Kanji("N5", 1), kanjiTarget?.module)
        assertEquals("N5 Kanji's Part 2", kanjiTarget?.title)
        assertEquals(20, kanjiTarget?.total)
        assertEquals(12, kanjiTarget?.done)

        // En son açılan tamamlanmamış oturum seçilmeli
        val completedRecent = LearningSessionStateEntity(
            moduleType = "katakana",
            remainingItemIDs = emptyList(),
            wrongItemIDs = emptyList(),
            isCompleted = true,
            lastOpenedAt = 5000L
        )
        val reviewSession = LearningSessionStateEntity(
            moduleType = "review_kanji",
            remainingItemIDs = listOf("日"),
            isCompleted = false,
            lastOpenedAt = 4000L
        )

        val picked = ResumeTarget.fromSessions(
            listOf(hiraganaSession, kanjiSession, completedRecent, reviewSession)
        )
        assertEquals("kanji_N5_part2", picked?.sessionKey)
    }

    @Test
    fun `HomeViewModel buildHomeUiState computes streak review waiting count and module learned counts`() {
        val userProgress = UserProgressEntity(
            currentStreak = 4,
            longestStreak = 7,
            lastStudyDate = fixedNowMillis,
            totalXP = 650
        )
        val items = listOf(
            LearningItemProgressEntity(
                itemID = "あ",
                itemKind = LearnableItemKind.HIRAGANA_CHARACTER,
                repetitionCount = 2,
                isLearned = true,
                dueDate = fixedNowMillis - 10_000L // zamanı gelmiş (due)
            ),
            LearningItemProgressEntity(
                itemID = "い",
                itemKind = LearnableItemKind.HIRAGANA_CHARACTER,
                repetitionCount = 1,
                isLearned = true,
                dueDate = fixedNowMillis + 86_400_000L // henüz zamanı gelmemiş
            ),
            LearningItemProgressEntity(
                itemID = "日",
                itemKind = LearnableItemKind.KANJI,
                repetitionCount = 1,
                isLearned = false,
                needsReview = true, // yanlış yapılmış
                dueDate = fixedNowMillis + 86_400_000L
            )
        )
        val sessions = listOf(
            LearningSessionStateEntity(
                moduleType = "vocab_N5_part1",
                remainingItemIDs = List(15) { "v_$it" },
                isCompleted = false,
                lastOpenedAt = fixedNowMillis
            )
        )

        val homeState = HomeViewModel.buildHomeUiState(
            userProgress = userProgress,
            allProgress = items,
            sessions = sessions,
            nowMillis = fixedNowMillis
        )

        assertEquals(4, homeState.activeStreak)
        assertEquals(2, homeState.reviewWaitingCount) // "あ" (due) + "日" (needsReview)
        assertEquals(2, homeState.learnedCount(LearnableItemKind.HIRAGANA_CHARACTER))
        assertEquals(1, homeState.learnedCount(LearnableItemKind.KANJI))
        assertEquals("N5 Words Part 1", homeState.resumeTarget?.title)
        assertEquals(10, homeState.resumeTarget?.done) // 25 - 15 = 10
    }

    // MARK: - 2. ProgressScreen & İstatistik Testleri

    @Test
    fun `ProgressViewModel buildProgressUiState calculates XP levels 7-day charts and SRS distribution`() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
        val today = Instant.ofEpochMilli(fixedNowMillis).atZone(fixedZone).toLocalDate()
        val todayStr = today.format(formatter)
        val yesterdayStr = today.minusDays(1).format(formatter)

        val userProgress = UserProgressEntity(
            currentStreak = 3,
            longestStreak = 5,
            lastStudyDate = fixedNowMillis,
            totalItemsLearned = 5,
            totalXP = 1250
        )

        val activities = listOf(
            DailyActivityEntity(dateString = todayStr, xpEarned = 120, itemsReviewed = 12, timeSpentSeconds = 1800), // 30 dk
            DailyActivityEntity(dateString = yesterdayStr, xpEarned = 80, itemsReviewed = 8, timeSpentSeconds = 900)  // 15 dk
        )

        val items = listOf(
            LearningItemProgressEntity("あ", LearnableItemKind.HIRAGANA_CHARACTER, repetitionCount = 1, isLearned = true), // Çırak
            LearningItemProgressEntity("い", LearnableItemKind.HIRAGANA_CHARACTER, repetitionCount = 3, isLearned = true), // Çırak
            LearningItemProgressEntity("日", LearnableItemKind.KANJI, repetitionCount = 4, isLearned = true),             // Kalfa
            LearningItemProgressEntity("月", LearnableItemKind.KANJI, repetitionCount = 5, isLearned = true),             // Usta
            LearningItemProgressEntity("水", LearnableItemKind.KANJI, repetitionCount = 6, isLearned = true),             // Aydınlanmış
            LearningItemProgressEntity("火", LearnableItemKind.KANJI, repetitionCount = 7, isLearned = true)              // Kalıcı (Burned)
        )

        val state = ProgressViewModel.buildProgressUiState(
            userProgress = userProgress,
            allProgress = items,
            dailyActivities = activities,
            levelProgressRecords = listOf(UserLevelProgressEntity(levelRaw = "N5", isUnlocked = true)),
            nowMillis = fixedNowMillis,
            zoneId = fixedZone
        )

        assertEquals(3, state.activeStreak)
        assertEquals(5, state.longestStreak)
        assertEquals(1250, state.totalXP)
        assertEquals(3, state.userLevel) // (1250 / 500) + 1 = 3
        assertEquals(250, state.currentLevelXP)
        assertEquals(6, state.totalLearnedItemsCount)
        assertEquals(6, state.totalMasteredItemsCount)

        // Son 7 gün grafik doğrulaması
        assertEquals(7, state.last7Days.size)
        assertEquals(45, state.totalMinutesThisWeek) // 30 + 15
        assertEquals(200, state.totalXpThisWeek)     // 120 + 80
        assertEquals(30, state.last7Days.last().minutes)
        assertEquals(120, state.last7Days.last().xpEarned)

        // SRS 5 kademe dağılımı doğrulaması
        val srsMap = state.srsDistribution.associate { it.stage to it.count }
        assertEquals(2, srsMap[SrsStageGroup.APPRENTICE])
        assertEquals(1, srsMap[SrsStageGroup.GURU])
        assertEquals(1, srsMap[SrsStageGroup.MASTER])
        assertEquals(1, srsMap[SrsStageGroup.ENLIGHTENED])
        assertEquals(1, srsMap[SrsStageGroup.BURNED])

        // JLPT Seviye listesi
        assertEquals(5, state.jlptLevels.size)
        assertTrue(state.jlptLevels.first { it.level == JLPTLevel.N5 }.isUnlocked)
        assertFalse(state.jlptLevels.first { it.level == JLPTLevel.N4 }.isUnlocked)
    }

    // MARK: - 3. ReviewListScreen & Tekrar Filtreleme Testleri

    @Test
    fun `ReviewViewModel filters due and needsReview items and builds practice questions`() {
        val hiragana = ContentStore.loadCharacters(CharacterType.HIRAGANA)
        val katakana = ContentStore.loadCharacters(CharacterType.KATAKANA)
        val kanji = ContentStore.loadKanji("N5")
        val vocab = ContentStore.loadVocabulary("N5")
        val grammar = ContentStore.loadGrammar("N5")

        val sampleKanjiId = kanji.first().id
        val sampleVocabId = vocab.first().id
        val sampleGrammarId = grammar.first().id

        val progressList = listOf(
            LearningItemProgressEntity(
                itemID = "あ",
                itemKind = LearnableItemKind.HIRAGANA_CHARACTER,
                repetitionCount = 2,
                isLearned = true,
                dueDate = fixedNowMillis - 5_000L // Due
            ),
            LearningItemProgressEntity(
                itemID = sampleKanjiId,
                itemKind = LearnableItemKind.KANJI,
                repetitionCount = 4,
                isLearned = true,
                needsReview = true, // Yanlış yapılmış
                dueDate = fixedNowMillis + 100_000L
            ),
            LearningItemProgressEntity(
                itemID = sampleVocabId,
                itemKind = LearnableItemKind.VOCABULARY_WORD,
                repetitionCount = 5,
                isLearned = true,
                needsReview = false,
                dueDate = fixedNowMillis + 500_000L // Henüz zamanı gelmemiş -> filtrelenmeli
            ),
            LearningItemProgressEntity(
                itemID = sampleGrammarId,
                itemKind = LearnableItemKind.GRAMMAR,
                repetitionCount = 1,
                isLearned = false,
                needsReview = true
            )
        )

        val reviewState = ReviewViewModel.buildReviewUiState(
            allProgress = progressList,
            hiragana = hiragana,
            katakana = katakana,
            kanji = kanji,
            vocabulary = vocab,
            grammar = grammar,
            nowMillis = fixedNowMillis
        )

        assertFalse(reviewState.isEmpty)
        assertEquals(1, reviewState.hiraganaItems.size)
        assertEquals("Çırak", reviewState.hiraganaItems.first().srsLevelTitle)

        assertEquals(1, reviewState.kanjiItems.size)
        assertEquals("Kalfa", reviewState.kanjiItems.first().srsLevelTitle)

        assertTrue("Zamanı gelmemiş kelime tekrar listesinde olmamalı", reviewState.vocabularyItems.isEmpty())
        assertEquals(1, reviewState.grammarItems.size)

        // Practice question üretimi
        val kanjiQuestions = ReviewViewModel.buildPracticeQuestions(
            kind = LearnableItemKind.KANJI,
            reviewItems = reviewState.kanjiItems,
            hiragana = hiragana,
            katakana = katakana,
            kanji = kanji,
            vocabulary = vocab,
            grammar = grammar
        )
        assertEquals(1, kanjiQuestions.size)
        assertEquals(4, kanjiQuestions.first().options.size)
        assertTrue(kanjiQuestions.first().options.contains(kanjiQuestions.first().correctAnswer))
    }

    // MARK: - 4. DictionaryScreen Arama ve Filtreleme Testleri

    @Test
    fun `DictionaryViewModel filters vocabulary kanji grammar and verbs across Japanese Romaji and Turkish`() {
        val words = ContentStore.loadVocabulary("N5")
        val kanjiList = ContentStore.loadKanji("N5")
        val grammarList = ContentStore.loadGrammar("N5")

        // Kelime araması: Japonca, Romaji ve Türkçe
        val sampleWord = words.first()
        assertTrue(DictionaryViewModel.filterVocabulary(words, sampleWord.hiragana).contains(sampleWord))
        assertTrue(DictionaryViewModel.filterVocabulary(words, sampleWord.romaji.uppercase()).contains(sampleWord))
        assertTrue(DictionaryViewModel.filterVocabulary(words, sampleWord.turkishMeaning.take(4)).contains(sampleWord))

        // Kanji araması: Karakter, okunuş ve Türkçe anlam
        val sampleKanji = kanjiList.first()
        assertTrue(DictionaryViewModel.filterKanji(kanjiList, sampleKanji.character).contains(sampleKanji))
        assertTrue(DictionaryViewModel.filterKanji(kanjiList, sampleKanji.meaning.take(3)).contains(sampleKanji))

        // Gramer araması ve kategori filtresi
        val particlePoints = DictionaryViewModel.filterGrammar(
            points = grammarList,
            query = "",
            category = GrammarCategory.PARTICLE
        )
        assertTrue(particlePoints.isNotEmpty())
        assertTrue(particlePoints.all { it.category == GrammarCategory.PARTICLE })

        val sampleGrammar = grammarList.first()
        val searchedGrammar = DictionaryViewModel.filterGrammar(
            points = grammarList,
            query = sampleGrammar.romaji
        )
        assertTrue(searchedGrammar.contains(sampleGrammar))

        // Fiil kütüphanesi çıkarımı ve çekim kontrolü
        val verbs = DictionaryViewModel.extractVerbs(words)
        assertTrue("N5 kelime listesinden fiiller çıkarılmalı", verbs.isNotEmpty())
        val firstVerb = verbs.first()
        val conjugation = firstVerb.conjugationResult()
        assertEquals(10, conjugation.forms.size)
    }
}
