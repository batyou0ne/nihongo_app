package com.batuhankeskn0.kaimon.ui.screens.grammar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.local.ActivityDao
import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.local.LearningItemProgressEntity
import com.batuhankeskn0.kaimon.data.local.LevelProgressDao
import com.batuhankeskn0.kaimon.data.local.ProgressDao
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import com.batuhankeskn0.kaimon.data.model.GrammarPoint
import com.batuhankeskn0.kaimon.data.model.GrammarQuestion
import com.batuhankeskn0.kaimon.data.model.GrammarQuestionKind
import com.batuhankeskn0.kaimon.data.model.GrammarUnit
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import com.batuhankeskn0.kaimon.domain.engine.SpacedRepetitionService
import com.batuhankeskn0.kaimon.domain.engine.XPManager
import com.batuhankeskn0.kaimon.ui.components.FuriganaText
import com.batuhankeskn0.kaimon.ui.components.LessonCompleteView
import com.batuhankeskn0.kaimon.ui.components.ProgressBar
import com.batuhankeskn0.kaimon.ui.navigation.LocalTabBarManager
import com.batuhankeskn0.kaimon.ui.theme.PrimaryButton
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.max

/**
 * `wordOrder` sorusundaki kelime bloğu — aynı metinli iki kart olabildiği için
 * (ör. iki tane `は`) benzersiz kimlik `id` üzerinden takip edilir.
 */
data class WordOrderToken(
    val id: String = UUID.randomUUID().toString(),
    val text: String
)

/**
 * Pratik sonunda özet ekranında gösterilen yanlış cevap kaydı.
 */
data class GrammarWrongEntry(
    val id: String = UUID.randomUUID().toString(),
    val pointTitle: String,
    val prompt: String,
    val answer: String
)

/**
 * Gramer pratik oturumunun 3 soru tipini (`fillBlank`, `wordOrder`, `multipleChoice`),
 * puanlama, SRS ve XP kazanım akışını yöneten durum denetleyicisi.
 * Hem `GrammarPracticeScreen` tarafından kullanılır hem de JVM birim testlerinde doğrulanır.
 */
