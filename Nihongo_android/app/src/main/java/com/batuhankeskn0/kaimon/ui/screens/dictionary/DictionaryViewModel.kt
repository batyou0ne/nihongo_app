package com.batuhankeskn0.kaimon.ui.screens.dictionary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhankeskn0.kaimon.data.model.GrammarCategory
import com.batuhankeskn0.kaimon.data.model.GrammarPoint
import com.batuhankeskn0.kaimon.data.model.Kanji
import com.batuhankeskn0.kaimon.data.model.VocabularyWord
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.VerbConjugationEngine
import com.batuhankeskn0.kaimon.domain.engine.VerbConjugationResult
import com.batuhankeskn0.kaimon.domain.engine.VerbType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * Sözlük sekmeleri / filtreleri: Kelime, Kanji ve Gramer.
 */
enum class DictionaryTab {
    VOCABULARY,
    KANJI,
    GRAMMAR;

    val localizedTitle: String
        get() = when (this) {
            VOCABULARY -> L10n.dictionaryVocabTab
            KANJI -> L10n.dictionaryKanjiTab
            GRAMMAR -> L10n.dictionaryGrammarTab
        }
}

/**
 * Fiil çekim kütüphanesi satır modeli (`Views/Reference/VerbCategoryListView.swift` karşılığı).
 */
data class VerbEntry(
    val id: String,
    val hiragana: String,
    val kanji: String?,
    val romaji: String,
    val turkishMeaning: String,
    val verbType: VerbType
) {
    fun conjugationResult(): VerbConjugationResult =
        VerbConjugationEngine.conjugate(hiragana = hiragana, kanji = kanji)
}

/**
 * `DictionaryScreen` arayüz durumu.
 */
data class DictionaryUiState(
    val selectedTab: DictionaryTab = DictionaryTab.VOCABULARY,
    val searchQuery: String = "",
    val selectedGrammarCategory: GrammarCategory? = null,
    val filteredWords: List<VocabularyWord> = emptyList(),
    val filteredKanji: List<Kanji> = emptyList(),
    val filteredGrammar: List<GrammarPoint> = emptyList(),
    val filteredVerbs: List<VerbEntry> = emptyList(),
    val grammarCategoryCounts: Map<GrammarCategory, Int> = emptyMap(),
    val selectedWord: VocabularyWord? = null,
    val selectedKanji: Kanji? = null,
    val selectedGrammarPoint: GrammarPoint? = null,
    val selectedVerb: VerbEntry? = null
)

