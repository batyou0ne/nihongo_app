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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.model.GrammarCategory
import com.batuhankeskn0.kaimon.data.model.GrammarPoint
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.ui.screens.dictionary.DictionaryViewModel
import com.batuhankeskn0.kaimon.ui.screens.grammar.VerbConjugationScreen
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Seçilen gramer kategorisindeki tüm kuralları kilitsiz olarak listeler (`Views/Reference/GrammarCategoryListView.swift` karşılığı).
 * Üstte arama çubuğu bulunur; öğeye tıklandığında kategoriye göre `VerbConjugationScreen` veya `GrammarReferenceDetailScreen` açılır.
 */
@Composable
fun GrammarCategoryListScreen(
    category: GrammarCategory,
    accentColor: Color = Theme.accent,
    level: String = "N5",
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var searchText by remember { mutableStateOf("") }
    var selectedPoint by remember { mutableStateOf<GrammarPoint?>(null) }

    val allPoints by produceState<List<GrammarPoint>>(
        initialValue = emptyList(),
        key1 = category,
        key2 = level
    ) {
        value = withContext(Dispatchers.Default) {
            ContentStore.loadGrammar(level = level)
                .filter { it.category == category }
                .sortedBy { it.difficulty }
        }
    }

    val filtered = remember(allPoints, searchText) {
        DictionaryViewModel.filterGrammar(
            points = allPoints,
            query = searchText,
            category = category
        )
    }

    BackHandler(enabled = selectedPoint != null || onBack != null) {
        if (selectedPoint != null) {
            selectedPoint = null
        } else {
            onBack?.invoke()
        }
    }

    val currentPoint = selectedPoint
    if (currentPoint != null) {
        if (category == GrammarCategory.VERB) {
            VerbConjugationScreen(
                hiragana = "たべる",
                kanji = "食べる",
                turkishMeaning = L10n.grammarSampleVerbMeaning,
                allowVerbSelection = true,
                onDismiss = { selectedPoint = null },
                modifier = modifier
            )
        } else {
            GrammarReferenceDetailScreen(
                point = currentPoint,
                accentColor = accentColor,
                onDismiss = { selectedPoint = null },
                modifier = modifier
            )
        }
        return
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
                text = category.displayName,
                style = Theme.heading(20.sp),
                color = Theme.ink
            )
        }

        // Arama Çubuğu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
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
                        text = L10n.grammarCategorySearchPlaceholder,
                        fontSize = 15.sp,
                        color = Theme.secondaryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 15.sp, color = Theme.ink),
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
                items(filtered, key = { it.id }) { point ->
                    GrammarCategoryPointRow(
                        point = point,
                        accentColor = accentColor,
                        onClick = {
                            FeedbackManager.shared.playSelection()
                            selectedPoint = point
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GrammarCategoryPointRow(
    point: GrammarPoint,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .inkBordered()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = point.pattern,
            style = Theme.heading(18.sp),
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(110.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = point.title,
                style = Theme.heading(15.sp),
                color = Theme.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = point.romaji,
                fontSize = 12.sp,
                color = Theme.secondaryInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Zorluk Noktaları (1..3)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in 1..3) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            color = if (i <= point.difficulty) accentColor else accentColor.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Theme.secondaryInk,
            modifier = Modifier.size(14.dp)
        )
    }
}
