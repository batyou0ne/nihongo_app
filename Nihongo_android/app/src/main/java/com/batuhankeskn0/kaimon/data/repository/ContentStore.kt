package com.batuhankeskn0.kaimon.data.repository

import android.content.Context
import com.batuhankeskn0.kaimon.data.model.CharacterType
import com.batuhankeskn0.kaimon.data.model.ConversationScenario
import com.batuhankeskn0.kaimon.data.model.GrammarPoint
import com.batuhankeskn0.kaimon.data.model.GrammarUnit
import com.batuhankeskn0.kaimon.data.model.JapaneseCharacter
import com.batuhankeskn0.kaimon.data.model.Kanji
import com.batuhankeskn0.kaimon.data.model.Story
import com.batuhankeskn0.kaimon.data.model.VocabularyWord
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

/**
 * Statik içerik JSON'larını (hiragana/katakana/kanji/kelime/gramer/hikaye/senaryo) yükleyen ortak servis.
 * ReviewListScreen, ExamGenerator, LevelProgressionService gibi birden fazla modülün verisine
 * aynı anda ihtiyaç duyan bileşenler için tek noktadan önbellekli (in-memory cached) erişim sağlar.
 */
@Singleton
class ContentStore {

    private var jsonLoader: (String) -> String?

    @Inject
    constructor(@ApplicationContext context: Context) {
        val appContext = context.applicationContext
        this.jsonLoader = { fileName ->
            runCatching {
                appContext.assets.open("$fileName.json")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
            }.getOrNull()
        }
        sharedInstance = this
    }

    constructor(customLoader: (String) -> String? = Companion::defaultFileSystemLoader) {
        this.jsonLoader = customLoader
    }

    private val charactersCache = ConcurrentHashMap<CharacterType, List<JapaneseCharacter>>()
    private val kanjiCache = ConcurrentHashMap<String, List<Kanji>>()
    private val vocabularyCache = ConcurrentHashMap<String, List<VocabularyWord>>()
    private val grammarCache = ConcurrentHashMap<String, List<GrammarPoint>>()
    private val grammarSyllabusCache = ConcurrentHashMap<String, List<GrammarUnit>>()
    private val storiesCache = ConcurrentHashMap<String, List<Story>>()

    @Volatile
    private var scenariosCache: List<ConversationScenario>? = null

    fun loadCharacters(type: CharacterType): List<JapaneseCharacter> {
        charactersCache[type]?.let { return it }

        val fileName = if (type == CharacterType.HIRAGANA) "HiraganaData" else "KatakanaData"
        val loaded = load<List<JapaneseCharacter>>(fileName) ?: return emptyList()
        val typedList = loaded.map { it.copy(type = type) }
        if (typedList.isNotEmpty()) {
            charactersCache[type] = typedList
        }
        return typedList
    }

    fun loadKanji(level: String = "N5"): List<Kanji> {
        val normalizedLevel = level.uppercase()
        kanjiCache[normalizedLevel]?.let { return it }

        val loaded = load<List<Kanji>>("${normalizedLevel}KanjiData") ?: return emptyList()
        if (loaded.isNotEmpty()) {
            kanjiCache[normalizedLevel] = loaded
        }
        return loaded
    }

    fun loadVocabulary(level: String = "N5"): List<VocabularyWord> {
        val normalizedLevel = level.uppercase()
        vocabularyCache[normalizedLevel]?.let { return it }

        val loaded = load<List<VocabularyWord>>("${normalizedLevel}VocabularyData") ?: return emptyList()
        if (loaded.isNotEmpty()) {
            vocabularyCache[normalizedLevel] = loaded
        }
        return loaded
    }

    fun loadGrammar(level: String = "N5"): List<GrammarPoint> {
        val normalizedLevel = level.uppercase()
        grammarCache[normalizedLevel]?.let { return it }

        val loaded = load<List<GrammarPoint>>("${normalizedLevel}GrammarData") ?: return emptyList()
        if (loaded.isNotEmpty()) {
            grammarCache[normalizedLevel] = loaded
        }
        return loaded
    }

    fun loadGrammarSyllabus(level: String = "N5"): List<GrammarUnit> {
        val normalizedLevel = level.uppercase()
        grammarSyllabusCache[normalizedLevel]?.let { return it }

        val loaded = load<List<GrammarUnit>>("${normalizedLevel}GrammarSyllabus") ?: return emptyList()
        if (loaded.isNotEmpty()) {
            grammarSyllabusCache[normalizedLevel] = loaded
        }
        return loaded
    }

    fun loadStories(level: String = "N5"): List<Story> {
        val normalizedLevel = level.uppercase()
        storiesCache[normalizedLevel]?.let { return it }

        val loaded = load<List<Story>>("${normalizedLevel}StoriesData") ?: return emptyList()
        if (loaded.isNotEmpty()) {
            storiesCache[normalizedLevel] = loaded
        }
        return loaded
    }

