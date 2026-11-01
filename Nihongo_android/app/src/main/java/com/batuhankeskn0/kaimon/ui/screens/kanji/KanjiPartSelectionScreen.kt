package com.batuhankeskn0.kaimon.ui.screens.kanji

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.local.LearningItemProgressEntity
import com.batuhankeskn0.kaimon.data.local.LevelProgressDao
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import com.batuhankeskn0.kaimon.data.local.UserLevelProgressEntity
import com.batuhankeskn0.kaimon.data.model.Kanji
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.FeatureGate
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import com.batuhankeskn0.kaimon.ui.components.ProBadge
import com.batuhankeskn0.kaimon.ui.components.ProgressBar
import com.batuhankeskn0.kaimon.ui.screens.learning.FlashcardSessionScreen
import com.batuhankeskn0.kaimon.ui.screens.quiz.QuizScreen
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Bir Kanji parçasının durumunu temsil eden veri modeli (UI ve birim testleri için).
 */
data class KanjiPartStatus(
    val partIndex: Int,
    val count: Int,
    val learnedCount: Int,
    val learnedRatio: Double,
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
    val isProLocked: Boolean
) {
    val learnedPercent: Int
        get() = (learnedRatio * 100.0).toInt().coerceIn(0, 100)
}

/**
 * Verilen Kanji parçasındaki öğelerin öğrenilme oranını (`0.0..1.0`) hesaplar.
 */
fun computeKanjiPartLearnedRatio(
    part: List<Kanji>,
    progressByID: Map<String, LearningItemProgressEntity>,
    isPartCompleted: Boolean = false
): Double {
    if (part.isEmpty()) return 0.0
    if (isPartCompleted) return 1.0
    val learned = part.count { kanji ->
        val prog = progressByID[kanji.id]
        prog != null && (prog.isLearned || prog.repetitionCount >= 1)
    }
    return min(1.0, learned.toDouble() / max(1, part.size).toDouble())
}

/**
 * Tüm Kanji parçalarının kilit, Pro ve öğrenilme yüzdesi durumlarını hesaplar.
 */
fun buildKanjiPartStatuses(
    level: JLPTLevel,
    parts: List<List<Kanji>>,
    levelProgress: UserLevelProgressEntity?,
    progressByID: Map<String, LearningItemProgressEntity>,
    featureGate: FeatureGate = FeatureGate.shared,
    progressionService: LevelProgressionService = LevelProgressionService.shared
): List<KanjiPartStatus> = parts.mapIndexed { index, part ->
    val isUnlocked = progressionService.isPartUnlocked(
        module = "kanji",
        level = level,
        partIndex = index,
        progress = levelProgress
    )
    val isCompleted = levelProgress?.isPartCompleted(module = "kanji", index = index) == true
    val isProLocked = !featureGate.isUnitAvailable(index = index)
    val ratio = computeKanjiPartLearnedRatio(
        part = part,
        progressByID = progressByID,
        isPartCompleted = isCompleted
    )
    val learnedCount = if (isCompleted) {
        part.size
    } else {
        part.count { kanji ->
            val prog = progressByID[kanji.id]
            prog != null && (prog.isLearned || prog.repetitionCount >= 1)
        }
    }

    KanjiPartStatus(
        partIndex = index,
        count = part.size,
        learnedCount = learnedCount,
        learnedRatio = ratio,
        isUnlocked = isUnlocked,
        isCompleted = isCompleted,
        isProLocked = isProLocked
    )
}

/**
 * Bir Kanji parçasını tamamlandı olarak işaretler (`LevelProgressionService.markPartCompleted(module = "kanji", ...)`).
 */
suspend fun markKanjiPartCompleted(
    level: JLPTLevel,
    partIndex: Int,
    progressionService: LevelProgressionService = LevelProgressionService.shared,
    dao: LevelProgressDao = UserDatabaseManager.shared.levelProgressDao
): UserLevelProgressEntity = progressionService.markPartCompleted(
    module = "kanji",
    level = level,
    partIndex = partIndex,
    dao = dao
)

private sealed interface ActiveKanjiSession {
    data class Flashcard(val partIndex: Int) : ActiveKanjiSession
    data class Quiz(val partIndex: Int) : ActiveKanjiSession
}

