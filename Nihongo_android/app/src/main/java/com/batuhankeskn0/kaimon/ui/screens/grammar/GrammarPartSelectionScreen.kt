package com.batuhankeskn0.kaimon.ui.screens.grammar

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TableChart
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.local.LevelProgressDao
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import com.batuhankeskn0.kaimon.data.local.UserLevelProgressEntity
import com.batuhankeskn0.kaimon.data.model.GrammarPoint
import com.batuhankeskn0.kaimon.data.model.GrammarUnit
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.FeatureGate
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import com.batuhankeskn0.kaimon.ui.components.ProBadge
import com.batuhankeskn0.kaimon.ui.components.SectionLabel
import com.batuhankeskn0.kaimon.ui.screens.reference.GrammarReferenceScreen
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bir Gramer ünitesinin kilit, Pro ve tamamlanma durumunu temsil eden model.
 */
data class GrammarUnitStatus(
    val unitIndex: Int,
    val unit: GrammarUnit,
    val points: List<GrammarPoint>,
    val learnedCount: Int,
    val isUnlocked: Boolean,
    val isCompleted: Boolean,
    val isProLocked: Boolean
)

/**
 * Seçilen seviyedeki tüm gramer ünitelerinin (`GrammarSyllabus`) durumlarını hesaplar.
 */
fun buildGrammarUnitStatuses(
    level: JLPTLevel,
    syllabus: List<GrammarUnit>,
    pointsByKey: Map<String, GrammarPoint>,
    levelProgress: UserLevelProgressEntity?,
    learnedIDs: Set<String>,
    featureGate: FeatureGate = FeatureGate.shared,
    progressionService: LevelProgressionService = LevelProgressionService.shared
): List<GrammarUnitStatus> = syllabus.mapIndexed { index, unit ->
    val unitPoints = unit.grammarKeys.mapNotNull { pointsByKey[it] }
    val isFinishedByItems = isGrammarUnitFinished(unit, pointsByKey, learnedIDs)
    val isMarkedCompleted = levelProgress?.isPartCompleted(module = "grammar", index = index) == true
    val isCompleted = isFinishedByItems || isMarkedCompleted
    val isUnlocked = progressionService.isPartUnlocked(
        module = "grammar",
        level = level,
        partIndex = index,
        progress = levelProgress
    )
    val isProLocked = !featureGate.isUnitAvailable(index = index)
    val learnedCount = unitPoints.count { learnedIDs.contains(it.id) }

    GrammarUnitStatus(
        unitIndex = index,
        unit = unit,
        points = unitPoints,
        learnedCount = learnedCount,
        isUnlocked = isUnlocked,
        isCompleted = isCompleted,
        isProLocked = isProLocked
    )
}

/**
 * Bir gramer ünitesini tamamlandı olarak işaretler (`LevelProgressionService.markPartCompleted(module = "grammar", ...)`).
 */
suspend fun markGrammarUnitCompleted(
    level: JLPTLevel,
    unitIndex: Int,
    progressionService: LevelProgressionService = LevelProgressionService.shared,
    dao: LevelProgressDao = UserDatabaseManager.shared.levelProgressDao
): UserLevelProgressEntity = progressionService.markPartCompleted(
    module = "grammar",
    level = level,
    partIndex = unitIndex,
    dao = dao
)

private sealed interface ActiveGrammarSubScreen {
    data class Lesson(val point: GrammarPoint, val unitIndex: Int) : ActiveGrammarSubScreen
    data class UnitPractice(val points: List<GrammarPoint>, val title: String, val unitIndex: Int) : ActiveGrammarSubScreen
    data object AutoSession : ActiveGrammarSubScreen
    data object VerbConjugator : ActiveGrammarSubScreen
    data object ReferenceLibrary : ActiveGrammarSubScreen
}

/**
 * Seçilen JLPT seviyesinin gramer konularını bir "Learning Path" (öğrenme yolu) olarak
 * ünitelere böler (`Views/Grammar/GrammarPartSelectionView.swift` birebir karşılığı).
 *
 * - Bölüm İçi Kilit (Intra-Level Gating): Ünite 1 açıktır; sonraki üniteler önceki ünite bitince açılır.
 * - Üst bölümde Otomatik Gramer Oturumu, Fiil Çekimleyici ve Gramer Kütüphanesi kısayollarını barındırır.
 */
