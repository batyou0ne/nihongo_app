package com.batuhankeskn0.kaimon

import com.batuhankeskn0.kaimon.data.model.CharacterType
import com.batuhankeskn0.kaimon.data.model.ChatMessage
import com.batuhankeskn0.kaimon.data.model.ExamMode
import com.batuhankeskn0.kaimon.data.model.ExamQuestion
import com.batuhankeskn0.kaimon.data.model.ExamQuestionType
import com.batuhankeskn0.kaimon.data.model.GrammarCategory
import com.batuhankeskn0.kaimon.data.model.StoryType
import com.batuhankeskn0.kaimon.data.model.VocabularyWord
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.AppLanguage
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.data.service.LanguageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Faz 2 — Domain Modelleri, ContentStore ve Lokalizasyon (L10n) Birim Testleri.
 * Gerçek 28 JSON veri dosyasını (`HiraganaData`, `KatakanaData`, `N5..N1` Kanji/Vocab/Grammar/Syllabus/Stories
 * ve `ScenariosData`) sıfır hata ile parse ettiğini, `FlashcardItem`/`QuizItem` kontratlarını,
 * bölümleme (`kanjiParts`, `vocabularyParts`) mantığını ve `L10n` dil geçişlerini doğrular.
 */
class ContentStoreTest {

    private val jlptLevels = listOf("N5", "N4", "N3", "N2", "N1")

    @Before
    fun setUp() {
        ContentStore.clearCache()
        LanguageManager.current = AppLanguage.TURKISH
    }

    @Test
    fun `loadCharacters parses HiraganaData and KatakanaData and assigns CharacterType`() {
        val hiragana = ContentStore.loadCharacters(CharacterType.HIRAGANA)
        val katakana = ContentStore.loadCharacters(CharacterType.KATAKANA)

        assertTrue("HiraganaData.json boş olmamalı", hiragana.size >= 46)
        assertTrue("KatakanaData.json boş olmamalı", katakana.size >= 46)

        assertTrue(hiragana.all { it.type == CharacterType.HIRAGANA })
        assertTrue(katakana.all { it.type == CharacterType.KATAKANA })

        val firstHira = hiragana.first()
        assertEquals("あ", firstHira.character)
        assertEquals("あ", firstHira.id)
        assertEquals("あ", firstHira.prompt)
        assertEquals("a", firstHira.correctAnswer)
        assertEquals("あ = a · a", firstHira.flipRecap)
        assertEquals("あ", firstHira.speechText)
        assertNull(firstHira.promptReading)
        assertNull(firstHira.contextHint)
        assertTrue(firstHira.exampleWords.isNotEmpty())

        val firstKata = katakana.first()
        assertEquals("ア", firstKata.character)
        assertEquals(CharacterType.KATAKANA, firstKata.type)
        assertEquals("Katakana", firstKata.type.displayName)
    }

    @Test
    fun `loadKanji parses N5 to N1 KanjiData and verifies FlashcardItem contract`() {
        for (level in jlptLevels) {
            val kanjiList = ContentStore.loadKanji(level)
            assertTrue("$level Kanji listesi boş olmamalı", kanjiList.isNotEmpty())

            kanjiList.forEach { kanji ->
                assertTrue("$level Kanji karakteri boş olmamalı", kanji.character.isNotBlank())
                assertEquals(kanji.character, kanji.id)
                assertEquals(kanji.character, kanji.prompt)
                assertEquals(kanji.meaning, kanji.correctAnswer)
                assertTrue(kanji.flipRecap.startsWith("${kanji.character} — "))
            }
        }
    }

