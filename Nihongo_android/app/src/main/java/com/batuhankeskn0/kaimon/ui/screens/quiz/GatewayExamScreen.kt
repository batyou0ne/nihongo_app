package com.batuhankeskn0.kaimon.ui.screens.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import com.batuhankeskn0.kaimon.data.model.ExamMode
import com.batuhankeskn0.kaimon.data.model.ExamQuestion
import com.batuhankeskn0.kaimon.data.model.ExamQuestionType
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.ExamGenerator
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import com.batuhankeskn0.kaimon.ui.components.LessonCompleteView
import com.batuhankeskn0.kaimon.ui.components.ProgressBar
import com.batuhankeskn0.kaimon.ui.navigation.LocalTabBarManager
import com.batuhankeskn0.kaimon.ui.theme.PrimaryButton
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * JLPT Seviye Kapı Sınavı (`ExamMode.GATEWAY`) ve Seviye Atlama Meydan Okuması (`ExamMode.TEST_OUT`)
 * durum ve kurallarını yöneten denetleyici.
 *
 * iOS `Views/Quiz/GatewayExamView.swift` içindeki işleyişin birebir karşılığıdır:
 * - `ExamMode.GATEWAY`: Seviye ilerlemesi `%85`'e ulaştığında açılır, 30 karma soru, `%80` başarı şartı.
 * - `ExamMode.TEST_OUT`: Kilitli bir seviyeyi doğrudan açmak isteyenler için **3 Can Haklı (❤️❤️❤️)**
 *   meydan okuma modu; 3 yanlışta (`hearts == 0`) sınav derhal sona erer (`isFailed = true`).
 * - Sınav başarıyla geçildiğinde `onExamPassed` tetiklenir (`LevelProgressionService.unlockNextLevel` + XP ödülü).
 */
