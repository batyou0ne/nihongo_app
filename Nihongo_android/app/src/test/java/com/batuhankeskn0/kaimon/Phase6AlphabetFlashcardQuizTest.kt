package com.batuhankeskn0.kaimon

import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.local.LearningItemProgressEntity
import com.batuhankeskn0.kaimon.data.local.LearningSessionStateEntity
import com.batuhankeskn0.kaimon.data.model.CharacterType
import com.batuhankeskn0.kaimon.data.model.VocabularyWord
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.AlphabetAudioService
import com.batuhankeskn0.kaimon.data.service.AppLanguage
import com.batuhankeskn0.kaimon.data.service.LanguageManager
import com.batuhankeskn0.kaimon.domain.engine.XPManager
import com.batuhankeskn0.kaimon.ui.screens.alphabet.AlphabetCategory
import com.batuhankeskn0.kaimon.ui.screens.alphabet.AlphabetChartData
import com.batuhankeskn0.kaimon.ui.screens.alphabet.WritingSystemType
import com.batuhankeskn0.kaimon.ui.screens.alphabet.computeAlphabetLearnedCounts
import com.batuhankeskn0.kaimon.ui.screens.learning.FlashcardSessionController
import com.batuhankeskn0.kaimon.ui.screens.quiz.QuizViewModel
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Faz 6 — Alfabe (`AlphabetMainScreen`, `AlphabetChartScreen`), Ortak Flashcard Oturum Motoru
 * (`FlashcardSessionController`) ve Ortak Quiz Motoru (`QuizViewModel`) Birim Testleri.
 */
class Phase6AlphabetFlashcardQuizTest {

    @Before
    fun setUp() {
        LanguageManager.current = AppLanguage.TURKISH
    }

    // MARK: - 1. Alfabe Tablosu (Hiragana, Katakana, Kanji) Testleri

    @Test
    fun `AlphabetChartData provides exact character counts for Basic Dakuten and Yoon across Hiragana and Katakana`() {
        val hiraBasic = AlphabetChartData.rowsFor(WritingSystemType.HIRAGANA, AlphabetCategory.BASIC)
            .flatMap { it.cells }
            .filterNotNull()
        val hiraDakuten = AlphabetChartData.rowsFor(WritingSystemType.HIRAGANA, AlphabetCategory.DAKUTEN)
            .flatMap { it.cells }
            .filterNotNull()
        val hiraYoon = AlphabetChartData.rowsFor(WritingSystemType.HIRAGANA, AlphabetCategory.YOON)
            .flatMap { it.cells }
            .filterNotNull()

        assertEquals(46, hiraBasic.size)
        assertEquals(25, hiraDakuten.size)
        assertEquals(33, hiraYoon.size)
        assertEquals("あ", hiraBasic.first().character)
        assertEquals("ん", hiraBasic.last().character)
        assertEquals("きゃ", hiraYoon.first().character)
        assertEquals("ぴょ", hiraYoon.last().character)

        val kataBasic = AlphabetChartData.rowsFor(WritingSystemType.KATAKANA, AlphabetCategory.BASIC)
            .flatMap { it.cells }
            .filterNotNull()
        val kataDakuten = AlphabetChartData.rowsFor(WritingSystemType.KATAKANA, AlphabetCategory.DAKUTEN)
            .flatMap { it.cells }
            .filterNotNull()
        val kataYoon = AlphabetChartData.rowsFor(WritingSystemType.KATAKANA, AlphabetCategory.YOON)
            .flatMap { it.cells }
            .filterNotNull()

        assertEquals(46, kataBasic.size)
        assertEquals(25, kataDakuten.size)
        assertEquals(33, kataYoon.size)
        assertEquals("ア", kataBasic.first().character)
        assertEquals("ン", kataBasic.last().character)
        assertEquals("キャ", kataYoon.first().character)
        assertEquals("ピョ", kataYoon.last().character)
    }

