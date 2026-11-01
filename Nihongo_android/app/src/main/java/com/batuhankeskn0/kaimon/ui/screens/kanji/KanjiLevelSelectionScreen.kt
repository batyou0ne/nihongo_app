package com.batuhankeskn0.kaimon.ui.screens.kanji

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.model.ExamMode
import com.batuhankeskn0.kaimon.data.service.FeatureGate
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import com.batuhankeskn0.kaimon.ui.components.LevelSelectionRow
import com.batuhankeskn0.kaimon.ui.components.LockedLevelInfoSheet
import com.batuhankeskn0.kaimon.ui.screens.quiz.GatewayExamScreen
import com.batuhankeskn0.kaimon.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Kanji modülünün ilk ekranı: JLPT seviyesi seçimi (`N5`–`N1`).
 * N5 varsayılan açıktır; üst seviyeler (`N4`–`N1`) önceki seviye tamamlanana kadar kilitlidir
 * ve tıklandığında `LockedLevelInfoSheet` açılır.
 * iOS `Views/Kanji/KanjiLevelSelectionView.swift` karşılığıdır.
 */
@Composable
fun KanjiLevelSelectionScreen(
    modifier: Modifier = Modifier,
    onSelectLevel: ((JLPTLevel) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onOpenPaywall: (() -> Unit)? = null,
    featureGate: FeatureGate = FeatureGate.shared
) {
    var internalSelectedLevel by remember { mutableStateOf<JLPTLevel?>(null) }
    var selectedLockedLevel by remember { mutableStateOf<JLPTLevel?>(null) }
    var activeTestOutLevel by remember { mutableStateOf<JLPTLevel?>(null) }
    var showExamQuotaAlert by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

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

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching {
                LevelProgressionService.shared.ensureInitialProgress()
            }
        }
    }

    BackHandler(enabled = activeTestOutLevel != null || internalSelectedLevel != null || onBack != null) {
        when {
            activeTestOutLevel != null -> activeTestOutLevel = null
            internalSelectedLevel != null -> internalSelectedLevel = null
            onBack != null -> onBack()
        }
    }

    // 1. Test-Out Sınavı Açık İse
    val examLevel = activeTestOutLevel
    if (examLevel != null) {
        GatewayExamScreen(
            level = examLevel,
            mode = ExamMode.TEST_OUT,
            onDismiss = {
                activeTestOutLevel = null
                refreshTrigger += 1
            },
            onSuccess = {
                refreshTrigger += 1
            },
            modifier = modifier
        )
        return
    }

    // 2. Dahili Parça Seçim Ekranı Açık İse (`onSelectLevel` dışarıdan verilmediğinde)
    val chosenLevel = internalSelectedLevel
    if (chosenLevel != null) {
        KanjiPartSelectionScreen(
            level = chosenLevel.rawValue,
            onBack = {
                internalSelectedLevel = null
                refreshTrigger += 1
            },
            onOpenPaywall = onOpenPaywall,
            featureGate = featureGate,
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
    ) {
        // Üst Başlık / Geri Butonu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onBack != null) {
                Row(
                    modifier = Modifier
                        .clickable(onClick = onBack)
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
                text = "Kanji",
                style = Theme.heading(20.sp),
                color = Theme.ink
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            JLPTLevel.allCases.forEach { level ->
                val isUnlocked = unlockedLevels.contains(level)
                LevelSelectionRow(
                    level = level,
                    title = L10n.kanjiLevel(level.rawValue),
                    unlockedSubtitle = L10n.kanjiCount(level.targetCounts.kanji),
                    isUnlocked = isUnlocked,
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    onClick = {
                        FeedbackManager.shared.playSelection()
                        if (isUnlocked) {
                            if (onSelectLevel != null) {
                                onSelectLevel(level)
                            } else {
                                internalSelectedLevel = level
                            }
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
                if (featureGate.canStartExam) {
                    featureGate.recordExam()
                    activeTestOutLevel = prevLevel
                } else {
                    showExamQuotaAlert = true
                }
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
