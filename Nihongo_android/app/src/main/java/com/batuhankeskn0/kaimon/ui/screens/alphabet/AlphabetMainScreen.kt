package com.batuhankeskn0.kaimon.ui.screens.alphabet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.local.LearningItemProgressEntity
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import com.batuhankeskn0.kaimon.data.local.UserLevelProgressEntity
import com.batuhankeskn0.kaimon.data.model.CharacterType
import com.batuhankeskn0.kaimon.data.model.JapaneseCharacter
import com.batuhankeskn0.kaimon.data.model.Kanji
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import com.batuhankeskn0.kaimon.ui.components.LevelSelectionRow
import com.batuhankeskn0.kaimon.ui.components.LockedLevelInfoSheet
import com.batuhankeskn0.kaimon.ui.components.ModuleCard
import com.batuhankeskn0.kaimon.ui.components.SectionLabel
import com.batuhankeskn0.kaimon.ui.screens.learning.CharacterCardView
import com.batuhankeskn0.kaimon.ui.screens.learning.FlashcardSessionScreen
import com.batuhankeskn0.kaimon.ui.screens.learning.LearningScreen
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Alfabe ana ekranı görünüm modu (`AlphabetMainView.swift` -> `AlphabetDisplayMode`).
 */
enum class AlphabetDisplayMode {
    PRACTICE,
    CHART
}

/**
 * Alfabe sekmesi içindeki alt navigasyon hedefleri.
 */
sealed interface AlphabetDestination {
    data object Root : AlphabetDestination
    data class Learning(val characterType: CharacterType) : AlphabetDestination
    data object KanjiLevels : AlphabetDestination
    data class KanjiParts(val level: String) : AlphabetDestination
    data class KanjiSession(val level: String, val partIndex: Int) : AlphabetDestination
}

/**
 * Alfabe Ana Ekranı (`Views/AlphabetMainView.swift` karşılığı).
 *
 * - Üstte `"Kartlarla Çalış"` (`PRACTICE`) ve `"Alfabe Tablosu"` (`CHART`) sekme seçicisi sunar.
 * - `"Kartlarla Çalış"` sekmesinde Hiragana, Katakana ve Kanji modül kartlarını (`ModuleCard`)
 *   öğrenilen öğe sayılarıyla gösterir ve tıklanınca ilgili `LearningScreen` / Kanji akışını açar.
 * - `"Alfabe Tablosu"` sekmesinde etkileşimli, sesli `AlphabetChartScreen` ekranını gösterir.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun AlphabetMainScreen(
    modifier: Modifier = Modifier,
    initialDestination: AlphabetDestination = AlphabetDestination.Root,
    onOpenKanjiExternal: (() -> Unit)? = null
) {
    var displayMode by rememberSaveable { mutableStateOf(AlphabetDisplayMode.PRACTICE) }
    var destination by remember { mutableStateOf(initialDestination) }

    LaunchedEffect(initialDestination) {
        destination = initialDestination
    }

    val allProgress by produceState<List<LearningItemProgressEntity>>(initialValue = emptyList()) {
        UserDatabaseManager.shared.currentUidFlow
            .flatMapLatest { uid ->
                UserDatabaseManager.shared.getDatabase(uid).progressDao().observeAllItemProgress()
            }
            .flowOn(Dispatchers.IO)
            .collect { items ->
                value = items
            }
    }

    val learnedCounts = remember(allProgress) {
        computeAlphabetLearnedCounts(allProgress)
    }

    BackHandler(enabled = destination != AlphabetDestination.Root) {
        destination = when (val current = destination) {
            is AlphabetDestination.Learning -> AlphabetDestination.Root
            is AlphabetDestination.KanjiLevels -> AlphabetDestination.Root
            is AlphabetDestination.KanjiParts -> AlphabetDestination.KanjiLevels
            is AlphabetDestination.KanjiSession -> AlphabetDestination.KanjiParts(current.level)
            AlphabetDestination.Root -> AlphabetDestination.Root
        }
    }

    when (val current = destination) {
        is AlphabetDestination.Learning -> {
            LearningScreen(
                characterType = current.characterType,
                onBack = { destination = AlphabetDestination.Root },
                modifier = modifier
            )
        }
        is AlphabetDestination.KanjiLevels -> {
            KanjiLevelSelectionContent(
                onSelectLevel = { level ->
                    destination = AlphabetDestination.KanjiParts(level.rawValue)
                },
                onBack = { destination = AlphabetDestination.Root },
                modifier = modifier
            )
        }
        is AlphabetDestination.KanjiParts -> {
            KanjiPartSelectionContent(
                level = current.level,
                onSelectPart = { partIndex ->
                    destination = AlphabetDestination.KanjiSession(
                        level = current.level,
                        partIndex = partIndex
                    )
                },
                onBack = { destination = AlphabetDestination.KanjiLevels },
                modifier = modifier
            )
        }
        is AlphabetDestination.KanjiSession -> {
            val allKanji by produceState<List<Kanji>>(
                initialValue = emptyList(),
                key1 = current.level
            ) {
                value = withContext(Dispatchers.IO) {
                    ContentStore.loadKanji(level = current.level)
                }
            }
            val parts = remember(allKanji, current.level) {
                if (allKanji.isEmpty()) emptyList() else ContentStore.kanjiParts(level = current.level)
            }
            val partItems = parts.getOrNull(current.partIndex).orEmpty()

            if (partItems.isNotEmpty()) {
                FlashcardSessionScreen(
                    sessionKey = "kanji_${current.level}_part${current.partIndex + 1}",
                    itemKind = LearnableItemKind.KANJI,
                    allItems = partItems,
                    distractorPool = allKanji,
                    accentColor = Theme.accent,
                    title = L10n.kanjiPartTitle(current.level, current.partIndex + 1),
                    onBack = { destination = AlphabetDestination.KanjiParts(current.level) },
                    modifier = modifier
                )
            }
        }
        AlphabetDestination.Root -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(Theme.paper)
            ) {
                // Başlık
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 10.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = L10n.alphabetTitle,
                        style = Theme.display(36.sp),
                        color = Theme.ink
                    )
                }

                // Sekme Değiştirici: Kartlarla Çalış (Varsayılan) / Alfabe Tablosu (Sesli)
                AlphabetModeSegmentedPicker(
                    displayMode = displayMode,
                    onModeSelected = { mode ->
                        FeedbackManager.shared.playSelection()
                        displayMode = mode
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp)
                )

                if (displayMode == AlphabetDisplayMode.PRACTICE) {
                    PracticeCardsTabContent(
                        learnedCounts = learnedCounts,
                        onOpenHiragana = {
                            FeedbackManager.shared.playSelection()
                            destination = AlphabetDestination.Learning(CharacterType.HIRAGANA)
                        },
                        onOpenKatakana = {
                            FeedbackManager.shared.playSelection()
                            destination = AlphabetDestination.Learning(CharacterType.KATAKANA)
                        },
                        onOpenKanji = {
                            FeedbackManager.shared.playSelection()
                            if (onOpenKanjiExternal != null) {
                                onOpenKanjiExternal()
                            } else {
                                destination = AlphabetDestination.KanjiLevels
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AlphabetChartScreen(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Öğrenilen karakter/kanji sayılarını (`repetitionCount >= 1`) hesaplar.
 */
