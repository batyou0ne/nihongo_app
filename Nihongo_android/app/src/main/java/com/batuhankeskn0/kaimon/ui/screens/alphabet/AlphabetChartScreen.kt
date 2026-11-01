package com.batuhankeskn0.kaimon.ui.screens.alphabet

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.model.Kanji
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.AlphabetAudioService
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Tek bir alfabe karakterinin model yapısı (`AlphabetChartView.swift` -> `AlphabetChartItem`).
 */
data class AlphabetChartItem(
    val character: String,
    val romaji: String,
    val pronunciation: String
) {
    val id: String
        get() = character
}

/**
 * Sütunlu satır yapısı (`AlphabetChartView.swift` -> `AlphabetChartRow`).
 */
data class AlphabetChartRow(
    val header: String,
    val cells: List<AlphabetChartItem?>
) {
    val id: String
        get() = header
}

/**
 * Desteklenen Japonca yazı sistemleri (`AlphabetChartView.swift` -> `WritingSystemType`).
 */
enum class WritingSystemType(val rawValue: String) {
    HIRAGANA("hiragana"),
    KATAKANA("katakana"),
    KANJI("kanji");

    val title: String
        get() = when (this) {
            HIRAGANA -> L10n.writingSystemHiragana
            KATAKANA -> L10n.writingSystemKatakana
            KANJI -> L10n.writingSystemKanji
        }

    companion object {
        val allCases: List<WritingSystemType>
            get() = entries
    }
}

/**
 * Alfabe alt kategorileri: Temel (46 Harf), Tenten & Maru (Dakuten/Handakuten) ve Birleşik Heceler (Yoon).
 */
enum class AlphabetCategory(val rawValue: String) {
    BASIC("basic"),
    DAKUTEN("dakuten"),
    YOON("yoon");

    val title: String
        get() = when (this) {
            BASIC -> L10n.basicAlphabetTab
            DAKUTEN -> L10n.dakutenAlphabetTab
            YOON -> L10n.yoonAlphabetTab
        }

    val columnHeaders: List<String>
        get() = when (this) {
            BASIC, DAKUTEN -> listOf("A", "I", "U", "E", "O")
            YOON -> listOf("YA", "YU", "YO")
        }

    companion object {
        val allCases: List<AlphabetCategory>
            get() = entries
    }
}

/**
 * Hiragana, Katakana ve Kanji için etkileşimli, sesli alfabe ve karakter tablosu görünümü.
 * Her karaktere dokunulduğunda `AlphabetAudioService.shared.play(character)` aracılığıyla
 * sesli telaffuz çalar ve aktif hücreyi vermilyon çerçeve + büyüme animasyonuyla vurgular.
 *
 * iOS `Views/AlphabetChartView.swift` ekranının birebir Jetpack Compose karşılığıdır.
 */
