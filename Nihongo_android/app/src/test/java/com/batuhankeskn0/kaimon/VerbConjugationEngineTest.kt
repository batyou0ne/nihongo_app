package com.batuhankeskn0.kaimon

import com.batuhankeskn0.kaimon.domain.engine.VerbConjugationEngine
import com.batuhankeskn0.kaimon.domain.engine.VerbType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Faz 3 — `VerbConjugationEngine` (Godan alt türleri, Ichidan, istisnalar ve düzensiz fiiller) birim testleri.
 */
class VerbConjugationEngineTest {

    @Test
    fun `classify detects all Godan endings accurately`() {
        assertEquals(VerbType.GODAN_U, VerbConjugationEngine.classify("あう"))
        assertEquals(VerbType.GODAN_KU, VerbConjugationEngine.classify("かく"))
        assertEquals(VerbType.GODAN_GU, VerbConjugationEngine.classify("およぐ"))
        assertEquals(VerbType.GODAN_SU, VerbConjugationEngine.classify("はなす"))
        assertEquals(VerbType.GODAN_TSU, VerbConjugationEngine.classify("まつ"))
        assertEquals(VerbType.GODAN_NU, VerbConjugationEngine.classify("しぬ"))
        assertEquals(VerbType.GODAN_BU, VerbConjugationEngine.classify("あそぶ"))
        assertEquals(VerbType.GODAN_MU, VerbConjugationEngine.classify("よむ"))
        assertEquals(VerbType.GODAN_RU, VerbConjugationEngine.classify("のる"))
        assertEquals(VerbType.GODAN_RU, VerbConjugationEngine.classify("つくる"))
        assertEquals(VerbType.GODAN_RU, VerbConjugationEngine.classify("わかる"))

        assertTrue(VerbType.GODAN_U.isGodan)
        assertTrue(VerbType.GODAN_RU.isGodan)
    }

    @Test
    fun `classify distinguishes Ichidan verbs from Godan-ru exceptions`() {
        // Ichidan fiiller (i-row / e-row + る)
        val ichidanVerbs = listOf("たべる", "みる", "おきる", "ねる", "おしえる", "かりる", "あける")
        for (verb in ichidanVerbs) {
            val type = VerbConjugationEngine.classify(verb)
            assertEquals("$verb Ichidan olmalı", VerbType.ICHIDAN, type)
            assertFalse(type.isGodan)
        }

        // İ/E sesi ile bitmesine rağmen Godan-る olan istisnalar
        val godanExceptions = listOf(
            "きる", "しる", "はいる", "はしる", "かえる",
            "ちる", "まいる", "まじる", "せびる"
        )
        for (verb in godanExceptions) {
            val type = VerbConjugationEngine.classify(verb)
            assertEquals("$verb Godan-る istisnası olmalı", VerbType.GODAN_RU, type)
            assertTrue(type.isGodan)
        }
    }

    @Test
    fun `classify detects Suru, Kuru, Aru irregular verbs and Unknown`() {
        assertEquals(VerbType.SURU, VerbConjugationEngine.classify("する"))
        assertEquals(VerbType.SURU, VerbConjugationEngine.classify("べんきょうする"))
        assertEquals(VerbType.KURU, VerbConjugationEngine.classify("くる"))
        assertEquals(VerbType.KURU, VerbConjugationEngine.classify("来る"))
        assertEquals(VerbType.ARU, VerbConjugationEngine.classify("ある"))
        assertEquals(VerbType.UNKNOWN, VerbConjugationEngine.classify(""))

        assertFalse(VerbType.SURU.isGodan)
        assertFalse(VerbType.KURU.isGodan)
        assertFalse(VerbType.ARU.isGodan)
    }

    @Test
    fun `conjugate produces all 10 forms for Ichidan verb with Kanji`() {
        val result = VerbConjugationEngine.conjugate(hiragana = "たべる", kanji = "食べる")
        assertEquals("たべる", result.dictionaryForm)
        assertEquals("食べる", result.kanji)
        assertEquals(VerbType.ICHIDAN, result.verbType)
        assertEquals(10, result.forms.size)

        val formsById = result.forms.associateBy { it.id }
        assertEquals("食べる", formsById["dict"]?.japanese)
        assertEquals("taberu", formsById["dict"]?.romaji)
        assertEquals("食べます", formsById["masu"]?.japanese)
        assertEquals("tabemasu", formsById["masu"]?.romaji)
        assertEquals("食べません", formsById["masen"]?.japanese)
        assertEquals("食べました", formsById["mashita"]?.japanese)
        assertEquals("食べませんでした", formsById["masen_deshita"]?.japanese)
        assertEquals("食べて", formsById["te"]?.japanese)
        assertEquals("tabete", formsById["te"]?.romaji)
        assertEquals("食べた", formsById["ta"]?.japanese)
        assertEquals("食べない", formsById["nai"]?.japanese)
        assertEquals("食べられる", formsById["potential"]?.japanese)
        assertEquals("taberareru", formsById["potential"]?.romaji)
        assertEquals("食べましょう", formsById["mashou"]?.japanese)
    }

