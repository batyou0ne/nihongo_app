package com.batuhankeskn0.kaimon.ui.screens.reading

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.local.ActivityDao
import com.batuhankeskn0.kaimon.data.local.ProgressDao
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import com.batuhankeskn0.kaimon.data.model.Story
import com.batuhankeskn0.kaimon.data.model.StorySentence
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.XPManager
import com.batuhankeskn0.kaimon.ui.components.FuriganaText
import com.batuhankeskn0.kaimon.ui.components.LessonCompleteView
import com.batuhankeskn0.kaimon.ui.navigation.LocalTabBarManager
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Hikaye okuma görünüm modu: Tam Metin veya Cümle Cümle.
 */
enum class StoryReadingMode {
    CONTINUOUS,
    SENTENCE_BY_SENTENCE
}

/**
 * Hikayedeki tüm cümlelerin kelime listelerini sırasını koruyarak tekilleştirir (`StoryReaderView.allVocabulary` karşılığı).
 */
fun extractUniqueStoryVocabulary(story: Story): List<String> {
    val uniqueVocab = mutableListOf<String>()
    val seen = HashSet<String>()
    for (sentence in story.sentences) {
        for (vocab in sentence.vocabulary) {
            if (seen.add(vocab)) {
                uniqueVocab.add(vocab)
            }
        }
    }
    return uniqueVocab
}

/**
 * Hikaye tamamlandığında `XPManager.addXP(STORY_COMPLETED)` ödülünü ve günlük çalışma serisini kaydeder.
 * `(yeniSeri, seriArttıMı)` çiftini döner.
 */
suspend fun recordStoryCompleted(
    progressDao: ProgressDao = UserDatabaseManager.shared.progressDao,
    activityDao: ActivityDao = UserDatabaseManager.shared.activityDao,
    xpManager: XPManager = XPManager.shared,
    nowMillis: Long = System.currentTimeMillis()
): Pair<Int, Boolean> {
    xpManager.addXP(
        action = XPManager.XPAction.STORY_COMPLETED,
        progressDao = progressDao,
        activityDao = activityDao,
        nowMillis = nowMillis
    )
    val userProgress = progressDao.getOrCreateUserProgress()
    val prevStreak = userProgress.activeStreak(nowMillis)
    userProgress.recordStudySession(nowMillis)
    progressDao.upsertUserProgress(userProgress)
    val newStreak = userProgress.activeStreak(nowMillis)
    return newStreak to (newStreak > prevStreak)
}

