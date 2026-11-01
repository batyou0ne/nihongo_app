package com.batuhankeskn0.kaimon.ui.screens.reading

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.model.ExamMode
import com.batuhankeskn0.kaimon.data.model.Story
import com.batuhankeskn0.kaimon.data.model.StoryType
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.FeatureGate
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import com.batuhankeskn0.kaimon.ui.components.LevelSelectorBar
import com.batuhankeskn0.kaimon.ui.components.LockedLevelInfoSheet
import com.batuhankeskn0.kaimon.ui.components.SectionLabel
import com.batuhankeskn0.kaimon.ui.screens.quiz.GatewayExamScreen
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hikayeleri yazı sistemine (`StoryType`: `hiragana`, `hiraganaKatakana`, `hiraganaKanji`, `all`)
 * göre filtreler ve sıralı gruplar halinde döner (`ReadingMainView.groupedStories` karşılığı).
 */
fun groupStoriesByType(
    stories: List<Story>,
    filterType: StoryType? = null
): List<Pair<StoryType, List<Story>>> {
    val grouped = stories.groupBy { it.type }
    val typesToInclude = if (filterType != null) listOf(filterType) else StoryType.allCases
    return typesToInclude.mapNotNull { type ->
        val items = grouped[type]
        if (!items.isNullOrEmpty()) type to items else null
    }
}

private data class ActiveReadingExamRequest(
    val level: JLPTLevel,
    val mode: ExamMode
)

/**
 * Okuma & Hikaye Modülü Ana Ekranı (`Views/ReadingMainView.swift` birebir karşılığı).
 *
 * - N5–N1 seviyelerine (`LevelSelectorBar`) ve yazı tipine (`StoryType`: `hiragana`, `hiraganaKatakana`,
 *   `hiraganaKanji`, `all`) göre hikayeleri listeler.
 * - Seçilen hikayeye tıklandığında `StoryReaderScreen` açılır.
 */
@Composable
fun ReadingMainScreen(
    modifier: Modifier = Modifier,
    onOpenPaywall: (() -> Unit)? = null,
    featureGate: FeatureGate = FeatureGate.shared
) {
    var selectedLevel by remember { mutableStateOf(JLPTLevel.N5) }
    var selectedStoryType by remember { mutableStateOf<StoryType?>(null) }
    var selectedStory by remember { mutableStateOf<Story?>(null) }
    var selectedLockedLevel by remember { mutableStateOf<JLPTLevel?>(null) }
    var activeExam by remember { mutableStateOf<ActiveReadingExamRequest?>(null) }
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

    val allStories by produceState<List<Story>>(
        initialValue = emptyList(),
        key1 = selectedLevel
    ) {
        value = withContext(Dispatchers.IO) {
            ContentStore.loadStories(level = selectedLevel.rawValue)
        }
    }

    val groupedStories = remember(allStories, selectedStoryType) {
        groupStoriesByType(stories = allStories, filterType = selectedStoryType)
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching {
                LevelProgressionService.shared.ensureInitialProgress()
            }
        }
    }

    BackHandler(enabled = activeExam != null || selectedStory != null) {
        when {
            activeExam != null -> activeExam = null
            selectedStory != null -> selectedStory = null
        }
    }

    // 1. Test-Out Sınavı Açık İse
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

    // 2. Hikaye Okuyucu Açık İse
    val activeStory = selectedStory
    if (activeStory != null) {
        StoryReaderScreen(
            story = activeStory,
            onBack = { selectedStory = null },
            modifier = modifier
        )
        return
    }

    // 3. Ana Okuma Ekranı
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
            text = L10n.readingTitle,
            style = Theme.display(36.sp),
            color = Theme.ink,
            modifier = Modifier.padding(top = 10.dp)
        )

        // N5–N1 Seviye Seçim Barı
        LevelSelectorBar(
            selectedLevel = selectedLevel,
            isLevelUnlocked = { unlockedLevels.contains(it) },
            onLevelSelected = { level ->
                FeedbackManager.shared.playSelection()
                selectedLevel = level
            },
            onLockedLevelClicked = { locked ->
                FeedbackManager.shared.playSelection()
                selectedLockedLevel = locked
            }
        )

        // Yazı Tipi (StoryType) Filtre Çubuğu
        if (allStories.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StoryTypeFilterChip(
                    label = L10n.allFilterLabel,
                    isSelected = selectedStoryType == null,
                    onClick = {
                        FeedbackManager.shared.playSelection()
                        selectedStoryType = null
                    }
                )
                StoryType.allCases.forEach { type ->
                    StoryTypeFilterChip(
                        label = type.displayName,
                        isSelected = selectedStoryType == type,
                        onClick = {
                            FeedbackManager.shared.playSelection()
                            selectedStoryType = type
                        }
                    )
                }
            }
        }

        // Hikaye Grupları
        if (groupedStories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = L10n.noStoriesForLevel,
                    fontSize = 15.sp,
                    color = Theme.secondaryInk
                )
            }
        } else {
            groupedStories.forEach { (storyType, stories) ->
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionLabel(text = storyType.displayName.uppercase())

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        stories.forEach { story ->
                            StoryRowCard(
                                story = story,
                                onClick = {
                                    FeedbackManager.shared.playSelection()
                                    selectedStory = story
                                }
                            )
                        }
                    }
                }
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
                    activeExam = ActiveReadingExamRequest(level = prevLevel, mode = ExamMode.TEST_OUT)
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

@Composable
private fun StoryTypeFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Theme.ink else Theme.paper
    val fgColor = if (isSelected) Theme.paper else Theme.ink

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color = bgColor, shape = CircleShape)
            .border(
                width = 1.5.dp,
                color = Theme.ink,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = fgColor
        )
    }
}

@Composable
private fun StoryRowCard(
    story: Story,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .inkBordered()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.width(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Book,
                contentDescription = null,
                tint = Theme.accent,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = story.title,
                style = Theme.heading(18.sp),
                color = Theme.ink
            )
            Text(
                text = L10n.sentenceCount(story.sentences.size),
                fontSize = 12.sp,
                color = Theme.secondaryInk
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Theme.ink,
            modifier = Modifier.size(16.dp)
        )
    }
}
