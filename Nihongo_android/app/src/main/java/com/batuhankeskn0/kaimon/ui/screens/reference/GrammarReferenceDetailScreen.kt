package com.batuhankeskn0.kaimon.ui.screens.reference

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.model.GrammarPoint
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.ui.components.FuriganaText
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered

/**
 * Edat, Sıfat, Bağlaç veya İfade kategorisindeki bir gramer noktasının tam başvuru ekranı.
 * Formül, açıklama ve 4 satırlı örnek cümleleri sesli okunuşuyla gösterir (`Views/Reference/GrammarReferenceDetailView.swift` karşılığı).
 */
@Composable
fun GrammarReferenceDetailScreen(
    point: GrammarPoint,
    accentColor: Color = Theme.accent,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (onDismiss != null) {
        BackHandler { onDismiss() }
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
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = L10n.grammarReferenceTitle,
                style = Theme.heading(20.sp),
                color = Theme.ink
            )

            if (onDismiss != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Theme.ink.copy(alpha = 0.08f))
                        .clickable {
                            FeedbackManager.shared.playSelection()
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = L10n.closeButton,
                        tint = Theme.secondaryInk,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Başlık ve Zorluk Rozeti
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = point.pattern,
                            style = Theme.display(34.sp),
                            color = accentColor
                        )
                        Text(
                            text = point.romaji,
                            fontSize = 15.sp,
                            color = Theme.secondaryInk
                        )
                    }

                    // Zorluk Rozeti (1..3)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = L10n.difficultyLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Theme.secondaryInk
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (i in 1..3) {
                                Box(
                                    modifier = Modifier
                                        .width(18.dp)
                                        .height(7.dp)
                                        .background(
                                            color = if (i <= point.difficulty) accentColor else accentColor.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                )
                            }
                        }
                    }
                }

                Text(
                    text = point.title,
                    style = Theme.heading(20.sp),
                    color = Theme.ink
                )

                Text(
                    text = point.explanation,
                    fontSize = 15.sp,
                    color = Theme.ink,
                    lineHeight = 22.sp
                )
            }

            // 2. Formül Kutusu
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .inkBordered()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Functions,
                        contentDescription = null,
                        tint = Theme.secondaryInk,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = L10n.formulaLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Theme.secondaryInk
                    )
                }

                Text(
                    text = point.formula,
                    style = Theme.heading(17.sp),
                    color = Theme.ink,
                    lineHeight = 23.sp
                )
            }

            // 3. Örnek Cümleler (4 satır: japanese, hiragana, romaji, turkish + sesli okunuş)
            if (point.examples.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FormatQuote,
                            contentDescription = null,
                            tint = Theme.ink,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = L10n.exampleSentencesLabel,
                            style = Theme.display(18.sp),
                            color = Theme.ink
                        )
                    }

                    point.examples.forEach { example ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .inkBordered()
                                .clickable {
                                    FeedbackManager.shared.playSelection()
                                    AudioService.shared.speak(example.hiragana ?: example.japanese)
                                }
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                FuriganaText(
                                    text = example.japanese,
                                    style = Theme.heading(18.sp),
                                    color = Theme.ink,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 8.dp)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.14f))
                                        .clickable {
                                            FeedbackManager.shared.playSelection()
                                            AudioService.shared.speak(example.hiragana ?: example.japanese)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = L10n.listenPronunciation,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            if (!example.hiragana.isNullOrBlank()) {
                                Text(
                                    text = example.hiragana,
                                    fontSize = 12.sp,
                                    color = Theme.secondaryInk
                                )
                            }

                            Text(
                                text = example.romaji,
                                fontSize = 12.sp,
                                color = Theme.secondaryInk
                            )

                            Text(
                                text = example.turkish,
                                fontSize = 14.sp,
                                color = Theme.ink,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // 4. Ders İpucu
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                HorizontalDivider(color = Theme.secondaryInk.copy(alpha = 0.25f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = L10n.grammarLessonHint(point.title),
                        fontSize = 12.sp,
                        color = Theme.secondaryInk,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
