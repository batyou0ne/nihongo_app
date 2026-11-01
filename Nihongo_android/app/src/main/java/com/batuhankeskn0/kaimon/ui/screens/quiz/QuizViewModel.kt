package com.batuhankeskn0.kaimon.ui.screens.quiz

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.local.LearningItemProgressEntity
import com.batuhankeskn0.kaimon.data.model.QuizItem
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.domain.engine.SpacedRepetitionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/**
 * `QuizViewModel` tarafından dışarıya sunulan salt-okunur (`StateFlow`) durum modeli.
 */
data class QuizUiState<Item : QuizItem>(
    val questions: List<Item> = emptyList(),
    val currentIndex: Int = 0,
    val options: List<String> = emptyList(),
    val selectedAnswer: String? = null,
    val isAnswerCorrect: Boolean? = null,
    val score: Int = 0,
    val currentQuestionModeIsAlternate: Boolean = false
) {
    val currentQuestion: Item?
        get() = questions.getOrNull(currentIndex)

    val isFinished: Boolean
        get() = currentIndex >= questions.size

    val progressFraction: Double
        get() = if (questions.isEmpty()) 0.0 else currentIndex.toDouble() / questions.size.toDouble()
}

/**
 * Tek bir quiz oturumunu yönetir: soru sırası, 4 şıklı çoktan seçmeli seçenek üretimi,
 * `distractorPool` kullanımı, `%50` olasılıkla `alternatePrompt` boşluk doldurma modu,
 * skor takibi ve her cevaptan sonra `SpacedRepetitionService` + `FeedbackManager` tetiklemesi.
 *
 * iOS `ViewModels/QuizViewModel.swift` sınıfının birebir Kotlin karşılığıdır.
 */
class QuizViewModel<Item : QuizItem>(
    questions: List<Item>,
    val itemKind: LearnableItemKind,
    private val progressLookup: (String) -> LearningItemProgressEntity? = { null },
    private val shuffled: Boolean = true,
    distractorPool: List<Item>? = null,
    private val autoUpdateProgress: Boolean = true,
    private val triggerFeedback: Boolean = true,
    private val alternateModeSelector: () -> Boolean = { Random.nextBoolean() },
    private val onProgressUpdated: ((LearningItemProgressEntity, Boolean) -> Unit)? = null
) {

    var questions: List<Item> by mutableStateOf(if (shuffled) questions.shuffled() else questions)
        private set

    var currentIndex: Int by mutableIntStateOf(0)
        private set

    var options: List<String> by mutableStateOf(emptyList())
        private set

    var selectedAnswer: String? by mutableStateOf(null)
        private set

    var isAnswerCorrect: Boolean? by mutableStateOf(null)
        private set

    var score: Int by mutableIntStateOf(0)
        private set

    var currentQuestionModeIsAlternate: Boolean by mutableStateOf(false)
        private set

    /**
     * Yanlış şıklar bu havuzdan seçilir. Varsayılan olarak `questions` ile aynıdır, ama
     * küçük bir soru seti (ör. sadece 2-3 karakterlik bir "yanlışları tekrar et" turu)
     * kullanılırken 4 şık tamamlanamaz — bu yüzden çağıran taraf isterse daha geniş bir
     * havuz (ör. modülün tüm 46 karakteri) verebilir.
     */
    private val distractorPool: List<Item> = distractorPool ?: questions

    private val _uiState = MutableStateFlow(
        QuizUiState(questions = this.questions)
    )
    val uiState: StateFlow<QuizUiState<Item>> = _uiState.asStateFlow()

    val currentQuestion: Item?
        get() = questions.getOrNull(currentIndex)

    val isFinished: Boolean
        get() = currentIndex >= questions.size

    val progressFraction: Double
        get() = if (questions.isEmpty()) 0.0 else currentIndex.toDouble() / questions.size.toDouble()

    init {
        generateOptions()
        syncUiState()
    }

    /**
     * Aktif sorunun beklenen doğru cevabını döner (`alternate` modda `alternateAnswer`, normal modda `correctAnswer`).
     */
    val currentExpectedAnswer: String?
        get() {
            val question = currentQuestion ?: return null
            return if (currentQuestionModeIsAlternate) {
                question.alternateAnswer ?: question.correctAnswer
            } else {
                question.correctAnswer
            }
        }

    /**
     * Doğru cevap + rastgele 3 yanlış şık üretir. Şıklar her zaman `distractorPool`'dan
     * gelir, `questions` (o turun kendi soru seti) değil — böylece 2-3 karakterlik bir tekrar
     * turunda bile eksiksiz 4 şık çıkar.
     */
    fun generateOptions() {
        val question = currentQuestion
        if (question == null) {
            options = emptyList()
            currentQuestionModeIsAlternate = false
            syncUiState()
            return
        }

        // Rastgele %50 ihtimalle alternate mode kullan (fill-in-the-blank)
        val useAlt = alternateModeSelector() &&
            question.alternatePrompt != null &&
            question.alternateAnswer != null
        currentQuestionModeIsAlternate = useAlt

        val answer = if (useAlt) question.alternateAnswer!! else question.correctAnswer

        val distractors = distractorPool
            .asSequence()
            .filter { it.id != question.id }
            .map { if (useAlt) (it.alternateAnswer ?: it.prompt) else it.correctAnswer }
            .filter { it != answer }
            .distinct()
            .toMutableList()

        distractors.shuffle()

        val choices = LinkedHashSet<String>()
        choices.addAll(distractors.take(3))
        choices.add(answer)
        options = choices.toList().shuffled()
        syncUiState()
    }

    /**
     * Kullanıcının seçtiği şıkkı değerlendirir, skoru günceller, ses/titreşim geri bildirimi
     * verir ve `autoUpdateProgress` aktifse `SpacedRepetitionService.updateProgress` çağırır.
     */
    fun submitAnswer(answer: String) {
        val question = currentQuestion ?: return
        if (selectedAnswer != null) return

        selectedAnswer = answer
        val expected = if (currentQuestionModeIsAlternate) {
            question.alternateAnswer ?: question.correctAnswer
        } else {
            question.correctAnswer
        }
        val correct = (answer == expected)
        isAnswerCorrect = correct
        if (correct) {
            score += 1
        }

        if (triggerFeedback) {
            if (correct) {
                FeedbackManager.shared.playSuccess()
                AudioService.shared.playCorrectSound()
            } else {
                FeedbackManager.shared.playError()
                AudioService.shared.playWrongSound()
            }
        }

        if (autoUpdateProgress) {
            progressLookup(question.id)?.let { progress ->
                SpacedRepetitionService.shared.updateProgress(progress = progress, correct = correct)
                onProgressUpdated?.invoke(progress, correct)
            }
        }

        syncUiState()
    }

    /**
     * Bir sonraki soruya geçer ve yeni şıkları üretir.
     */
    fun moveToNext() {
        selectedAnswer = null
        isAnswerCorrect = null
        currentIndex += 1
        generateOptions()
    }

    private fun syncUiState() {
        _uiState.value = QuizUiState(
            questions = questions,
            currentIndex = currentIndex,
            options = options,
            selectedAnswer = selectedAnswer,
            isAnswerCorrect = isAnswerCorrect,
            score = score,
            currentQuestionModeIsAlternate = currentQuestionModeIsAlternate
        )
    }
}