@Composable
fun GrammarPartSelectionScreen(
    level: String,
    onBack: (() -> Unit)? = null,
    onOpenPaywall: (() -> Unit)? = null,
    featureGate: FeatureGate = FeatureGate.shared,
    modifier: Modifier = Modifier
) {
    val jlptLevel = remember(level) { JLPTLevel.fromRawValue(level) ?: JLPTLevel.N5 }

    var activeSubScreen by remember { mutableStateOf<ActiveGrammarSubScreen?>(null) }
    var lockedUnitId by remember { mutableIntStateOf(0) }
    var showPaywallAlert by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val syllabusAndPoints by produceState<Pair<List<GrammarUnit>, Map<String, GrammarPoint>>>(
        initialValue = emptyList<GrammarUnit>() to emptyMap(),
        key1 = level
    ) {
        value = withContext(Dispatchers.IO) {
            val syl = ContentStore.loadGrammarSyllabus(level = level)
            val pts = ContentStore.loadGrammar(level = level).associateBy { it.key }
            syl to pts
        }
    }

    val syllabus = syllabusAndPoints.first
    val pointsDict = syllabusAndPoints.second

    val learnedAndProgress by produceState<Pair<Set<String>, UserLevelProgressEntity?>>(
        initialValue = emptySet<String>() to null,
        key1 = jlptLevel,
        key2 = syllabus,
        key3 = refreshTrigger
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                LevelProgressionService.shared.ensureInitialProgress()
                if (syllabus.isNotEmpty() && pointsDict.isNotEmpty()) {
                    syncGrammarUnitsCompletion(
                        level = jlptLevel,
                        syllabus = syllabus,
                        pointsByKey = pointsDict
                    )
                }
                val learned = UserDatabaseManager.shared.progressDao
                    .getProgressByKind(LearnableItemKind.GRAMMAR)
                    .filter { it.isLearned || it.repetitionCount >= 1 }
                    .map { it.itemID }
                    .toSet()
                val progress = LevelProgressionService.shared.getProgress(jlptLevel)
                learned to progress
            }.getOrDefault(emptySet<String>() to null)
        }
    }

    val learnedIDs = learnedAndProgress.first
    val levelProgress = learnedAndProgress.second

    LaunchedEffect(jlptLevel) {
        withContext(Dispatchers.IO) {
            runCatching {
                LevelProgressionService.shared.ensureInitialProgress()
            }
        }
    }

    BackHandler(enabled = activeSubScreen != null || onBack != null) {
        if (activeSubScreen != null) {
            activeSubScreen = null
            refreshTrigger += 1
        } else {
            onBack?.invoke()
        }
    }

    when (val sub = activeSubScreen) {
        is ActiveGrammarSubScreen.Lesson -> {
            GrammarLessonScreen(
                point = sub.point,
                level = level,
                unitIndex = sub.unitIndex,
                isSessionMode = false,
                onBack = {
                    activeSubScreen = null
                    refreshTrigger += 1
                },
                modifier = modifier
            )
            return
        }
        is ActiveGrammarSubScreen.UnitPractice -> {
            GrammarPracticeScreen(
                points = sub.points,
                title = sub.title,
                level = level,
                unitIndex = sub.unitIndex,
                onFinish = {
                    activeSubScreen = null
                    refreshTrigger += 1
                },
                modifier = modifier
            )
            return
        }
        ActiveGrammarSubScreen.AutoSession -> {
            GrammarSessionScreen(
                level = level,
                onDismiss = {
                    activeSubScreen = null
                    refreshTrigger += 1
                },
                modifier = modifier
            )
            return
        }
        ActiveGrammarSubScreen.VerbConjugator -> {
            VerbConjugationScreen(
                hiragana = "たべる",
                kanji = "食べる",
                turkishMeaning = L10n.grammarSampleVerbMeaning,
                allowVerbSelection = true,
                onDismiss = { activeSubScreen = null },
                modifier = modifier
            )
            return
        }
        ActiveGrammarSubScreen.ReferenceLibrary -> {
            GrammarReferenceScreen(
                level = level,
                onBack = { activeSubScreen = null },
                modifier = modifier
            )
            return
        }
        null -> Unit
    }

    val unitStatuses = remember(jlptLevel, syllabus, pointsDict, levelProgress, learnedIDs, featureGate.revision) {
        buildGrammarUnitStatuses(
            level = jlptLevel,
            syllabus = syllabus,
            pointsByKey = pointsDict,
            levelProgress = levelProgress,
            learnedIDs = learnedIDs,
            featureGate = featureGate
        )
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onBack != null) {
                Row(
                    modifier = Modifier
                        .clickable {
                            FeedbackManager.shared.playSelection()
                            onBack()
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
                text = L10n.grammarLevel(level),
                style = Theme.heading(20.sp),
                color = Theme.ink,
                modifier = Modifier.weight(1f)
            )
        }

        if (syllabus.isEmpty()) {
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
                    .padding(20.dp)
                    .padding(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // 1. Otomatik Gramer Oturumu Kartı
                GrammarAutoSessionCard(
                    level = level,
                    onStartSession = {
                        FeedbackManager.shared.playSelection()
                        activeSubScreen = ActiveGrammarSubScreen.AutoSession
                    }
                )

                // 2. Fiil Çekimleyici & Gramer Kütüphanesi Kısayolları
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel(text = L10n.grammarShortcutsSectionLabel)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GrammarShortcutMiniCard(
                            title = L10n.verbConjugatorShortcutTitle,
                            icon = Icons.Filled.TableChart,
                            onClick = {
                                FeedbackManager.shared.playSelection()
                                activeSubScreen = ActiveGrammarSubScreen.VerbConjugator
                            },
                            modifier = Modifier.weight(1f)
                        )
                        GrammarShortcutMiniCard(
                            title = L10n.grammarLibraryShortcutTitle,
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            onClick = {
                                FeedbackManager.shared.playSelection()
                                activeSubScreen = ActiveGrammarSubScreen.ReferenceLibrary
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3. Ünite Listesi (Learning Path)
                Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    SectionLabel(text = L10n.grammarSyllabusSectionLabel(unitStatuses.size))

                    unitStatuses.forEach { status ->
                        GrammarUnitSection(
                            status = status,
                            learnedIDs = learnedIDs,
                            onTopicClick = { point ->
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
                                        activeSubScreen = ActiveGrammarSubScreen.Lesson(
                                            point = point,
                                            unitIndex = status.unitIndex
                                        )
                                    }
                                    else -> {
                                        lockedUnitId = status.unit.id
                                    }
                                }
                            },
                            onPracticeUnitClick = {
                                FeedbackManager.shared.playSelection()
                                if (status.points.isNotEmpty()) {
                                    activeSubScreen = ActiveGrammarSubScreen.UnitPractice(
                                        points = status.points,
                                        title = L10n.grammarUnitTitle(status.unit.id, status.unit.title),
                                        unitIndex = status.unitIndex
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (lockedUnitId > 0) {
        AlertDialog(
            onDismissRequest = { lockedUnitId = 0 },
            title = { Text(text = L10n.grammarUnitLockedTitle, style = Theme.heading(18.sp)) },
            text = { Text(text = L10n.grammarUnitLockedMessage(lockedUnitId)) },
            confirmButton = {
                TextButton(onClick = { lockedUnitId = 0 }) {
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
private fun GrammarAutoSessionCard(
    level: String,
    onStartSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .inkBordered(lineWidth = 2.5.dp)
            .clickable(onClick = onStartSession)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = L10n.grammarAutoSessionTitle(level),
            style = Theme.heading(20.sp),
            color = Theme.ink
        )
        Text(
            text = L10n.grammarAutoSessionSubtitle,
            fontSize = 14.sp,
            color = Theme.secondaryInk
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                modifier = Modifier
                    .background(color = Theme.accent, shape = CircleShape)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Theme.paper,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = L10n.grammarStartAutoSessionButton,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Theme.paper
                )
            }
        }
    }
}

@Composable
private fun GrammarShortcutMiniCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .inkBordered()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Theme.accent,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = Theme.heading(14.sp),
            color = Theme.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Theme.secondaryInk,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun GrammarUnitSection(
    status: GrammarUnitStatus,
    learnedIDs: Set<String>,
    onTopicClick: (GrammarPoint) -> Unit,
    onPracticeUnitClick: () -> Unit
) {
    val isInteractiveUnlocked = status.isUnlocked && !status.isProLocked

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isInteractiveUnlocked) 1f else 0.6f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Ünite Başlığı
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = L10n.grammarUnitTitle(status.unit.id, status.unit.title),
                        style = Theme.display(22.sp),
                        color = if (isInteractiveUnlocked) Theme.ink else Theme.secondaryInk
                    )

                    when {
                        status.isCompleted -> {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Theme.success,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        status.isProLocked -> {
                            ProBadge()
                        }
                        !status.isUnlocked -> {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = Theme.secondaryInk,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (isInteractiveUnlocked && status.points.isNotEmpty()) {
                    val pillShape = RoundedCornerShape(10.dp)
                    Box(
                        modifier = Modifier
                            .clip(pillShape)
                            .background(color = Theme.paper, shape = pillShape)
                            .border(width = 1.5.dp, color = Theme.ink, shape = pillShape)
                            .clickable(onClick = onPracticeUnitClick)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = L10n.grammarPracticeUnitButton,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Theme.ink
                        )
                    }
                }
            }

            Text(
                text = status.unit.description,
                fontSize = 14.sp,
                color = Theme.secondaryInk,
                lineHeight = 20.sp
            )
        }

        // Ünite Konuları
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            status.points.forEach { point ->
                GrammarTopicRow(
                    point = point,
                    isUnlocked = isInteractiveUnlocked,
                    isDone = learnedIDs.contains(point.id),
                    onClick = { onTopicClick(point) }
                )
            }
        }
    }
}

@Composable
private fun GrammarTopicRow(
    point: GrammarPoint,
    isUnlocked: Boolean,
    isDone: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isDone) Theme.success.copy(alpha = 0.12f) else Theme.paper

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = bgColor)
            .border(width = 2.dp, color = Theme.ink)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = point.pattern,
            style = Theme.heading(19.sp),
            color = if (isUnlocked) Theme.accent else Theme.secondaryInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(120.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = point.title,
                style = Theme.heading(16.sp),
                color = if (isUnlocked) Theme.ink else Theme.secondaryInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = point.romaji,
                fontSize = 12.sp,
                color = Theme.secondaryInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        when {
            isDone -> {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Theme.success,
                    modifier = Modifier.size(20.dp)
                )
            }
            isUnlocked -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Theme.ink,
                    modifier = Modifier.size(16.dp)
                )
            }
            else -> {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Theme.secondaryInk,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