/**
 * Seçilen JLPT seviyesinin kanjilerini eşit parçalara böler (`"N5 Kanji's Part 1"` gibi).
 * Bölüm İçi Kilit (Intra-Level Gating): Part 1 her zaman açıktır; sonraki parçalar
 * ancak bir önceki parça tamamlandığında açılır.
 * Ayrıca `FeatureGate.isUnitAvailable` ile Pro kilit kontrolü ve her parçanın öğrenilme yüzdesini gösterir.
 * iOS `Views/Kanji/KanjiPartSelectionView.swift` karşılığıdır.
 */
@Composable
fun KanjiPartSelectionScreen(
    level: String,
    modifier: Modifier = Modifier,
    onSelectPart: ((Int) -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onOpenPaywall: (() -> Unit)? = null,
    featureGate: FeatureGate = FeatureGate.shared
) {
    val jlptLevel = remember(level) { JLPTLevel.fromRawValue(level) ?: JLPTLevel.N5 }
    val scope = rememberCoroutineScope()

    var lockedPartNumber by remember { mutableIntStateOf(0) }
    var showPaywallAlert by remember { mutableStateOf(false) }
    var activeSession by remember { mutableStateOf<ActiveKanjiSession?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val allKanji by produceState<List<Kanji>>(initialValue = emptyList(), key1 = level) {
        value = withContext(Dispatchers.IO) {
            ContentStore.loadKanji(level = level)
        }
    }

    val parts = remember(allKanji, level) {
        if (allKanji.isEmpty()) emptyList() else ContentStore.kanjiParts(level = level)
    }

    val levelProgress by produceState<UserLevelProgressEntity?>(
        initialValue = null,
        key1 = jlptLevel,
        key2 = refreshTrigger
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                LevelProgressionService.shared.ensureInitialProgress()
                LevelProgressionService.shared.getProgress(jlptLevel)
            }.getOrNull()
        }
    }

    val kanjiProgressByID by produceState<Map<String, LearningItemProgressEntity>>(
        initialValue = emptyMap(),
        key1 = level,
        key2 = refreshTrigger
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                UserDatabaseManager.shared.progressDao
                    .getProgressByKind(LearnableItemKind.KANJI)
                    .associateBy { it.itemID }
            }.getOrDefault(emptyMap())
        }
    }

    LaunchedEffect(jlptLevel) {
        withContext(Dispatchers.IO) {
            runCatching {
                LevelProgressionService.shared.ensureInitialProgress()
            }
        }
    }

    BackHandler(enabled = activeSession != null || onBack != null) {
        if (activeSession != null) {
            activeSession = null
            refreshTrigger += 1
        } else {
            onBack?.invoke()
        }
    }

    // Aktif Flashcard veya Quiz oturumu varsa göster
    when (val session = activeSession) {
        is ActiveKanjiSession.Flashcard -> {
            val partItems = parts.getOrNull(session.partIndex).orEmpty()
            if (partItems.isNotEmpty()) {
                FlashcardSessionScreen(
                    sessionKey = "kanji_${level}_part${session.partIndex + 1}",
                    itemKind = LearnableItemKind.KANJI,
                    allItems = partItems,
                    distractorPool = allKanji,
                    accentColor = Theme.accent,
                    title = L10n.kanjiPartTitle(level, session.partIndex + 1),
                    onBack = {
                        activeSession = null
                        refreshTrigger += 1
                    },
                    modifier = modifier
                )
                return
            }
        }
        is ActiveKanjiSession.Quiz -> {
            val partItems = parts.getOrNull(session.partIndex).orEmpty()
            if (partItems.isNotEmpty()) {
                QuizScreen(
                    questions = partItems,
                    itemKind = LearnableItemKind.KANJI,
                    accentColor = Theme.accent,
                    distractorPool = allKanji,
                    progressLookup = { kanjiProgressByID[it] },
                    onDismiss = {
                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                markKanjiPartCompleted(
                                    level = jlptLevel,
                                    partIndex = session.partIndex
                                )
                            }
                            withContext(Dispatchers.Main) {
                                activeSession = null
                                refreshTrigger += 1
                            }
                        }
                    },
                    modifier = modifier
                )
                return
            }
        }
        null -> Unit
    }

    val partStatuses = remember(jlptLevel, parts, levelProgress, kanjiProgressByID, featureGate.revision) {
        buildKanjiPartStatuses(
            level = jlptLevel,
            parts = parts,
            levelProgress = levelProgress,
            progressByID = kanjiProgressByID,
            featureGate = featureGate
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
    ) {
        // Üst Bar
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
                text = L10n.kanjiLevel(level),
                style = Theme.heading(20.sp),
                color = Theme.ink
            )
        }

        if (allKanji.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Theme.accent)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                partStatuses.forEach { status ->
                    KanjiPartRow(
                        level = level,
                        status = status,
                        onRowClick = {
                            FeedbackManager.shared.playSelection()
                            when {
                                status.isProLocked -> {
                                    if (onOpenPaywall != null) {
                                        onOpenPaywall()
                                    } else {
                                        showPaywallAlert = true
                                    }
                                }
                                status.isUnlocked -> {
                                    if (onSelectPart != null) {
                                        onSelectPart(status.partIndex)
                                    } else {
                                        activeSession = ActiveKanjiSession.Flashcard(status.partIndex)
                                    }
                                }
                                else -> {
                                    lockedPartNumber = status.partIndex + 1
                                }
                            }
                        },
                        onStartQuiz = {
                            FeedbackManager.shared.playSelection()
                            activeSession = ActiveKanjiSession.Quiz(status.partIndex)
                        }
                    )
                }
            }
        }
    }

    if (lockedPartNumber > 0) {
        AlertDialog(
            onDismissRequest = { lockedPartNumber = 0 },
            title = { Text(text = L10n.partLockedTitle, style = Theme.heading(18.sp)) },
            text = { Text(text = L10n.partLockedMessage(lockedPartNumber)) },
            confirmButton = {
                TextButton(onClick = { lockedPartNumber = 0 }) {
                    Text(text = L10n.okButton, color = Theme.accent)
                }
            }
        )
    }

    if (showPaywallAlert) {
        AlertDialog(
            onDismissRequest = { showPaywallAlert = false },
            title = { Text(text = L10n.proName, style = Theme.heading(18.sp)) },
            text = { Text(text = L10n.paywallBenefitContent) },
            confirmButton = {
                TextButton(onClick = { showPaywallAlert = false }) {
                    Text(text = L10n.okButton, color = Theme.accent)
                }
            }
        )
    }
}

