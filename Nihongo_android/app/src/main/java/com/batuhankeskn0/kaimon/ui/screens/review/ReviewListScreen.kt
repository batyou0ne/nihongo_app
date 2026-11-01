package com.batuhankeskn0.kaimon.ui.screens.review

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.service.AlphabetAudioService
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.ui.components.LessonCompleteView
import com.batuhankeskn0.kaimon.ui.components.ProgressBar
import com.batuhankeskn0.kaimon.ui.navigation.LocalTabBarManager
import com.batuhankeskn0.kaimon.ui.theme.PrimaryButton
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered

/**
 * "Tekrar Çalışılması Gerekenler" Ekranı (`Views/Review/ReviewListView.swift` karşılığı).
 *
 * - Tekrar zamanı gelen (`dueDate <= now`) ve yanlış yapılan (`needsReview`) öğeleri modüle göre gruplar.
 * - Her öğenin telaffuz sesini dinleme (`VolumeUp`) ve `"Öğrendim ✓"` ile onaylama imkanı sunar.
 * - `"Bunlarla çalış →"` butonuyla tek dokunuşta Tekrar Quiz / Flashcard oturumu başlatır.
 */
@Composable
fun ReviewListScreen(
    modifier: Modifier = Modifier,
    viewModel: ReviewViewModel = viewModel(),
    onBack: (() -> Unit)? = null,
    onStartExternalReviewSession: ((LearnableItemKind, List<ReviewDisplayItem>) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ReviewListScreenContent(
        uiState = uiState,
        onBack = onBack,
        onSpeakItem = { item ->
            FeedbackManager.shared.playSelection()
            viewModel.speakItem(item)
        },
        onMarkAsLearned = { kind, itemId ->
            FeedbackManager.shared.playSuccess()
            viewModel.markAsLearned(kind, itemId)
        },
        onStartPractice = { kind ->
            FeedbackManager.shared.playSelection()
            val items = uiState.itemsForKind(kind)
            if (onStartExternalReviewSession != null) {
                onStartExternalReviewSession(kind, items)
            } else {
                viewModel.startPracticeSession(kind)
            }
        },
        onSelectPracticeOption = { option ->
            val currentQ = uiState.activePracticeSession?.currentQuestion
            if (currentQ != null) {
                if (option == currentQ.correctAnswer) {
                    FeedbackManager.shared.playSuccess()
                    AudioService.shared.playCorrectSound()
                } else {
                    FeedbackManager.shared.playError()
                    AudioService.shared.playWrongSound()
                }
            }
            viewModel.selectPracticeOption(option)
        },
        onNextPracticeQuestion = {
            FeedbackManager.shared.playSelection()
            viewModel.nextPracticeQuestion()
        },
        onClosePracticeSession = {
            AudioService.shared.stop()
            viewModel.closePracticeSession()
        },
        modifier = modifier
    )
}

@Composable
fun ReviewListScreenContent(
    uiState: ReviewUiState,
    onBack: (() -> Unit)? = null,
    onSpeakItem: (ReviewDisplayItem) -> Unit,
    onMarkAsLearned: (LearnableItemKind, String) -> Unit,
    onStartPractice: (LearnableItemKind) -> Unit,
    onSelectPracticeOption: (String) -> Unit,
    onNextPracticeQuestion: () -> Unit,
    onClosePracticeSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSession = uiState.activePracticeSession
    if (activeSession != null) {
        ReviewPracticeSessionOverlay(
            session = activeSession,
            onSelectOption = onSelectPracticeOption,
            onNextQuestion = onNextPracticeQuestion,
            onClose = onClosePracticeSession,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
    ) {
        // Üst Navigasyon Barı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .inkBordered(lineWidth = 1.5.dp, cornerRadius = 12.dp)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = L10n.backButton,
                        tint = Theme.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = L10n.reviewNavTitle,
                style = Theme.display(32.sp),
                color = Theme.ink
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            if (uiState.isEmpty) {
                ReviewEmptyState()
            } else {
                val modules = listOf(
                    LearnableItemKind.HIRAGANA_CHARACTER to uiState.hiraganaItems,
                    LearnableItemKind.KATAKANA_CHARACTER to uiState.katakanaItems,
                    LearnableItemKind.KANJI to uiState.kanjiItems,
                    LearnableItemKind.VOCABULARY_WORD to uiState.vocabularyItems,
                    LearnableItemKind.GRAMMAR to uiState.grammarItems
                )

                modules.forEach { (kind, items) ->
                    if (items.isNotEmpty()) {
                        ReviewModuleSection(
                            kind = kind,
                            items = items,
                            onStartPractice = { onStartPractice(kind) },
                            onSpeakItem = onSpeakItem,
                            onMarkAsLearned = { item -> onMarkAsLearned(kind, item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "🎉",
            fontSize = 44.sp
        )
        Text(
            text = L10n.reviewEmptyTitle,
            style = Theme.display(28.sp),
            color = Theme.ink
        )
        Text(
            text = L10n.reviewEmptyMessage,
            fontSize = 15.sp,
            color = Theme.secondaryInk
        )
    }
}

@Composable
private fun ReviewModuleSection(
    kind: LearnableItemKind,
    items: List<ReviewDisplayItem>,
    onStartPractice: () -> Unit,
    onSpeakItem: (ReviewDisplayItem) -> Unit,
    onMarkAsLearned: (ReviewDisplayItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${kind.displayName} (${items.size})",
                style = Theme.display(24.sp),
                color = Theme.ink
            )
            Text(
                text = L10n.practiceWithThese(),
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Theme.accent,
                modifier = Modifier
                    .clickable(onClick = onStartPractice)
                    .padding(vertical = 4.dp)
            )
        }

        items.forEach { item ->
            ReviewItemRow(
                item = item,
                onSpeak = { onSpeakItem(item) },
                onMarkLearned = { onMarkAsLearned(item) }
            )
        }
    }
}

@Composable
private fun ReviewItemRow(
    item: ReviewDisplayItem,
    onSpeak: () -> Unit,
    onMarkLearned: () -> Unit
) {
    val isGrammar = item.kind == LearnableItemKind.GRAMMAR
    val badgeBgColor = if (isGrammar) Theme.ink.copy(alpha = 0.1f) else Theme.accent.copy(alpha = 0.1f)
    val badgeTextColor = if (isGrammar) Theme.ink else Theme.accent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .inkBordered()
            .clickable(onClick = onSpeak)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isGrammar) {
            // iOS ReviewListView.grammarSection satır düzeni
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.prompt,
                    style = Theme.heading(20.sp),
                    color = Theme.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(color = badgeBgColor, shape = CircleShape)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.srsLevelTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    }

                    Text(
                        text = item.subtitle,
                        fontSize = 14.sp,
                        color = Theme.secondaryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            // iOS ReviewListView.reviewRow satır düzeni
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.prompt,
                    style = Theme.heading(26.sp),
                    color = Theme.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .background(color = badgeBgColor, shape = CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.srsLevelTitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }
            }

            Text(
                text = item.subtitle,
                fontSize = 14.sp,
                color = Theme.secondaryInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // Sesli Dinleme Butonu
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onSpeak),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = L10n.listenPronunciation,
                tint = Theme.accent,
                modifier = Modifier.size(22.dp)
            )
        }

        // "Öğrendim ✓" Butonu
        Box(
            modifier = Modifier
                .background(color = Theme.ink)
                .clickable(onClick = onMarkLearned)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = L10n.learnedButton,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.paper
            )
        }
    }
}

