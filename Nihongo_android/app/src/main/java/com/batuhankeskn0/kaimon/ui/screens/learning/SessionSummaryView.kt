package com.batuhankeskn0.kaimon.ui.screens.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.ui.theme.PrimaryButton
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered

/**
 * Özet ekranındaki tek bir "yanlış yapılan öğe" satırının verisi.
 * iOS `SessionSummaryItem` yapısının birebir karşılığıdır.
 */
data class SessionSummaryItem(
    val id: String,
    val prompt: String,
    val detail: String,
    val wrongCount: Int
)

/**
 * Bir modül oturumu (tüm tekrar turları dahil) bittiğinde gösterilen özet ekranı.
 * Hiragana, Katakana, Kanji ve Kelimeler — hepsi `FlashcardSessionScreen` üzerinden
 * bu ortak bileşeni kullanır.
 * iOS `Views/Learning/SessionSummaryView.swift` bileşeninin birebir karşılığıdır.
 */
@Composable
fun SessionSummaryView(
    totalAnswers: Int,
    wrongItems: List<SessionSummaryItem>,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalWrong = wrongItems.sumOf { it.wrongCount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Başlık
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = null,
                    tint = Theme.accent,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = L10n.sessionSummary,
                    style = Theme.display(32.sp),
                    color = Theme.ink
                )
            }

            // İstatistik Kutuları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SummaryStatBox(
                    value = totalAnswers,
                    label = L10n.cardsSeenLabel,
                    modifier = Modifier.weight(1f)
                )
                SummaryStatBox(
                    value = totalWrong,
                    label = L10n.wrongAnswersLabel,
                    modifier = Modifier.weight(1f)
                )
            }

            // Yanlış Yapılanlar Listesi veya Mükemmel Skor Mesajı
            if (wrongItems.isEmpty()) {
                Text(
                    text = L10n.perfectScore,
                    style = Theme.heading(17.sp),
                    color = Theme.ink
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = L10n.wrongItemsTitle,
                        style = Theme.heading(19.sp),
                        color = Theme.ink
                    )

                    wrongItems.forEach { item ->
                        WrongItemSummaryRow(item = item)
                    }

                    Text(
                        text = L10n.addedToReview,
                        fontSize = 13.sp,
                        color = Theme.secondaryInk,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Bitir Butonu
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            PrimaryButton(
                text = L10n.finishButton,
                onClick = onFinish
            )
        }
    }
}

@Composable
private fun SummaryStatBox(
    value: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .inkBordered()
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$value",
            style = Theme.display(32.sp),
            color = Theme.accent
        )
        Text(
            text = label,
            fontSize = 14.sp,
            color = Theme.secondaryInk
        )
    }
}

@Composable
private fun WrongItemSummaryRow(
    item: SessionSummaryItem,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .inkBordered()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = item.prompt,
            style = Theme.heading(26.sp),
            color = Theme.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.detail,
                fontSize = 14.sp,
                color = Theme.secondaryInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.size(4.dp))

        Text(
            text = "${item.wrongCount}×",
            style = Theme.heading(17.sp),
            color = Theme.accent
        )
    }
}