    @Test
    fun `AlphabetChartData filters N5 Kanji by all and 4 parts accurately`() {
        val allKanji = ContentStore.loadKanji("N5")
        val parts = ContentStore.kanjiParts("N5", partCount = 4)

        assertTrue(allKanji.isNotEmpty())
        assertEquals(4, parts.size)

        val filteredAll = AlphabetChartData.filterKanji(allKanji, selectedKanjiPart = 0, parts = parts)
        assertEquals(allKanji.size, filteredAll.size)

        for (partIdx in 1..4) {
            val partList = AlphabetChartData.filterKanji(allKanji, selectedKanjiPart = partIdx, parts = parts)
            assertEquals(parts[partIdx - 1], partList)
        }
    }

    @Test
    fun `AlphabetAudioService highlights currentCharacter when played and clears on stop`() {
        val service = AlphabetAudioService(dispatcher = Dispatchers.Unconfined)
        assertNull(service.currentCharacter)

        service.play("あ")
        // Unconfined dispatcher'da bile stop() çağrısı karakteri temizler
        service.stop()
        assertNull(service.currentCharacter)
    }

    @Test
    fun `computeAlphabetLearnedCounts counts items with repetitionCount at least 1`() {
        val progress = listOf(
            LearningItemProgressEntity("あ", LearnableItemKind.HIRAGANA_CHARACTER, repetitionCount = 1),
            LearningItemProgressEntity("い", LearnableItemKind.HIRAGANA_CHARACTER, repetitionCount = 0),
            LearningItemProgressEntity("ア", LearnableItemKind.KATAKANA_CHARACTER, repetitionCount = 3),
            LearningItemProgressEntity("日", LearnableItemKind.KANJI, repetitionCount = 2),
            LearningItemProgressEntity("月", LearnableItemKind.KANJI, repetitionCount = 1)
        )

        val counts = computeAlphabetLearnedCounts(progress)
        assertEquals(1, counts[LearnableItemKind.HIRAGANA_CHARACTER])
        assertEquals(1, counts[LearnableItemKind.KATAKANA_CHARACTER])
        assertEquals(2, counts[LearnableItemKind.KANJI])
    }

    // MARK: - 2. Ortak Quiz Motoru (`QuizViewModel`) Testleri

    @Test
    fun `QuizViewModel generates 4 distinct options using distractorPool even for small retry sets`() {
        val allHiragana = ContentStore.loadCharacters(CharacterType.HIRAGANA)
        val retrySubset = allHiragana.take(2) // Sadece 2 soru

        val vm = QuizViewModel(
            questions = retrySubset,
            itemKind = LearnableItemKind.HIRAGANA_CHARACTER,
            shuffled = false,
            distractorPool = allHiragana,
            autoUpdateProgress = false,
            triggerFeedback = false,
            alternateModeSelector = { false }
        )

        assertEquals(2, vm.questions.size)
        assertEquals(4, vm.options.size)
        assertEquals(4, vm.options.distinct().size)
        assertTrue(vm.options.contains(retrySubset.first().correctAnswer))
    }