class GrammarPracticeController(
    val points: List<GrammarPoint>,
    private val shuffleWordOrderChoices: Boolean = true,
    private val onPointFinalized: ((GrammarPoint, Boolean) -> Int)? = null,
    private val onAllPointsFinished: (() -> Unit)? = null
) {
    var pointIndex: Int by mutableIntStateOf(0)
        private set

    var questionIndex: Int by mutableIntStateOf(0)
        private set

    var selectedChoice: String? by mutableStateOf(null)
        private set

    var isChoiceLocked: Boolean by mutableStateOf(false)
        private set

    var orderAssembled: List<WordOrderToken> by mutableStateOf(emptyList())
        private set

    var orderPool: List<WordOrderToken> by mutableStateOf(emptyList())
        private set

    var isOrderChecked: Boolean by mutableStateOf(false)
        private set

    var isOrderCorrect: Boolean by mutableStateOf(false)
        private set

    var pointWrongCount: Int by mutableIntStateOf(0)
        private set

    var totalAnswered: Int by mutableIntStateOf(0)
        private set

    var totalWrong: Int by mutableIntStateOf(0)
        private set

    var wrongSummary: List<GrammarWrongEntry> by mutableStateOf(emptyList())
        private set

    var isFinished: Boolean by mutableStateOf(false)
        private set

    var sessionEarnedXP: Int by mutableIntStateOf(0)
        private set

    val currentPoint: GrammarPoint?
        get() = points.getOrNull(pointIndex)

    val currentQuestion: GrammarQuestion?
        get() = currentPoint?.questions?.getOrNull(questionIndex)

    val totalQuestions: Int
        get() = points.sumOf { it.questions.size }

    val answeredFraction: Double
        get() = if (totalQuestions == 0) 0.0 else totalAnswered.toDouble() / totalQuestions.toDouble()

    val hasAnswered: Boolean
        get() = isChoiceLocked || isOrderChecked

    val canCheckOrder: Boolean
        get() = orderPool.isEmpty() && orderAssembled.isNotEmpty()

    init {
        skipEmptyPointsIfNeeded()
        resetQuestionState()
    }

    private fun skipEmptyPointsIfNeeded() {
        while (pointIndex < points.size && points[pointIndex].questions.isEmpty()) {
            val point = points[pointIndex]
            val xp = onPointFinalized?.invoke(point, true) ?: 0
            sessionEarnedXP += xp
            pointIndex += 1
        }
        if (pointIndex >= points.size) {
            isFinished = true
            onAllPointsFinished?.invoke()
        }
    }

    fun resetQuestionState() {
        selectedChoice = null
        isChoiceLocked = false
        isOrderChecked = false
        isOrderCorrect = false
        orderAssembled = emptyList()
        val q = currentQuestion
        orderPool = if (q != null && q.kind == GrammarQuestionKind.WORD_ORDER) {
            val rawChoices = if (shuffleWordOrderChoices) q.choices.shuffled() else q.choices
            rawChoices.map { WordOrderToken(text = it) }
        } else {
            emptyList()
        }
    }

    /**
     * `fillBlank` veya `multipleChoice` sorusunda şık seçimi yapar.
     */
    fun selectChoice(option: String): Boolean {
        val q = currentQuestion ?: return false
        if (q.kind == GrammarQuestionKind.WORD_ORDER || isChoiceLocked) return false
        selectedChoice = option
        isChoiceLocked = true
        val isCorrect = option == q.answer
        registerAnswer(correct = isCorrect)
        return isCorrect
    }

    /**
     * `wordOrder` sorusunda havuzdaki bir kelime bloğuna dokunulduğunda cevaba ekler.
     */
    fun selectPoolToken(tokenId: String) {
        if (isOrderChecked) return
        val token = orderPool.firstOrNull { it.id == tokenId } ?: return
        orderPool = orderPool.filterNot { it.id == tokenId }
        orderAssembled = orderAssembled + token
    }

    /**
     * `wordOrder` sorusunda dizilen cevaptaki bir kelime bloğuna dokunulduğunda havuza geri gönderir.
     */
    fun removeAssembledToken(tokenId: String) {
        if (isOrderChecked) return
        val token = orderAssembled.firstOrNull { it.id == tokenId } ?: return
        orderAssembled = orderAssembled.filterNot { it.id == tokenId }
        orderPool = orderPool + token
    }

    /**
     * `wordOrder` sorusunda tüm bloklar yerleştirildikten sonra sıralamayı kontrol eder.
     */
    fun checkOrder(): Boolean {
        val q = currentQuestion ?: return false
        if (q.kind != GrammarQuestionKind.WORD_ORDER || isOrderChecked || !canCheckOrder) return false
        isOrderChecked = true
        val joined = orderAssembled.joinToString("") { it.text }
        val tokensMatch = q.answerTokens != null && orderAssembled.map { it.text } == q.answerTokens
        isOrderCorrect = (joined == q.answer) || tokensMatch
        registerAnswer(correct = isOrderCorrect)
        return isOrderCorrect
    }

    private fun registerAnswer(correct: Boolean) {
        totalAnswered += 1
        if (!correct) {
            val point = currentPoint ?: return
            val question = currentQuestion ?: return
            pointWrongCount += 1
            totalWrong += 1
            wrongSummary = wrongSummary + GrammarWrongEntry(
                pointTitle = point.title,
                prompt = question.prompt,
                answer = question.answer
            )
        }
    }

    /**
     * Sonraki soruya veya konuya ilerler; tüm konular bittiğinde oturumu tamamlar.
     */
    fun advance() {
        val point = currentPoint ?: return

        if (questionIndex + 1 < point.questions.size) {
            questionIndex += 1
            resetQuestionState()
            return
        }

        // Konu tamamlandı -> SRS & XP
        val allCorrectForPoint = (pointWrongCount == 0)
        val earned = onPointFinalized?.invoke(point, allCorrectForPoint) ?: 0
        sessionEarnedXP += earned

        if (pointIndex + 1 < points.size) {
            pointIndex += 1
            questionIndex = 0
            pointWrongCount = 0
            skipEmptyPointsIfNeeded()
            if (!isFinished) {
                resetQuestionState()
            }
        } else {
            isFinished = true
            onAllPointsFinished?.invoke()
        }
    }
}

/**
 * Bir gramer konusunun tamamlanmasını veritabanına (`LearningItemProgressEntity`, `SpacedRepetitionService`,
 * `XPManager`) kaydeder ve kazanılan XP miktarını döner.
 */
