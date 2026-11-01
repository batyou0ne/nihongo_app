package com.batuhankeskn0.kaimon.ui.screens.grammar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.model.ExamMode
import com.batuhankeskn0.kaimon.data.service.FeatureGate
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressBreakdown
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import com.batuhankeskn0.kaimon.ui.components.LevelSelectionRow
import com.batuhankeskn0.kaimon.ui.components.LevelSelectorBar
import com.batuhankeskn0.kaimon.ui.components.LockedLevelInfoSheet
import com.batuhankeskn0.kaimon.ui.components.SectionLabel
import com.batuhankeskn0.kaimon.ui.components.UtilityCard
import com.batuhankeskn0.kaimon.ui.screens.quiz.GatewayExamScreen
import com.batuhankeskn0.kaimon.ui.screens.reference.GrammarReferenceScreen
import com.batuhankeskn0.kaimon.ui.screens.vocabulary.LevelExamStatusCard
import com.batuhankeskn0.kaimon.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gramer sekmesi içindeki alt navigasyon hedefleri.
 */
sealed interface GrammarDestination {
    data object Root : GrammarDestination
    data class PartSelection(val level: String) : GrammarDestination
    data class Session(val level: String) : GrammarDestination
    data object VerbConjugator : GrammarDestination
    data object ReferenceLibrary : GrammarDestination
}

private data class ActiveGrammarExamRequest(
    val level: JLPTLevel,
    val mode: ExamMode
)

/**
 * Gramer Modülü Ana Ekranı (`Views/GrammarMainView.swift` karşılığı).
 *
 * İçerdiği Bölümler:
 * 1. N5–N1 JLPT seviye sekmeleri (`LevelSelectorBar`) ve Seviye Kapı / Test-Out sınav kartı
 * 2. Fiil Çekimleyici (`VerbConjugationScreen`) ve Gramer Kütüphanesi (`GrammarReferenceScreen`) kısayolları
 * 3. N5–N1 JLPT seviye seçim satırları (`LevelSelectionRow`); açık seviyelerde `GrammarPartSelectionScreen`
 *    (ünite listesi ve ders/pratik akışı), kilitli seviyelerde `LockedLevelInfoSheet`.
 */
@Composable
fun GrammarMainScreen(
    modifier: Modifier = Modifier,
    initialDestination: GrammarDestination = GrammarDestination.Root,
    onOpenPaywall: (() -> Unit)? = null,
    featureGate: FeatureGate = FeatureGate.shared
) {
    var destination by remember { mutableStateOf(initialDestination) }
    var selectedTabLevel by remember { mutableStateOf(JLPTLevel.N5) }
    var selectedLockedLevel by remember { mutableStateOf<JLPTLevel?>(null) }
    var activeExam by remember { mutableStateOf<ActiveGrammarExamRequest?>(null) }
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

    BackHandler(enabled = activeExam != null || destination != GrammarDestination.Root) {
        if (activeExam != null) {
            activeExam = null
        } else {
            destination = GrammarDestination.Root
        }
    }

    // 1. Aktif Sınav Varsa
    val examReq = activeExam
    if (examReq != null) {
        GatewayExamScreen(
            level = examReq.level,
            mode = examReq.mode,
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
        is GrammarDestination.PartSelection -> {
            GrammarPartSelectionScreen(
                level = current.level,
                onBack = {
                    destination = GrammarDestination.Root
                    refreshTrigger += 1
                },
                onOpenPaywall = onOpenPaywall,
                featureGate = featureGate,
                modifier = modifier
            )
            return
        }
        is GrammarDestination.Session -> {
            GrammarSessionScreen(
                level = current.level,
                onDismiss = {
                    destination = GrammarDestination.Root
                    refreshTrigger += 1
                },
                modifier = modifier
            )
            return
        }
        GrammarDestination.VerbConjugator -> {
            VerbConjugationScreen(
                hiragana = "たべる",
                kanji = "食べる",
                turkishMeaning = L10n.grammarSampleVerbMeaning,
                allowVerbSelection = true,
                onDismiss = { destination = GrammarDestination.Root },
                modifier = modifier
            )
            return
        }
        GrammarDestination.ReferenceLibrary -> {
            GrammarReferenceScreen(
                level = selectedTabLevel.rawValue,
                onBack = { destination = GrammarDestination.Root },
                modifier = modifier
            )
            return
        }
        GrammarDestination.Root -> Unit
    }

    fun tryStartExam(level: JLPTLevel, mode: ExamMode) {
        if (featureGate.canStartExam) {
            featureGate.recordExam()
            activeExam = ActiveGrammarExamRequest(level = level, mode = mode)
        } else {
            showExamQuotaAlert = true
        }
    }

    // 3. Ana Gramer Ekranı
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
            text = L10n.grammarTitle,
            style = Theme.display(36.sp),
            color = Theme.ink,
            modifier = Modifier.padding(top = 10.dp)
        )

        // N5–N1 Seviye Seçici
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

        // Seviye İlerleme ve Kapı Sınavı Durum Kartı
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

        // N5–N1 Gramer Seviye Satırları
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            JLPTLevel.allCases.forEach { level ->
                val isUnlocked = unlockedLevels.contains(level)
                LevelSelectionRow(
                    level = level,
                    title = L10n.grammarLevel(level.rawValue),
                    unlockedSubtitle = L10n.topicCount(level.targetCounts.grammar),
                    isUnlocked = isUnlocked,
                    icon = Icons.AutoMirrored.Filled.Article,
                    onClick = {
                        FeedbackManager.shared.playSelection()
                        if (isUnlocked) {
                            selectedTabLevel = level
                            destination = GrammarDestination.PartSelection(level.rawValue)
                        } else {
                            selectedLockedLevel = level
                        }
                    }
                )
            }
        }

        // Fiil Çekimleyici ve Gramer Kütüphanesi Kısayolları
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel(text = L10n.grammarShortcutsSectionLabel)

            UtilityCard(
                title = L10n.verbConjugatorShortcutTitle,
                subtitle = L10n.verbConjugatorShortcutSubtitle,
                icon = Icons.Filled.TableChart,
                isHighlighted = false,
                onClick = {
                    FeedbackManager.shared.playSelection()
                    destination = GrammarDestination.VerbConjugator
                }
            )

            UtilityCard(
                title = L10n.grammarLibraryShortcutTitle,
                subtitle = L10n.grammarLibraryShortcutSubtitle,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                isHighlighted = false,
                onClick = {
                    FeedbackManager.shared.playSelection()
                    destination = GrammarDestination.ReferenceLibrary
                }
            )
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
