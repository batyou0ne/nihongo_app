package com.batuhankeskn0.kaimon.ui.screens.quiz

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.local.LearningItemProgressEntity
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import com.batuhankeskn0.kaimon.data.model.FlashcardItem
import com.batuhankeskn0.kaimon.data.model.QuizItem
import com.batuhankeskn0.kaimon.data.service.AlphabetAudioService
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.XPManager
import com.batuhankeskn0.kaimon.ui.components.LessonCompleteView
import com.batuhankeskn0.kaimon.ui.components.ProgressBar
import com.batuhankeskn0.kaimon.ui.theme.PrimaryButton
import com.batuhankeskn0.kaimon.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * Çoktan seçmeli ortak Quiz ekranı (`Views/Quiz/QuizView.swift` karşılığı).
 * `Item` hem `JapaneseCharacter`, hem `Kanji`, hem de `VocabularyWord` olabilir —
 * modül fark etmeksizin aynı Brutalist arayüzü ve `QuizViewModel` motorunu kullanır.
 */
@Composable
fun <Item : QuizItem> QuizScreen(
    questions: List<Item>,
    itemKind: LearnableItemKind,
    modifier: Modifier = Modifier,
    accentColor: Color = Theme.accent,
    distractorPool: List<Item>? = null,
    progressLookup: (String) -> LearningItemProgressEntity? = { null },
    onDismiss: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()

    var hasRecordedStreak by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    var showLessonComplete by remember { mutableStateOf(false) }
    var sessionEarnedXP by remember { mutableIntStateOf(0) }
    var completedStreak by remember { mutableIntStateOf(0) }
    var didStreakIncrease by remember { mutableStateOf(false) }

    val viewModel = remember(questions, itemKind) {
        QuizViewModel(
            questions = questions,
            itemKind = itemKind,
            progressLookup = progressLookup,
            shuffled = true,
            distractorPool = distractorPool ?: questions,
            autoUpdateProgress = true,
            triggerFeedback = true,
            onProgressUpdated = { updatedProgress, _ ->
                scope.launch(Dispatchers.IO) {
                    runCatching {
                        UserDatabaseManager.shared.progressDao.upsertItemProgress(updatedProgress)
                    }
                }
            }
        )
    }

    LaunchedEffect(viewModel.currentIndex) {
        showHint = false
    }

    LaunchedEffect(viewModel.isFinished) {
        if (viewModel.isFinished && !hasRecordedStreak) {
            hasRecordedStreak = true
            val fallbackXp = max(viewModel.score, 1) * 5
            if (sessionEarnedXP == 0) {
                sessionEarnedXP = fallbackXp
            }
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val dao = UserDatabaseManager.shared.progressDao
                    val userProgress = dao.getOrCreateUserProgress()
                    val prevStreak = userProgress.activeStreak
                    userProgress.recordStudySession()
                    dao.upsertUserProgress(userProgress)
                    val newStreak = userProgress.activeStreak
                    completedStreak = newStreak
                    didStreakIncrease = newStreak > prevStreak
                }
                showLessonComplete = true
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Üst Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = L10n.closeButton,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                )

                Text(
                    text = L10n.quizTitle,
                    style = Theme.heading(18.sp),
                    color = Theme.ink
                )

                Text(
                    text = if (viewModel.questions.isNotEmpty() && !viewModel.isFinished) {
                        "${viewModel.currentIndex + 1}/${viewModel.questions.size}"
                    } else {
                        ""
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Theme.secondaryInk
                )
            }

            if (viewModel.isFinished) {
                QuizResultContent(
                    score = viewModel.score,
                    total = viewModel.questions.size,
                    onFinish = onDismiss,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                QuizActiveContent(
                    viewModel = viewModel,
                    itemKind = itemKind,
                    accentColor = accentColor,
                    showHint = showHint,
                    onShowHint = { showHint = true },
                    onOptionSelected = { option ->
                        val currentQ = viewModel.currentQuestion ?: return@QuizActiveContent
                        val wasLearned = progressLookup(currentQ.id)?.isLearned == true
                        viewModel.submitAnswer(option)
                        if (viewModel.isAnswerCorrect == true) {
                            val action = if (wasLearned) {
                                XPManager.XPAction.CARD_REVIEWED
                            } else {
                                XPManager.XPAction.NEW_CARD_LEARNED
                            }
                            sessionEarnedXP += action.points
                            scope.launch(Dispatchers.IO) {
                                runCatching {
                                    XPManager.shared.addXP(action = action)
                                }
                            }
                        }
                    },
                    onContinue = {
                        showHint = false
                        viewModel.moveToNext()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (showLessonComplete) {
            LessonCompleteView(
                xpGained = sessionEarnedXP,
                streak = completedStreak,
                streakIncreased = didStreakIncrease,
                ringColor = accentColor,
                onContinue = { showLessonComplete = false }
            )
        }
    }
}

@Composable
private fun <Item : QuizItem> QuizActiveContent(
    viewModel: QuizViewModel<Item>,
    itemKind: LearnableItemKind,
    accentColor: Color,
    showHint: Boolean,
    onShowHint: () -> Unit,
    onOptionSelected: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val question = viewModel.currentQuestion ?: return

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        ProgressBar(fraction = viewModel.progressFraction)

        // Soru Metni + Ses Butonu
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val isAlt = viewModel.currentQuestionModeIsAlternate
            val promptText = if (isAlt) {
                question.alternatePrompt ?: question.prompt
            } else {
                question.prompt
            }

            Text(
                text = promptText,
                style = Theme.display(if (isAlt || promptText.length > 4) 28.sp else 80.sp),
                color = Theme.ink,
                textAlign = TextAlign.Center
            )

            val flashcardItem = question as? FlashcardItem
            if (flashcardItem != null) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f))
                        .clickable {
                            if (itemKind == LearnableItemKind.HIRAGANA_CHARACTER ||
                                itemKind == LearnableItemKind.KATAKANA_CHARACTER
                            ) {
                                AlphabetAudioService.shared.play(flashcardItem.speechText)
                            } else {
                                AudioService.shared.speak(flashcardItem.speechText)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = L10n.listenPronunciation,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Bağlam İpucu (ContextHint)
        val flashcardItem = question as? FlashcardItem
        val hint = flashcardItem?.contextHint
        if (hint != null) {
            if (showHint || viewModel.currentQuestionModeIsAlternate) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!viewModel.currentQuestionModeIsAlternate) {
                        Text(
                            text = hint.sentence,
                            style = Theme.heading(19.sp),
                            color = Theme.accent,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = hint.translation,
                        fontSize = 14.sp,
                        color = Theme.secondaryInk,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Text(
                    text = L10n.hintButton,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Theme.secondaryInk,
                    modifier = Modifier
                        .clickable(onClick = onShowHint)
                        .padding(vertical = 4.dp, horizontal = 12.dp)
                )
            }
        }

        // 4 Şık Butonları (Brutalist)
        val expectedAnswer = viewModel.currentExpectedAnswer
        val showResult = viewModel.selectedAnswer != null

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            viewModel.options.forEach { option ->
                val isSelected = viewModel.selectedAnswer == option
                val isCorrectAnswer = option == expectedAnswer

                QuizOptionButton(
                    option = option,
                    isSelected = isSelected,
                    isCorrectAnswer = isCorrectAnswer,
                    showResult = showResult,
                    onClick = { onOptionSelected(option) }
                )
            }
        }

        if (showResult) {
            PrimaryButton(
                text = L10n.continueButton,
                onClick = onContinue,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun QuizOptionButton(
    option: String,
    isSelected: Boolean,
    isCorrectAnswer: Boolean,
    showResult: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        !showResult -> Theme.paper
        isCorrectAnswer -> Theme.success.copy(alpha = 0.25f)
        isSelected -> Theme.accent.copy(alpha = 0.25f)
        else -> Theme.paper
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = bgColor, shape = RectangleShape)
            .border(width = 2.dp, color = Theme.ink, shape = RectangleShape)
            .clickable(enabled = !showResult, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = option,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Theme.ink,
            modifier = Modifier.weight(1f)
        )

        if (showResult && isCorrectAnswer) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Theme.success,
                modifier = Modifier.size(22.dp)
            )
        } else if (showResult && isSelected) {
            Icon(
                imageVector = Icons.Filled.Cancel,
                contentDescription = null,
                tint = Theme.accent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun QuizResultContent(
    score: Int,
    total: Int,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (score == total && total > 0) Icons.Filled.Star else Icons.Filled.Verified,
            contentDescription = null,
            tint = Theme.accent,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = L10n.scoreResult(score, total),
            style = Theme.heading(24.sp),
            color = Theme.ink,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = L10n.finishButton,
            onClick = onFinish
        )
    }
}