suspend fun recordGrammarPointCompletion(
    point: GrammarPoint,
    allQuestionsCorrect: Boolean,
    progressDao: ProgressDao = UserDatabaseManager.shared.progressDao,
    activityDao: ActivityDao = UserDatabaseManager.shared.activityDao,
    srsService: SpacedRepetitionService = SpacedRepetitionService.shared,
    xpManager: XPManager = XPManager.shared,
    nowMillis: Long = System.currentTimeMillis()
): Int {
    val existing = progressDao.getItemProgress(
        itemID = point.id
    ) ?: LearningItemProgressEntity(
        itemID = point.id,
        itemKind = LearnableItemKind.GRAMMAR
    )

    val wasLearned = existing.isLearned
    val updated = srsService.updateProgress(
        progress = existing,
        correct = allQuestionsCorrect,
        nowMillis = nowMillis
    )
    if (!allQuestionsCorrect) {
        updated.needsReview = true
    }
    progressDao.upsertItemProgress(updated)

    if (allQuestionsCorrect) {
        val action = if (wasLearned) {
            XPManager.XPAction.CARD_REVIEWED
        } else {
            XPManager.XPAction.GRAMMAR_COMPLETED
        }
        xpManager.addXP(
            action = action,
            progressDao = progressDao,
            activityDao = activityDao,
            nowMillis = nowMillis
        )
        return action.points
    }
    return 0
}

/**
 * Bir gramer ünitesindeki tüm konular öğrenilmişse (`repetitionCount >= 1` veya `isLearned`),
 * `LevelProgressionService.markPartCompleted(module = "grammar", ...)` ile üniteyi tamamlandı işaretler.
 */
suspend fun syncGrammarUnitsCompletion(
    level: JLPTLevel,
    syllabus: List<GrammarUnit>,
    pointsByKey: Map<String, GrammarPoint>,
    progressDao: ProgressDao = UserDatabaseManager.shared.progressDao,
    levelProgressDao: LevelProgressDao = UserDatabaseManager.shared.levelProgressDao,
    progressionService: LevelProgressionService = LevelProgressionService.shared
): Set<Int> {
    val grammarProgress = progressDao.getProgressByKind(LearnableItemKind.GRAMMAR)
    val learnedIDs = grammarProgress
        .filter { it.isLearned || it.repetitionCount >= 1 }
        .map { it.itemID }
        .toSet()

    val completedIndices = mutableSetOf<Int>()
    syllabus.forEachIndexed { index, unit ->
        if (isGrammarUnitFinished(unit, pointsByKey, learnedIDs)) {
            progressionService.markPartCompleted(
                module = "grammar",
                level = level,
                partIndex = index,
                dao = levelProgressDao
            )
            completedIndices.add(index)
        }
    }
    return completedIndices
}

/**
 * Verilen `GrammarUnit` içindeki tüm gramer anahtarlarının öğrenilmiş olup olmadığını denetler.
 */
fun isGrammarUnitFinished(
    unit: GrammarUnit,
    pointsByKey: Map<String, GrammarPoint>,
    learnedIDs: Set<String>
): Boolean {
    if (unit.grammarKeys.isEmpty()) return false
    for (key in unit.grammarKeys) {
        val point = pointsByKey[key] ?: return false
        if (!learnedIDs.contains(point.id)) return false
    }
    return true
}