/**
 * Sözlük Ekranı (`DictionaryScreen`) ViewModel'i.
 *
 * - Kelime, Kanji ve Gramer sekmeleri / filtreleri sunar.
 * - `searchQuery` üzerinde `debounce(250L)` uygulayarak Japonca (Kanji/Kana), Romaji ve Türkçe anlama
 *   göre akıcı anlık arama yapar.
 * - Kartlara tıklandığında telaffuz sesini çalar (`AudioService.shared.speak`) ve detay sheet'ini açar.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class DictionaryViewModel @Inject constructor(
    private val contentStore: ContentStore
) : ViewModel() {

    constructor() : this(contentStore = ContentStore.shared)

    private val _selectedTab = MutableStateFlow(DictionaryTab.VOCABULARY)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedGrammarCategory = MutableStateFlow<GrammarCategory?>(null)

    private val _selectedWord = MutableStateFlow<VocabularyWord?>(null)
    private val _selectedKanji = MutableStateFlow<Kanji?>(null)
    private val _selectedGrammarPoint = MutableStateFlow<GrammarPoint?>(null)
    private val _selectedVerb = MutableStateFlow<VerbEntry?>(null)

    private val _uiState = MutableStateFlow(DictionaryUiState())
    val uiState: StateFlow<DictionaryUiState> = _uiState.asStateFlow()

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @Volatile
    private var allWords: List<VocabularyWord> = emptyList()

    @Volatile
    private var allKanji: List<Kanji> = emptyList()

    @Volatile
    private var allGrammar: List<GrammarPoint> = emptyList()

    @Volatile
    private var allVerbs: List<VerbEntry> = emptyList()

    init {
        loadCatalogAndObserveSearch()
    }

    private fun ensureLoaded() {
        if (allWords.isEmpty()) {
            allWords = contentStore.loadVocabulary(level = "N5")
        }
        if (allKanji.isEmpty()) {
            allKanji = contentStore.loadKanji(level = "N5")
        }
        if (allGrammar.isEmpty()) {
            allGrammar = contentStore.loadGrammar(level = "N5")
        }
        if (allVerbs.isEmpty()) {
            allVerbs = extractVerbs(allWords)
        }
    }

    private fun loadCatalogAndObserveSearch() {
        viewModelScope.launch(Dispatchers.Default) {
            ensureLoaded()
            val categoryCounts = GrammarCategory.allCases.associateWith { cat ->
                allGrammar.count { it.category == cat }
            }
            _uiState.value = _uiState.value.copy(
                filteredWords = allWords,
                filteredKanji = allKanji,
                filteredGrammar = allGrammar,
                filteredVerbs = allVerbs,
                grammarCategoryCounts = categoryCounts
            )
        }

        val debouncedSearchFlow = _searchQuery
            .debounce(250L)
            .distinctUntilChanged()

        viewModelScope.launch {
            combine(
                _selectedTab,
                debouncedSearchFlow,
                _selectedGrammarCategory
            ) { tab, query, grammarCategory ->
                ensureLoaded()
                val categoryCounts = GrammarCategory.allCases.associateWith { cat ->
                    allGrammar.count { it.category == cat }
                }
                Triple(
                    tab to grammarCategory,
                    query,
                    categoryCounts
                )
            }
                .flowOn(Dispatchers.Default)
                .collect { (tabAndCat, query, categoryCounts) ->
                    val (tab, grammarCategory) = tabAndCat
                    val words = filterVocabulary(allWords, query)
                    val kanji = filterKanji(allKanji, query)
                    val grammar = filterGrammar(allGrammar, query, grammarCategory)
                    val verbs = filterVerbs(allVerbs, query)

                    _uiState.value = _uiState.value.copy(
                        selectedTab = tab,
                        selectedGrammarCategory = grammarCategory,
                        filteredWords = words,
                        filteredKanji = kanji,
                        filteredGrammar = grammar,
                        filteredVerbs = verbs,
                        grammarCategoryCounts = categoryCounts
                    )
                }
        }
    }

    fun selectTab(tab: DictionaryTab) {
        _selectedTab.value = tab
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun clearSearch() {
        onSearchQueryChanged("")
    }

    fun selectGrammarCategory(category: GrammarCategory?) {
        _selectedGrammarCategory.value = category
        _uiState.value = _uiState.value.copy(
            selectedGrammarCategory = category,
            filteredGrammar = filterGrammar(allGrammar, _searchQuery.value, category),
            filteredVerbs = filterVerbs(allVerbs, _searchQuery.value)
        )
    }

    // MARK: - Seçim ve Sesli Telaffuz

    fun onWordClicked(word: VocabularyWord) {
        AudioService.shared.speak(word.speechText)
        _selectedWord.value = word
        _uiState.value = _uiState.value.copy(selectedWord = word)
    }

    fun onKanjiClicked(kanji: Kanji) {
        AudioService.shared.speak(kanji.character)
        _selectedKanji.value = kanji
        _uiState.value = _uiState.value.copy(selectedKanji = kanji)
    }

    fun onGrammarPointClicked(point: GrammarPoint) {
        val speech = point.examples.firstOrNull()?.let { it.hiragana ?: it.japanese } ?: point.pattern
        AudioService.shared.speak(speech)
        _selectedGrammarPoint.value = point
        _uiState.value = _uiState.value.copy(selectedGrammarPoint = point)
    }

    fun onVerbClicked(verb: VerbEntry) {
        AudioService.shared.speak(verb.hiragana)
        _selectedVerb.value = verb
        _uiState.value = _uiState.value.copy(selectedVerb = verb)
    }

    fun dismissDetails() {
        _selectedWord.value = null
        _selectedKanji.value = null
        _selectedGrammarPoint.value = null
        _selectedVerb.value = null
        _uiState.value = _uiState.value.copy(
            selectedWord = null,
            selectedKanji = null,
            selectedGrammarPoint = null,
            selectedVerb = null
        )
    }

    companion object {
        private val turkishLocale = Locale("tr", "TR")
        private val verbEndings = setOf('う', 'く', 'ぐ', 'す', 'つ', 'ぬ', 'ぶ', 'む', 'る')
        private val nonVerbHints = listOf("(ünlem)", "(bağlaç)", "(edat)", "(sıfat)", "(zamir)")

        private fun String.containsQueryIgnoreCase(query: String): Boolean {
            if (query.isBlank()) return true
            val qTrim = query.trim()
            if (this.contains(qTrim, ignoreCase = true)) return true
            val lowerTrThis = this.lowercase(turkishLocale)
            val lowerTrQ = qTrim.lowercase(turkishLocale)
            if (lowerTrThis.contains(lowerTrQ)) return true
            val lowerRootThis = this.lowercase(Locale.ROOT)
            val lowerRootQ = qTrim.lowercase(Locale.ROOT)
            return lowerRootThis.contains(lowerRootQ)
        }

        fun filterVocabulary(
            words: List<VocabularyWord>,
            query: String
        ): List<VocabularyWord> {
            val q = query.trim()
            if (q.isEmpty()) return words
            return words.filter { word ->
                word.turkishMeaning.containsQueryIgnoreCase(q) ||
                    word.kanji.contains(q) ||
                    word.hiragana.contains(q) ||
                    word.romaji.containsQueryIgnoreCase(q)
            }
        }

        fun filterKanji(
            kanjiList: List<Kanji>,
            query: String
        ): List<Kanji> {
            val q = query.trim()
            if (q.isEmpty()) return kanjiList
            return kanjiList.filter { kanji ->
                kanji.character.contains(q) ||
                    kanji.meaning.containsQueryIgnoreCase(q) ||
                    kanji.onyomi.containsQueryIgnoreCase(q) ||
                    kanji.kunyomi.containsQueryIgnoreCase(q) ||
                    kanji.exampleWords.any { ex ->
                        ex.hiragana.contains(q) ||
                            ex.romaji.containsQueryIgnoreCase(q) ||
                            ex.turkishMeaning.containsQueryIgnoreCase(q)
                    }
            }
        }

        fun filterGrammar(
            points: List<GrammarPoint>,
            query: String,
            category: GrammarCategory? = null
        ): List<GrammarPoint> {
            val byCategory = if (category == null) {
                points
            } else {
                points.filter { it.category == category }.sortedBy { it.difficulty }
            }
            val q = query.trim()
            if (q.isEmpty()) return byCategory
            return byCategory.filter { point ->
                point.pattern.contains(q) ||
                    point.title.containsQueryIgnoreCase(q) ||
                    point.romaji.containsQueryIgnoreCase(q) ||
                    point.explanation.containsQueryIgnoreCase(q) ||
                    point.formula.containsQueryIgnoreCase(q)
            }
        }

        /**
         * iOS `VerbCategoryListView.loadVerbs()` mantığıyla N5 kelime listesinden fiilleri çıkarır.
         */
        fun extractVerbs(words: List<VocabularyWord>): List<VerbEntry> {
            val seen = HashSet<String>()
            return words.mapNotNull { word ->
                val hira = word.hiragana
                if (hira.isEmpty()) return@mapNotNull null
                val lastChar = hira.last()
                if (lastChar !in verbEndings) return@mapNotNull null
                val meaning = word.turkishMeaning
                if (meaning.isEmpty()) return@mapNotNull null
                if (nonVerbHints.any { meaning.contains(it) }) return@mapNotNull null
                if (!seen.add(hira)) return@mapNotNull null

                val kanji = word.kanji.takeIf { it.isNotEmpty() }
                val verbType = VerbConjugationEngine.classify(hira)
                VerbEntry(
                    id = hira,
                    hiragana = hira,
                    kanji = kanji,
                    romaji = word.romaji,
                    turkishMeaning = meaning,
                    verbType = verbType
                )
            }.sortedBy { it.hiragana }
        }

        fun filterVerbs(
            verbs: List<VerbEntry>,
            query: String
        ): List<VerbEntry> {
            val q = query.trim()
            if (q.isEmpty()) return verbs
            return verbs.filter { verb ->
                verb.hiragana.contains(q) ||
                    (verb.kanji?.contains(q) == true) ||
                    verb.romaji.containsQueryIgnoreCase(q) ||
                    verb.turkishMeaning.containsQueryIgnoreCase(q)
            }
        }
    }
}