class GatewayExamController(
    val level: JLPTLevel,
    val mode: ExamMode,
    initialQuestions: List<ExamQuestion> = emptyList(),
    private val triggerFeedback: Boolean = false,
    private val onExamPassed: (completedLevel: JLPTLevel, scoreRatio: Double, isTestOut: Boolean, xpReward: Int) -> Unit = { _, _, _, _ -> }
) {
    var questions: List<ExamQuestion> by mutableStateOf(initialQuestions)
        private set

    var currentIndex: Int by mutableIntStateOf(0)
        private set

    var hearts: Int by mutableIntStateOf(MAX_HEARTS)
        private set

    var correctAnswersCount: Int by mutableIntStateOf(0)
        private set

    var selectedOption: String? by mutableStateOf(null)
        private set

    var isAnswerCorrect: Boolean? by mutableStateOf(null)
        private set

    var isFailed: Boolean by mutableStateOf(false)
        private set

    var isPassed: Boolean by mutableStateOf(false)
        private set

    var showLessonComplete: Boolean by mutableStateOf(false)

    val currentQuestion: ExamQuestion?
        get() = questions.getOrNull(currentIndex)

    val progressFraction: Double
        get() = if (questions.isEmpty()) 0.0 else currentIndex.toDouble() / questions.size.toDouble()

    val scoreRatio: Double
        get() = correctAnswersCount.toDouble() / max(1, questions.size).toDouble()

    val xpReward: Int
        get() = if (mode == ExamMode.TEST_OUT) TEST_OUT_XP_REWARD else GATEWAY_XP_REWARD

    /**
     * Kutlama ekranında gösterilecek açılan seviye etiketi.
     * iOS `GatewayExamView.swift` satır 98 ile uyumlu olarak `level.rawValue` veya açılan hedef seviyeyi sunar.
     */
    val celebrationLevel: JLPTLevel
        get() = level.nextLevel ?: level

    fun loadQuestions(loaded: List<ExamQuestion>) {
        questions = loaded
        currentIndex = 0
        hearts = MAX_HEARTS
        correctAnswersCount = 0
        selectedOption = null
        isAnswerCorrect = null
        isFailed = false
        isPassed = false
        showLessonComplete = false
    }

    /**
     * Kullanıcının seçtiği şıkkı değerlendirir.
     * @return Seçilen şık doğruysa `true`, yanlışsa `false`.
     */
    fun submitAnswer(option: String): Boolean {
        val question = currentQuestion ?: return false
        if (selectedOption != null || isFailed || isPassed) {
            return isAnswerCorrect == true
        }

        selectedOption = option
        val isCorrect = (option == question.correctAnswer)
        isAnswerCorrect = isCorrect

        if (isCorrect) {
            correctAnswersCount += 1
            if (triggerFeedback) {
                FeedbackManager.shared.playSuccess()
                AudioService.shared.playCorrectSound()
            }
        } else {
            if (triggerFeedback) {
                FeedbackManager.shared.playError()
                AudioService.shared.playWrongSound()
            }
            if (mode == ExamMode.TEST_OUT) {
                hearts = max(0, hearts - 1)
            }
        }

        return isCorrect
    }

    /**
     * Cevap verildikten sonra (UI'da 1.2 sn gecikmenin ardından) çağrılır:
     * - `TEST_OUT` modunda can (`hearts`) 0'a düştüyse sınavı anında başarısız bitirir (`isFailed = true`).
     * - Son soruya gelindiyse `handleExamFinish()` çalıştırır.
     * - Aksi halde bir sonraki soruya ilerler.
     */
    fun advanceAfterAnswer() {
        if (isFailed || isPassed) return

        if (mode == ExamMode.TEST_OUT && hearts == 0) {
            isFailed = true
            return
        }

        if (currentIndex + 1 >= questions.size) {
            handleExamFinish()
        } else {
            currentIndex += 1
            selectedOption = null
            isAnswerCorrect = null
        }
    }

    fun handleExamFinish() {
        val total = max(1, questions.size)
        val ratio = correctAnswersCount.toDouble() / total.toDouble()

        val passed = if (mode == ExamMode.TEST_OUT) {
            hearts > 0
        } else {
            ratio >= PASSING_SCORE_RATIO
        }

        if (passed) {
            isPassed = true
            showLessonComplete = true
            onExamPassed(level, ratio, mode == ExamMode.TEST_OUT, xpReward)
        } else {
            isFailed = true
        }
    }

    companion object {
        const val MAX_HEARTS: Int = 3
        const val PASSING_SCORE_RATIO: Double = 0.80
        const val GATEWAY_XP_REWARD: Int = 250
        const val TEST_OUT_XP_REWARD: Int = 350
    }
}

/**
 * JLPT Seviye Kapı Sınavı (`ExamMode.GATEWAY`) ve Test-Out Sınavı (`ExamMode.TEST_OUT`) ekranı.
 * iOS `Views/Quiz/GatewayExamView.swift` bileşeninin birebir Jetpack Compose karşılığıdır.
 */
