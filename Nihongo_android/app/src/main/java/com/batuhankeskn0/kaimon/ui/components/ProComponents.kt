package com.batuhankeskn0.kaimon.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.ui.theme.Theme

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
            tint = Color.White,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = "PRO",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
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

/**
 * Paywall avantaj satırı: vermilyon ikon + metin (iOS `PaywallView.benefitRow`).
 */
@Composable
fun ProBenefitRow(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Theme.accent,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Theme.ink
        )
    }
}

/**
 * Paywall plan seçim kartı (iOS `PaywallView.planCard`): seçili planda vermilyon `3.dp` çerçeve ve
 * `%8` vermilyon zemin; seçili değilse `1.5.dp` soluk mürekkep çerçeve. Yıllık planda "EN AVANTAJLI" rozeti gösterilir.
 */
@Composable
fun ProPlanCard(
    title: String,
    subtitle: String,
    price: String,
    priceSuffix: String,
    isSelected: Boolean,
    showBestValueBadge: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val border = if (isSelected) {
        BorderStroke(3.dp, Theme.accent)
    } else {
        BorderStroke(1.5.dp, Theme.ink.copy(alpha = 0.35f))
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) Theme.accent.copy(alpha = 0.08f) else Theme.paper,
                shape = shape
            )
            .clip(shape)
            .border(border = border, shape = shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) Theme.accent else Theme.secondaryInk,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = title, style = Theme.heading(18), color = Theme.ink)
                if (showBestValueBadge) {
                    Text(
                        text = L10n.planBestValue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier
                            .background(color = Theme.accent, shape = CircleShape)
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
            Text(text = subtitle, fontSize = 13.sp, color = Theme.secondaryInk)
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = price, style = Theme.heading(18), color = Theme.ink)
            if (priceSuffix.isNotEmpty()) {
                Text(text = priceSuffix, fontSize = 12.sp, color = Theme.secondaryInk)
            } else {
                Box(modifier = Modifier.width(1.dp))
            }
        }
    }
}
