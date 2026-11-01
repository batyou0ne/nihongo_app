package com.batuhankeskn0.kaimon.ui.screens.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.local.LearningItemProgressEntity
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import com.batuhankeskn0.kaimon.data.model.CharacterType
import com.batuhankeskn0.kaimon.data.model.FlashcardItem
import com.batuhankeskn0.kaimon.data.model.GrammarPoint
import com.batuhankeskn0.kaimon.data.model.JapaneseCharacter
import com.batuhankeskn0.kaimon.data.model.Kanji
import com.batuhankeskn0.kaimon.data.model.VocabularyWord
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.AlphabetAudioService
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.SpacedRepetitionService
import com.batuhankeskn0.kaimon.domain.engine.XPManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Tekrar listesindeki tek bir öğenin görüntüleme modeli.
 */
data class ReviewDisplayItem(
    val id: String,
    val kind: LearnableItemKind,
    val prompt: String,
    val promptReading: String? = null,
    val subtitle: String,
    val srsLevelTitle: String,
    val speechText: String,
    val repetitionCount: Int,
    val needsReview: Boolean
)

/**
 * Tek dokunuşla başlatılan Tekrar Quiz / Flashcard oturumundaki tek bir soru/kart.
 */
data class ReviewSessionQuestion(
    val itemId: String,
    val kind: LearnableItemKind,
    val prompt: String,
    val promptReading: String? = null,
    val flipRecap: String,
    val speechText: String,
    val correctAnswer: String,
    val options: List<String>
)

/**
 * Aktif Tekrar Quiz / Flashcard oturumu durumu.
 */
data class ReviewPracticeSessionState(
    val kind: LearnableItemKind,
    val title: String,
    val questions: List<ReviewSessionQuestion>,
    val currentIndex: Int = 0,
    val selectedOption: String? = null,
    val isFlipped: Boolean = false,
    val correctCount: Int = 0,
    val isCompleted: Boolean = false
) {
    val currentQuestion: ReviewSessionQuestion?
        get() = questions.getOrNull(currentIndex)

    val progressFraction: Double
        get() = if (questions.isNotEmpty()) (currentIndex.toDouble() / questions.size.toDouble()) else 0.0
}

/**
 * `ReviewListScreen` arayüz durumu.
 */
data class ReviewUiState(
    val hiraganaItems: List<ReviewDisplayItem> = emptyList(),
    val katakanaItems: List<ReviewDisplayItem> = emptyList(),
    val kanjiItems: List<ReviewDisplayItem> = emptyList(),
    val vocabularyItems: List<ReviewDisplayItem> = emptyList(),
    val grammarItems: List<ReviewDisplayItem> = emptyList(),
    val activePracticeSession: ReviewPracticeSessionState? = null
) {
    val isEmpty: Boolean
        get() = hiraganaItems.isEmpty() &&
            katakanaItems.isEmpty() &&
            kanjiItems.isEmpty() &&
            vocabularyItems.isEmpty() &&
            grammarItems.isEmpty()

    val totalCount: Int
        get() = hiraganaItems.size +
            katakanaItems.size +
            kanjiItems.size +
            vocabularyItems.size +
            grammarItems.size

    fun itemsForKind(kind: LearnableItemKind): List<ReviewDisplayItem> =
        when (kind) {
            LearnableItemKind.HIRAGANA_CHARACTER -> hiraganaItems
            LearnableItemKind.KATAKANA_CHARACTER -> katakanaItems
            LearnableItemKind.KANJI -> kanjiItems
            LearnableItemKind.VOCABULARY_WORD -> vocabularyItems
            LearnableItemKind.GRAMMAR -> grammarItems
        }
}