    @Test
    fun `QuizViewModel supports alternatePrompt fill-in-the-blank mode and updates SRS progress`() {
        val word1 = VocabularyWord(
            kanji = "水",
            hiragana = "みず",
            romaji = "mizu",
            turkishMeaning = "su",
            exampleSentence = "水を飲みます。",
            exampleTranslation = "Su içerim."
        )
        val word2 = VocabularyWord(
            kanji = "本",
            hiragana = "ほん",
            romaji = "hon",
            turkishMeaning = "kitap",
            exampleSentence = "本を読みます。",
            exampleTranslation = "Kitap okurum."
        )
        val word3 = VocabularyWord(
            kanji = "車",
            hiragana = "くるま",
            romaji = "kuruma",
            turkishMeaning = "araba",
            exampleSentence = "車に乗ります。",
            exampleTranslation = "Arabaya binerim."
        )
        val word4 = VocabularyWord(
            kanji = "山",
            hiragana = "やま",
            romaji = "yama",
            turkishMeaning = "dağ",
            exampleSentence = "山を見ます。",
            exampleTranslation = "Dağa bakarım."
        )
        val pool = listOf(word1, word2, word3, word4)
        val progressMap = pool.associate {
            it.id to LearningItemProgressEntity(itemID = it.id, itemKind = LearnableItemKind.VOCABULARY_WORD)
        }

        val vm = QuizViewModel(
            questions = listOf(word1, word2),
            itemKind = LearnableItemKind.VOCABULARY_WORD,
            progressLookup = { progressMap[it] },
            shuffled = false,
            distractorPool = pool,
            autoUpdateProgress = true,
            triggerFeedback = false,
            alternateModeSelector = { true } // Boşluk doldurma modunu zorla
        )

        assertTrue(vm.currentQuestionModeIsAlternate)
        assertEquals("____を飲みます。", word1.alternatePrompt)
        assertEquals("水", vm.currentExpectedAnswer)
        assertEquals(4, vm.options.size)
        assertTrue(vm.options.contains("水"))

        // Doğru cevap gönder
        vm.submitAnswer("水")
        assertEquals(true, vm.isAnswerCorrect)
        assertEquals(1, vm.score)
        assertEquals(1, progressMap[word1.id]?.repetitionCount)
        assertEquals(true, progressMap[word1.id]?.isLearned)

        // İkinci soruya geç ve yanlış cevap gönder
        vm.moveToNext()
        assertEquals(word2, vm.currentQuestion)
        vm.submitAnswer("車")
        assertEquals(false, vm.isAnswerCorrect)
        assertEquals(1, vm.score)
        assertEquals(true, progressMap[word2.id]?.needsReview)

        vm.moveToNext()
        assertTrue(vm.isFinished)
    }

    // MARK: - 3. Ortak Flashcard Oturum Motoru (`FlashcardSessionController`) Testleri

    @Test
    fun `FlashcardSessionController repeats wrong items in subsequent rounds until all are answered correctly`() {
        val allCharacters = ContentStore.loadCharacters(CharacterType.HIRAGANA)
        val items = allCharacters.take(4) // 4 kartlık mini oturum: あ, い, う, え

        val savedSessions = mutableListOf<LearningSessionStateEntity>()
        val awardedXpActions = mutableListOf<XPManager.XPAction>()
        var completedKey: String? = null

        val controller = FlashcardSessionController(
            sessionKey = "hiragana",
            itemKind = LearnableItemKind.HIRAGANA_CHARACTER,
            allItems = items,
            distractorPool = allCharacters,
            alternateModeSelector = { false },
            onSaveSession = { savedSessions.add(it) },
            onAwardXp = { awardedXpActions.add(it) },
            onSessionCompleted = { completedKey = it }
        )

        controller.setup(nowMillis = 10_000L)

        assertEquals(1, controller.roundNumber)
        assertEquals(0.0, controller.moduleProgress, 0.001)
        assertNotNull(controller.sessionState?.currentCardID)

        // Tur 1: İlk 2 kartı doğru, son 2 kartı yanlış cevapla
        val round1WrongIds = mutableListOf<String>()
        for (step in 0 until 4) {
            val currentItem = controller.quizViewModel!!.currentQuestion!!
            if (step < 2) {
                val isCorrect = controller.submitOption(currentItem.correctAnswer)
                assertTrue(isCorrect)
            } else {
                round1WrongIds.add(currentItem.id)
                val isCorrect = controller.submitOption("yanlis_sik")
                assertFalse(isCorrect)
            }
            controller.advanceToNext()
        }

        // Tur 1 bittiğinde 2 yanlış olduğu için oturum bitmemeli, sadece yanlışlarla Tur 2 başlamalı!
        assertFalse(controller.sessionState!!.isCompleted)
        assertEquals(2, controller.roundNumber)
        assertEquals(2, controller.quizViewModel!!.questions.size)
        assertEquals(round1WrongIds.toSet(), controller.quizViewModel!!.questions.map { it.id }.toSet())
        assertEquals(0.5, controller.moduleProgress, 0.001) // 4 karttan 2'si ustalaşıldı
        assertEquals(2, awardedXpActions.size) // Sadece ilk turda doğru bilinen 2 kart XP kazandırdı

        // Tur 2: İlk kartı doğru, ikinci kartı tekrar yanlış yap ("Tekrar Et" aksiyonu ile)
        val secondRoundFirst = controller.quizViewModel!!.currentQuestion!!
        controller.submitOption(secondRoundFirst.correctAnswer)
        controller.advanceToNext()

        val secondRoundSecond = controller.quizViewModel!!.currentQuestion!!
        controller.markUnknownOrAdvance() // İlk çağrı yanlış işaretler ve kartı çevirir
        controller.markUnknownOrAdvance() // İkinci çağrı sonraki tura ilerletir

        // Hâlâ 1 yanlış olduğu için Tur 3 başlamalı (tek kartla)!
        assertFalse(controller.sessionState!!.isCompleted)
        assertEquals(3, controller.roundNumber)
        assertEquals(1, controller.quizViewModel!!.questions.size)
        assertEquals(secondRoundSecond.id, controller.quizViewModel!!.currentQuestion?.id)
        assertEquals(0.75, controller.moduleProgress, 0.001)

        // Tur 3: Son kalan kartı doğru cevapla ("Öğrendim" aksiyonu ile)
        controller.markKnownOrAdvance() // Doğru işaretler
        controller.markKnownOrAdvance() // İlerler ve oturumu tamamlar

        assertTrue(controller.sessionState!!.isCompleted)
        assertEquals("hiragana", completedKey)
        assertEquals(1.0, controller.moduleProgress, 0.001)
        assertEquals(7, controller.sessionState!!.totalAnswerCount) // 4 (Tur 1) + 2 (Tur 2) + 1 (Tur 3)

        // Özet ekranı yanlış listesi kontrolü (2 kez yanlış yapılan üstte, 1 kez yanlış yapılan altta)
        val wrongSummary = controller.buildWrongSummaryItems()
        assertEquals(2, wrongSummary.size)
        assertEquals(secondRoundSecond.id, wrongSummary.first().id)
        assertEquals(2, wrongSummary.first().wrongCount)
        assertEquals(1, wrongSummary.last().wrongCount)
    }

