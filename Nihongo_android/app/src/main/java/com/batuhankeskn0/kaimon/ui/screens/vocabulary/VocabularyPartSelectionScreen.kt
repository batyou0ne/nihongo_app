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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.batuhankeskn0.kaimon.data.model.VocabularyWord
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.FeatureGate
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import com.batuhankeskn0.kaimon.ui.components.FuriganaText
import com.batuhankeskn0.kaimon.ui.components.ProBadge
import com.batuhankeskn0.kaimon.ui.components.ProgressBar
import com.batuhankeskn0.kaimon.ui.components.SectionLabel
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
 * Bir Kelime (Vocabulary) parçasının durumunu temsil eden veri modeli (UI ve birim testleri için).
 */
data class VocabPartStatus(
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
 * Kullanıcının henüz tamamlamadığı ilk kelime parçasının indeksini döner.
 * iOS `VocabularyPartSelectionView.swift` -> `currentPartIndex` karşılığıdır.
 */
fun findCurrentVocabPartIndex(
    partsCount: Int,
    levelProgress: UserLevelProgressEntity?
): Int {
    if (partsCount <= 0) return 0
    for (index in 0 until partsCount) {
        if (levelProgress?.isPartCompleted(module = "vocab", index = index) != true) {
            return index
        }
    }
    return max(0, partsCount - 1)
}

/**
 * Verilen Kelime parçasındaki öğelerin öğrenilme oranını (`0.0..1.0`) hesaplar.
 */
fun computeVocabPartLearnedRatio(
    part: List<VocabularyWord>,
    progressByID: Map<String, LearningItemProgressEntity>,
    isPartCompleted: Boolean = false
): Double {
    if (part.isEmpty()) return 0.0
    if (isPartCompleted) return 1.0
    val learned = part.count { word ->
        val prog = progressByID[word.id]
        prog != null && (prog.isLearned || prog.repetitionCount >= 1)
    }
    return min(1.0, learned.toDouble() / max(1, part.size).toDouble())
}

/**
 * Tüm Kelime parçalarının kilit, Pro ve öğrenilme yüzdesi durumlarını hesaplar.
 */
fun buildVocabPartStatuses(
    level: JLPTLevel,
    parts: List<List<VocabularyWord>>,
    levelProgress: UserLevelProgressEntity?,
    progressByID: Map<String, LearningItemProgressEntity>,
    featureGate: FeatureGate = FeatureGate.shared,
    progressionService: LevelProgressionService = LevelProgressionService.shared
): List<VocabPartStatus> = parts.mapIndexed { index, part ->
    val isUnlocked = progressionService.isPartUnlocked(
        module = "vocab",
        level = level,
        partIndex = index,
        progress = levelProgress
    )
    val isCompleted = levelProgress?.isPartCompleted(module = "vocab", index = index) == true
    val isProLocked = !featureGate.isUnitAvailable(index = index)
    val ratio = computeVocabPartLearnedRatio(
        part = part,
        progressByID = progressByID,
        isPartCompleted = isCompleted
    )
    val learnedCount = if (isCompleted) {
        part.size
    } else {
        part.count { word ->
            val prog = progressByID[word.id]
            prog != null && (prog.isLearned || prog.repetitionCount >= 1)
        }
    }

    VocabPartStatus(
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
 * Bir Kelime parçasını tamamlandı olarak işaretler (`LevelProgressionService.markPartCompleted(module = "vocab", ...)`).
 */
suspend fun markVocabPartCompleted(
    level: JLPTLevel,
    partIndex: Int,
    progressionService: LevelProgressionService = LevelProgressionService.shared,
    dao: LevelProgressDao = UserDatabaseManager.shared.levelProgressDao
): UserLevelProgressEntity = progressionService.markPartCompleted(
    module = "vocab",
    level = level,
    partIndex = partIndex,
    dao = dao
)

private sealed interface ActiveVocabSession {
    data class Flashcard(val partIndex: Int) : ActiveVocabSession
    data class Quiz(val partIndex: Int) : ActiveVocabSession
}

/**
 * Seçilen JLPT seviyesinin kelimelerini 25'erlik parçalara böler (`ContentStore.vocabularyParts`).
 * Kullanıcının kaldığı parçayı öne çıkarır, kelime listesi önizleme + sesli dinleme,
 * Flashcard ve Quiz oturumlarını başlatır.
 * iOS `Views/Vocabulary/VocabularyPartSelectionView.swift` karşılığıdır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyPartSelectionScreen(
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
    var previewPartIndex by remember { mutableStateOf<Int?>(null) }
    var activeSession by remember { mutableStateOf<ActiveVocabSession?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val allWords by produceState<List<VocabularyWord>>(initialValue = emptyList(), key1 = level) {
        value = withContext(Dispatchers.IO) {
            ContentStore.loadVocabulary(level = level)
        }
    }

    val parts = remember(allWords, level) {
        if (allWords.isEmpty()) emptyList() else ContentStore.vocabularyParts(level = level)
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

    val vocabProgressByID by produceState<Map<String, LearningItemProgressEntity>>(
        initialValue = emptyMap(),
        key1 = level,
        key2 = refreshTrigger
    ) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                UserDatabaseManager.shared.progressDao
                    .getProgressByKind(LearnableItemKind.VOCABULARY_WORD)
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
        is ActiveVocabSession.Flashcard -> {
            val partItems = parts.getOrNull(session.partIndex).orEmpty()
            if (partItems.isNotEmpty()) {
                FlashcardSessionScreen(
                    sessionKey = "vocab_${level}_part${session.partIndex + 1}",
                    itemKind = LearnableItemKind.VOCABULARY_WORD,
                    allItems = partItems,
                    distractorPool = allWords,
                    accentColor = Theme.accent,
                    title = L10n.vocabularySessionTitle(level, session.partIndex + 1),
                    onBack = {
                        activeSession = null
                        refreshTrigger += 1
                    },
                    modifier = modifier
                )
                return
            }
        }
        is ActiveVocabSession.Quiz -> {
            val partItems = parts.getOrNull(session.partIndex).orEmpty()
            if (partItems.isNotEmpty()) {
                QuizScreen(
                    questions = partItems,
                    itemKind = LearnableItemKind.VOCABULARY_WORD,
                    accentColor = Theme.accent,
                    distractorPool = allWords,
                    progressLookup = { vocabProgressByID[it] },
                    onDismiss = {
                        scope.launch(Dispatchers.IO) {
                            runCatching {
                                markVocabPartCompleted(
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

    val currentPartIndex = remember(parts.size, levelProgress) {
        findCurrentVocabPartIndex(partsCount = parts.size, levelProgress = levelProgress)
    }

    val partStatuses = remember(jlptLevel, parts, levelProgress, vocabProgressByID, featureGate.revision) {
        buildVocabPartStatuses(
            level = jlptLevel,
            parts = parts,
            levelProgress = levelProgress,
            progressByID = vocabProgressByID,
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
                text = L10n.vocabularyLevel(level),
                style = Theme.heading(20.sp),
                color = Theme.ink
            )
        }

        if (allWords.isEmpty()) {
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
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Kaldığın Yerden Devam Et Hızlı Giriş Kartı
                if (currentPartIndex in parts.indices && featureGate.isUnitAvailable(index = currentPartIndex)) {
                    VocabContinueCard(
                        level = level,
                        partIndex = currentPartIndex,
                        totalParts = parts.size,
                        wordCount = parts[currentPartIndex].size,
                        onClick = {
                            FeedbackManager.shared.playSelection()
                            if (onSelectPart != null) {
                                onSelectPart(currentPartIndex)
                            } else {
                                activeSession = ActiveVocabSession.Flashcard(currentPartIndex)
                            }
                        }
                    )
                }

                SectionLabel(text = L10n.allPartsSectionLabel(parts.size))

                // 25'erli Kelime Parçaları Listesi
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    partStatuses.forEach { status ->
                        VocabPartRow(
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
                                            activeSession = ActiveVocabSession.Flashcard(status.partIndex)
                                        }
                                    }
                                    else -> {
                                        lockedPartNumber = status.partIndex + 1
                                    }
                                }
                            },
                            onPreviewWords = {
                                FeedbackManager.shared.playSelection()
                                previewPartIndex = status.partIndex
                            },
                            onStartQuiz = {
                                FeedbackManager.shared.playSelection()
                                activeSession = ActiveVocabSession.Quiz(status.partIndex)
                            }
                        )
                    }
                }
            }
        }
    }

    // Kelime Listesi Önizleme & Sesli Dinleme Sheet'i
    val previewIdx = previewPartIndex
    if (previewIdx != null && previewIdx in parts.indices) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { previewPartIndex = null },
            sheetState = sheetState,
            containerColor = Theme.paper,
            contentColor = Theme.ink
        ) {
            VocabWordPreviewSheetContent(
                title = L10n.vocabularySessionTitle(level, previewIdx + 1),
                words = parts[previewIdx],
                onSpeakWord = { word ->
                    AudioService.shared.speak(word.hiragana.ifEmpty { word.displayText })
                },
                onStartFlashcards = {
                    val idx = previewIdx
                    previewPartIndex = null
                    activeSession = ActiveVocabSession.Flashcard(idx)
                },
                onStartQuiz = {
                    val idx = previewIdx
                    previewPartIndex = null
                    activeSession = ActiveVocabSession.Quiz(idx)
                }
            )
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
private fun VocabContinueCard(
    level: String,
    partIndex: Int,
    totalParts: Int,
    wordCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .inkBordered(lineWidth = 2.5.dp)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = L10n.resumeLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Theme.accent
            )
            Text(
                text = L10n.partFractionLabel(partIndex + 1, totalParts),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.secondaryInk
            )
        }

        Text(
            text = L10n.vocabularyContinueCardTitle(level, partIndex + 1),
            style = Theme.heading(20.sp),
            color = Theme.ink
        )

        Text(
            text = L10n.vocabularyContinueCardSubtitle(wordCount),
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
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = L10n.startStudyButton,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Theme.paper
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Theme.paper,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun VocabPartRow(
    level: String,
    status: VocabPartStatus,
    onRowClick: () -> Unit,
    onPreviewWords: () -> Unit,
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
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
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
                    style = Theme.heading(18.sp),
                    color = Color.White
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = L10n.vocabularyPartTitle(level, status.partIndex + 1),
                    style = Theme.heading(18.sp),
                    color = if (isInteractiveUnlocked) Theme.ink else Theme.secondaryInk
                )
                Text(
                    text = when {
                        status.isProLocked -> L10n.proUnlockHint
                        !status.isUnlocked -> L10n.partLockedSubtitle
                        status.isCompleted -> "${L10n.vocabPartCompletedSubtitle(status.count)} • ${L10n.learnedPercentLabel(status.learnedPercent)}"
                        else -> "${L10n.wordCount(status.count)} • ${L10n.learnedPercentLabel(status.learnedPercent)}"
                    },
                    fontSize = 12.sp,
                    color = Theme.secondaryInk
                )
            }

            when {
                status.isProLocked -> {
                    ProBadge()
                }
                isInteractiveUnlocked -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Kelime Önizleme & Sesli Dinleme Butonu
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color = Theme.ink.copy(alpha = 0.06f), shape = CircleShape)
                                .clickable(onClick = onPreviewWords),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = L10n.wordListPreviewButton,
                                tint = Theme.ink,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Hızlı Quiz Butonu
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(color = Theme.paper, shape = CircleShape)
                                .border(width = 1.5.dp, color = Theme.ink, shape = CircleShape)
                                .clickable(onClick = onStartQuiz)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = L10n.quizTitle,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Theme.ink
                            )
                        }

                        if (status.isCompleted) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = Theme.success,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Theme.ink,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
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

        if (isInteractiveUnlocked) {
            ProgressBar(
                fraction = status.learnedRatio,
                height = 6.dp
            )
        }
    }
}

@Composable
private fun VocabWordPreviewSheetContent(
    title: String,
    words: List<VocabularyWord>,
    onSpeakWord: (VocabularyWord) -> Unit,
    onStartFlashcards: () -> Unit,
    onStartQuiz: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = Theme.heading(20.sp),
                    color = Theme.ink
                )
                Text(
                    text = "${L10n.wordListPreviewButton} • ${L10n.wordCount(words.size)}",
                    fontSize = 13.sp,
                    color = Theme.secondaryInk
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val pillShape = RoundedCornerShape(10.dp)
                Box(
                    modifier = Modifier
                        .clip(pillShape)
                        .background(color = Theme.accent, shape = pillShape)
                        .clickable(onClick = onStartFlashcards)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = L10n.startFlashcardsButton,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Theme.paper
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(pillShape)
                        .background(color = Theme.paper, shape = pillShape)
                        .border(width = 1.5.dp, color = Theme.ink, shape = pillShape)
                        .clickable(onClick = onStartQuiz)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = L10n.quizTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Theme.ink
                    )
                }
            }
        }

        HorizontalDivider(color = Theme.secondaryInk.copy(alpha = 0.25f))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(words, key = { it.id }) { word ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .inkBordered(lineWidth = 1.5.dp)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FuriganaText(
                            text = word.displayText,
                            style = Theme.heading(20.sp),
                            color = Theme.ink
                        )
                        Text(
                            text = if (word.hiragana.isNotEmpty() && word.hiragana != word.displayText) {
                                "${word.hiragana} (${word.romaji})"
                            } else {
                                word.romaji
                            },
                            fontSize = 13.sp,
                            color = Theme.secondaryInk
                        )
                        Text(
                            text = word.turkishMeaning,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Theme.accent
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color = Theme.accent.copy(alpha = 0.12f), shape = CircleShape)
                            .clickable { onSpeakWord(word) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = L10n.listenButton,
                            tint = Theme.accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