/**
 * Tekrar Ekranı (`ReviewListScreen`) ViewModel'i.
 * iOS `Views/Review/ReviewListView.swift` davranışını birebir uygular:
 * - `needsReview || (isLearned && dueDate <= now)` öğelerini filtreler ve modüle göre gruplar.
 * - Sesli okuma (`speakItem`) ve tek dokunuşla `"Öğrendim ✓"` (`markAsLearned`) aksiyonlarını çalıştırır.
 * - `"Bunlarla çalış →"` ile ilgili modülün tekrar öğeleri için mini Flashcard / Quiz oturumu başlatır.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val userDatabaseManager: UserDatabaseManager,
    private val contentStore: ContentStore,
    private val spacedRepetitionService: SpacedRepetitionService
) : ViewModel() {

    constructor() : this(
        userDatabaseManager = UserDatabaseManager.shared,
        contentStore = ContentStore.shared,
        spacedRepetitionService = SpacedRepetitionService.shared
    )

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    @Volatile
    private var cachedHiragana: List<JapaneseCharacter> = emptyList()

    @Volatile
    private var cachedKatakana: List<JapaneseCharacter> = emptyList()

    @Volatile
    private var cachedKanji: List<Kanji> = emptyList()

    @Volatile
    private var cachedVocabulary: List<VocabularyWord> = emptyList()

    @Volatile
    private var cachedGrammar: List<GrammarPoint> = emptyList()

    init {
        observeReviewItems()
    }

    private fun ensureCatalogLoaded() {
        if (cachedHiragana.isEmpty()) {
            cachedHiragana = contentStore.loadCharacters(CharacterType.HIRAGANA)
        }
        if (cachedKatakana.isEmpty()) {
            cachedKatakana = contentStore.loadCharacters(CharacterType.KATAKANA)
        }
        if (cachedKanji.isEmpty()) {
            cachedKanji = JLPTLevel.allCases
                .flatMap { contentStore.loadKanji(it.rawValue) }
                .distinctBy { it.id }
        }
        if (cachedVocabulary.isEmpty()) {
            cachedVocabulary = JLPTLevel.allCases
                .flatMap { contentStore.loadVocabulary(it.rawValue) }
                .distinctBy { it.id }
        }
        if (cachedGrammar.isEmpty()) {
            cachedGrammar = JLPTLevel.allCases
                .flatMap { contentStore.loadGrammar(it.rawValue) }
                .distinctBy { it.id }
        }
    }

    private fun observeReviewItems() {
        viewModelScope.launch {
            userDatabaseManager.currentUidFlow
                .flatMapLatest { uid ->
                    val db = userDatabaseManager.getDatabase(uid)
                    db.progressDao().observeAllItemProgress().map { allProgress ->
                        ensureCatalogLoaded()
                        buildReviewUiState(
                            allProgress = allProgress,
                            hiragana = cachedHiragana,
                            katakana = cachedKatakana,
                            kanji = cachedKanji,
                            vocabulary = cachedVocabulary,
                            grammar = cachedGrammar,
                            currentSession = _uiState.value.activePracticeSession
                        )
                    }
                }
                .flowOn(Dispatchers.IO)
                .collect { newState ->
                    _uiState.value = newState
                }
        }
    }

    /**
     * Bir öğeyi "Öğrendim ✓" olarak işaretler (`SpacedRepetitionService.updateAndSaveProgress`).
     * iOS `ReviewListView.markAsLearned(kind:itemID:)` karşılığıdır.
     */
    fun markAsLearned(
        kind: LearnableItemKind,
        itemID: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val db = userDatabaseManager.getDatabase()
            val progressDao = db.progressDao()
            val existing = progressDao.getItemProgress(itemID)
                ?: LearningItemProgressEntity(itemID = itemID, itemKind = kind)
            spacedRepetitionService.updateAndSaveProgress(
                progress = existing,
                correct = true,
                progressDao = progressDao,
                nowMillis = nowMillis
            )
            runCatching {
                XPManager.shared.addXP(
                    action = XPManager.XPAction.CARD_REVIEWED,
                    progressDao = progressDao,
                    activityDao = db.activityDao(),
                    nowMillis = nowMillis
                )
            }
        }
    }

    /**
     * İlgili öğenin telaffuz sesini çalar.
     */
    fun speakItem(item: ReviewDisplayItem) {
        if (item.kind == LearnableItemKind.HIRAGANA_CHARACTER ||
            item.kind == LearnableItemKind.KATAKANA_CHARACTER
        ) {
            AlphabetAudioService.shared.play(item.speechText)
        } else {
            AudioService.shared.speak(item.speechText)
        }
    }

    /**
     * Seçilen modüldeki tekrar öğeleri için tek dokunuşla Flashcard / Quiz oturumu başlatır.
     */
    fun startPracticeSession(kind: LearnableItemKind) {
        viewModelScope.launch(Dispatchers.IO) {
            ensureCatalogLoaded()
            val currentState = _uiState.value
            val reviewItems = currentState.itemsForKind(kind)
            if (reviewItems.isEmpty()) return@launch

            val questions = buildPracticeQuestions(
                kind = kind,
                reviewItems = reviewItems,
                hiragana = cachedHiragana,
                katakana = cachedKatakana,
                kanji = cachedKanji,
                vocabulary = cachedVocabulary,
                grammar = cachedGrammar
            )
            if (questions.isEmpty()) return@launch

            val session = ReviewPracticeSessionState(
                kind = kind,
                title = L10n.reviewPrefix(kind.displayName),
                questions = questions
            )
            _uiState.value = _uiState.value.copy(activePracticeSession = session)
        }
    }

    fun selectPracticeOption(option: String, nowMillis: Long = System.currentTimeMillis()) {
        val session = _uiState.value.activePracticeSession ?: return
        if (session.selectedOption != null || session.isCompleted) return
        val question = session.currentQuestion ?: return

        val isCorrect = option == question.correctAnswer
        val updatedSession = session.copy(
            selectedOption = option,
            isFlipped = true,
            correctCount = if (isCorrect) session.correctCount + 1 else session.correctCount
        )
        _uiState.value = _uiState.value.copy(activePracticeSession = updatedSession)

        viewModelScope.launch(Dispatchers.IO) {
            val db = userDatabaseManager.getDatabase()
            val progressDao = db.progressDao()
            val existing = progressDao.getItemProgress(question.itemId)
                ?: LearningItemProgressEntity(itemID = question.itemId, itemKind = question.kind)
            spacedRepetitionService.updateAndSaveProgress(
                progress = existing,
                correct = isCorrect,
                progressDao = progressDao,
                nowMillis = nowMillis
            )
            if (isCorrect) {
                runCatching {
                    XPManager.shared.addXP(
                        action = XPManager.XPAction.CARD_REVIEWED,
                        progressDao = progressDao,
                        activityDao = db.activityDao(),
                        nowMillis = nowMillis
                    )
                }
            }
        }
    }

    fun flipPracticeCard() {
        val session = _uiState.value.activePracticeSession ?: return
        _uiState.value = _uiState.value.copy(
            activePracticeSession = session.copy(isFlipped = !session.isFlipped)
        )
    }

    fun nextPracticeQuestion() {
        val session = _uiState.value.activePracticeSession ?: return
        val nextIdx = session.currentIndex + 1
        if (nextIdx >= session.questions.size) {
            _uiState.value = _uiState.value.copy(
                activePracticeSession = session.copy(isCompleted = true)
            )
        } else {
            _uiState.value = _uiState.value.copy(
                activePracticeSession = session.copy(
                    currentIndex = nextIdx,
                    selectedOption = null,
                    isFlipped = false
                )
            )
        }
    }

    fun closePracticeSession() {
        _uiState.value = _uiState.value.copy(activePracticeSession = null)
    }

    companion object {
        fun filterDueOrNeedsReview(
            allProgress: List<LearningItemProgressEntity>,
            nowMillis: Long = System.currentTimeMillis()
        ): List<LearningItemProgressEntity> =
            allProgress.filter { it.needsReview || (it.isLearned && it.dueDate <= nowMillis) }

        fun srsLevelTitleFor(repetitionCount: Int): String =
            SpacedRepetitionService.SRSLevel.fromRawValue(repetitionCount)?.title ?: L10n.srsStageNew

        fun buildReviewUiState(
            allProgress: List<LearningItemProgressEntity>,
            hiragana: List<JapaneseCharacter>,
            katakana: List<JapaneseCharacter>,
            kanji: List<Kanji>,
            vocabulary: List<VocabularyWord>,
            grammar: List<GrammarPoint>,
            currentSession: ReviewPracticeSessionState? = null,
            nowMillis: Long = System.currentTimeMillis()
        ): ReviewUiState {
            val reviewProgress = filterDueOrNeedsReview(allProgress, nowMillis)
            val progressByKindAndId = reviewProgress.associateBy { it.itemKind to it.itemID }

            fun <T : FlashcardItem> mapFlashcardItems(
                kind: LearnableItemKind,
                catalog: List<T>
            ): List<ReviewDisplayItem> {
                val ids = reviewProgress.asSequence()
                    .filter { it.itemKind == kind }
                    .map { it.itemID }
                    .toSet()
                if (ids.isEmpty()) return emptyList()

                return catalog.asSequence()
                    .filter { ids.contains(it.id) }
                    .map { item ->
                        val prog = progressByKindAndId[kind to item.id]
                        val repCount = prog?.repetitionCount ?: 0
                        ReviewDisplayItem(
                            id = item.id,
                            kind = kind,
                            prompt = item.prompt,
                            promptReading = item.promptReading,
                            subtitle = item.flipRecap,
                            srsLevelTitle = srsLevelTitleFor(repCount),
                            speechText = item.speechText,
                            repetitionCount = repCount,
                            needsReview = prog?.needsReview ?: false
                        )
                    }
                    .toList()
            }

            val grammarIds = reviewProgress.asSequence()
                .filter { it.itemKind == LearnableItemKind.GRAMMAR }
                .map { it.itemID }
                .toSet()

            val grammarDisplayItems = if (grammarIds.isEmpty()) {
                emptyList()
            } else {
                grammar.asSequence()
                    .filter { grammarIds.contains(it.id) }
                    .map { point ->
                        val prog = progressByKindAndId[LearnableItemKind.GRAMMAR to point.id]
                        val repCount = prog?.repetitionCount ?: 0
                        val exampleSpeech = point.examples.firstOrNull()?.let { it.hiragana ?: it.japanese }
                            ?: point.pattern
                        ReviewDisplayItem(
                            id = point.id,
                            kind = LearnableItemKind.GRAMMAR,
                            prompt = point.pattern,
                            promptReading = point.romaji,
                            subtitle = point.title,
                            srsLevelTitle = srsLevelTitleFor(repCount),
                            speechText = exampleSpeech,
                            repetitionCount = repCount,
                            needsReview = prog?.needsReview ?: false
                        )
                    }
                    .toList()
            }

            return ReviewUiState(
                hiraganaItems = mapFlashcardItems(LearnableItemKind.HIRAGANA_CHARACTER, hiragana),
                katakanaItems = mapFlashcardItems(LearnableItemKind.KATAKANA_CHARACTER, katakana),
                kanjiItems = mapFlashcardItems(LearnableItemKind.KANJI, kanji),
                vocabularyItems = mapFlashcardItems(LearnableItemKind.VOCABULARY_WORD, vocabulary),
                grammarItems = grammarDisplayItems,
                activePracticeSession = currentSession
            )
        }

        fun buildPracticeQuestions(
            kind: LearnableItemKind,
            reviewItems: List<ReviewDisplayItem>,
            hiragana: List<JapaneseCharacter>,
            katakana: List<JapaneseCharacter>,
            kanji: List<Kanji>,
            vocabulary: List<VocabularyWord>,
            grammar: List<GrammarPoint>
        ): List<ReviewSessionQuestion> {
            return when (kind) {
                LearnableItemKind.HIRAGANA_CHARACTER ->
                    buildFlashcardQuestions(kind, reviewItems, hiragana)
                LearnableItemKind.KATAKANA_CHARACTER ->
                    buildFlashcardQuestions(kind, reviewItems, katakana)
                LearnableItemKind.KANJI ->
                    buildFlashcardQuestions(kind, reviewItems, kanji)
                LearnableItemKind.VOCABULARY_WORD ->
                    buildFlashcardQuestions(kind, reviewItems, vocabulary)
                LearnableItemKind.GRAMMAR -> {
                    val map = grammar.associateBy { it.id }
                    val allTitles = grammar.map { it.title }.distinct()
                    reviewItems.mapNotNull { display ->
                        val point = map[display.id] ?: return@mapNotNull null
                        val firstQ = point.questions.firstOrNull()
                        if (firstQ != null && firstQ.choices.isNotEmpty()) {
                            ReviewSessionQuestion(
                                itemId = point.id,
                                kind = kind,
                                prompt = firstQ.prompt,
                                promptReading = firstQ.promptReading ?: point.pattern,
                                flipRecap = "${point.pattern} — ${point.title}\n${point.explanation}",
                                speechText = display.speechText,
                                correctAnswer = firstQ.answer,
                                options = firstQ.choices.shuffled()
                            )
                        } else {
                            val distractors = allTitles.filter { it != point.title }.shuffled().take(3)
                            val options = (distractors + point.title).shuffled()
                            ReviewSessionQuestion(
                                itemId = point.id,
                                kind = kind,
                                prompt = point.pattern,
                                promptReading = point.romaji,
                                flipRecap = "${point.pattern} — ${point.title}\n${point.explanation}",
                                speechText = display.speechText,
                                correctAnswer = point.title,
                                options = options
                            )
                        }
                    }
                }
            }
        }

        private fun <T : FlashcardItem> buildFlashcardQuestions(
            kind: LearnableItemKind,
            reviewItems: List<ReviewDisplayItem>,
            pool: List<T>
        ): List<ReviewSessionQuestion> {
            val itemMap = pool.associateBy { it.id }
            val allAnswers = pool.map { it.correctAnswer }.distinct()
            return reviewItems.mapNotNull { display ->
                val item = itemMap[display.id] ?: return@mapNotNull null
                val distractors = allAnswers
                    .filter { it != item.correctAnswer }
                    .shuffled()
                    .take(3)
                val options = (distractors + item.correctAnswer).shuffled()
                ReviewSessionQuestion(
                    itemId = item.id,
                    kind = kind,
                    prompt = item.prompt,
                    promptReading = item.promptReading,
                    flipRecap = item.flipRecap,
                    speechText = item.speechText,
                    correctAnswer = item.correctAnswer,
                    options = options
                )
            }
        }
    }
}