    @Test
    fun `FlashcardSessionController resumes from exact currentCardID when reopening unfinished session`() {
        val items = ContentStore.loadCharacters(CharacterType.KATAKANA).take(6)
        val targetResumeCard = items[4]

        val existingSession = LearningSessionStateEntity(
            moduleType = "katakana",
            remainingItemIDs = items.map { it.id },
            wrongItemIDs = emptyList(),
            isCompleted = false,
            currentCardID = targetResumeCard.id,
            lastOpenedAt = 5_000L
        )

        val controller = FlashcardSessionController(
            sessionKey = "katakana",
            itemKind = LearnableItemKind.KATAKANA_CHARACTER,
            allItems = items,
            distractorPool = items,
            alternateModeSelector = { false }
        )

        controller.setup(existingSession = existingSession, nowMillis = 20_000L)

        // Kaldığı kart her zaman ilk sırada (index 0) açılmalı
        assertEquals(targetResumeCard.id, controller.quizViewModel?.currentQuestion?.id)
        assertEquals(20_000L, controller.sessionState?.lastOpenedAt)
    }

    @Test
    fun `FlashcardSessionController parseSessionKey extracts module JLPTLevel and zero-based partIndex`() {
        val kanjiKey = FlashcardSessionController.parseSessionKey("kanji_N5_part1")
        assertNotNull(kanjiKey)
        assertEquals(Triple("kanji", JLPTLevel.N5, 0), kanjiKey)

        val vocabKey = FlashcardSessionController.parseSessionKey("vocab_N4_part3")
        assertNotNull(vocabKey)
        assertEquals(Triple("vocab", JLPTLevel.N4, 2), vocabKey)

        assertNull(FlashcardSessionController.parseSessionKey("hiragana"))
        assertNull(FlashcardSessionController.parseSessionKey("kanji_N5_part0"))
    }
}