    @Test
    fun `loadVocabulary parses N5 to N1 VocabularyData and verifies FlashcardItem contract`() {
        for (level in jlptLevels) {
            val vocabList = ContentStore.loadVocabulary(level)
            assertTrue("$level Kelime listesi boş olmamalı", vocabList.isNotEmpty())

            vocabList.forEach { word ->
                assertEquals("${word.hiragana}|${word.turkishMeaning}", word.id)
                assertEquals(word.displayText, word.prompt)
                assertEquals(word.turkishMeaning, word.correctAnswer)
                assertEquals(word.hiragana, word.speechText)

                if (word.kanji.isEmpty()) {
                    assertEquals(word.hiragana, word.displayText)
                    assertNull(word.promptReading)
                    assertEquals("${word.hiragana} · ${word.romaji}", word.flipRecap)
                } else {
                    assertEquals(word.kanji, word.displayText)
                    assertEquals(word.hiragana, word.promptReading)
                    assertEquals("${word.kanji} — ${word.hiragana} · ${word.romaji}", word.flipRecap)
                }
            }
        }

        // Fill-in-the-blank alternatePrompt doğrulaması
        val sampleWithKanji = VocabularyWord(
            kanji = "会う",
            hiragana = "あう",
            romaji = "au",
            turkishMeaning = "buluşmak, görüşmek",
            exampleSentence = "明日、友達と会う予定です。",
            exampleTranslation = "Yarın arkadaşımla buluşmayı planlıyorum."
        )
        assertEquals("明日、友達と____予定です。", sampleWithKanji.alternatePrompt)
        assertEquals("会う", sampleWithKanji.alternateAnswer)
        assertNotNull(sampleWithKanji.contextHint)
        assertEquals("明日、友達と会う予定です。", sampleWithKanji.contextHint?.sentence)
        assertEquals("Yarın arkadaşımla buluşmayı planlıyorum.", sampleWithKanji.contextHint?.translation)
    }

    @Test
    fun `loadGrammar and loadGrammarSyllabus parse N5 to N1 without errors`() {
        assertEquals(0, GrammarCategory.PARTICLE.order)
        assertEquals(1, GrammarCategory.EXPRESSION.order)
        assertEquals(2, GrammarCategory.ADJECTIVE.order)
        assertEquals(3, GrammarCategory.VERB.order)
        assertEquals(4, GrammarCategory.CONJUNCTION.order)

        for (level in jlptLevels) {
            val grammarPoints = ContentStore.loadGrammar(level)
            val syllabusUnits = ContentStore.loadGrammarSyllabus(level)

            assertTrue("$level GrammarData boş olmamalı", grammarPoints.isNotEmpty())
            assertTrue("$level GrammarSyllabus boş olmamalı", syllabusUnits.isNotEmpty())

            grammarPoints.forEach { point ->
                assertEquals(point.key, point.id)
                assertTrue(point.pattern.isNotBlank())
                assertTrue(point.examples.isNotEmpty())
                assertTrue(point.questions.isNotEmpty())
            }

            val keysSet = grammarPoints.map { it.key }.toSet()
            syllabusUnits.forEach { unit ->
                assertTrue("$level ünite başlığı boş olmamalı", unit.title.isNotBlank())
                assertTrue("$level ünite grammarKeys boş olmamalı", unit.grammarKeys.isNotEmpty())
                unit.grammarKeys.forEach { key ->
                    assertTrue(
                        "$level müfredatındaki '$key' anahtarı ${level}GrammarData içinde bulunmalı",
                        keysSet.contains(key)
                    )
                }
            }
        }
    }

    @Test
    fun `loadStories parses N5 to N1 StoriesData without errors`() {
        for (level in jlptLevels) {
            val stories = ContentStore.loadStories(level)
            assertTrue("$level StoriesData boş olmamalı", stories.isNotEmpty())

            stories.forEach { story ->
                assertTrue(story.id.isNotBlank())
                assertTrue(story.title.isNotBlank())
                assertTrue("$level '${story.title}' hikayesi cümle içermeli", story.sentences.isNotEmpty())
                story.sentences.forEach { sentence ->
                    assertTrue(sentence.japaneseText.isNotBlank())
                    assertTrue(sentence.turkishTranslation.isNotBlank())
                }
            }
        }
    }

