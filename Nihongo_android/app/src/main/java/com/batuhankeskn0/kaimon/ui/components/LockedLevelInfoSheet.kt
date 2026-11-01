package com.batuhankeskn0.kaimon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressBreakdown
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Kilitli bir JLPT seviyesine (`N4`–`N1`) tıklandığında açılan bilgilendirme ve yönlendirme `ModalBottomSheet` sayfası.
 * Bir önceki seviyenin (`previousLevel`) tamamlama yüzdesini gösterir ve kullanıcıya
 * "Eksik Dersleri Tamamla" veya "Seviyeyi Sınavla Atla (Test-Out)" seçeneklerini sunar.
 * iOS `LockedLevelInfoSheet.swift` karşılığıdır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockedLevelInfoSheet(
    level: JLPTLevel,
    progressBreakdown: LevelProgressBreakdown? = null,
    onDismiss: () -> Unit,
    onStartTestOut: (JLPTLevel) -> Unit = {}
) {
    val previousLevel = level.previousLevel ?: JLPTLevel.N5
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val loadedBreakdown by produceState(
        initialValue = progressBreakdown ?: LevelProgressBreakdown(0.0, 0.0, 0.0, 0.0),
        key1 = previousLevel,
        key2 = progressBreakdown
    ) {
        if (progressBreakdown != null) {
            value = progressBreakdown
        } else {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    LevelProgressionService.shared.calculateLevelProgress(previousLevel)
                }.getOrDefault(LevelProgressBreakdown(0.0, 0.0, 0.0, 0.0))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Theme.paper,
        contentColor = Theme.ink
    ) {
        LockedLevelInfoContent(
            level = level,
            previousLevel = previousLevel,
            progressData = loadedBreakdown,
            onDismiss = onDismiss,
            onStartTestOut = {
                onStartTestOut(previousLevel)
            }
        )
    }
}

/**
 * `LockedLevelInfoSheet` içeriği (sunum/stateless katmanı).
 */
@Composable
fun LockedLevelInfoContent(
    level: JLPTLevel,
    previousLevel: JLPTLevel = level.previousLevel ?: JLPTLevel.N5,
    progressData: LevelProgressBreakdown,
    onDismiss: () -> Unit,
    onStartTestOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme.paper)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Üst Kapat Butonu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = L10n.closeButton,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Theme.ink,
                modifier = Modifier
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            )
        }

        // Kilit İkonu ve Başlık
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = Theme.accent.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Theme.accent,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = L10n.levelLockedTitle(level.rawValue),
                style = Theme.display(28.sp),
                color = Theme.ink,
                textAlign = TextAlign.Center
            )

            Text(
                text = L10n.levelLockedDescription(previousLevel.rawValue),
                fontSize = 15.sp,
                color = Theme.secondaryInk,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Ön Seviye İlerleme Kartı
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .inkBordered()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val overallPercent = (progressData.overall * 100).toInt()
            val isReadyForGateway = progressData.overall >= 0.85

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = L10n.levelProgressStatus(previousLevel.rawValue),
                    style = Theme.heading(17.sp),
                    color = Theme.ink
                )
                Text(
                    text = "%$overallPercent / %85",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isReadyForGateway) Theme.success else Theme.accent
                )
            }

            ProgressBar(fraction = progressData.overall)

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = Theme.secondaryInk.copy(alpha = 0.25f)
            )

            ProgressMetricRow(
                title = L10n.kanjiPartsMetric,
                fraction = progressData.kanji
            )
            ProgressMetricRow(
                title = L10n.vocabPartsMetric,
                fraction = progressData.vocab
            )
            ProgressMetricRow(
                title = L10n.grammarUnitsMetric,
                fraction = progressData.grammar
            )
        }

        // Aksiyon Butonları
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val buttonShape = RoundedCornerShape(14.dp)

            // Seviyeyi Sınavla Atla (Test-Out)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(buttonShape)
                    .background(color = Theme.accent, shape = buttonShape)
                    .clickable(onClick = onStartTestOut),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = Theme.paper,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = L10n.testOutButton,
                    style = Theme.heading(16.sp),
                    color = Theme.paper
                )
            }

            // Eksik Dersleri Tamamla
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(buttonShape)
                    .background(color = Theme.paper, shape = buttonShape)
                    .border(width = 2.dp, color = Theme.ink, shape = buttonShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = L10n.completeMissingLessonsButton,
                    style = Theme.heading(16.sp),
                    color = Theme.ink
                )
            }
        }
    }
}

@Composable
private fun ProgressMetricRow(
    title: String,
    fraction: Double
) {
    val percent = (fraction * 100).toInt()
    val isMet = fraction >= 0.85

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            color = Theme.ink,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "%$percent",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Theme.secondaryInk
        )
        Icon(
            imageVector = if (isMet) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (isMet) Theme.success else Theme.secondaryInk,
            modifier = Modifier.size(18.dp)
        )
    }
}