    fun loadScenarios(): List<ConversationScenario> {
        scenariosCache?.let { return it }

        val loaded = load<List<ConversationScenario>>("ScenariosData") ?: return emptyList()
        if (loaded.isNotEmpty()) {
            scenariosCache = loaded
        }
        return loaded
    }

    // MARK: - Bölümler (part)

    /**
     * Kanji seviyesini eşit `partCount` (varsayılan 4) parçaya böler. Hem KanjiPartSelectionView hem de ana
     * ekrandaki "Kaldığın yer" kartı bunu kullanır — bölümleme mantığı tek yerde
     * kalsın ki iki taraf farklı kartlara işaret etmesin.
     */
    fun kanjiParts(level: String = "N5", partCount: Int = 4): List<List<Kanji>> =
        chunksByPartCount(items = loadKanji(level = level), partCount = partCount)

    /**
     * Kelime seviyesini 25'erlik parçalara böler (aynı gerekçe: bkz. kanjiParts).
     */
    fun vocabularyParts(level: String = "N5", partSize: Int = 25): List<List<VocabularyWord>> =
        chunksBySize(items = loadVocabulary(level = level), size = partSize)

    fun clearCache() {
        charactersCache.clear()
        kanjiCache.clear()
        vocabularyCache.clear()
        grammarCache.clear()
        grammarSyllabusCache.clear()
        storiesCache.clear()
        scenariosCache = null
    }

    internal fun attachContext(context: Context) {
        val appContext = context.applicationContext
        this.jsonLoader = { fileName ->
            runCatching {
                appContext.assets.open("$fileName.json")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
            }.getOrNull()
        }
    }

    private fun <T> chunksByPartCount(items: List<T>, partCount: Int): List<List<T>> {
        if (items.isEmpty() || partCount <= 0) return emptyList()
        val size = ceil(items.size.toDouble() / partCount.toDouble()).toInt()
        return chunksBySize(items = items, size = size)
    }

    private fun <T> chunksBySize(items: List<T>, size: Int): List<List<T>> {
        if (items.isEmpty() || size <= 0) return emptyList()
        return items.chunked(size)
    }

    private inline fun <reified T> load(fileName: String): T? {
        val rawJson = jsonLoader(fileName) ?: return null
        return runCatching {
            json.decodeFromString<T>(rawJson)
        }.getOrNull()
    }

    companion object {
        val json: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        @Volatile
        private var sharedInstance: ContentStore = ContentStore()

        val shared: ContentStore
            get() = sharedInstance

        fun init(context: Context) {
            sharedInstance.attachContext(context)
        }

        fun loadCharacters(type: CharacterType): List<JapaneseCharacter> =
            sharedInstance.loadCharacters(type)

        fun loadKanji(level: String = "N5"): List<Kanji> =
            sharedInstance.loadKanji(level = level)

        fun loadVocabulary(level: String = "N5"): List<VocabularyWord> =
            sharedInstance.loadVocabulary(level = level)

        fun loadGrammar(level: String = "N5"): List<GrammarPoint> =
            sharedInstance.loadGrammar(level = level)

        fun loadGrammarSyllabus(level: String = "N5"): List<GrammarUnit> =
            sharedInstance.loadGrammarSyllabus(level = level)

        fun loadStories(level: String = "N5"): List<Story> =
            sharedInstance.loadStories(level = level)

        fun loadScenarios(): List<ConversationScenario> =
            sharedInstance.loadScenarios()

        fun kanjiParts(level: String = "N5", partCount: Int = 4): List<List<Kanji>> =
            sharedInstance.kanjiParts(level = level, partCount = partCount)

        fun vocabularyParts(level: String = "N5", partSize: Int = 25): List<List<VocabularyWord>> =
            sharedInstance.vocabularyParts(level = level, partSize = partSize)

        fun clearCache() {
            sharedInstance.clearCache()
        }

        /**
         * JVM birim testlerinde (Android Context olmadığında) ortak iOS `Resources/`
         * dizininden JSON dosyalarını sıfır kopya ile okuyan varsayılan yükleyici.
         */
        internal fun defaultFileSystemLoader(fileName: String): String? {
            val candidates = listOf(
                File("../../Nihongo_app/Nihongo_app/Resources/$fileName.json"),
                File("../Nihongo_app/Nihongo_app/Resources/$fileName.json"),
                File("Nihongo_app/Nihongo_app/Resources/$fileName.json")
            )
            val file = candidates.firstOrNull { it.exists() && it.isFile } ?: return null
            return runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
        }
    }
}