fun computeAlphabetLearnedCounts(
    allProgress: List<LearningItemProgressEntity>
): Map<LearnableItemKind, Int> = mapOf(
    LearnableItemKind.HIRAGANA_CHARACTER to allProgress.count {
        it.itemKind == LearnableItemKind.HIRAGANA_CHARACTER && it.repetitionCount >= 1
    },
    LearnableItemKind.KATAKANA_CHARACTER to allProgress.count {
        it.itemKind == LearnableItemKind.KATAKANA_CHARACTER && it.repetitionCount >= 1
    },
    LearnableItemKind.KANJI to allProgress.count {
        it.itemKind == LearnableItemKind.KANJI && it.repetitionCount >= 1
    }
)

@Composable
private fun AlphabetModeSegmentedPicker(
    displayMode: AlphabetDisplayMode,
    onModeSelected: (AlphabetDisplayMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .background(color = Theme.ink.copy(alpha = 0.06f), shape = shape)
            .border(width = 1.5.dp, color = Theme.ink.copy(alpha = 0.2f), shape = shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val tabs = listOf(
            AlphabetDisplayMode.PRACTICE to L10n.practiceCardsTab,
            AlphabetDisplayMode.CHART to L10n.alphabetChartTitle
        )
        tabs.forEach { (mode, title) ->
            val isSelected = displayMode == mode
            val pillShape = RoundedCornerShape(9.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(pillShape)
                    .background(
                        color = if (isSelected) Theme.paper else Color.Transparent,
                        shape = pillShape
                    )
                    .then(
                        if (isSelected) {
                            Modifier.border(width = 1.5.dp, color = Theme.ink, shape = pillShape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onModeSelected(mode) }
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Theme.ink else Theme.secondaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PracticeCardsTabContent(
    learnedCounts: Map<LearnableItemKind, Int>,
    onOpenHiragana: () -> Unit,
    onOpenKatakana: () -> Unit,
    onOpenKanji: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previewCharacters by produceState<List<JapaneseCharacter>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) {
            ContentStore.loadCharacters(CharacterType.HIRAGANA).take(5)
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionLabel(text = L10n.sectionLearn)

        // Satır 1: Hiragana & Katakana
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModuleCard(
                kind = LearnableItemKind.HIRAGANA_CHARACTER,
                subtitle = "あいうえお",
                learned = learnedCounts[LearnableItemKind.HIRAGANA_CHARACTER] ?: 0,
                onClick = onOpenHiragana,
                modifier = Modifier.weight(1f)
            )
            ModuleCard(
                kind = LearnableItemKind.KATAKANA_CHARACTER,
                subtitle = "アイウエオ",
                learned = learnedCounts[LearnableItemKind.KATAKANA_CHARACTER] ?: 0,
                onClick = onOpenKatakana,
                modifier = Modifier.weight(1f)
            )
        }

        // Satır 2: Kanji
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModuleCard(
                kind = LearnableItemKind.KANJI,
                subtitle = L10n.kanjiModuleSubtitle(4),
                learned = learnedCounts[LearnableItemKind.KANJI] ?: 0,
                onClick = onOpenKanji,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        // Hızlı Karakter Kartı Önizleme (`CharacterCardView`)
        if (previewCharacters.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                previewCharacters.forEach { character ->
                    CharacterCardView(character = character)
                }
            }
        }
    }
}

/**
 * Alfabe modülü içinden erişilen Kanji Seviye Seçimi (`Views/Kanji/KanjiLevelSelectionView.swift` karşılığı).
 */
@Composable
private fun KanjiLevelSelectionContent(
    onSelectLevel: (JLPTLevel) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLockedLevel by remember { mutableStateOf<JLPTLevel?>(null) }

    val unlockedLevels by produceState(initialValue = setOf(JLPTLevel.N5)) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val records = LevelProgressionService.shared.ensureInitialProgress()
                JLPTLevel.allCases.filter {
                    LevelProgressionService.shared.isLevelUnlocked(it, records)
                }.toSet()
            }.getOrDefault(setOf(JLPTLevel.N5))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
                .padding(20.dp)
                .padding(bottom = 90.dp),
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
                        if (isUnlocked) {
                            onSelectLevel(level)
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
            onStartTestOut = { selectedLockedLevel = null }
        )
    }
}

/**
 * Seçilen JLPT seviyesinin Kanji bölüm seçimi (`Views/Kanji/KanjiPartSelectionView.swift` karşılığı).
 */
@Composable
private fun KanjiPartSelectionContent(
    level: String,
    onSelectPart: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val jlptLevel = remember(level) { JLPTLevel.fromRawValue(level) ?: JLPTLevel.N5 }
    var lockedPartNumber by remember { mutableIntStateOf(0) }

    val allKanji by produceState<List<Kanji>>(initialValue = emptyList(), key1 = level) {
        value = withContext(Dispatchers.IO) {
            ContentStore.loadKanji(level = level)
        }
    }

    val parts = remember(allKanji, level) {
        if (allKanji.isEmpty()) emptyList() else ContentStore.kanjiParts(level = level)
    }

    val levelProgress by produceState<UserLevelProgressEntity?>(initialValue = null, key1 = jlptLevel) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                LevelProgressionService.shared.getProgress(jlptLevel)
            }.getOrNull()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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

            Text(
                text = L10n.kanjiLevel(level),
                style = Theme.heading(20.sp),
                color = Theme.ink
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .padding(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            parts.forEachIndexed { index, part ->
                val isUnlocked = LevelProgressionService.shared.isPartUnlocked(
                    module = "kanji",
                    level = jlptLevel,
                    partIndex = index,
                    progress = levelProgress
                )
                val isCompleted = levelProgress?.isPartCompleted(module = "kanji", index = index) == true

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isUnlocked) 1f else 0.6f)
                        .inkBordered()
                        .clickable {
                            if (isUnlocked) {
                                onSelectPart(index)
                            } else {
                                lockedPartNumber = index + 1
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = when {
                                    !isUnlocked -> Theme.secondaryInk
                                    isCompleted -> Theme.success
                                    else -> Theme.accent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = Theme.heading(19.sp),
                            color = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = L10n.kanjiPartTitle(level, index + 1),
                            style = Theme.heading(19.sp),
                            color = if (isUnlocked) Theme.ink else Theme.secondaryInk
                        )
                        Text(
                            text = when {
                                !isUnlocked -> L10n.partLockedSubtitle
                                isCompleted -> L10n.kanjiPartCompletedSubtitle(part.size)
                                else -> L10n.kanjiCount(part.size)
                            },
                            fontSize = 14.sp,
                            color = Theme.secondaryInk
                        )
                    }

                    Icon(
                        imageVector = when {
                            isCompleted -> Icons.Filled.CheckCircle
                            isUnlocked -> Icons.AutoMirrored.Filled.ArrowForward
                            else -> Icons.Filled.Lock
                        },
                        contentDescription = null,
                        tint = when {
                            isCompleted -> Theme.success
                            isUnlocked -> Theme.ink
                            else -> Theme.secondaryInk
                        },
                        modifier = Modifier.size(20.dp)
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
}
