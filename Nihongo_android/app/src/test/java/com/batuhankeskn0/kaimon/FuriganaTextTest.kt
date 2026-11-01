package com.batuhankeskn0.kaimon

import com.batuhankeskn0.kaimon.data.service.AlphabetAudioService
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.ui.components.FuriganaTextParser
import com.batuhankeskn0.kaimon.ui.components.FuriganaToken
import com.batuhankeskn0.kaimon.ui.navigation.MainTab
import com.batuhankeskn0.kaimon.ui.navigation.TabBarManager
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Faz 4 — `FuriganaTextParser` (manuel etiketler, Kuromoji otomatik furigana, okurigana hizalama),
 * `AudioService.normalizeKey`, `AlphabetAudioService` ve `TabBarManager` birim testleri.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FuriganaTextTest {

    @Test
    fun `parseManualTags extracts bracketed furigana and plain text segments`() {
        val input = "私は日本語[にほんご]を勉強[べんきょう]します。"
        val tokens = FuriganaTextParser.parseManualTags(input)

        val expected = listOf(
            FuriganaToken(kanji = "私は", reading = null),
            FuriganaToken(kanji = "日本語", reading = "にほんご"),
            FuriganaToken(kanji = "を", reading = null),
            FuriganaToken(kanji = "勉強", reading = "べんきょう"),
            FuriganaToken(kanji = "します。", reading = null)
        )
        assertEquals(expected, tokens)
    }

    @Test
    fun `parseManualTags handles single kanji tag and okurigana inside or outside tag`() {
        val simple = FuriganaTextParser.parse("漢字[かんじ]")
        assertEquals(listOf(FuriganaToken(kanji = "漢字", reading = "かんじ")), simple)

        val outsideOkurigana = FuriganaTextParser.parse("食[た]べる")
        assertEquals(
            listOf(
                FuriganaToken(kanji = "食", reading = "た"),
                FuriganaToken(kanji = "べる", reading = null)
            ),
            outsideOkurigana
        )

        val insideOkurigana = FuriganaTextParser.parse("食べる[たべる]")
        assertEquals(
            listOf(
                FuriganaToken(kanji = "食", reading = "た"),
                FuriganaToken(kanji = "べる", reading = null)
            ),
            insideOkurigana
        )
    }

    @Test
    fun `katakanaToHiragana converts full katakana range accurately`() {
        assertEquals("かんじ", FuriganaTextParser.katakanaToHiragana("カンジ"))
        assertEquals("おちゃ", FuriganaTextParser.katakanaToHiragana("オチャ"))
        assertEquals("がくせい", FuriganaTextParser.katakanaToHiragana("ガクセイ"))
        assertEquals("こーひー", FuriganaTextParser.katakanaToHiragana("コーヒー"))
    }

    @Test
    fun `alignKanjiAndReading strips suffix okurigana and prefix kana accurately`() {
        // 1. Suffix okurigana: 会う -> 会[あ] + う
        assertEquals(
            listOf(
                FuriganaToken(kanji = "会", reading = "あ"),
                FuriganaToken(kanji = "う", reading = null)
            ),
            FuriganaTextParser.alignKanjiAndReading("会う", "あう")
        )

        // 2. Çok karakterli suffix okurigana: 嬉しい -> 嬉[うれ] + しい
        assertEquals(
            listOf(
                FuriganaToken(kanji = "嬉", reading = "うれ"),
                FuriganaToken(kanji = "しい", reading = null)
            ),
            FuriganaTextParser.alignKanjiAndReading("嬉しい", "うれしい")
        )

        // 3. Prefix kana: お茶 -> お + 茶[ちゃ]
        assertEquals(
            listOf(
                FuriganaToken(kanji = "お", reading = null),
                FuriganaToken(kanji = "茶", reading = "ちゃ")
            ),
            FuriganaTextParser.alignKanjiAndReading("お茶", "おちゃ")
        )

        // 4. Hem prefix hem suffix kana: お会い -> お + 会[あ] + い
        assertEquals(
            listOf(
                FuriganaToken(kanji = "お", reading = null),
                FuriganaToken(kanji = "会", reading = "あ"),
                FuriganaToken(kanji = "い", reading = null)
            ),
            FuriganaTextParser.alignKanjiAndReading("お会い", "おあい")
        )

        // 5. Saf Kanji kelimesi: 日本語 -> 日本語[にほんご]
        assertEquals(
            listOf(FuriganaToken(kanji = "日本語", reading = "にほんご")),
            FuriganaTextParser.alignKanjiAndReading("日本語", "にほんご")
        )
    }

    @Test
    fun `generateAutoFurigana tokenizes Japanese sentences and aligns okurigana with Kuromoji`() {
        val tokens1 = FuriganaTextParser.generateAutoFurigana("私は学生です。")
        assertTrue(tokens1.any { it.kanji == "私" && it.reading == "わたし" })
        assertTrue(tokens1.any { it.kanji == "学生" && it.reading == "がくせい" })
        assertTrue(tokens1.filter { it.kanji == "は" || it.kanji == "です" || it.kanji == "。" }.all { it.reading == null })

        val tokens2 = FuriganaTextParser.generateAutoFurigana("明日、友達と会う予定です。")
        // 会う kelimesinde '会' köküne 'あ' okunuşu verilmeli, 'う' okurigana olarak ayrılmalı
        assertTrue(tokens2.contains(FuriganaToken(kanji = "会", reading = "あ")))
        assertTrue(tokens2.contains(FuriganaToken(kanji = "う", reading = null)))
        assertTrue(tokens2.contains(FuriganaToken(kanji = "友達", reading = "ともだち")))
        assertTrue(tokens2.contains(FuriganaToken(kanji = "予定", reading = "よてい")))
    }

    @Test
    fun `audioService normalizeKey strips brackets trims whitespace and normalizes NFC`() {
        assertEquals("漢字", AudioService.normalizeKey("  漢字[かんじ]  "))
        assertEquals("私は日本語を勉強します。", AudioService.normalizeKey("私は日本語[にほんご]を勉強[べんきょう]します。"))
        // Decomposed 'が' (か + \u3099) -> Precomposed NFC 'が' (\u304C)
        assertEquals("が", AudioService.normalizeKey("か\u3099"))
    }

    @Test
    fun `alphabetAudioService highlights character for 700ms and triggers audio and haptic`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val mockAudio = mockk<AudioService>(relaxed = true)
        val mockFeedback = mockk<FeedbackManager>(relaxed = true)
        val service = AlphabetAudioService(
            audioService = mockAudio,
            feedbackManager = mockFeedback,
            dispatcher = testDispatcher
        )

        assertNull(service.currentCharacter)
        service.play("あ")

        assertEquals("あ", service.currentCharacter)
        verify(exactly = 1) { mockFeedback.playSelection() }
        verify(exactly = 1) { mockAudio.speak("あ", any(), any()) }

        advanceTimeBy(699L)
        assertEquals("あ", service.currentCharacter)

        advanceTimeBy(2L)
        assertNull(service.currentCharacter)
    }

    @Test
    fun `tabBarManager toggles visibility state and MainTab order matches iOS`() {
        val manager = TabBarManager()
        assertFalse(manager.isHidden)

        manager.hide()
        assertTrue(manager.isHidden)
        assertTrue(manager.isHiddenFlow.value)

        manager.show()
        assertFalse(manager.isHidden)
        assertFalse(manager.isHiddenFlow.value)

        assertEquals(
            listOf(
                MainTab.HOME,
                MainTab.ALPHABET,
                MainTab.GRAMMAR,
                MainTab.VOCABULARY,
                MainTab.READING,
                MainTab.DICTIONARY
            ),
            MainTab.entries
        )
        assertEquals("あ", MainTab.ALPHABET.textIcon)
    }
}
