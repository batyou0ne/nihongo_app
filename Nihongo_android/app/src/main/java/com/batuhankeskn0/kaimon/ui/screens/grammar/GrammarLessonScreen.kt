package com.batuhankeskn0.kaimon.ui.screens.grammar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.model.GrammarCategory
import com.batuhankeskn0.kaimon.data.model.GrammarExample
import com.batuhankeskn0.kaimon.data.model.GrammarPoint
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.ui.components.FuriganaText
import com.batuhankeskn0.kaimon.ui.theme.PrimaryButton
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered

/**
 * Tek bir gramer konusunun ders anlatım ekranı (`Views/Grammar/GrammarLessonView.swift` birebir karşılığı).
 *
 * Sırasıyla:
 * 1. Başlık (`pattern`, `romaji`, `explanation`)
 * 2. Formül kutusu (`formula`)
 * 3. 4 satırlı örnek cümleler (`japanese`, `hiragana`, `romaji`, `turkish`) ve sesli okuma butonu
 * 4. Fiil kategorisindeyse Fiil Çekim Tablosu (`VerbConjugationScreen`) kısayol kartı
 * 5. Oturum modundaysa `"Anladım, Devam Et →"`, tekli moddaysa `"Pratiğe geç →"` butonu.
 */
@Composable
fun GrammarLessonScreen(
    point: GrammarPoint,
    level: String = "N5",
    unitIndex: Int? = null,
    isSessionMode: Boolean = false,
    onNext: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showConjugationTable by remember { mutableStateOf(false) }
    var isPracticingSingle by remember { mutableStateOf(false) }

    BackHandler(enabled = showConjugationTable || isPracticingSingle || onBack != null) {
        when {
            showConjugationTable -> showConjugationTable = false
            isPracticingSingle -> isPracticingSingle = false
            else -> onBack?.invoke()
        }
    }

    if (showConjugationTable) {
        VerbConjugationScreen(
            hiragana = "たべる",
            kanji = "食べる",
            turkishMeaning = L10n.grammarSampleVerbMeaning,
            allowVerbSelection = true,
            onDismiss = { showConjugationTable = false },
            modifier = modifier
        )
        return
    }

    if (isPracticingSingle) {
        GrammarPracticeScreen(
            points = listOf(point),
            title = point.title,
            level = level,
            unitIndex = unitIndex,
            onFinish = {
                isPracticingSingle = false
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
        // Üst Navigasyon Barı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onBack != null) {
                Row(
                    modifier = Modifier
                        .clickable {
                            FeedbackManager.shared.playSelection()
                            onBack()
                        }
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
            }

            Text(
                text = point.title,
                style = Theme.heading(19.sp),
                color = Theme.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Başlık ve Açıklama
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = point.pattern,
                    style = Theme.display(34.sp),
                    color = Theme.accent
                )
                Text(
                    text = point.romaji,
                    fontSize = 15.sp,
                    color = Theme.secondaryInk
                )
                Text(
                    text = point.explanation,
                    fontSize = 16.sp,
                    color = Theme.ink,
                    lineHeight = 23.sp
                )
            }

            // 2. Formül Kutusu
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .inkBordered()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = L10n.formulaLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Theme.secondaryInk
                )
                Text(
                    text = point.formula,
                    style = Theme.heading(18.sp),
                    color = Theme.ink,
                    lineHeight = 24.sp
                )
            }

            // 3. Örnekler (4 satırlı: japanese, hiragana, romaji, turkish + sesli okunuş)
            if (point.examples.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = L10n.grammarExamplesTitle,
                        style = Theme.display(20.sp),
                        color = Theme.ink
                    )

                    point.examples.forEach { example ->
                        GrammarExampleCard(
                            example = example,
                            onSpeak = {
                                FeedbackManager.shared.playSelection()
                                AudioService.shared.speak(example.hiragana ?: example.japanese)
                            }
                        )
                    }
                }
            }

            // 4. Fiil Kategorisi İse Çekim Tablosu Kısayolu
            if (point.category == GrammarCategory.VERB) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .inkBordered()
                        .clickable {
                            FeedbackManager.shared.playSelection()
                            showConjugationTable = true
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.TableChart,
                        contentDescription = null,
                        tint = Theme.accent,
                        modifier = Modifier.size(22.dp)
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = L10n.grammarVerbConjugationCardTitle,
                            style = Theme.heading(16.sp),
                            color = Theme.ink
                        )
                        Text(
                            text = L10n.grammarVerbConjugationCardSubtitle,
                            fontSize = 12.sp,
                            color = Theme.secondaryInk
                        )
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallMade,
                        contentDescription = null,
                        tint = Theme.secondaryInk,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 5. Devam / Pratik Butonu
            if (isSessionMode) {
                PrimaryButton(
                    text = L10n.grammarUnderstoodNextButton,
                    onClick = {
                        FeedbackManager.shared.playSelection()
                        onNext?.invoke()
                    }
                )
            } else {
                PrimaryButton(
                    text = L10n.grammarStartPracticeButton,
                    onClick = {
                        FeedbackManager.shared.playSelection()
                        isPracticingSingle = true
                    }
                )
            }
        }
    }
}

@Composable
fun GrammarExampleCard(
    example: GrammarExample,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .inkBordered()
            .clickable(onClick = onSpeak)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Satır 1: Japonca (FuriganaText) + Ses Butonu
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FuriganaText(
                text = example.japanese,
                style = Theme.heading(20.sp),
                color = Theme.ink,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp)
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Theme.accent.copy(alpha = 0.14f))
                    .clickable(onClick = onSpeak),
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

        // Satır 2: Hiragana okunuşu (varsa)
        if (!example.hiragana.isNullOrBlank()) {
            Text(
                text = example.hiragana,
                fontSize = 13.sp,
                color = Theme.secondaryInk
            )
        }

        // Satır 3: Romaji
        Text(
            text = example.romaji,
            fontSize = 14.sp,
            color = Theme.secondaryInk
        )

        // Satır 4: Türkçe anlamı
        Text(
            text = example.turkish,
            fontSize = 15.sp,
            color = Theme.ink,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