/**
 * "Bunlarla çalış →" tıklandığında açılan interaktif Tekrar Flashcard / Quiz oturumu.
 */
@Composable
private fun ReviewPracticeSessionOverlay(
    session: ReviewPracticeSessionState,
    onSelectOption: (String) -> Unit,
    onNextQuestion: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabBarManager = LocalTabBarManager.current
    DisposableEffect(tabBarManager) {
        val wasHidden = tabBarManager.isHidden
        tabBarManager.hide()
        onDispose {
            if (!wasHidden) {
                tabBarManager.show()
            }
        }
    }

    if (session.isCompleted) {
        LessonCompleteView(
            xpGained = (session.correctCount * 10).coerceAtLeast(10),
            title = "${session.title} • ${L10n.scoreResult(session.correctCount, session.questions.size)}",
            ctaTitle = L10n.finishButton,
            onContinue = onClose,
            modifier = modifier
        )
        return
    }

    val question = session.currentQuestion ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Üst Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .inkBordered(lineWidth = 1.5.dp, cornerRadius = 12.dp)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = L10n.closeButton,
                    tint = Theme.ink,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = session.title,
                style = Theme.heading(18.sp),
                color = Theme.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                textAlign = TextAlign.Center
            )

            Text(
                text = "${session.currentIndex + 1}/${session.questions.size}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.secondaryInk
            )
        }

        ProgressBar(fraction = session.progressFraction)

        // Soru / Flashcard Kartı
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .inkBordered(lineWidth = 3.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!question.promptReading.isNullOrBlank()) {
                Text(
                    text = question.promptReading,
                    fontSize = 16.sp,
                    color = Theme.secondaryInk,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text = question.prompt,
                style = Theme.display(if (question.prompt.length <= 4) 56.sp else 28.sp),
                color = Theme.ink,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Ses Butonu
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Theme.accent.copy(alpha = 0.12f))
                    .clickable {
                        if (question.kind == LearnableItemKind.HIRAGANA_CHARACTER ||
                            question.kind == LearnableItemKind.KATAKANA_CHARACTER
                        ) {
                            AlphabetAudioService.shared.play(question.speechText)
                        } else {
                            AudioService.shared.speak(question.speechText)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = L10n.listenPronunciation,
                    tint = Theme.accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = L10n.listenPronunciation,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Theme.accent
                )
            }

            if (session.selectedOption != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = question.flipRecap,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Theme.secondaryInk,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Çoktan Seçmeli Şıklar
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            question.options.forEach { option ->
                val isAnswered = session.selectedOption != null
                val isSelected = session.selectedOption == option
                val isCorrectOption = option == question.correctAnswer

                val borderColor = when {
                    !isAnswered -> Theme.ink
                    isCorrectOption -> Theme.success
                    isSelected -> Theme.accent
                    else -> Theme.ink.copy(alpha = 0.35f)
                }

                val bgColor = when {
                    !isAnswered -> Theme.paper
                    isCorrectOption -> Theme.success.copy(alpha = 0.15f)
                    isSelected -> Theme.accent.copy(alpha = 0.15f)
                    else -> Theme.paper
                }

                val shape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = bgColor, shape = shape)
                        .clip(shape)
                        .border(width = 2.dp, color = borderColor, shape = shape)
                        .clickable(enabled = !isAnswered) {
                            onSelectOption(option)
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = option,
                        style = Theme.heading(16.sp),
                        color = Theme.ink
                    )
                }
            }
        }

        if (session.selectedOption != null) {
            val isLast = session.currentIndex + 1 >= session.questions.size
            PrimaryButton(
                text = if (isLast) L10n.finishButton else L10n.continueButton,
                onClick = onNextQuestion
            )
        }
    }
}
