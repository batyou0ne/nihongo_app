package com.batuhankeskn0.kaimon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered

/**
 * Bölüm başlığı: küçük, harf aralıklı (`1.2.sp`), ikincil renkte (`Theme.secondaryInk`).
 * iOS `SectionLabel` karşılığıdır.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp,
        color = Theme.secondaryInk,
        modifier = modifier
    )
}

/**
 * Yuvarlak köşeli (`5.dp`), siyah kenarlıklı (`2.dp`) ilerleme çubuğu — dolu kısım vermilyon (`Theme.accent`).
 * iOS `ProgressBar` karşılığıdır.
 */
@Composable
fun ProgressBar(
    fraction: Double,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp
) {
    val clamped = fraction.coerceIn(0.0, 1.0).toFloat()
    val shape = RoundedCornerShape(5.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(color = Theme.paper, shape = shape)
            .clip(shape)
            .border(width = 2.dp, color = Theme.ink, shape = shape),
        contentAlignment = Alignment.CenterStart
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = clamped)
                    .background(color = Theme.accent, shape = shape)
            )
        }
    }
}

@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp
) {
    ProgressBar(
        fraction = fraction.toDouble(),
        modifier = modifier,
        height = height
    )
}

/**
 * Öğrenme modülü kartı: solda büyük vermilyon Japonca karakter, sağda ok ikonu,
 * ortada modül adı ve alt başlık, alt satırda öğrenilen oranı ve ilerleme çubuğu.
 * iOS `ModuleCard` karşılığıdır.
 */
@Composable
fun ModuleCard(
    kind: LearnableItemKind,
    subtitle: String,
    learned: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val total = kind.totalCount
    val fraction = if (total > 0) learned.toDouble() / total.toDouble() else 0.0

    val cardModifier = modifier
        .aspectRatio(1f)
        .inkBordered()
        .then(
            if (onClick != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        )
        .padding(14.dp)

    Column(
        modifier = cardModifier,
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = kind.symbol,
                style = Theme.display(34.sp),
                color = Theme.accent
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CallMade,
                contentDescription = null,
                tint = Theme.ink,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = kind.displayName,
                style = Theme.heading(17.sp),
                color = Theme.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Theme.secondaryInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "$learned/$total",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.secondaryInk
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        ProgressBar(fraction = fraction)
    }
}

/**
 * Tekrar Çalış / İlerleme / Sohbet Pratiği kartı.
 * `isHighlighted` ise vermilyon (`Theme.accent`) zeminle öne çıkar.
 * iOS `HomeView.UtilityCard` karşılığıdır.
 */
@Composable
fun UtilityCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(16.dp)
    val bgColor = if (isHighlighted) Theme.accent else Theme.paper
    val primaryColor = if (isHighlighted) Theme.paper else Theme.ink
    val secondaryColor = if (isHighlighted) Theme.paper.copy(alpha = 0.85f) else Theme.secondaryInk
    val iconColor = if (isHighlighted) Theme.paper else Theme.accent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = bgColor, shape = shape)
            .clip(shape)
            .border(width = 2.dp, color = Theme.ink, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.width(30.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = Theme.heading(17.sp),
                color = primaryColor
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = secondaryColor
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = primaryColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * 7 günlük çalışma serisi şeridi (soldan sağa 6 gün önce -> bugün).
 * iOS `HomeView.streakStrip` karşılığıdır.
 */
@Composable
fun StreakStrip(
    streak: Int,
    studiedDaysAgo: (Int) -> Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .inkBordered()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            tint = if (streak > 0) Theme.accent else Theme.secondaryInk,
            modifier = Modifier.size(22.dp)
        )

        Text(
            text = if (streak > 0) L10n.streakText(streak) else L10n.noStreak,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (streak > 0) Theme.ink else Theme.secondaryInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (daysAgo in 6 downTo 0) {
                val studied = studiedDaysAgo(daysAgo)
                val boxShape = RoundedCornerShape(4.dp)
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .background(
                            color = if (studied) Theme.accent else Theme.paper,
                            shape = boxShape
                        )
                        .border(
                            width = 1.5.dp,
                            color = if (studied) Theme.accent else Theme.secondaryInk,
                            shape = boxShape
                        )
                )
            }
        }
    }
}