/**
 * Gramer Pratik Ekranı (`Views/Grammar/GrammarPracticeView.swift` birebir karşılığı).
 *
 * - 3 farklı pratik soru tipi:
 *   1. `FILL_BLANK` (Cümledeki boşluğa doğru edatı/eki seçme)
 *   2. `WORD_ORDER` (Karışık verilen Japonca kelime bloklarına dokunarak doğru sırayla cümle kurma)
 *   3. `MULTIPLE_CHOICE` (4 şıklı anlam/kullanım sorusu)
 * - Konu tamamlandığında `LearningItemProgress` (`itemKind = GRAMMAR`),
 *   `LevelProgressionService.markPartCompleted(module = "grammar", ...)` ve
 *   `XPManager.addXP(GRAMMAR_COMPLETED)` güncellemesi yapar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GrammarPracticeScreen(
    points: List<GrammarPoint>,
    title: String = L10n.grammarPracticeTitle,
    level: String = "N5",
    unitIndex: Int? = null,
    onFinish: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tabBarManager = LocalTabBarManager.current
    val scope = rememberCoroutineScope()
    val jlptLevel = remember(level) { JLPTLevel.fromRawValue(level) ?: JLPTLevel.N5 }

    var showLessonComplete by remember { mutableStateOf(false) }
    var completedStreak by remember { mutableIntStateOf(0) }
    var didStreakIncrease by remember { mutableStateOf(false) }
    var earnedXpState by remember { mutableIntStateOf(0) }

    DisposableEffect(tabBarManager) {
        val wasHidden = tabBarManager.isHidden
        tabBarManager.hide()
        onDispose {
            if (!wasHidden) {
                tabBarManager.show()
            }
        }
    }

    val controller = remember(points) {
        GrammarPracticeController(
            points = points,
            onPointFinalized = { point, allCorrect ->
                val xp = if (allCorrect) XPManager.XPAction.GRAMMAR_COMPLETED.points else 0
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        val awarded = recordGrammarPointCompletion(
                            point = point,
                            allQuestionsCorrect = allCorrect
                        )
                        val syllabus = ContentStore.loadGrammarSyllabus(level = level)
                        val allGrammar = ContentStore.loadGrammar(level = level)
                        val pointsDict = allGrammar.associateBy { it.key }
                        syncGrammarUnitsCompletion(
                            level = jlptLevel,
                            syllabus = syllabus,
                            pointsByKey = pointsDict
                        )
                        if (unitIndex != null && allCorrect) {
                            // Eğer ünite bazlı pratik tamamlandıysa ve tüm konular öğrenildiyse
                            val unit = syllabus.getOrNull(unitIndex)
                            if (unit != null) {
                                val learnedIDs = UserDatabaseManager.shared.progressDao
                                    .getProgressByKind(LearnableItemKind.GRAMMAR)
                                    .filter { it.isLearned || it.repetitionCount >= 1 }
                                    .map { it.itemID }
                                    .toSet()
                                if (isGrammarUnitFinished(unit, pointsDict, learnedIDs)) {
                                    LevelProgressionService.shared.markPartCompleted(
                                        module = "grammar",
                                        level = jlptLevel,
                                        partIndex = unitIndex
                                    )
                                }
                            }
                        }
                        withContext(Dispatchers.Main) {
                            if (awarded > 0) {
                                earnedXpState += awarded
                            }
                        }
                    }
                }
                xp
            },
            onAllPointsFinished = {
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        val nowMillis = System.currentTimeMillis()
                        val progressDao = UserDatabaseManager.shared.progressDao
                        val userProgress = progressDao.getOrCreateUserProgress()
                        val prevStreak = userProgress.activeStreak(nowMillis)
                        userProgress.recordStudySession(on = nowMillis)
                        progressDao.upsertUserProgress(userProgress)
                        val newStreak = userProgress.activeStreak(nowMillis)

                        withContext(Dispatchers.Main) {
                            completedStreak = newStreak
                            didStreakIncrease = newStreak > prevStreak
                            showLessonComplete = true
                        }
                    }
                }
            }
        )
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching {
                val progressDao = UserDatabaseManager.shared.progressDao
                for (point in points) {
                    val existing = progressDao.getItemProgress(point.id)
                    if (existing == null) {
                        progressDao.upsertItemProgress(
                            LearningItemProgressEntity(
                                itemID = point.id,
                                itemKind = LearnableItemKind.GRAMMAR
                            )
                        )
                    }
                }
            }
        }
    }

    if (onFinish != null) {
        BackHandler { onFinish() }
    }

    if (showLessonComplete) {
        val displayXp = max(20, if (earnedXpState > 0) earnedXpState else controller.sessionEarnedXP)
        LessonCompleteView(
            xpGained = displayXp,
            streak = completedStreak,
            streakIncreased = didStreakIncrease,
            ringColor = Theme.accent,
            onContinue = { showLessonComplete = false },
            modifier = modifier
        )
        return
    }

    if (controller.isFinished) {
        GrammarPracticeSummaryContent(
            totalAnswered = controller.totalAnswered,
            totalWrong = controller.totalWrong,
            wrongSummary = controller.wrongSummary,
            onFinish = { onFinish?.invoke() },
            modifier = modifier
        )
        return
    }

    val point = controller.currentPoint
    val question = controller.currentQuestion
    if (point == null || question == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Theme.paper),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Theme.accent)
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
            .padding(top = 12.dp)
    ) {
        // Üst Navigasyon Barı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onFinish != null) {
                Row(
                    modifier = Modifier
                        .clickable {
                            FeedbackManager.shared.playSelection()
                            onFinish()
                        }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = L10n.backButton,
                        tint = Theme.ink,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = L10n.backButton,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Theme.ink
                    )
                }
            }

            Text(
                text = title,
                style = Theme.heading(18.sp),
                color = Theme.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // İlerleme Çubuğu ve Konu Sayacı
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ProgressBar(fraction = controller.answeredFraction)
            if (points.size > 1) {
                Text(
                    text = L10n.grammarTopicProgress(controller.pointIndex + 1, points.size),
                    fontSize = 12.sp,
                    color = Theme.secondaryInk
                )
            }
        }

        // Soru İçeriği
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = point.pattern,
                style = Theme.heading(18.sp),
                color = Theme.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            when (question.kind) {
                GrammarQuestionKind.FILL_BLANK,
                GrammarQuestionKind.MULTIPLE_CHOICE -> {
                    GrammarChoiceQuestionContent(
                        question = question,
                        selectedChoice = controller.selectedChoice,
                        isChoiceLocked = controller.isChoiceLocked,
                        onSelectChoice = { option ->
                            val isCorrect = controller.selectChoice(option)
                            if (isCorrect) {
                                FeedbackManager.shared.playSuccess()
                                AudioService.shared.playCorrectSound()
                            } else {
                                FeedbackManager.shared.playError()
                                AudioService.shared.playWrongSound()
                            }
                        }
                    )
                }
                GrammarQuestionKind.WORD_ORDER -> {
                    GrammarWordOrderQuestionContent(
                        question = question,
                        orderAssembled = controller.orderAssembled,
                        orderPool = controller.orderPool,
                        isOrderChecked = controller.isOrderChecked,
                        isOrderCorrect = controller.isOrderCorrect,
                        onSelectPoolToken = { tokenId ->
                            FeedbackManager.shared.playSelection()
                            controller.selectPoolToken(tokenId)
                        },
                        onRemoveAssembledToken = { tokenId ->
                            FeedbackManager.shared.playSelection()
                            controller.removeAssembledToken(tokenId)
                        }
                    )
                }
            }
        }

        // Alt Aksiyon Butonu ("Kontrol Et" / "Devam Et")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            if (question.kind == GrammarQuestionKind.WORD_ORDER && !controller.isOrderChecked) {
                PrimaryButton(
                    text = L10n.grammarCheckOrderButton,
                    enabled = controller.canCheckOrder,
                    onClick = {
                        val isCorrect = controller.checkOrder()
                        if (isCorrect) {
                            FeedbackManager.shared.playSuccess()
                            AudioService.shared.playCorrectSound()
                        } else {
                            FeedbackManager.shared.playError()
                            AudioService.shared.playWrongSound()
                        }
                    }
                )
            } else if (controller.hasAnswered) {
                PrimaryButton(
                    text = L10n.continueButton,
                    onClick = {
                        FeedbackManager.shared.playSelection()
                        controller.advance()
                    }
                )
            }
        }
    }
}

@Composable
private fun GrammarChoiceQuestionContent(
    question: GrammarQuestion,
    selectedChoice: String?,
    isChoiceLocked: Boolean,
    onSelectChoice: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = if (question.kind == GrammarQuestionKind.FILL_BLANK) {
                L10n.grammarFillBlankPromptLabel
            } else {
                L10n.grammarMultipleChoicePromptLabel
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Theme.secondaryInk
        )

        FuriganaText(
            text = question.prompt,
            style = Theme.heading(22.sp),
            color = Theme.ink
        )

        if (!question.promptReading.isNullOrBlank()) {
            Text(
                text = question.promptReading,
                fontSize = 14.sp,
                color = Theme.secondaryInk
            )
        }

        if (!question.hint.isNullOrBlank()) {
            Text(
                text = question.hint,
                fontSize = 15.sp,
                color = Theme.secondaryInk
            )
        }

        // 2 Sütunlu Şık Izgarası
        val rows = remember(question.choices) { question.choices.chunked(2) }
        Column(
            modifier = Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rows.forEach { rowChoices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowChoices.forEach { option ->
                        val isSelected = selectedChoice == option
                        val isCorrect = option == question.answer

                        val bgColor = when {
                            !isChoiceLocked -> Theme.paper
                            isCorrect -> Theme.success
                            isSelected -> Theme.accent
                            else -> Theme.paper
                        }
                        val fgColor = if (isChoiceLocked && (isCorrect || isSelected)) {
                            Color.White
                        } else {
                            Theme.ink
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .background(color = bgColor)
                                .border(width = 2.dp, color = Theme.ink)
                                .clickable(enabled = !isChoiceLocked) {
                                    onSelectChoice(option)
                                }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = fgColor,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (rowChoices.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GrammarWordOrderQuestionContent(
    question: GrammarQuestion,
    orderAssembled: List<WordOrderToken>,
    orderPool: List<WordOrderToken>,
    isOrderChecked: Boolean,
    isOrderCorrect: Boolean,
    onSelectPoolToken: (String) -> Unit,
    onRemoveAssembledToken: (String) -> Unit
) {
    val answerBoxTint = when {
        !isOrderChecked -> Theme.paper
        isOrderCorrect -> Theme.success.copy(alpha = 0.18f)
        else -> Theme.accent.copy(alpha = 0.18f)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = L10n.grammarWordOrderPromptLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Theme.secondaryInk
        )

        FuriganaText(
            text = question.prompt,
            style = Theme.heading(20.sp),
            color = Theme.ink
        )

        if (!question.hint.isNullOrBlank()) {
            Text(
                text = question.hint,
                fontSize = 14.sp,
                color = Theme.secondaryInk
            )
        }

        // Kurulan Cevap Kutusu
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .background(color = answerBoxTint)
                .border(width = 2.dp, color = Theme.ink)
                .padding(10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (orderAssembled.isEmpty()) {
                Text(
                    text = L10n.grammarWordOrderEmptyHint,
                    fontSize = 13.sp,
                    color = Theme.secondaryInk
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    orderAssembled.forEach { token ->
                        WordOrderTokenChip(
                            text = token.text,
                            enabled = !isOrderChecked,
                            onClick = { onRemoveAssembledToken(token.id) }
                        )
                    }
                }
            }
        }

        // Kelime Havuzu
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            orderPool.forEach { token ->
                WordOrderTokenChip(
                    text = token.text,
                    enabled = !isOrderChecked,
                    onClick = { onSelectPoolToken(token.id) }
                )
            }
        }

        if (isOrderChecked && !isOrderCorrect) {
            Text(
                text = L10n.grammarCorrectAnswerPrefix(question.answer),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.accent
            )
        }
    }
}

@Composable
private fun WordOrderTokenChip(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.75f)
            .background(color = Theme.paper)
            .border(width = 2.dp, color = Theme.ink)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Theme.ink
        )
    }
}

@Composable
private fun GrammarPracticeSummaryContent(
    totalAnswered: Int,
    totalWrong: Int,
    wrongSummary: List<GrammarWrongEntry>,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Theme.accent,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = L10n.grammarPracticeFinishedTitle,
                style = Theme.display(32.sp),
                color = Theme.ink
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GrammarStatBox(
                value = max(0, totalAnswered - totalWrong),
                label = L10n.grammarStatCorrect,
                modifier = Modifier.weight(1f)
            )
            GrammarStatBox(
                value = totalWrong,
                label = L10n.grammarStatWrong,
                modifier = Modifier.weight(1f)
            )
        }

        if (wrongSummary.isEmpty()) {
            Text(
                text = L10n.perfectScore,
                style = Theme.heading(17.sp),
                color = Theme.ink
            )
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = L10n.grammarReviewAgainTitle,
                    style = Theme.heading(19.sp),
                    color = Theme.ink
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    wrongSummary.forEach { entry ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .inkBordered()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = entry.pointTitle,
                                style = Theme.heading(15.sp),
                                color = Theme.accent
                            )
                            Text(
                                text = entry.prompt,
                                fontSize = 14.sp,
                                color = Theme.ink,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = L10n.grammarCorrectAnswerPrefix(entry.answer),
                                fontSize = 12.sp,
                                color = Theme.secondaryInk
                            )
                        }
                    }
                }

                Text(
                    text = L10n.grammarWrongAddedToReview,
                    fontSize = 12.sp,
                    color = Theme.secondaryInk
                )
            }
        }

        PrimaryButton(
            text = L10n.finishButton,
            onClick = {
                FeedbackManager.shared.playSelection()
                onFinish()
            }
        )
    }
}

@Composable
private fun GrammarStatBox(
    value: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .inkBordered()
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$value",
            style = Theme.display(32.sp),
            color = Theme.accent
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = Theme.secondaryInk
        )
    }
}