@Composable
fun GatewayExamScreen(
    level: JLPTLevel,
    mode: ExamMode,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onSuccess: (() -> Unit)? = null
) {
    val tabBarManager = LocalTabBarManager.current
    val scope = rememberCoroutineScope()

    var activeStreak by remember { mutableIntStateOf(1) }
    var isLoading by remember { mutableStateOf(true) }
    var autoAdvanceJob by remember { mutableStateOf<Job?>(null) }

    val controller = remember(level, mode) {
        GatewayExamController(
            level = level,
            mode = mode,
            triggerFeedback = true,
            onExamPassed = { completedLevel, scoreRatio, isTestOut, xp ->
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        val db = UserDatabaseManager.shared.getDatabase()
                        LevelProgressionService.shared.unlockNextLevel(
                            afterCompleting = completedLevel,
                            score = scoreRatio,
                            isTestOut = isTestOut,
                            dao = db.levelProgressDao()
                        )
                        val userProgress = db.progressDao().getOrCreateUserProgress()
                        userProgress.totalXP += xp
                        db.progressDao().upsertUserProgress(userProgress)
                        activeStreak = max(1, userProgress.activeStreak)
                    }
                }
            }
        )
    }

    DisposableEffect(tabBarManager) {
        tabBarManager.hide()
        onDispose {
            autoAdvanceJob?.cancel()
            tabBarManager.show()
        }
    }

    BackHandler {
        autoAdvanceJob?.cancel()
        onDismiss()
    }

    LaunchedEffect(level, mode) {
        isLoading = true
        val (generatedQuestions, streak) = withContext(Dispatchers.IO) {
            val qs = ExamGenerator.generateExam(level = level, mode = mode)
            val currentStreak = runCatching {
                UserDatabaseManager.shared.progressDao.getOrCreateUserProgress().activeStreak
            }.getOrDefault(1)
            qs to max(1, currentStreak)
        }
        activeStreak = streak
        controller.loadQuestions(generatedQuestions)
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Üst Toolbar: Kapat (X) + Soru Türü Rozeti + Can Barı (Test-Out) veya Soru Sayacı (Gateway)
            GatewayExamTopBar(
                currentQuestion = if (!controller.isFailed && !controller.isPassed) controller.currentQuestion else null,
                mode = mode,
                hearts = controller.hearts,
                currentIndex = controller.currentIndex,
                totalQuestions = controller.questions.size,
                onClose = {
                    autoAdvanceJob?.cancel()
                    onDismiss()
                }
            )

            when {
                isLoading || controller.questions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Theme.accent)
                    }
                }
                controller.isFailed -> {
                    GatewayExamFailedView(
                        mode = mode,
                        correctAnswersCount = controller.correctAnswersCount,
                        totalQuestions = controller.questions.size,
                        onBackToLessons = {
                            autoAdvanceJob?.cancel()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                controller.isPassed -> {
                    GatewayExamPassedSummaryView(
                        level = level,
                        onContinue = {
                            onSuccess?.invoke()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    val question = controller.currentQuestion
                    if (question != null) {
                        GatewayExamQuestionContent(
                            question = question,
                            progressFraction = controller.progressFraction,
                            selectedOption = controller.selectedOption,
                            onOptionClick = { option ->
                                if (controller.selectedOption != null) return@GatewayExamQuestionContent
                                controller.submitAnswer(option)
                                autoAdvanceJob?.cancel()
                                autoAdvanceJob = scope.launch {
                                    delay(1200L)
                                    controller.advanceAfterAnswer()
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // Kutlama Ekranı (fullScreenCover karşılığı)
        if (controller.showLessonComplete) {
            LessonCompleteView(
                xpGained = controller.xpReward,
                streak = activeStreak,
                streakIncreased = false,
                title = L10n.levelUnlockedTitle(controller.celebrationLevel.rawValue),
                ctaTitle = L10n.continueButton,
                ringColor = Theme.accent,
                onContinue = {
                    controller.showLessonComplete = false
                    onSuccess?.invoke()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun GatewayExamTopBar(
    currentQuestion: ExamQuestion?,
    mode: ExamMode,
    hearts: Int,
    currentIndex: Int,
    totalQuestions: Int,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Sol: X Butonu
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = L10n.closeButton,
                tint = Theme.ink,
                modifier = Modifier.size(22.dp)
            )
        }

        // Orta: Soru Türü Rozeti (Kanji / Kelime / Gramer)
        if (currentQuestion != null) {
            Row(
                modifier = Modifier
                    .background(
                        color = Theme.accent.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = currentQuestion.type.toIconVector(),
                    contentDescription = null,
                    tint = Theme.accent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = currentQuestion.type.rawValue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Theme.accent
                )
            }
        } else {
            Spacer(modifier = Modifier.size(1.dp))
        }

        // Sağ: Test-Out ise 3 Can (❤️❤️❤️), Gateway ise Soru Sayacı
        if (mode == ExamMode.TEST_OUT) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until GatewayExamController.MAX_HEARTS) {
                    val isAlive = i < hearts
                    Icon(
                        imageVector = if (isAlive) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isAlive) Theme.accent else Theme.secondaryInk,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else if (totalQuestions > 0) {
            Text(
                text = "${currentIndex + 1} / $totalQuestions",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.secondaryInk
            )
        } else {
            Spacer(modifier = Modifier.size(1.dp))
        }
    }
}

@Composable
private fun GatewayExamQuestionContent(
    question: ExamQuestion,
    progressFraction: Double,
    selectedOption: String?,
    onOptionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // İlerleme Çubuğu
        ProgressBar(fraction = progressFraction)

        Spacer(modifier = Modifier.weight(1f))

        // Soru Kartı
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .inkBordered(lineWidth = 2.dp)
                .padding(vertical = 28.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!question.subPrompt.isNullOrEmpty()) {
                Text(
                    text = question.subPrompt,
                    fontSize = 15.sp,
                    color = Theme.secondaryInk,
                    textAlign = TextAlign.Center
                )
            }

            Text(
                text = question.prompt,
                style = Theme.display(if (question.prompt.length > 15) 24.sp else 44.sp),
                color = Theme.ink,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 2x2 Şıklar Izgarası
        val rows = question.options.chunked(2)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            rows.forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowOptions.forEach { option ->
                        GatewayExamOptionButton(
                            option = option,
                            correctAnswer = question.correctAnswer,
                            selectedOption = selectedOption,
                            onClick = { onOptionClick(option) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowOptions.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GatewayExamOptionButton(
    option: String,
    correctAnswer: String,
    selectedOption: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = selectedOption == option
    val isCorrectChoice = option == correctAnswer
    val showResult = selectedOption != null

    val bgColor = when {
        showResult && isCorrectChoice -> Theme.success
        showResult && isSelected -> Theme.accent
        else -> Theme.paper
    }
    val fgColor = when {
        showResult && (isCorrectChoice || isSelected) -> Color.White
        else -> Theme.ink
    }

    Box(
        modifier = modifier
            .height(64.dp)
            .background(color = bgColor, shape = RectangleShape)
            .border(width = 2.dp, color = Theme.ink, shape = RectangleShape)
            .clickable(enabled = selectedOption == null, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = option,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = fgColor,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GatewayExamFailedView(
    mode: ExamMode,
    correctAnswersCount: Int,
    totalQuestions: Int,
    onBackToLessons: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(90.dp)
                .background(
                    color = Theme.accent.copy(alpha = 0.12f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Cancel,
                contentDescription = null,
                tint = Theme.accent,
                modifier = Modifier.size(48.dp)
            )
        }

        Text(
            text = L10n.examFailedTitle,
            style = Theme.display(28.sp),
            color = Theme.ink,
            textAlign = TextAlign.Center
        )

        Text(
            text = if (mode == ExamMode.TEST_OUT) {
                L10n.examFailedTestOutMessage
            } else {
                L10n.examFailedGatewayMessage(correctAnswersCount, totalQuestions)
            },
            fontSize = 15.sp,
            color = Theme.secondaryInk,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        val buttonShape = RoundedCornerShape(14.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(buttonShape)
                .background(color = Theme.ink, shape = buttonShape)
                .clickable(onClick = onBackToLessons),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = L10n.backToLessonsButton,
                style = Theme.heading(16.sp),
                color = Theme.paper
            )
        }
    }
}

@Composable
private fun GatewayExamPassedSummaryView(
    level: JLPTLevel,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Theme.success,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = L10n.congratulations,
            style = Theme.display(32.sp),
            color = Theme.ink
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = L10n.examPassedSummarySubtitle(level.rawValue),
            style = Theme.heading(18.sp),
            color = Theme.secondaryInk,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        PrimaryButton(
            text = L10n.continueButton,
            onClick = onContinue
        )
    }
}

private fun ExamQuestionType.toIconVector(): ImageVector =
    when (this) {
        ExamQuestionType.KANJI -> Icons.AutoMirrored.Filled.MenuBook
        ExamQuestionType.VOCABULARY -> Icons.Filled.Book
        ExamQuestionType.GRAMMAR -> Icons.AutoMirrored.Filled.Article
    }