/**
 * Gramer, Kelime ve Kanji ana ekranlarında kullanılan JLPT seviye seçim satırı (N5–N1).
 * Kilitli seviyeler `%60` opaklıkla ve sağda kilit ikonuyla gösterilir.
 */
@Composable
fun LevelSelectionRow(
    level: JLPTLevel,
    title: String,
    unlockedSubtitle: String,
    isUnlocked: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (isUnlocked) 1f else 0.6f)
            .inkBordered()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isUnlocked) Theme.accent else Theme.secondaryInk,
                modifier = Modifier.size(26.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = Theme.heading(19.sp),
                color = if (isUnlocked) Theme.ink else Theme.secondaryInk
            )
            Text(
                text = if (isUnlocked) unlockedSubtitle else L10n.lockedLevelSubtitle(level.subtitle),
                fontSize = 14.sp,
                color = Theme.secondaryInk
            )
        }

        Icon(
            imageVector = if (isUnlocked) Icons.AutoMirrored.Filled.ArrowForward else Icons.Filled.Lock,
            contentDescription = null,
            tint = if (isUnlocked) Theme.ink else Theme.secondaryInk,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * Okuma ekranı gibi modüllerde yatay kaydırılabilir JLPT seviye seçim hapları (N5–N1).
 * iOS `ReadingMainView` seviye seçim barının karşılığıdır.
 */
@Composable
fun LevelSelectorBar(
    selectedLevel: JLPTLevel,
    isLevelUnlocked: (JLPTLevel) -> Boolean,
    onLevelSelected: (JLPTLevel) -> Unit,
    onLockedLevelClicked: (JLPTLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JLPTLevel.allCases.forEach { level ->
            val unlocked = isLevelUnlocked(level)
            val isSelected = selectedLevel == level
            val bgColor = if (isSelected) Theme.accent else Theme.paper
            val fgColor = when {
                isSelected -> Theme.paper
                unlocked -> Theme.ink
                else -> Theme.secondaryInk
            }

            Row(
                modifier = Modifier
                    .alpha(if (unlocked) 1f else 0.6f)
                    .clip(CircleShape)
                    .background(color = bgColor, shape = CircleShape)
                    .then(
                        if (!isSelected) {
                            Modifier.border(width = 1.5.dp, color = Theme.ink, shape = CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable {
                        if (unlocked) {
                            onLevelSelected(level)
                        } else {
                            onLockedLevelClicked(level)
                        }
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = level.rawValue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = fgColor
                )
                if (!unlocked) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = fgColor,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Pro'ya özel içerikte kilit ikonunun yerine gösterilen küçük rozet.
 * iOS `ProBadge` karşılığıdır.
 */
@Composable
fun ProBadge(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(color = Theme.accent, shape = CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.WorkspacePremium,
            contentDescription = null,
            tint = ColorWhiteConstant,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = "PRO",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ColorWhiteConstant
        )
    }
}

/**
 * Günlük AI mesaj hakkı göstergesi ve Pro yükseltme daveti.
 * iOS `AIQuotaBanner` karşılığıdır.
 */
@Composable
fun AIQuotaBanner(
    remaining: Int,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = Theme.paper)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (remaining > 0) Icons.Filled.ChatBubble else Icons.Filled.Lock,
            contentDescription = null,
            tint = Theme.accent,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = if (remaining > 0) L10n.aiQuotaRemaining(remaining) else L10n.aiQuotaExhausted,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Theme.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(modifier = Modifier.clickable(onClick = onUpgrade)) {
            ProBadge()
        }
    }
}

private val ColorWhiteConstant = androidx.compose.ui.graphics.Color.White