    @Test
    fun `loadScenarios parses ScenariosData without errors`() {
        val scenarios = ContentStore.loadScenarios()
        assertTrue("ScenariosData.json boş olmamalı", scenarios.isNotEmpty())

        scenarios.forEach { scenario ->
            assertTrue(scenario.id.isNotBlank())
            assertTrue(scenario.title.isNotBlank())
            assertTrue(scenario.steps.isNotEmpty())
            scenario.steps.forEach { step ->
                assertTrue(step.botMessage.japanese.isNotBlank())
                assertTrue(step.options.isNotEmpty())
                assertTrue("Her senaryo adımında en az bir doğru şık olmalı", step.options.any { it.isCorrect })
            }
        }
    }

    @Test
    fun `kanjiParts and vocabularyParts partition items identically to iOS ContentStore`() {
        val n5Kanji = ContentStore.loadKanji("N5")
        val kanjiParts = ContentStore.kanjiParts(level = "N5", partCount = 4)
        assertEquals(4, kanjiParts.size)
        assertEquals(n5Kanji.size, kanjiParts.flatten().size)
        assertEquals(n5Kanji, kanjiParts.flatten())

        val n5Vocab = ContentStore.loadVocabulary("N5")
        val vocabParts = ContentStore.vocabularyParts(level = "N5", partSize = 25)
        assertTrue(vocabParts.isNotEmpty())
        assertTrue(vocabParts.dropLast(1).all { it.size == 25 })
        assertTrue(vocabParts.last().size <= 25)
        assertEquals(n5Vocab.size, vocabParts.flatten().size)
        assertEquals(n5Vocab, vocabParts.flatten())
    }

    @Test
    fun `ContentStore caches parsed JSON in memory across repeated calls`() {
        val loadCount = AtomicInteger(0)
        val customStore = ContentStore { fileName ->
            loadCount.incrementAndGet()
            ContentStore.defaultFileSystemLoader(fileName)
        }

        val firstCall = customStore.loadKanji("N5")
        val secondCall = customStore.loadKanji("N5")

        assertTrue(firstCall.isNotEmpty())
        assertEquals(firstCall, secondCall)
        assertEquals("İkinci çağrı bellekteki önbellekten dönmeli", 1, loadCount.get())
    }

    @Test
    fun `ExamModels and ChatMessage models behave identically to iOS`() {
        assertEquals("Seviye Kapı Sınavı", ExamMode.GATEWAY.title)
        assertEquals("Seviye Atlama (Test-Out) Sınavı", ExamMode.TEST_OUT.title)
        assertEquals("character.book.closed.fill", ExamQuestionType.KANJI.badgeIcon)

        val examQ = ExamQuestion(
            id = "q1",
            type = ExamQuestionType.KANJI,
            prompt = "日",
            subPrompt = "ひ / ニチ",
            options = listOf("gün, güneş", "ay", "su", "ateş"),
            correctAnswer = "gün, güneş"
        )
        assertEquals("gün, güneş", examQ.correctAnswer)

        val msg = ChatMessage(text = "こんにちは", isUser = true)
        assertTrue(msg.id.isNotBlank())
        assertFalse(msg.isError)
    }

    @Test
    fun `LanguageManager and L10n switch between Turkish and English reactively`() {
        LanguageManager.current = AppLanguage.TURKISH
        assertEquals(AppLanguage.TURKISH, LanguageManager.currentLanguageFlow.value)
        assertEquals("Ana", L10n.tabHome)
        assertEquals("5 günlük seri", L10n.streakText(5))
        assertEquals("Edatlar", GrammarCategory.PARTICLE.displayName)
        assertEquals("Sadece Hiragana", StoryType.HIRAGANA.displayName)
        assertEquals("2 sa 15 dk", L10n.timeFormatted(hours = 2, minutes = 15))

        LanguageManager.current = AppLanguage.ENGLISH
        assertEquals(AppLanguage.ENGLISH, LanguageManager.currentLanguageFlow.value)
        assertEquals("Home", L10n.tabHome)
        assertEquals("5 day streak", L10n.streakText(5))
        assertEquals("Particles", GrammarCategory.PARTICLE.displayName)
        assertEquals("Hiragana Only", StoryType.HIRAGANA.displayName)
        assertEquals("2h 15m", L10n.timeFormatted(hours = 2, minutes = 15))

        // Varsayılana geri döndür
        LanguageManager.current = AppLanguage.TURKISH
    }
}