@Composable
fun AlphabetChartScreen(
    modifier: Modifier = Modifier,
    alphabetAudioService: AlphabetAudioService = AlphabetAudioService.shared
) {
    var selectedSystem by rememberSaveable { mutableStateOf(WritingSystemType.HIRAGANA) }
    var selectedCategory by rememberSaveable { mutableStateOf(AlphabetCategory.BASIC) }
    var selectedKanjiPart by rememberSaveable { mutableIntStateOf(0) } // 0: Tümü (80), 1..4: Bölüm 1..4
    var lastTappedItem by remember { mutableStateOf<AlphabetChartItem?>(null) }
    var lastTappedKanji by remember { mutableStateOf<Kanji?>(null) }

    val allKanji by produceState<List<Kanji>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) {
            ContentStore.loadKanji(level = "N5")
        }
    }

    val kanjiParts = remember(allKanji) {
        if (allKanji.isEmpty()) emptyList() else ContentStore.kanjiParts(level = "N5", partCount = 4)
    }

    val filteredKanji = remember(allKanji, kanjiParts, selectedKanjiPart) {
        AlphabetChartData.filterKanji(
            allKanji = allKanji,
            selectedKanjiPart = selectedKanjiPart,
            parts = kanjiParts
        )
    }

    val currentRows = remember(selectedSystem, selectedCategory) {
        AlphabetChartData.rowsFor(system = selectedSystem, category = selectedCategory)
    }

    val currentPlayingChar = alphabetAudioService.currentCharacter

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // MARK: - Üst Seçici Kontrolleri
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hiragana / Katakana / Kanji Değiştirici
            WritingSystemSegmentedControl(
                selectedSystem = selectedSystem,
                onSelectSystem = { system ->
                    FeedbackManager.shared.playSelection()
                    selectedSystem = system
                    lastTappedItem = null
                    lastTappedKanji = null
                }
            )

            // Alt Kategori / Filtre
            if (selectedSystem == WritingSystemType.KANJI) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val totalCount = if (allKanji.isNotEmpty()) allKanji.size else 80
                    for (partIndex in 0..4) {
                        val label = if (partIndex == 0) {
                            L10n.kanjiChartAllFilter(totalCount)
                        } else {
                            L10n.kanjiChartPartFilter(partIndex)
                        }
                        val isSelected = selectedKanjiPart == partIndex
                        FilterChipPill(
                            label = label,
                            isSelected = isSelected,
                            onClick = {
                                FeedbackManager.shared.playSelection()
                                selectedKanjiPart = partIndex
                            }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AlphabetCategory.allCases.forEach { category ->
                        FilterChipPill(
                            label = category.title,
                            isSelected = selectedCategory == category,
                            onClick = {
                                FeedbackManager.shared.playSelection()
                                selectedCategory = category
                            }
                        )
                    }
                }
            }
        }

        // MARK: - Son Dokunulan Karakter Bilgi Çubuğu
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            if (selectedSystem == WritingSystemType.KANJI) {
                val tappedKanji = lastTappedKanji
                if (tappedKanji != null) {
                    LastTappedKanjiBanner(
                        kanji = tappedKanji,
                        onPlayAudio = {
                            alphabetAudioService.play(tappedKanji.character)
                        }
                    )
                } else {
                    ChartSubtitleHint(text = L10n.kanjiChartSubtitle)
                }
            } else {
                val tappedItem = lastTappedItem
                if (tappedItem != null) {
                    LastTappedCharacterBanner(
                        item = tappedItem,
                        onPlayAudio = {
                            alphabetAudioService.play(tappedItem.character)
                        }
                    )
                } else {
                    ChartSubtitleHint(text = L10n.alphabetChartSubtitle)
                }
            }
        }

        // MARK: - İçerik Izgarası (Kanji veya Hiragana/Katakana)
        if (selectedSystem == WritingSystemType.KANJI) {
            val kanjiRows = remember(filteredKanji) { filteredKanji.chunked(4) }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                kanjiRows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 0 until 4) {
                            val kanji = rowItems.getOrNull(i)
                            if (kanji != null) {
                                KanjiCellView(
                                    kanji = kanji,
                                    isCurrentlyPlaying = currentPlayingChar == kanji.character,
                                    onTap = {
                                        lastTappedKanji = kanji
                                        lastTappedItem = null
                                        alphabetAudioService.play(kanji.character)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        } else {
            val headers = selectedCategory.columnHeaders
            // Sütun Başlıkları (A, I, U, E, O veya YA, YU, YO)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(26.dp))
                headers.forEach { header ->
                    Text(
                        text = header,
                        style = Theme.heading(13.sp),
                        color = Theme.secondaryInk,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Alfabe Tablosu Izgarası
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                currentRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = row.header,
                            style = Theme.heading(12.sp),
                            color = Theme.secondaryInk,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(26.dp)
                        )

                        row.cells.forEach { item ->
                            if (item != null) {
                                AlphabetCellView(
                                    item = item,
                                    isCurrentlyPlaying = currentPlayingChar == item.character,
                                    onTap = {
                                        lastTappedItem = item
                                        lastTappedKanji = null
                                        alphabetAudioService.play(item.character)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                EmptyAlphabetCellView(
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WritingSystemSegmentedControl(
    selectedSystem: WritingSystemType,
    onSelectSystem: (WritingSystemType) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = Theme.ink.copy(alpha = 0.06f), shape = shape)
            .border(width = 1.5.dp, color = Theme.ink.copy(alpha = 0.2f), shape = shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        WritingSystemType.allCases.forEach { system ->
            val isSelected = selectedSystem == system
            val pillShape = RoundedCornerShape(9.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(pillShape)
                    .background(
                        color = if (isSelected) Theme.paper else androidx.compose.ui.graphics.Color.Transparent,
                        shape = pillShape
                    )
                    .then(
                        if (isSelected) {
                            Modifier.border(width = 1.5.dp, color = Theme.ink, shape = pillShape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSelectSystem(system) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = system.title,
                    fontSize = 13.sp,
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
private fun FilterChipPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                color = if (isSelected) Theme.accent else Theme.paper,
                shape = CircleShape
            )
            .then(
                if (!isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = Theme.ink.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = Theme.heading(13.sp),
            color = if (isSelected) Theme.paper else Theme.ink
        )
    }
}

@Composable
private fun ChartSubtitleHint(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.TouchApp,
            contentDescription = null,
            tint = Theme.accent,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = Theme.secondaryInk
        )
    }
}

@Composable
private fun LastTappedCharacterBanner(
    item: AlphabetChartItem,
    onPlayAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = Theme.paper, shape = shape)
            .border(width = 1.5.dp, color = Theme.accent.copy(alpha = 0.35f), shape = shape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = item.character,
            style = Theme.display(28.sp),
            color = Theme.accent
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.romaji,
                style = Theme.heading(15.sp),
                color = Theme.ink
            )
            Text(
                text = item.pronunciation,
                fontSize = 12.sp,
                color = Theme.secondaryInk
            )
        }

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(color = Theme.accent, shape = CircleShape)
                .clickable(onClick = onPlayAudio)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = Theme.paper,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = L10n.listenButton,
                style = Theme.heading(13.sp),
                color = Theme.paper
            )
        }
    }
}

@Composable
private fun LastTappedKanjiBanner(
    kanji: Kanji,
    onPlayAudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = Theme.paper, shape = shape)
            .border(width = 1.5.dp, color = Theme.accent.copy(alpha = 0.35f), shape = shape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = kanji.character,
            style = Theme.display(30.sp),
            color = Theme.accent
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = kanji.meaning,
                style = Theme.heading(15.sp),
                color = Theme.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (kanji.kunyomi.isNotEmpty()) {
                    Text(
                        text = "Kun: ${kanji.kunyomi}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Theme.secondaryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (kanji.onyomi.isNotEmpty()) {
                    Text(
                        text = "On: ${kanji.onyomi}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Theme.secondaryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(color = Theme.accent, shape = CircleShape)
                .clickable(onClick = onPlayAudio)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = Theme.paper,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = L10n.listenButton,
                style = Theme.heading(13.sp),
                color = Theme.paper
            )
        }
    }
}

/**
 * Tek bir karakter kutucuğu (`AlphabetChartView.swift` -> `AlphabetCellView`).
 * Aktif çalınırken vermilyon çerçeve ve `1.06f` büyüme animasyonuyla vurgulanır.
 */
@Composable
fun AlphabetCellView(
    item: AlphabetChartItem,
    isCurrentlyPlaying: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrentlyPlaying) 1.06f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "AlphabetCellScale"
    )
    val shape = RoundedCornerShape(12.dp)
    val bgColor = if (isCurrentlyPlaying) Theme.accent.copy(alpha = 0.12f) else Theme.paper
    val borderColor = if (isCurrentlyPlaying) Theme.accent else Theme.ink.copy(alpha = 0.12f)
    val borderWidth = if (isCurrentlyPlaying) 2.dp else 1.dp

    Column(
        modifier = modifier
            .height(66.dp)
            .scale(scale)
            .background(color = bgColor, shape = shape)
            .clip(shape)
            .border(width = borderWidth, color = borderColor, shape = shape)
            .clickable(onClick = onTap)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = item.character,
            style = Theme.display(22.sp),
            color = if (isCurrentlyPlaying) Theme.accent else Theme.ink
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = item.romaji,
            style = Theme.heading(11.sp),
            color = if (isCurrentlyPlaying) Theme.accent else Theme.secondaryInk
        )
    }
}

/**
 * Boşluk hücresi (ör. Y ve W satırlarındaki boş sütunlar).
 */
@Composable
private fun EmptyAlphabetCellView(
    modifier: Modifier = Modifier
) {
    val strokeColor = Theme.ink.copy(alpha = 0.08f)
    val bgColor = Theme.paper.copy(alpha = 0.3f)

    Canvas(
        modifier = modifier.height(66.dp)
    ) {
        val cornerPx = 12.dp.toPx()
        drawRoundRect(
            color = bgColor,
            cornerRadius = CornerRadius(cornerPx, cornerPx)
        )
        drawRoundRect(
            color = strokeColor,
            cornerRadius = CornerRadius(cornerPx, cornerPx),
            style = Stroke(
                width = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        )
    }
}

/**
 * Görsel Kanji tablosu hücresi (`AlphabetChartView.swift` -> `KanjiCellView`).
 */
@Composable
fun KanjiCellView(
    kanji: Kanji,
    isCurrentlyPlaying: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isCurrentlyPlaying) 1.06f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "KanjiCellScale"
    )
    val shape = RoundedCornerShape(12.dp)
    val bgColor = if (isCurrentlyPlaying) Theme.accent.copy(alpha = 0.12f) else Theme.paper
    val borderColor = if (isCurrentlyPlaying) Theme.accent else Theme.ink.copy(alpha = 0.12f)
    val borderWidth = if (isCurrentlyPlaying) 2.dp else 1.dp

    Column(
        modifier = modifier
            .height(74.dp)
            .scale(scale)
            .background(color = bgColor, shape = shape)
            .clip(shape)
            .border(width = borderWidth, color = borderColor, shape = shape)
            .clickable(onClick = onTap)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = kanji.character,
            style = Theme.display(24.sp),
            color = if (isCurrentlyPlaying) Theme.accent else Theme.ink
        )
        Text(
            text = kanji.meaning,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (isCurrentlyPlaying) Theme.accent else Theme.secondaryInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = kanji.kunyomi.ifEmpty { kanji.onyomi },
            fontSize = 9.sp,
            color = Theme.secondaryInk.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Hiragana & Katakana (Temel 46, Dakuten/Handakuten 25, Birleşik Heceler / Yoon 33) satır verileri
 * ve Kanji bölüm filtreleme yardımcısı.
 */
object AlphabetChartData {

    fun filterKanji(
        allKanji: List<Kanji>,
        selectedKanjiPart: Int,
        parts: List<List<Kanji>>
    ): List<Kanji> {
        if (selectedKanjiPart == 0) return allKanji
        val index = selectedKanjiPart - 1
        if (index in parts.indices) {
            return parts[index]
        }
        return allKanji
    }

    fun rowsFor(
        system: WritingSystemType,
        category: AlphabetCategory
    ): List<AlphabetChartRow> = when (system) {
        WritingSystemType.HIRAGANA -> when (category) {
            AlphabetCategory.BASIC -> hiraganaBasicRows
            AlphabetCategory.DAKUTEN -> hiraganaDakutenRows
            AlphabetCategory.YOON -> hiraganaYoonRows
        }
        WritingSystemType.KATAKANA -> when (category) {
            AlphabetCategory.BASIC -> katakanaBasicRows
            AlphabetCategory.DAKUTEN -> katakanaDakutenRows
            AlphabetCategory.YOON -> katakanaYoonRows
        }
        WritingSystemType.KANJI -> emptyList()
    }

    // MARK: - Hiragana Temel (46)
    val hiraganaBasicRows: List<AlphabetChartRow> = listOf(
        AlphabetChartRow(
            header = "A",
            cells = listOf(
                AlphabetChartItem("あ", "a", "a"),
                AlphabetChartItem("い", "i", "i"),
                AlphabetChartItem("う", "u", "u"),
                AlphabetChartItem("え", "e", "e"),
                AlphabetChartItem("お", "o", "o")
            )
        ),
        AlphabetChartRow(
            header = "K",
            cells = listOf(
                AlphabetChartItem("か", "ka", "ka"),
                AlphabetChartItem("き", "ki", "ki"),
                AlphabetChartItem("く", "ku", "ku"),
                AlphabetChartItem("け", "ke", "ke"),
                AlphabetChartItem("こ", "ko", "ko")
            )
        ),
        AlphabetChartRow(
            header = "S",
            cells = listOf(
                AlphabetChartItem("さ", "sa", "sa"),
                AlphabetChartItem("し", "shi", "şi"),
                AlphabetChartItem("す", "su", "su"),
                AlphabetChartItem("せ", "se", "se"),
                AlphabetChartItem("そ", "so", "so")
            )
        ),
        AlphabetChartRow(
            header = "T",
            cells = listOf(
                AlphabetChartItem("た", "ta", "ta"),
                AlphabetChartItem("ち", "chi", "çi"),
                AlphabetChartItem("つ", "tsu", "tsu"),
                AlphabetChartItem("て", "te", "te"),
                AlphabetChartItem("と", "to", "to")
            )
        ),
        AlphabetChartRow(
            header = "N",
            cells = listOf(
                AlphabetChartItem("な", "na", "na"),
                AlphabetChartItem("に", "ni", "ni"),
                AlphabetChartItem("ぬ", "nu", "nu"),
                AlphabetChartItem("ね", "ne", "ne"),
                AlphabetChartItem("no", "no", "no").copy(character = "の")
            )
        ),
        AlphabetChartRow(
            header = "H",
            cells = listOf(
                AlphabetChartItem("は", "ha", "ha"),
                AlphabetChartItem("ひ", "hi", "hi"),
                AlphabetChartItem("ふ", "fu", "fu"),
                AlphabetChartItem("へ", "he", "he"),
                AlphabetChartItem("ほ", "ho", "ho")
            )
        ),
        AlphabetChartRow(
            header = "M",
            cells = listOf(
                AlphabetChartItem("ま", "ma", "ma"),
                AlphabetChartItem("み", "mi", "mi"),
                AlphabetChartItem("む", "mu", "mu"),
                AlphabetChartItem("め", "me", "me"),
                AlphabetChartItem("も", "mo", "mo")
            )
        ),
        AlphabetChartRow(
            header = "Y",
            cells = listOf(
                AlphabetChartItem("や", "ya", "ya"),
                null,
                AlphabetChartItem("ゆ", "yu", "yu"),
                null,
                AlphabetChartItem("よ", "yo", "yo")
            )
        ),
        AlphabetChartRow(
            header = "R",
            cells = listOf(
                AlphabetChartItem("ら", "ra", "ra"),
                AlphabetChartItem("り", "ri", "ri"),
                AlphabetChartItem("る", "ru", "ru"),
                AlphabetChartItem("れ", "re", "re"),
                AlphabetChartItem("ろ", "ro", "ro")
            )
        ),
        AlphabetChartRow(
            header = "W",
            cells = listOf(
                AlphabetChartItem("わ", "wa", "va"),
                null,
                null,
                null,
                AlphabetChartItem("を", "wo", "o")
            )
        ),
        AlphabetChartRow(
            header = "N*",
            cells = listOf(
                AlphabetChartItem("ん", "n", "n"),
                null,
                null,
                null,
                null
            )
        )
    )

    // MARK: - Hiragana Dakuten & Handakuten (25)
    val hiraganaDakutenRows: List<AlphabetChartRow> = listOf(
        AlphabetChartRow(
            header = "G",
            cells = listOf(
                AlphabetChartItem("が", "ga", "ga"),
                AlphabetChartItem("ぎ", "gi", "gi"),
                AlphabetChartItem("ぐ", "gu", "gu"),
                AlphabetChartItem("げ", "ge", "ge"),
                AlphabetChartItem("ご", "go", "go")
            )
        ),
        AlphabetChartRow(
            header = "Z",
            cells = listOf(
                AlphabetChartItem("ざ", "za", "za"),
                AlphabetChartItem("じ", "ji", "ci"),
                AlphabetChartItem("ず", "zu", "zu"),
                AlphabetChartItem("ぜ", "ze", "ze"),
                AlphabetChartItem("ぞ", "zo", "zo")
            )
        ),
        AlphabetChartRow(
            header = "D",
            cells = listOf(
                AlphabetChartItem("だ", "da", "da"),
                AlphabetChartItem("ぢ", "ji", "ci"),
                AlphabetChartItem("づ", "zu", "zu"),
                AlphabetChartItem("で", "de", "de"),
                AlphabetChartItem("ど", "do", "do")
            )
        ),
        AlphabetChartRow(
            header = "B",
            cells = listOf(
                AlphabetChartItem("ば", "ba", "ba"),
                AlphabetChartItem("び", "bi", "bi"),
                AlphabetChartItem("ぶ", "bu", "bu"),
                AlphabetChartItem("べ", "be", "be"),
                AlphabetChartItem("ぼ", "bo", "bo")
            )
        ),
        AlphabetChartRow(
            header = "P",
            cells = listOf(
                AlphabetChartItem("ぱ", "pa", "pa"),
                AlphabetChartItem("ぴ", "pi", "pi"),
                AlphabetChartItem("ぷ", "pu", "pu"),
                AlphabetChartItem("ぺ", "pe", "pe"),
                AlphabetChartItem("ぽ", "po", "po")
            )
        )
    )

    // MARK: - Hiragana Birleşik Heceler / Yoon (33)
    val hiraganaYoonRows: List<AlphabetChartRow> = listOf(
        AlphabetChartRow(
            header = "KY",
            cells = listOf(
                AlphabetChartItem("きゃ", "kya", "kya"),
                AlphabetChartItem("きゅ", "kyu", "kyu"),
                AlphabetChartItem("きょ", "kyo", "kyo")
            )
        ),
        AlphabetChartRow(
            header = "SH",
            cells = listOf(
                AlphabetChartItem("しゃ", "sha", "şa"),
                AlphabetChartItem("しゅ", "shu", "şu"),
                AlphabetChartItem("しょ", "sho", "şo")
            )
        ),
        AlphabetChartRow(
            header = "CH",
            cells = listOf(
                AlphabetChartItem("ちゃ", "cha", "ça"),
                AlphabetChartItem("ちゅ", "chu", "çu"),
                AlphabetChartItem("ちょ", "cho", "ço")
            )
        ),
        AlphabetChartRow(
            header = "NY",
            cells = listOf(
                AlphabetChartItem("にゃ", "nya", "nya"),
                AlphabetChartItem("にゅ", "nyu", "nyu"),
                AlphabetChartItem("にょ", "nyo", "nyo")
            )
        ),
        AlphabetChartRow(
            header = "HY",
            cells = listOf(
                AlphabetChartItem("ひゃ", "hya", "hya"),
                AlphabetChartItem("ひゅ", "hyu", "hyu"),
                AlphabetChartItem("ひょ", "hyo", "hyo")
            )
        ),
        AlphabetChartRow(
            header = "MY",
            cells = listOf(
                AlphabetChartItem("みゃ", "mya", "mya"),
                AlphabetChartItem("みゅ", "myu", "myu"),
                AlphabetChartItem("みょ", "myo", "myo")
            )
        ),
        AlphabetChartRow(
            header = "RY",
            cells = listOf(
                AlphabetChartItem("りゃ", "rya", "rya"),
                AlphabetChartItem("りゅ", "ryu", "ryu"),
                AlphabetChartItem("りょ", "ryo", "ryo")
            )
        ),
        AlphabetChartRow(
            header = "GY",
            cells = listOf(
                AlphabetChartItem("ぎゃ", "gya", "gya"),
                AlphabetChartItem("ぎゅ", "gyu", "gyu"),
                AlphabetChartItem("ぎょ", "gyo", "gyo")
            )
        ),
        AlphabetChartRow(
            header = "J",
            cells = listOf(
                AlphabetChartItem("じゃ", "ja", "ca"),
                AlphabetChartItem("じゅ", "ju", "cu"),
                AlphabetChartItem("じょ", "jo", "co")
            )
        ),
        AlphabetChartRow(
            header = "BY",
            cells = listOf(
                AlphabetChartItem("びゃ", "bya", "bya"),
                AlphabetChartItem("びゅ", "byu", "byu"),
                AlphabetChartItem("びょ", "byo", "byo")
            )
        ),
        AlphabetChartRow(
            header = "PY",
            cells = listOf(
                AlphabetChartItem("ぴゃ", "pya", "pya"),
                AlphabetChartItem("ぴゅ", "pyu", "pyu"),
                AlphabetChartItem("ぴょ", "pyo", "pyo")
            )
        )
    )

    // MARK: - Katakana Temel (46)
    val katakanaBasicRows: List<AlphabetChartRow> = listOf(
        AlphabetChartRow(
            header = "A",
            cells = listOf(
                AlphabetChartItem("ア", "a", "a"),
                AlphabetChartItem("イ", "i", "i"),
                AlphabetChartItem("ウ", "u", "u"),
                AlphabetChartItem("エ", "e", "e"),
                AlphabetChartItem("オ", "o", "o")
            )
        ),
        AlphabetChartRow(
            header = "K",
            cells = listOf(
                AlphabetChartItem("カ", "ka", "ka"),
                AlphabetChartItem("キ", "ki", "ki"),
                AlphabetChartItem("ク", "ku", "ku"),
                AlphabetChartItem("ケ", "ke", "ke"),
                AlphabetChartItem("コ", "ko", "ko")
            )
        ),
        AlphabetChartRow(
            header = "S",
            cells = listOf(
                AlphabetChartItem("サ", "sa", "sa"),
                AlphabetChartItem("シ", "shi", "şi"),
                AlphabetChartItem("ス", "su", "su"),
                AlphabetChartItem("セ", "se", "se"),
                AlphabetChartItem("ソ", "so", "so")
            )
        ),
        AlphabetChartRow(
            header = "T",
            cells = listOf(
                AlphabetChartItem("タ", "ta", "ta"),
                AlphabetChartItem("チ", "chi", "çi"),
                AlphabetChartItem("ツ", "tsu", "tsu"),
                AlphabetChartItem("テ", "te", "te"),
                AlphabetChartItem("ト", "to", "to")
            )
        ),
        AlphabetChartRow(
            header = "N",
            cells = listOf(
                AlphabetChartItem("ナ", "na", "na"),
                AlphabetChartItem("ニ", "ni", "ni"),
                AlphabetChartItem("ヌ", "nu", "nu"),
                AlphabetChartItem("ネ", "ne", "ne"),
                AlphabetChartItem("ノ", "no", "no")
            )
        ),
        AlphabetChartRow(
            header = "H",
            cells = listOf(
                AlphabetChartItem("ハ", "ha", "ha"),
                AlphabetChartItem("ヒ", "hi", "hi"),
                AlphabetChartItem("フ", "fu", "fu"),
                AlphabetChartItem("ヘ", "he", "he"),
                AlphabetChartItem("ホ", "ho", "ho")
            )
        ),
        AlphabetChartRow(
            header = "M",
            cells = listOf(
                AlphabetChartItem("マ", "ma", "ma"),
                AlphabetChartItem("ミ", "mi", "mi"),
                AlphabetChartItem("ム", "mu", "mu"),
                AlphabetChartItem("メ", "me", "me"),
                AlphabetChartItem("モ", "mo", "mo")
            )
        ),
        AlphabetChartRow(
            header = "Y",
            cells = listOf(
                AlphabetChartItem("ヤ", "ya", "ya"),
                null,
                AlphabetChartItem("ユ", "yu", "yu"),
                null,
                AlphabetChartItem("ヨ", "yo", "yo")
            )
        ),
        AlphabetChartRow(
            header = "R",
            cells = listOf(
                AlphabetChartItem("ラ", "ra", "ra"),
                AlphabetChartItem("リ", "ri", "ri"),
                AlphabetChartItem("ル", "ru", "ru"),
                AlphabetChartItem("レ", "re", "re"),
                AlphabetChartItem("ロ", "ro", "ro")
            )
        ),
        AlphabetChartRow(
            header = "W",
            cells = listOf(
                AlphabetChartItem("ワ", "wa", "va"),
                null,
                null,
                null,
                AlphabetChartItem("ヲ", "wo", "o")
            )
        ),
        AlphabetChartRow(
            header = "N*",
            cells = listOf(
                AlphabetChartItem("ン", "n", "n"),
                null,
                null,
                null,
                null
            )
        )
    )

    // MARK: - Katakana Dakuten & Handakuten (25)
    val katakanaDakutenRows: List<AlphabetChartRow> = listOf(
        AlphabetChartRow(
            header = "G",
            cells = listOf(
                AlphabetChartItem("ガ", "ga", "ga"),
                AlphabetChartItem("ギ", "gi", "gi"),
                AlphabetChartItem("グ", "gu", "gu"),
                AlphabetChartItem("ゲ", "ge", "ge"),
                AlphabetChartItem("ゴ", "go", "go")
            )
        ),
        AlphabetChartRow(
            header = "Z",
            cells = listOf(
                AlphabetChartItem("ザ", "za", "za"),
                AlphabetChartItem("ジ", "ji", "ci"),
                AlphabetChartItem("ズ", "zu", "zu"),
                AlphabetChartItem("ゼ", "ze", "ze"),
                AlphabetChartItem("ゾ", "zo", "zo")
            )
        ),
        AlphabetChartRow(
            header = "D",
            cells = listOf(
                AlphabetChartItem("ダ", "da", "da"),
                AlphabetChartItem("ヂ", "ji", "ci"),
                AlphabetChartItem("ヅ", "zu", "zu"),
                AlphabetChartItem("デ", "de", "de"),
                AlphabetChartItem("ド", "do", "do")
            )
        ),
        AlphabetChartRow(
            header = "B",
            cells = listOf(
                AlphabetChartItem("バ", "ba", "ba"),
                AlphabetChartItem("ビ", "bi", "bi"),
                AlphabetChartItem("ブ", "bu", "bu"),
                AlphabetChartItem("ベ", "be", "be"),
                AlphabetChartItem("ボ", "bo", "bo")
            )
        ),
        AlphabetChartRow(
            header = "P",
            cells = listOf(
                AlphabetChartItem("パ", "pa", "pa"),
                AlphabetChartItem("ピ", "pi", "pi"),
                AlphabetChartItem("プ", "pu", "pu"),
                AlphabetChartItem("ペ", "pe", "pe"),
                AlphabetChartItem("ポ", "po", "po")
            )
        )
    )

    // MARK: - Katakana Birleşik Heceler / Yoon (33)
    val katakanaYoonRows: List<AlphabetChartRow> = listOf(
        AlphabetChartRow(
            header = "KY",
            cells = listOf(
                AlphabetChartItem("キャ", "kya", "kya"),
                AlphabetChartItem("キュ", "kyu", "kyu"),
                AlphabetChartItem("キョ", "kyo", "kyo")
            )
        ),
        AlphabetChartRow(
            header = "SH",
            cells = listOf(
                AlphabetChartItem("シャ", "sha", "şa"),
                AlphabetChartItem("シュ", "shu", "şu"),
                AlphabetChartItem("ショ", "sho", "şo")
            )
        ),
        AlphabetChartRow(
            header = "CH",
            cells = listOf(
                AlphabetChartItem("チャ", "cha", "ça"),
                AlphabetChartItem("チュ", "chu", "çu"),
                AlphabetChartItem("チョ", "cho", "ço")
            )
        ),
        AlphabetChartRow(
            header = "NY",
            cells = listOf(
                AlphabetChartItem("ニャ", "nya", "nya"),
                AlphabetChartItem("ニュ", "nyu", "nyu"),
                AlphabetChartItem("ニョ", "nyo", "nyo")
            )
        ),
        AlphabetChartRow(
            header = "HY",
            cells = listOf(
                AlphabetChartItem("ヒャ", "hya", "hya"),
                AlphabetChartItem("ヒュ", "hyu", "hyu"),
                AlphabetChartItem("ヒョ", "hyo", "hyo")
            )
        ),
        AlphabetChartRow(
            header = "MY",
            cells = listOf(
                AlphabetChartItem("ミャ", "mya", "mya"),
                AlphabetChartItem("ミュ", "myu", "myu"),
                AlphabetChartItem("ミョ", "myo", "myo")
            )
        ),
        AlphabetChartRow(
            header = "RY",
            cells = listOf(
                AlphabetChartItem("リャ", "rya", "rya"),
                AlphabetChartItem("リュ", "ryu", "ryu"),
                AlphabetChartItem("リョ", "ryo", "ryo")
            )
        ),
        AlphabetChartRow(
            header = "GY",
            cells = listOf(
                AlphabetChartItem("ギャ", "gya", "gya"),
                AlphabetChartItem("ギュ", "gyu", "gyu"),
                AlphabetChartItem("ギョ", "gyo", "gyo")
            )
        ),
        AlphabetChartRow(
            header = "J",
            cells = listOf(
                AlphabetChartItem("ジャ", "ja", "ca"),
                AlphabetChartItem("ジュ", "ju", "cu"),
                AlphabetChartItem("ジョ", "jo", "co")
            )
        ),
        AlphabetChartRow(
            header = "BY",
            cells = listOf(
                AlphabetChartItem("ビャ", "bya", "bya"),
                AlphabetChartItem("ビュ", "byu", "byu"),
                AlphabetChartItem("ビョ", "byo", "byo")
            )
        ),
        AlphabetChartRow(
            header = "PY",
            cells = listOf(
                AlphabetChartItem("ピャ", "pya", "pya"),
                AlphabetChartItem("ピュ", "pyu", "pyu"),
                AlphabetChartItem("ピョ", "pyo", "pyo")
            )
        )
    )
}
