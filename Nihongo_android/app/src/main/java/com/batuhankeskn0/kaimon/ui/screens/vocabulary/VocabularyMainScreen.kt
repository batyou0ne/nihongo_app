package com.batuhankeskn0.kaimon.ui.screens.vocabulary

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.model.ExamMode
import com.batuhankeskn0.kaimon.data.model.VocabularyWord
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.FeatureGate
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressBreakdown
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import com.batuhankeskn0.kaimon.ui.components.LevelSelectionRow
import com.batuhankeskn0.kaimon.ui.components.LevelSelectorBar
import com.batuhankeskn0.kaimon.ui.components.LockedLevelInfoSheet
import com.batuhankeskn0.kaimon.ui.components.ProgressBar
import com.batuhankeskn0.kaimon.ui.screens.learning.FlashcardSessionScreen
import com.batuhankeskn0.kaimon.ui.screens.quiz.GatewayExamScreen
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Kelime sekmesi içindeki alt navigasyon hedefleri.
 */
sealed interface VocabularyDestination {
    data object Root : VocabularyDestination
    data class PartSelection(val level: String) : VocabularyDestination
    data class PartSession(val level: String, val partIndex: Int) : VocabularyDestination
}

private data class ActiveExamRequest(
    val level: JLPTLevel,
    val mode: ExamMode
)

/**
 * Kelime Modülü Ana Ekranı (`Views/VocabularyMainView.swift` karşılığı).
 *
 * İçerdiği Bölümler:
 * 1. N5–N1 JLPT seviye sekmeleri (`LevelSelectorBar`)
 * 2. Seçili seviyenin ilerleme durumu ve Kapı Sınavı (`ExamMode.GATEWAY`) / Seviye Atlama (`ExamMode.TEST_OUT`) sınav kartı
 * 3. N5–N1 JLPT seviye seçim satırları (`LevelSelectionRow`); kilitli seviyelerde `LockedLevelInfoSheet`,
 *    açık seviyelerde 25'erli kelime parçaları ekranına (`VocabularyPartSelectionScreen`) geçiş.
 */