    @Test
    fun `conjugate produces correct sound changes for all 9 Godan subtypes`() {
        // 1. う -> って / わ / い / え
        val au = VerbConjugationEngine.conjugate("あう", "会う").forms.associateBy { it.id }
        assertEquals("会います", au["masu"]?.japanese)
        assertEquals("会って", au["te"]?.japanese)
        assertEquals("会った", au["ta"]?.japanese)
        assertEquals("会わない", au["nai"]?.japanese)
        assertEquals("会える", au["potential"]?.japanese)

        // 2. く -> いて / か / き / け
        val kaku = VerbConjugationEngine.conjugate("かく", "書く").forms.associateBy { it.id }
        assertEquals("書きます", kaku["masu"]?.japanese)
        assertEquals("書いて", kaku["te"]?.japanese)
        assertEquals("書いた", kaku["ta"]?.japanese)
        assertEquals("書かない", kaku["nai"]?.japanese)
        assertEquals("書ける", kaku["potential"]?.japanese)

        // 3. ぐ -> いで / が / ぎ / げ
        val oyogu = VerbConjugationEngine.conjugate("およぐ", "泳ぐ").forms.associateBy { it.id }
        assertEquals("泳ぎます", oyogu["masu"]?.japanese)
        assertEquals("泳いで", oyogu["te"]?.japanese)
        assertEquals("泳いだ", oyogu["ta"]?.japanese)
        assertEquals("泳がない", oyogu["nai"]?.japanese)
        assertEquals("泳げる", oyogu["potential"]?.japanese)

        // 4. す -> して / さ / し / せ
        val hanasu = VerbConjugationEngine.conjugate("はなす", "話す").forms.associateBy { it.id }
        assertEquals("話します", hanasu["masu"]?.japanese)
        assertEquals("話して", hanasu["te"]?.japanese)
        assertEquals("話さない", hanasu["nai"]?.japanese)
        assertEquals("話せる", hanasu["potential"]?.japanese)

        // 5. つ -> って / た / ち / て
        val matsu = VerbConjugationEngine.conjugate("まつ", "待つ").forms.associateBy { it.id }
        assertEquals("待ちます", matsu["masu"]?.japanese)
        assertEquals("待って", matsu["te"]?.japanese)
        assertEquals("待たない", matsu["nai"]?.japanese)
        assertEquals("待てる", matsu["potential"]?.japanese)

        // 6. ぬ -> んで / な / に / ね
        val shinu = VerbConjugationEngine.conjugate("しぬ", "死ぬ").forms.associateBy { it.id }
        assertEquals("死にます", shinu["masu"]?.japanese)
        assertEquals("死んで", shinu["te"]?.japanese)
        assertEquals("死なない", shinu["nai"]?.japanese)
        assertEquals("死ねる", shinu["potential"]?.japanese)

        // 7. ぶ -> んで / ば / び / べ
        val asobu = VerbConjugationEngine.conjugate("あそぶ", "遊ぶ").forms.associateBy { it.id }
        assertEquals("遊びます", asobu["masu"]?.japanese)
        assertEquals("遊んで", asobu["te"]?.japanese)
        assertEquals("遊ばない", asobu["nai"]?.japanese)
        assertEquals("遊べる", asobu["potential"]?.japanese)

        // 8. む -> んで / ま / み / め
        val yomu = VerbConjugationEngine.conjugate("よむ", "読む").forms.associateBy { it.id }
        assertEquals("読みます", yomu["masu"]?.japanese)
        assertEquals("読んで", yomu["te"]?.japanese)
        assertEquals("読まない", yomu["nai"]?.japanese)
        assertEquals("読める", yomu["potential"]?.japanese)

        // 9. る (Godan istisnası: かえる / 帰る) -> って / ら / り / れ
        val kaeru = VerbConjugationEngine.conjugate("かえる", "帰る").forms.associateBy { it.id }
        assertEquals("帰ります", kaeru["masu"]?.japanese)
        assertEquals("帰って", kaeru["te"]?.japanese)
        assertEquals("帰った", kaeru["ta"]?.japanese)
        assertEquals("帰らない", kaeru["nai"]?.japanese)
        assertEquals("帰れる", kaeru["potential"]?.japanese)
    }

    @Test
    fun `conjugate handles irregular Suru, Kuru and Aru forms accurately`() {
        // Compound Suru: べんきょうする / 勉強する
        val benkyou = VerbConjugationEngine.conjugate("べんきょうする", "勉強する").forms.associateBy { it.id }
        assertEquals("勉強する", benkyou["dict"]?.japanese)
        assertEquals("勉強します", benkyou["masu"]?.japanese)
        assertEquals("勉強して", benkyou["te"]?.japanese)
        assertEquals("勉強しない", benkyou["nai"]?.japanese)
        assertEquals("勉強できる", benkyou["potential"]?.japanese)

        // Kuru: くる
        val kuru = VerbConjugationEngine.conjugate("くる", "").forms.associateBy { it.id }
        assertEquals("きます", kuru["masu"]?.japanese)
        assertEquals("きて", kuru["te"]?.japanese)
        assertEquals("こない", kuru["nai"]?.japanese)
        assertEquals("こられる", kuru["potential"]?.japanese)

        // Aru: ある (özel olumsuz form: ない)
        val aruResult = VerbConjugationEngine.conjugate("ある", "")
        assertNull(aruResult.kanji)
        val aru = aruResult.forms.associateBy { it.id }
        assertEquals("あります", aru["masu"]?.japanese)
        assertEquals("あって", aru["te"]?.japanese)
        assertEquals("ない", aru["nai"]?.japanese)
        assertEquals("ありえる", aru["potential"]?.japanese)
    }
}
