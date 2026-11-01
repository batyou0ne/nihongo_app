package com.batuhankeskn0.kaimon.ui.screens.reference

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.model.GrammarCategory
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.VerbType
import com.batuhankeskn0.kaimon.ui.screens.dictionary.DictionaryViewModel
import com.batuhankeskn0.kaimon.ui.screens.dictionary.VerbEntry
import com.batuhankeskn0.kaimon.ui.screens.grammar.VerbConjugationScreen
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fiil kategorisi için özel kütüphane liste ekranı (`Views/Reference/VerbCategoryListView.swift` karşılığı).
 * N5 kelime hazinesinden fiilleri çeker; üstte arama çubuğu, altında grup göstergesi ve tüm fiil listesi bulunur.
 * Herhangi bir fiile tıklandığında `VerbConjugationScreen` açılır.
 */
@Composable
fun VerbCategoryListScreen(
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    var selectedVerb by remember { mutableStateOf<VerbEntry?>(null) }

    val verbs by produceState<List<VerbEntry>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.Default) {
            val words = ContentStore.loadVocabulary("N5")
            DictionaryViewModel.extractVerbs(words)
        }
    }

    val filtered = remember(verbs, searchText) {
        DictionaryViewModel.filterVerbs(verbs, searchText)
    }

    BackHandler(enabled = selectedVerb != null || onBack != null) {
        if (selectedVerb != null) {
            selectedVerb = null
        } else {
            onBack?.invoke()
        }
    }

    val currentVerb = selectedVerb
    if (currentVerb != null) {
        VerbConjugationScreen(
            hiragana = currentVerb.hiragana,
            kanji = currentVerb.kanji,
            turkishMeaning = currentVerb.turkishMeaning,
            allowVerbSelection = false,
            onDismiss = { selectedVerb = null },
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
                text = GrammarCategory.VERB.displayName,
                style = Theme.heading(20.sp),
                color = Theme.ink
            )
        }

        // Özel Brutalist Arama Çubuğu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .inkBordered(lineWidth = 1.5.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = Theme.secondaryInk,
                modifier = Modifier.size(20.dp)
            )

            Box(modifier = Modifier.weight(1f)) {
                if (searchText.isEmpty()) {
                    Text(
                        text = L10n.verbSearchPlaceholder,
                        fontSize = 16.sp,
                        color = Theme.secondaryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp, color = Theme.ink),
                    cursorBrush = SolidColor(Theme.accent),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (searchText.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = L10n.closeButton,
                    tint = Theme.secondaryInk,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .clickable { searchText = "" }
                )
            }
        }

        if (filtered.isEmpty() && searchText.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 60.dp, start = 20.dp, end = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = Theme.secondaryInk,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = L10n.noSearchResults(searchText),
                    style = Theme.heading(16.sp),
                    color = Theme.secondaryInk,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Grup Açıklama Şeridi (Type Legend)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            LegendChip(label = "Gr.1 Godan")
                            LegendChip(label = "Gr.2 Ichidan")
                            LegendChip(label = "する/くる")
                        }
                        Text(
                            text = L10n.verbCountLabel(filtered.size),
                            fontSize = 12.sp,
                            color = Theme.secondaryInk
                        )
                    }
                }

                items(filtered, key = { it.id }) { verb ->
                    VerbCategoryRow(
                        verb = verb,
                        onClick = {
                            FeedbackManager.shared.playSelection()
                            AudioService.shared.speak(verb.hiragana)
                            selectedVerb = verb
                        },
                        onSpeak = {
                            FeedbackManager.shared.playSelection()
                            AudioService.shared.speak(verb.hiragana)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendChip(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(color = Theme.accent, shape = CircleShape)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Theme.ink
        )
    }
}

@Composable
private fun VerbCategoryRow(
    verb: VerbEntry,
    onClick: () -> Unit,
    onSpeak: () -> Unit
) {
    val tagLabel = when (verb.verbType) {
        VerbType.ICHIDAN -> "Gr.2"
        VerbType.SURU -> "する"
        VerbType.KURU -> "くる"
        VerbType.ARU -> "ある"
        else -> "Gr.1"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .inkBordered()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Japonca (Kanji + Hiragana)
        Column(
            modifier = Modifier.width(92.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (!verb.kanji.isNullOrEmpty()) {
                Text(
                    text = verb.kanji,
                    style = Theme.heading(20.sp),
                    color = Theme.ink
                )
                Text(
                    text = verb.hiragana,
                    fontSize = 12.sp,
                    color = Theme.secondaryInk
                )
            } else {
                Text(
                    text = verb.hiragana,
                    style = Theme.heading(20.sp),
                    color = Theme.ink
                )
            }
        }

        // Anlam + Romaji
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = verb.turkishMeaning,
                fontSize = 14.sp,
                color = Theme.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = verb.romaji,
                fontSize = 12.sp,
                color = Theme.secondaryInk
            )
        }

        // Ses + Grup Rozeti + Tablo İkonu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Theme.accent.copy(alpha = 0.12f))
                    .clickable(onClick = onSpeak),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = L10n.listenPronunciation,
                    tint = Theme.accent,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = Theme.accent.copy(alpha = 0.13f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = tagLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Theme.accent
                    )
                }
                Icon(
                    imageVector = Icons.Filled.TableChart,
                    contentDescription = null,
                    tint = Theme.secondaryInk,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
