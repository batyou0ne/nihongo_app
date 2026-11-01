package com.batuhankeskn0.kaimon.ui.screens.progress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlin.math.max

/**
 * İlerleme Ekranı (`ProgressOverviewView` / `Views/Progress/ProgressView.swift` karşılığı).
 *
 * İçerdiği Bölümler:
 * 1. Aktif & En Uzun Seri (`streakSection`) + Toplam XP ve Toplam Öğrenilen Öğe özet kartları
 * 2. Seviye & XP İlerleme Çubuğu (`xpBarSection`)
 * 3. Son 7 Günün Harcanan Dakika ve Kazanılan XP Grafikleri (Jetpack Compose `Canvas` Brutalist Bar Chart)
 * 4. SRS Kademelerine (Çırak, Kalfa, Usta, Aydınlanmış, Kalıcı) Göre Dağılım
 * 5. JLPT Seviye Durumu (`N5`–`N1`)
 * 6. Modül Bazlı Öğrenilen Öğeler Listesi (`Hiragana`, `Katakana`, `Kanji`, `Kelimeler`, `Gramer`)
 */
@Composable
fun ProgressScreen(
    modifier: Modifier = Modifier,
    viewModel: ProgressViewModel = viewModel(),
    onBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onScreenAppear()
    }

    ProgressScreenContent(
        uiState = uiState,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun ProgressScreenContent(
    uiState: ProgressUiState,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
    ) {
        // Üst Navigasyon Barı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .inkBordered(lineWidth = 1.5.dp, cornerRadius = 12.dp)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = L10n.backButton,
                        tint = Theme.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = L10n.progressNavTitle,
                style = Theme.display(32.sp),
                color = Theme.ink
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // 1. Seri & Genel Özet İstatistikleri
            StreakAndSummarySection(uiState = uiState)

            // 2. XP ve Seviye Çubuğu
            XpBarSection(uiState = uiState)

            // 3. Son 7 Günlük Çalışma Süresi (Dakika) Grafiği
            StudyTimeChartSection(uiState = uiState)

            // 4. Son 7 Günlük Kazanılan XP Grafiği
            XpChartSection(uiState = uiState)

            // 5. SRS Kademelerine Göre Dağılım (Çırak, Kalfa, Usta, Aydınlanmış, Kalıcı)
            SrsDistributionSection(uiState = uiState)

            // 6. JLPT Seviye Durumu (N5 - N1)
            JlptLevelSection(uiState = uiState)

            // 7. Modül Bazlı Öğrenilen Öğeler Listesi
            LearnedModulesSection(uiState = uiState)
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = Theme.display(24.sp),
        color = Theme.ink
    )
}

@Composable
private fun StreakAndSummarySection(uiState: ProgressUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle(title = L10n.streakSection)

        // iOS ProgressView.streakSection birebir karşılığı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .inkBordered()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = Theme.accent,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = L10n.streakText(uiState.activeStreak),
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Theme.ink,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = L10n.longestStreak(uiState.longestStreak),
                fontSize = 14.sp,
                color = Theme.secondaryInk
            )
        }

        // Toplam XP ve Toplam Öğrenilen Öğe Sayısı Kartları
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryStatCard(
                label = L10n.totalXpLabel,
                value = "${uiState.totalXP} XP",
                modifier = Modifier.weight(1f)
            )
            SummaryStatCard(
                label = L10n.totalLearnedLabel,
                value = "${uiState.totalLearnedItemsCount}",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .inkBordered()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Theme.secondaryInk
        )
        Text(
            text = value,
            style = Theme.display(22.sp),
            color = Theme.accent
        )
    }
}

@Composable
private fun XpBarSection(uiState: ProgressUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .inkBordered()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = L10n.levelLabel(uiState.userLevel),
                style = Theme.display(28.sp),
                color = Theme.ink
            )
            Text(
                text = "${uiState.currentLevelXP} / ${uiState.nextLevelXP} XP",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.secondaryInk
            )
        }

        SharpProgressBar(fraction = uiState.xpFraction)
    }
}

@Composable
private fun StudyTimeChartSection(uiState: ProgressUiState) {
    val totalMinutes = uiState.totalMinutesThisWeek
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    val timeStr = L10n.timeFormatted(hours = hours, minutes = mins)

    val bars = remember(uiState.last7Days) {
        uiState.last7Days.map { it.dayLabel to it.minutes }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionTitle(title = L10n.studyTime)
            Text(
                text = timeStr,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.accent
            )
        }

        BrutalistBarChart(
            data = bars,
            unitSuffix = ""
        )
    }
}

@Composable
private fun XpChartSection(uiState: ProgressUiState) {
    val bars = remember(uiState.last7Days) {
        uiState.last7Days.map { it.dayLabel to it.xpEarned }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionTitle(title = L10n.chartXpEarned)
            Text(
                text = "+${uiState.totalXpThisWeek} XP",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.accent
            )
        }

        BrutalistBarChart(
            data = bars,
            unitSuffix = ""
        )
    }
}

/**
 * Jetpack Compose `Canvas` ile çizilen Brutalist Sütun Grafiği.
 * Son 7 günün verisini vermilyon (`Theme.accent`) dolgu ve keskin `Theme.ink` çerçevelerle görselleştirir.
 */
