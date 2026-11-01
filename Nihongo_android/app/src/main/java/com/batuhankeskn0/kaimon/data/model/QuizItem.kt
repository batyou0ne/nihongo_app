package com.batuhankeskn0.kaimon.data.model

import kotlinx.serialization.Serializable

/**
 * Kelime kartlarında örnek cümle ve çeviri ipucunu taşıyan veri modeli.
 * Swift'teki `(sentence: String, translation: String)` tuple yapısının birebir karşılığıdır.
 */
@Serializable
data class ContextHint(
    val sentence: String,
    val translation: String
) {
    val first: String
        get() = sentence

    val second: String
        get() = translation
}

/**
 * Quiz'e sokulabilecek her öğenin (karakter, kanji ya da kelime) uyması gereken arayüz.
 * QuizViewModel'in hem Hiragana/Katakana kartlarını hem de kelime/kanji setini aynı
 * çoktan seçmeli mantıkla çalıştırabilmesini sağlar.
 */
interface QuizItem {
    val id: String
    val prompt: String        // Ekranda büyük gösterilen Japonca metin.
    val correctAnswer: String // Doğru cevap (romaji ya da Türkçe anlam).

    val alternatePrompt: String?
        get() = null

    val alternateAnswer: String?
        get() = null
}

/**
 * `FlashcardSessionView`'ın (Hiragana/Katakana/Kanji/Kelime ortak akışı) ihtiyaç duyduğu
 * ek bilgiler: kart çevrilince üstte gösterilen özet satırı ve örnek kelimeler.
 */
interface FlashcardItem : QuizItem {
    val flipRecap: String
    val exampleWords: List<ExampleWord>

    /**
     * Kartta büyük prompt'un üstünde küçük/gri gösterilen okunuş (ör. kelimenin
     * hiraganası). Karakter/kanji kartlarında gerek yok, varsayılanı null.
     */
    val promptReading: String?
        get() = null

    /**
     * Sesli okuma butonunun okuyacağı metin. Varsayılanı prompt; kelimelerde TTS
     * kanjiyi yanlış okuyabildiği için hiragana kullanılır.
     */
    val speechText: String
        get() = prompt

    val contextHint: ContextHint?
        get() = null
}