/**
 * Hikaye Okuyucu Ekranı (`Views/StoryReaderView.swift` birebir karşılığı).
 *
 * Özellikler:
 * 1. Tam Metin (`CONTINUOUS`) veya Cümle Cümle (`SENTENCE_BY_SENTENCE`) okuma görünümü.
 * 2. `FuriganaText` entegrasyonu ve Furigana / Romaji / Türkçe Çeviri aç-kapat (toggle) butonları.
 * 3. Her cümlenin sesli okunması (`AudioService.shared.speak`) ve cümleye ait kelime kutucukları (`vocabulary`).
 * 4. Hikaye tamamlandığında `XPManager.addXP(STORY_COMPLETED)` ödülü ve `LessonCompleteView` ekranı.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StoryReaderScreen(
    story: Story,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tabBarManager = LocalTabBarManager.current
    val scope = rememberCoroutineScope()

    var readingMode by remember { mutableStateOf(StoryReadingMode.SENTENCE_BY_SENTENCE) }
    var showFurigana by remember { mutableStateOf(true) }
    var showRomaji by remember { mutableStateOf(false) }
    var showTranslation by remember { mutableStateOf(false) }
    var selectedSentence by remember { mutableStateOf<StorySentence?>(null) }

    var showLessonComplete by remember { mutableStateOf(false) }
    var streak by remember { mutableIntStateOf(0) }
    var streakIncreased by remember { mutableStateOf(false) }

    val allVocabulary = remember(story) { extractUniqueStoryVocabulary(story) }

    DisposableEffect(tabBarManager) {
        val wasHidden = tabBarManager.isHidden
        tabBarManager.hide()
        onDispose {
            AudioService.shared.stop()
            if (!wasHidden) {
                tabBarManager.show()
            }
        }
    }

    BackHandler(enabled = selectedSentence != null || showLessonComplete || onBack != null) {
        when {
            selectedSentence != null -> {
                AudioService.shared.stop()
                selectedSentence = null
            }
            showLessonComplete -> {
                showLessonComplete = false
                onBack?.invoke()
            }
            else -> onBack?.invoke()
        }
    }

    if (showLessonComplete) {
        LessonCompleteView(
            xpGained = XPManager.XPAction.STORY_COMPLETED.points,
            streak = streak,
            streakIncreased = streakIncreased,
            ringColor = Theme.accent,
            onContinue = {
                showLessonComplete = false
                onBack?.invoke()
            },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
    ) {
        // Üst Navigasyon Barı + Okuma Modu Seçici
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (onBack != null) {
                Row(
                    modifier = Modifier
                        .clickable {
                            FeedbackManager.shared.playSelection()
                            AudioService.shared.stop()
                            onBack()
                        }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
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
                        fontWeight = FontWeight.Bold,
                        color = Theme.ink
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(1.dp))
            }

            // Tam Metin / Cümle Cümle Görünüm Seçici
            Row(
                modifier = Modifier
                    .inkBordered(lineWidth = 1.5.dp, cornerRadius = 10.dp)
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                StoryModeChip(
                    label = L10n.readingModeContinuous,
                    isSelected = readingMode == StoryReadingMode.CONTINUOUS,
                    onClick = {
                        FeedbackManager.shared.playSelection()
                        readingMode = StoryReadingMode.CONTINUOUS
                    }
                )
                StoryModeChip(
                    label = L10n.readingModeSentenceBySentence,
                    isSelected = readingMode == StoryReadingMode.SENTENCE_BY_SENTENCE,
                    onClick = {
                        FeedbackManager.shared.playSelection()
                        readingMode = StoryReadingMode.SENTENCE_BY_SENTENCE
                    }
                )
            }
        }

        // Furigana / Romaji / Çeviri Toggle Çubuğu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StoryDisplayToggleButton(
                label = L10n.toggleFuriganaLabel,
                isActive = showFurigana,
                onClick = {
                    FeedbackManager.shared.playSelection()
                    showFurigana = !showFurigana
                }
            )
            StoryDisplayToggleButton(
                label = L10n.toggleRomajiLabel,
                isActive = showRomaji,
                onClick = {
                    FeedbackManager.shared.playSelection()
                    showRomaji = !showRomaji
                }
            )
            StoryDisplayToggleButton(
                label = L10n.toggleTranslationLabel,
                isActive = showTranslation,
                onClick = {
                    FeedbackManager.shared.playSelection()
                    showTranslation = !showTranslation
                }
            )
        }

        // Ana Hikaye İçeriği
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Text(
                text = story.title,
                style = Theme.display(28.sp),
                color = Theme.ink
            )

            Text(
                text = L10n.storyTapSentenceHint,
                fontSize = 13.sp,
                color = Theme.secondaryInk
            )

            if (readingMode == StoryReadingMode.CONTINUOUS) {
                StoryContinuousContent(
                    sentences = story.sentences,
                    selectedSentenceId = selectedSentence?.id,
                    showFurigana = showFurigana,
                    showRomaji = showRomaji,
                    showTranslation = showTranslation,
                    onSentenceClicked = { sentence ->
                        FeedbackManager.shared.playSelection()
                        AudioService.shared.speak(sentence.japaneseText)
                        selectedSentence = sentence
                    }
                )
            } else {
                StorySentenceBySentenceContent(
                    sentences = story.sentences,
                    selectedSentenceId = selectedSentence?.id,
                    showFurigana = showFurigana,
                    showRomaji = showRomaji,
                    showTranslation = showTranslation,
                    onSentenceClicked = { sentence ->
                        FeedbackManager.shared.playSelection()
                        selectedSentence = sentence
                    },
                    onSpeakSentence = { sentence ->
                        FeedbackManager.shared.playSelection()
                        AudioService.shared.speak(sentence.japaneseText)
                    }
                )
            }

            HorizontalDivider(color = Theme.secondaryInk.copy(alpha = 0.3f))

            // Hikayenin Tüm Kelimeleri Kutucukları
            if (allVocabulary.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = L10n.storyVocabulary,
                        style = Theme.heading(18.sp),
                        color = Theme.secondaryInk
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allVocabulary.forEach { vocab ->
                            StoryVocabChip(vocab = vocab)
                        }
                    }
                }
            }

            // "Hikayeyi Tamamla" Ödül Butonu
            val buttonShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .clip(buttonShape)
                    .background(color = Theme.accent, shape = buttonShape)
                    .clickable {
                        FeedbackManager.shared.playSelection()
                        scope.launch(Dispatchers.IO) {
                            val (newStreak, didIncrease) = runCatching {
                                recordStoryCompleted()
                            }.getOrDefault(1 to false)

                            withContext(Dispatchers.Main) {
                                streak = newStreak
                                streakIncreased = didIncrease
                                showLessonComplete = true
                            }
                        }
                    }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = L10n.completeStory,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Theme.paper
                )
            }
        }
    }

    // Cümle İncelemesi Alt Sayfası (SentenceDetailPopup)
    val detailSentence = selectedSentence
    if (detailSentence != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                AudioService.shared.stop()
                selectedSentence = null
            },
            sheetState = sheetState,
            containerColor = Theme.paper,
            contentColor = Theme.ink
        ) {
            SentenceDetailSheetContent(
                sentence = detailSentence,
                showFurigana = showFurigana,
                onSpeak = {
                    FeedbackManager.shared.playSelection()
                    AudioService.shared.speak(detailSentence.japaneseText)
                },
                onClose = {
                    AudioService.shared.stop()
                    selectedSentence = null
                }
            )
        }
    }
}

@Composable
private fun StoryModeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                color = if (isSelected) Theme.ink else Theme.paper,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Theme.paper else Theme.ink
        )
    }
}

@Composable
private fun StoryDisplayToggleButton(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isActive) Theme.accent else Theme.paper
    val fgColor = if (isActive) Theme.paper else Theme.ink

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color = bgColor, shape = CircleShape)
            .border(
                width = 1.5.dp,
                color = if (isActive) Theme.accent else Theme.ink,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = fgColor
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoryContinuousContent(
    sentences: List<StorySentence>,
    selectedSentenceId: String?,
    showFurigana: Boolean,
    showRomaji: Boolean,
    showTranslation: Boolean,
    onSentenceClicked: (StorySentence) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .inkBordered()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sentences.forEach { sentence ->
                val isSelected = sentence.id == selectedSentenceId
                val textColor = if (isSelected) Theme.accent else Theme.ink

                Box(
                    modifier = Modifier
                        .clickable { onSentenceClicked(sentence) }
                        .padding(vertical = 2.dp)
                ) {
                    if (showFurigana) {
                        FuriganaText(
                            text = sentence.japaneseText,
                            style = Theme.heading(22.sp),
                            color = textColor
                        )
                    } else {
                        Text(
                            text = sentence.japaneseText,
                            style = Theme.heading(22.sp),
                            color = textColor,
                            lineHeight = 32.sp
                        )
                    }
                }
            }
        }

        if (showRomaji || showTranslation) {
            HorizontalDivider(color = Theme.secondaryInk.copy(alpha = 0.25f))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sentences.forEach { sentence ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (showRomaji) {
                            Text(
                                text = sentence.romaji,
                                fontSize = 13.sp,
                                color = Theme.secondaryInk
                            )
                        }
                        if (showTranslation) {
                            Text(
                                text = sentence.turkishTranslation,
                                fontSize = 14.sp,
                                color = Theme.ink
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StorySentenceBySentenceContent(
    sentences: List<StorySentence>,
    selectedSentenceId: String?,
    showFurigana: Boolean,
    showRomaji: Boolean,
    showTranslation: Boolean,
    onSentenceClicked: (StorySentence) -> Unit,
    onSpeakSentence: (StorySentence) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        sentences.forEach { sentence ->
            val isSelected = sentence.id == selectedSentenceId
            val textColor = if (isSelected) Theme.accent else Theme.ink

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .inkBordered()
                    .clickable { onSentenceClicked(sentence) }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp)
                    ) {
                        if (showFurigana) {
                            FuriganaText(
                                text = sentence.japaneseText,
                                style = Theme.heading(22.sp),
                                color = textColor
                            )
                        } else {
                            Text(
                                text = sentence.japaneseText,
                                style = Theme.heading(22.sp),
                                color = textColor,
                                lineHeight = 30.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Theme.accent.copy(alpha = 0.14f))
                            .clickable { onSpeakSentence(sentence) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = L10n.listenPronunciation,
                            tint = Theme.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (showRomaji) {
                    Text(
                        text = sentence.romaji,
                        fontSize = 14.sp,
                        color = Theme.secondaryInk
                    )
                }

                if (showTranslation) {
                    Text(
                        text = sentence.turkishTranslation,
                        fontSize = 15.sp,
                        color = Theme.ink
                    )
                }

                if (sentence.vocabulary.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sentence.vocabulary.forEach { vocab ->
                            StoryVocabChip(vocab = vocab)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryVocabChip(vocab: String) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(color = Theme.ink.copy(alpha = 0.06f), shape = shape)
            .border(width = 1.dp, color = Theme.ink.copy(alpha = 0.35f), shape = shape)
            .clickable {
                val japanesePart = vocab.substringBefore("(").trim()
                if (japanesePart.isNotEmpty()) {
                    FeedbackManager.shared.playSelection()
                    AudioService.shared.speak(japanesePart)
                }
            }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = vocab,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Theme.ink
        )
    }
}

/**
 * iOS `SentenceDetailPopup` bileşeninin birebir karşılığı.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SentenceDetailSheetContent(
    sentence: StorySentence,
    showFurigana: Boolean,
    onSpeak: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Başlık + Kapat
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = L10n.sentenceReview,
                style = Theme.heading(18.sp),
                color = Theme.ink
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Theme.ink.copy(alpha = 0.08f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = L10n.closeButton,
                    tint = Theme.accent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Orijinal Metin & Ses
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                if (showFurigana) {
                    FuriganaText(
                        text = sentence.japaneseText,
                        style = Theme.heading(24.sp),
                        color = Theme.ink
                    )
                } else {
                    Text(
                        text = sentence.japaneseText,
                        style = Theme.heading(24.sp),
                        color = Theme.ink,
                        lineHeight = 32.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Theme.accent.copy(alpha = 0.14f))
                    .clickable(onClick = onSpeak),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = L10n.listenPronunciation,
                    tint = Theme.accent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        HorizontalDivider(color = Theme.secondaryInk.copy(alpha = 0.3f))

        // Romaji & Türkçe Çeviri
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Romaji",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.secondaryInk
            )
            Text(
                text = sentence.romaji,
                fontSize = 16.sp,
                color = Theme.ink
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = L10n.translationLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.secondaryInk
            )
            Text(
                text = sentence.turkishTranslation,
                fontSize = 16.sp,
                color = Theme.ink
            )
        }

        HorizontalDivider(color = Theme.secondaryInk.copy(alpha = 0.3f))

        // Anahtar Kelimeler
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = L10n.keyVocabulary,
                style = Theme.heading(18.sp),
                color = Theme.ink
            )

            if (sentence.vocabulary.isEmpty()) {
                Text(
                    text = L10n.noNewVocab,
                    fontSize = 15.sp,
                    color = Theme.secondaryInk
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sentence.vocabulary.forEach { vocab ->
                        StoryVocabChip(vocab = vocab)
                    }
                }
            }
        }
    }
}