@Composable
private fun KanjiPartRow(
    level: String,
    status: KanjiPartStatus,
    onRowClick: () -> Unit,
    onStartQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isInteractiveUnlocked = status.isUnlocked && !status.isProLocked

    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isInteractiveUnlocked) 1f else 0.6f)
            .inkBordered()
            .clickable(onClick = onRowClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = when {
                            !isInteractiveUnlocked -> Theme.secondaryInk
                            status.isCompleted -> Theme.success
                            else -> Theme.accent
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${status.partIndex + 1}",
                    style = Theme.heading(19.sp),
                    color = Color.White
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = L10n.kanjiPartTitle(level, status.partIndex + 1),
                    style = Theme.heading(19.sp),
                    color = if (isInteractiveUnlocked) Theme.ink else Theme.secondaryInk
                )
                Text(
                    text = when {
                        status.isProLocked -> L10n.proUnlockHint
                        !status.isUnlocked -> L10n.partLockedSubtitle
                        status.isCompleted -> "${L10n.kanjiPartCompletedSubtitle(status.count)} • ${L10n.learnedPercentLabel(status.learnedPercent)}"
                        else -> "${L10n.kanjiCount(status.count)} • ${L10n.learnedPercentLabel(status.learnedPercent)}"
                    },
                    fontSize = 14.sp,
                    color = Theme.secondaryInk
                )
            }

            when {
                status.isProLocked -> {
                    ProBadge()
                }
                status.isCompleted -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuizPillButton(onClick = onStartQuiz)
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Theme.success,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                isInteractiveUnlocked -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuizPillButton(onClick = onStartQuiz)
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Theme.ink,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                else -> {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = Theme.secondaryInk,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (isInteractiveUnlocked) {
            ProgressBar(
                fraction = status.learnedRatio,
                height = 6.dp
            )
        }
    }
}

@Composable
private fun QuizPillButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(color = Theme.paper, shape = CircleShape)
            .border(width = 1.5.dp, color = Theme.ink, shape = CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = L10n.quizTitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Theme.ink
        )
    }
}