@Composable
fun VocabularyMainScreen(
    modifier: Modifier = Modifier,
    initialDestination: VocabularyDestination = VocabularyDestination.Root,
    onOpenPaywall: (() -> Unit)? = null,
    featureGate: FeatureGate = FeatureGate.shared
) {
    var destination by remember { mutableStateOf(initialDestination) }
    var selectedTabLevel by remember { mutableStateOf(JLPTLevel.N5) }
    var selectedLockedLevel by remember { mutableStateOf<JLPTLevel?>(null) }
    var activeExam by remember { mutableStateOf<ActiveExamRequest?>(null) }
    var showExamQuotaAlert by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(initialDestination) {
        destination = initialDestination
    }

    val unlockedLevels by produceState(
        initialValue = setOf(JLPTLevel.N5),
        key1 = refreshTrigger
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val records = LevelProgressionService.shared.ensureInitialProgress()
                JLPTLevel.allCases.filter {
                    LevelProgressionService.shared.isLevelUnlocked(it, records)
                }.toSet()
            }.getOrDefault(setOf(JLPTLevel.N5))
        }
    }

    val selectedLevelBreakdown by produceState(
        initialValue = LevelProgressBreakdown(0.0, 0.0, 0.0, 0.0),
        key1 = selectedTabLevel,
        key2 = refreshTrigger
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                LevelProgressionService.shared.calculateLevelProgress(selectedTabLevel)
            }.getOrDefault(LevelProgressBreakdown(0.0, 0.0, 0.0, 0.0))
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching {
                LevelProgressionService.shared.ensureInitialProgress()
            }
        }
    }

    BackHandler(enabled = activeExam != null || destination != VocabularyDestination.Root) {
        if (activeExam != null) {
            activeExam = null
        } else {
            destination = when (val current = destination) {
                is VocabularyDestination.PartSession -> VocabularyDestination.PartSelection(current.level)
                is VocabularyDestination.PartSelection -> VocabularyDestination.Root
                VocabularyDestination.Root -> VocabularyDestination.Root
            }
        }
    }

    // 1. Kapı Sınavı veya Test-Out Sınavı Açık İse
    val examRequest = activeExam
    if (examRequest != null) {
        GatewayExamScreen(
            level = examRequest.level,
            mode = examRequest.mode,
            onDismiss = {
                activeExam = null
                refreshTrigger += 1
            },
            onSuccess = {
                refreshTrigger += 1
            },
            modifier = modifier
        )
        return
    }

    // 2. Alt Navigasyon Hedefleri
    when (val current = destination) {
        is VocabularyDestination.PartSelection -> {
            VocabularyPartSelectionScreen(
                level = current.level,
                onBack = {
                    destination = VocabularyDestination.Root
                    refreshTrigger += 1
                },
                onOpenPaywall = onOpenPaywall,
                featureGate = featureGate,
                modifier = modifier
            )
            return
        }
        is VocabularyDestination.PartSession -> {
            val allWords by produceState<List<VocabularyWord>>(
                initialValue = emptyList(),
                key1 = current.level
            ) {
                value = withContext(Dispatchers.IO) {
                    ContentStore.loadVocabulary(level = current.level)
                }
            }
            val parts = remember(allWords, current.level) {
                if (allWords.isEmpty()) emptyList() else ContentStore.vocabularyParts(level = current.level)
            }
            val partItems = parts.getOrNull(current.partIndex).orEmpty()

            if (partItems.isNotEmpty()) {
                FlashcardSessionScreen(
                    sessionKey = "vocab_${current.level}_part${current.partIndex + 1}",
                    itemKind = LearnableItemKind.VOCABULARY_WORD,
                    allItems = partItems,
                    distractorPool = allWords,
                    accentColor = Theme.accent,
                    title = L10n.vocabularySessionTitle(current.level, current.partIndex + 1),
                    onBack = {
                        destination = VocabularyDestination.PartSelection(current.level)
                        refreshTrigger += 1
                    },
                    modifier = modifier
                )
                return
            }
        }
        VocabularyDestination.Root -> Unit
    }

    fun tryStartExam(level: JLPTLevel, mode: ExamMode) {
        if (featureGate.canStartExam) {
            featureGate.recordExam()
            activeExam = ActiveExamRequest(level = level, mode = mode)
        } else {
            showExamQuotaAlert = true
        }
    }

    // 3. Ana Kelime Ekranı
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = L10n.vocabularyTitle,
            style = Theme.display(36.sp),
            color = Theme.ink,
            modifier = Modifier.padding(top = 10.dp)
        )

        // N5–N1 JLPT Seviye Sekmeleri
        LevelSelectorBar(
            selectedLevel = selectedTabLevel,
            isLevelUnlocked = { unlockedLevels.contains(it) },
            onLevelSelected = { level ->
                FeedbackManager.shared.playSelection()
                selectedTabLevel = level
            },
            onLockedLevelClicked = { locked ->
                FeedbackManager.shared.playSelection()
                selectedLockedLevel = locked
            }
        )

        // Seviye İlerleme Durumu ve Kapı Sınavı / Test-Out Sınav Kartı
        LevelExamStatusCard(
            level = selectedTabLevel,
            breakdown = selectedLevelBreakdown,
            isNextLevelUnlocked = selectedTabLevel.nextLevel?.let { unlockedLevels.contains(it) } ?: true,
            onStartGatewayExam = {
                FeedbackManager.shared.playSelection()
                tryStartExam(level = selectedTabLevel, mode = ExamMode.GATEWAY)
            },
            onStartTestOutExam = {
                FeedbackManager.shared.playSelection()
                tryStartExam(level = selectedTabLevel, mode = ExamMode.TEST_OUT)
            }
        )

        // N5–N1 Seviye Satırları (VocabularyMainView.swift birebir karşılığı)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            JLPTLevel.allCases.forEach { level ->
                val isUnlocked = unlockedLevels.contains(level)
                LevelSelectionRow(
                    level = level,
                    title = L10n.vocabularyLevel(level.rawValue),
                    unlockedSubtitle = L10n.wordCount(level.targetCounts.vocab),
                    isUnlocked = isUnlocked,
                    icon = Icons.Filled.Book,
                    onClick = {
                        FeedbackManager.shared.playSelection()
                        if (isUnlocked) {
                            selectedTabLevel = level
                            destination = VocabularyDestination.PartSelection(level.rawValue)
                        } else {
                            selectedLockedLevel = level
                        }
                    }
                )
            }
        }
    }

    selectedLockedLevel?.let { locked ->
        LockedLevelInfoSheet(
            level = locked,
            onDismiss = { selectedLockedLevel = null },
            onStartTestOut = { prevLevel ->
                selectedLockedLevel = null
                tryStartExam(level = prevLevel, mode = ExamMode.TEST_OUT)
            }
        )
    }

    if (showExamQuotaAlert) {
        AlertDialog(
            onDismissRequest = { showExamQuotaAlert = false },
            title = { Text(text = L10n.examQuotaExhaustedTitle, style = Theme.heading(18.sp)) },
            text = { Text(text = L10n.examQuotaExhaustedMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExamQuotaAlert = false
                        onOpenPaywall?.invoke()
                    }
                ) {
                    Text(text = L10n.subscribeButton, color = Theme.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExamQuotaAlert = false }) {
                    Text(text = L10n.closeButton, color = Theme.secondaryInk)
                }
            }
        )
    }
}

