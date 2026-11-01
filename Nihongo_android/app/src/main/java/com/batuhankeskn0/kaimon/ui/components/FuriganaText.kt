package com.batuhankeskn0.kaimon.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.atilika.kuromoji.ipadic.Tokenizer
import com.batuhankeskn0.kaimon.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Furigana ayrıştırma sonucu oluşan tek bir metin parçası (Kanji kökü + opsiyonel Hiragana okunuşu).
 */
@Immutable
data class FuriganaToken(
    val kanji: String,
    val reading: String? = null
)

/**
 * iOS `FuriganaText.swift` içindeki manuel etiket (`漢字[かんじ]`) ve otomatik Kuromoji
 * furigana + okurigana hizalama (`alignKanjiAndReading`) algoritmasının birebir Kotlin karşılığı.
 */
object FuriganaTextParser {

    private val kanjiRegex = Regex("[\\p{IsHan}々ヶヵ一-龯]")
    private val manualTagRegex = Regex("([\\p{IsHan}々ヶヵ一-龯]+[ぁ-ん]*)\\[([ぁ-んァ-ンー]+)\\]")

    /** Kuromoji IPADIC sözlüğü ağır olduğu için yalnızca ilk ihtiyaçta tekil (singleton) olarak yüklenir. */
    private val tokenizer: Tokenizer by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Tokenizer()
    }

    /** UI recomposition sırasında tekrar tekrar ayrıştırma yapmamak için bellek içi önbellek. */
    private val tokenCache = ConcurrentHashMap<String, List<FuriganaToken>>()

    /** Metnin Kanji içerip içermediğini kontrol eder (`\p{IsHan}`). */
    fun containsKanji(text: String): Boolean = kanjiRegex.containsMatchIn(text)

    /** Metnin manuel furigana etiketi (`[...]`) içerip içermediğini kontrol eder. */
    fun hasManualTags(text: String): Boolean = text.contains("[") && text.contains("]")

    /** Metnin furigana gerektiren bir içerik olup olmadığını döner. */
    fun hasFurigana(text: String): Boolean = hasManualTags(text) || containsKanji(text)

    /** Önbellekte hazır ayrıştırılmış token listesi varsa döner. */
    fun getCached(text: String): List<FuriganaToken>? = tokenCache[text]

    /**
     * Verilen metni ayrıştırır:
     * 1) `漢字[かんじ]` gibi köşeli parantez etiketleri varsa Regex ile ayıklar.
     * 2) Etiket yoksa ve Kanji (`\p{IsHan}`) içeriyorsa Kuromoji ile kelimeleri analiz edip okurigana hizalar.
     * 3) Sadece Hiragana/Katakana/Latin ise tek token döner.
     */
    fun parse(text: String): List<FuriganaToken> {
        if (text.isEmpty()) return emptyList()
        tokenCache[text]?.let { return it }

        val computed = when {
            hasManualTags(text) -> parseManualTags(text)
            containsKanji(text) -> generateAutoFurigana(text)
            else -> listOf(FuriganaToken(kanji = text, reading = null))
        }
        tokenCache[text] = computed
        return computed
    }

    /**
     * `漢字[かんじ]` formatındaki manuel etiketleri Regex ile ayıklar.
     * iOS `FuriganaText.parseManualTags` metodunun birebir karşılığıdır.
     */
    fun parseManualTags(text: String): List<FuriganaToken> {
        if (text.isEmpty()) return emptyList()
        val matches = manualTagRegex.findAll(text).toList()
        if (matches.isEmpty()) {
            return listOf(FuriganaToken(kanji = text, reading = null))
        }

        val result = mutableListOf<FuriganaToken>()
        var currentIndex = 0

        for (match in matches) {
            val matchStart = match.range.first
            if (matchStart > currentIndex) {
                val prefix = text.substring(currentIndex, matchStart)
                if (prefix.isNotEmpty()) {
                    result.add(FuriganaToken(kanji = prefix, reading = null))
                }
            }

            val rawKanji = match.groupValues[1]
            val rawReading = katakanaToHiragana(match.groupValues[2])
            result.addAll(alignKanjiAndReading(surface = rawKanji, readingHiragana = rawReading))

            currentIndex = match.range.last + 1
        }

        if (currentIndex < text.length) {
            val suffix = text.substring(currentIndex)
            if (suffix.isNotEmpty()) {
                result.add(FuriganaToken(kanji = suffix, reading = null))
            }
        }

        return if (result.isEmpty()) {
            listOf(FuriganaToken(kanji = text, reading = null))
        } else {
            result
        }
    }

    /**
     * Kuromoji (`Tokenizer`) ile metni morfolojik kelimelere ayırır, Katakana okunuşunu
     * Hiragana'ya çevirir ve `alignKanjiAndReading` ile okurigana hizalaması yapar.
     * iOS `FuriganaText.generateAutoFurigana` metodunun birebir karşılığıdır.
     */
    fun generateAutoFurigana(text: String): List<FuriganaToken> {
        if (text.isEmpty()) return emptyList()
        val kuromojiTokens = tokenizer.tokenize(text)
        val result = mutableListOf<FuriganaToken>()

        for (token in kuromojiTokens) {
            val surface = token.surface
            if (surface.isEmpty()) continue

            val hasKanji = containsKanji(surface)
            val rawReading = token.reading

            val readingHiragana = if (hasKanji && !rawReading.isNullOrEmpty() && rawReading != "*") {
                katakanaToHiragana(rawReading)
            } else {
                null
            }

            if (readingHiragana != null && readingHiragana != surface) {
                result.addAll(alignKanjiAndReading(surface = surface, readingHiragana = readingHiragana))
            } else {
                result.add(FuriganaToken(kanji = surface, reading = null))
            }
        }

        return result
    }

    /**
     * Okurigana (kelime sonundaki hiragana ekleri) ve önek (örn. お茶 -> おちゃ) hizalama algoritması.
     * iOS `FuriganaText.swift` (satır 100-125) içindeki algoritma ile birebir aynıdır:
     * - Son ekleri ayıklar (örn. `会う` + `あう` -> son `'う'` ortak -> `会[あ]` + `う`)
     * - Ön ekleri ayıklar (örn. `お茶` + `おちゃ` -> ilk `'お'` ortak -> `お` + `茶[ちゃ]`)
     */
    fun alignKanjiAndReading(surface: String, readingHiragana: String): List<FuriganaToken> {
        if (surface.isEmpty()) return emptyList()
        if (readingHiragana.isEmpty() || surface == readingHiragana || !containsKanji(surface)) {
            return listOf(FuriganaToken(kanji = surface, reading = null))
        }

        var k = surface
        var read = readingHiragana

        // Son ekleri (okurigana) ayıkla (örn. 会う -> あう, son 'う' ortak)
        val suffixBuilder = StringBuilder()
        while (k.length > 1 && read.length > 1 && k.last() == read.last()) {
            suffixBuilder.insert(0, k.last())
            k = k.dropLast(1)
            read = read.dropLast(1)
        }

        // Ön ekleri ayıkla (örn. お茶 -> おちゃ, ilk 'お' ortak)
        val prefixBuilder = StringBuilder()
        while (k.length > 1 && read.length > 1 && k.first() == read.first()) {
            prefixBuilder.append(k.first())
            k = k.drop(1)
            read = read.drop(1)
        }

        val tokens = mutableListOf<FuriganaToken>()
        if (prefixBuilder.isNotEmpty()) {
            tokens.add(FuriganaToken(kanji = prefixBuilder.toString(), reading = null))
        }
        tokens.add(
            FuriganaToken(
                kanji = k,
                reading = if (containsKanji(k) && read.isNotEmpty() && k != read) read else null
            )
        )
        if (suffixBuilder.isNotEmpty()) {
            tokens.add(FuriganaToken(kanji = suffixBuilder.toString(), reading = null))
        }

        return tokens
    }

    /**
     * Kuromoji'nin döndürdüğü Katakana okunuşunu (`カンジ`) Hiragana'ya (`かんじ`) dönüştürür.
     */
    fun katakanaToHiragana(input: String): String {
        if (input.isEmpty()) return input
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '\u30A1'..'\u30F6' -> sb.append((ch.code - 0x60).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}

/**
 * Metin içindeki Kanji'lerin okunuşunu (hiragana) satır kaydırmalı (`FlowRow`) olarak render eden bileşen.
 * - Eğer metin `漢字[かんじ]` gibi manuel etiketler içeriyorsa Regex ile ayıklar.
 * - Etiket yoksa ve Kanji içeriyorsa arka planda (`Dispatchers.Default`) Kuromoji ile kelimeleri analiz eder.
 * - iOS `FuriganaText.swift` bileşeninin Jetpack Compose karşılığıdır.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FuriganaText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Normal),
    readingStyle: TextStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal),
    color: Color = Theme.ink,
    readingAbove: Boolean = true
) {
    val hasFurigana = remember(text) { FuriganaTextParser.hasFurigana(text) }

    if (!hasFurigana) {
        Text(
            text = text,
            style = style,
            color = color,
            modifier = modifier
        )
        return
    }

    if (text.contains("\n")) {
        val lines = remember(text) { text.split("\n") }
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.Start
        ) {
            lines.forEach { line ->
                if (line.isEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                } else {
                    FuriganaText(
                        text = line,
                        style = style,
                        readingStyle = readingStyle,
                        color = color,
                        readingAbove = readingAbove
                    )
                }
            }
        }
        return
    }

    // Senkron önbellekte veya manuel etikette hemen al, Kuromoji gerekiyorsa Dispatchers.Default üzerinde çalıştır
    val initialTokens = remember(text) {
        FuriganaTextParser.getCached(text)
            ?: if (FuriganaTextParser.hasManualTags(text)) {
                FuriganaTextParser.parse(text)
            } else {
                null
            }
    }

    val tokens by produceState(initialValue = initialTokens, key1 = text) {
        if (value == null) {
            value = withContext(Dispatchers.Default) {
                FuriganaTextParser.parse(text)
            }
        }
    }

    val resolvedTokens = tokens
    if (resolvedTokens == null) {
        // Kuromoji ilk kez arka planda yüklenirken metni kaymasız göster
        Text(
            text = text,
            style = style,
            color = color,
            modifier = modifier
        )
        return
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val itemAlignment = if (readingAbove) Alignment.Bottom else Alignment.Top
        resolvedTokens.forEach { token ->
            Column(
                modifier = Modifier.align(itemAlignment),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                if (readingAbove) {
                    if (token.reading != null) {
                        Text(
                            text = token.reading,
                            style = readingStyle,
                            color = color.copy(alpha = 0.7f)
                        )
                    }
                    Text(
                        text = token.kanji,
                        style = style,
                        color = color
                    )
                } else {
                    Text(
                        text = token.kanji,
                        style = style,
                        color = color
                    )
                    if (token.reading != null) {
                        Text(
                            text = token.reading,
                            style = readingStyle,
                            color = color.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