@Composable
fun BrutalistBarChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    unitSuffix: String = ""
) {
    val accentColor = Theme.accent
    val inkColor = Theme.ink
    val secondaryInkColor = Theme.secondaryInk
    val maxVal = remember(data) { max(1, data.maxOfOrNull { it.second } ?: 1) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .inkBordered()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Üst değer etiketleri
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { (_, value) ->
                Text(
                    text = if (value > 0) "$value$unitSuffix" else "0",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (value > 0) inkColor else secondaryInkColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Canvas Bar Chart Alanı
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val count = data.size.coerceAtLeast(1)
            val slotWidth = size.width / count
            val barWidth = (slotWidth * 0.56f).coerceAtLeast(12.dp.toPx())
            val maxBarHeight = (size.height - 6.dp.toPx()).coerceAtLeast(1f)
            val strokePx = 2.dp.toPx()

            // Yatay kesikli kılavuz çizgileri (%25, %50, %75, %100)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            listOf(0.25f, 0.5f, 0.75f, 1.0f).forEach { level ->
                val y = size.height - (maxBarHeight * level)
                drawLine(
                    color = secondaryInkColor.copy(alpha = 0.22f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = dashEffect
                )
            }

            // Sütunlar
            data.forEachIndexed { index, (_, value) ->
                val ratio = (value.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                val rawHeight = maxBarHeight * ratio
                val barHeight = if (value > 0) rawHeight.coerceAtLeast(6.dp.toPx()) else 3.dp.toPx()
                val left = index * slotWidth + (slotWidth - barWidth) / 2f
                val top = size.height - barHeight

                if (value > 0) {
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    drawRoundRect(
                        color = inkColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = Stroke(width = strokePx)
                    )
                } else {
                    drawRoundRect(
                        color = secondaryInkColor.copy(alpha = 0.35f),
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }

            // Taban çizgisi
            drawLine(
                color = inkColor,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = strokePx
            )
        }

        // Alt gün etiketleri (Pzt, Sal, ...)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { (dayLabel, _) ->
                Text(
                    text = dayLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = secondaryInkColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SrsDistributionSection(uiState: ProgressUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle(title = L10n.srsDistributionSection)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .inkBordered()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.srsDistribution.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (item.count > 0) Theme.accent.copy(alpha = 0.14f) else Theme.ink.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = item.stage.localizedTitle,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.count > 0) Theme.accent else Theme.secondaryInk
                                )
                            }
                        }

                        Text(
                            text = "${item.count}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Theme.ink
                        )
                    }

                    SharpProgressBar(
                        fraction = item.fraction,
                        height = 10.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun JlptLevelSection(uiState: ProgressUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle(title = L10n.jlptLevelStatusSection)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            uiState.jlptLevels.forEach { item ->
                val isUnlocked = item.isUnlocked
                val overallPercent = (item.breakdown.overall * 100.0).toInt()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isUnlocked) 1f else 0.6f)
                        .inkBordered()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = item.level.rawValue,
                        style = Theme.heading(18.sp),
                        color = if (isUnlocked) Theme.accent else Theme.secondaryInk,
                        modifier = Modifier.width(36.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.level.subtitle,
                            fontSize = 14.sp,
                            color = if (isUnlocked) Theme.ink else Theme.secondaryInk
                        )
                        if (isUnlocked) {
                            SharpProgressBar(
                                fraction = item.breakdown.overall,
                                height = 8.dp
                            )
                        }
                    }

                    if (isUnlocked) {
                        Text(
                            text = "%$overallPercent",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Theme.ink
                        )
                    } else {
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
    }
}

@Composable
private fun LearnedModulesSection(uiState: ProgressUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle(title = L10n.learnedSection)

        uiState.moduleRows.forEach { row ->
            val title = when (row.kind) {
                LearnableItemKind.HIRAGANA_CHARACTER -> "Hiragana"
                LearnableItemKind.KATAKANA_CHARACTER -> "Katakana"
                LearnableItemKind.KANJI -> "${row.kind.displayName} (N5)"
                LearnableItemKind.VOCABULARY_WORD -> "${row.kind.displayName} (N5)"
                LearnableItemKind.GRAMMAR -> "${row.kind.displayName} (N5)"
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .inkBordered()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = Theme.heading(17.sp),
                        color = Theme.ink
                    )
                    Text(
                        text = "${row.learned}/${row.total}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Theme.secondaryInk
                    )
                }

                SharpProgressBar(fraction = row.fraction)

                if (row.mastered > 0) {
                    Text(
                        text = L10n.permanentlyLearned(row.mastered),
                        fontSize = 12.sp,
                        color = Theme.secondaryInk
                    )
                }
            }
        }
    }
}

/**
 * Keskin köşeli (`RectangleShape`), siyah kenarlıklı (`2.dp`) ilerleme çubuğu — dolu kısım vermilyon (`Theme.accent`).
 * iOS `ProgressOverviewView.progressBar(fraction:)` karşılığıdır.
 */
@Composable
fun SharpProgressBar(
    fraction: Double,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp
) {
    val clamped = fraction.coerceIn(0.0, 1.0).toFloat()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(color = Theme.paper, shape = RectangleShape)
            .border(width = 2.dp, color = Theme.ink, shape = RectangleShape),
        contentAlignment = Alignment.CenterStart
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = clamped)
                    .background(color = Theme.accent, shape = RectangleShape)
            )
        }
    }
}