/**
 * Seçili JLPT seviyesinin ilerleme durumunu ve Seviye Kapı Sınavı (`ExamMode.GATEWAY`) /
 * Seviye Atlama Sınavı (`ExamMode.TEST_OUT`) girişini gösteren kart.
 */
@Composable
fun LevelExamStatusCard(
    level: JLPTLevel,
    breakdown: LevelProgressBreakdown,
    isNextLevelUnlocked: Boolean,
    onStartGatewayExam: () -> Unit,
    onStartTestOutExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    val overallPercent = (breakdown.overall * 100.0).toInt().coerceIn(0, 100)
    val isReadyForGateway = breakdown.overall >= level.completionThreshold
    val nextLevel = level.nextLevel

    Column(
        modifier = modifier
            .fillMaxWidth()
            .inkBordered()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isReadyForGateway || isNextLevelUnlocked) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Filled.EmojiEvents
                    },
                    contentDescription = null,
                    tint = if (isReadyForGateway || isNextLevelUnlocked) Theme.success else Theme.accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = L10n.gatewayExamCardTitle(level.rawValue),
                    style = Theme.heading(18.sp),
                    color = Theme.ink
                )
            }

            Text(
                text = "%$overallPercent / %85",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isReadyForGateway) Theme.success else Theme.accent
            )
        }

        ProgressBar(fraction = breakdown.overall)

        // Modül kırılım özeti (Kanji / Kelime / Gramer)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${L10n.kanjiPartsMetric}: %${(breakdown.kanji * 100).toInt()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Theme.secondaryInk
            )
            Text(
                text = "${L10n.vocabPartsMetric}: %${(breakdown.vocab * 100).toInt()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Theme.secondaryInk
            )
            Text(
                text = "${L10n.grammarUnitsMetric}: %${(breakdown.grammar * 100).toInt()}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Theme.secondaryInk
            )
        }

        if (nextLevel != null) {
            HorizontalDivider(color = Theme.secondaryInk.copy(alpha = 0.2f))

            Text(
                text = if (isReadyForGateway) {
                    L10n.gatewayExamReadySubtitle(nextLevel.rawValue)
                } else {
                    L10n.gatewayExamProgressSubtitle(overallPercent)
                },
                fontSize = 13.sp,
                color = Theme.secondaryInk
            )

            val buttonShape = RoundedCornerShape(12.dp)

            if (isReadyForGateway) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(buttonShape)
                        .background(color = Theme.accent, shape = buttonShape)
                        .clickable(onClick = onStartGatewayExam),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = L10n.startGatewayExamButton,
                        style = Theme.heading(15.sp),
                        color = Theme.paper
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(buttonShape)
                        .background(color = Theme.paper, shape = buttonShape)
                        .border(width = 2.dp, color = Theme.ink, shape = buttonShape)
                        .clickable(onClick = onStartTestOutExam),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = Theme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = L10n.startTestOutChallengeButton,
                        style = Theme.heading(14.sp),
                        color = Theme.ink
                    )
                }
            }
        }
    }
}
