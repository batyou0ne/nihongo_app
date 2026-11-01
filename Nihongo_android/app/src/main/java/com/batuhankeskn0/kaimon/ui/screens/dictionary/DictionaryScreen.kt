package com.batuhankeskn0.kaimon.ui.screens.dictionary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.batuhankeskn0.kaimon.data.model.GrammarCategory
import com.batuhankeskn0.kaimon.data.model.GrammarPoint
import com.batuhankeskn0.kaimon.data.model.Kanji
import com.batuhankeskn0.kaimon.data.model.VocabularyWord
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.VerbType
import com.batuhankeskn0.kaimon.ui.components.FuriganaText
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered

/**
 * Kaimon Sözlük ve Dilbilgisi Kütüphanesi Ekranı (`Views/DictionaryView.swift` ve `Views/Reference/` karşılığı).
 *
 * Özellikler:
 * 1. Kelime, Kanji ve Gramer sekmeleri / filtreleri.
 * 2. Japonca (Kanji/Kana), Romaji ve Türkçe anlama göre `debounce` destekli akıcı anlık arama.
 * 3. Kartlara veya ses butonlarına tıklandığında telaffuz sesinin çalınması (`AudioService.shared.speak`)
 *    ve zengin Brutalist detay kartlarının (`ModalBottomSheet`) gösterilmesi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreen(
    modifier: Modifier = Modifier,
    viewModel: DictionaryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    DictionaryScreenContent(
        uiState = uiState,
        searchQuery = searchQuery,
        onTabSelected = { tab ->
            FeedbackManager.shared.playSelection()
            viewModel.selectTab(tab)
        },
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onClearSearch = {
            FeedbackManager.shared.playSelection()
            viewModel.clearSearch()
        },
        onSelectGrammarCategory = { category ->
            FeedbackManager.shared.playSelection()
            viewModel.selectGrammarCategory(category)
        },
        onWordClicked = { word ->
            FeedbackManager.shared.playSelection()
            viewModel.onWordClicked(word)
        },
        onSpeakWord = { word ->
            FeedbackManager.shared.playSelection()
            AudioService.shared.speak(word.speechText)
        },
        onKanjiClicked = { kanji ->
            FeedbackManager.shared.playSelection()
            viewModel.onKanjiClicked(kanji)
        },
        onSpeakKanji = { kanji ->
            FeedbackManager.shared.playSelection()
            AudioService.shared.speak(kanji.character)
        },
        onGrammarPointClicked = { point ->
            FeedbackManager.shared.playSelection()
            viewModel.onGrammarPointClicked(point)
        },
        onVerbClicked = { verb ->
            FeedbackManager.shared.playSelection()
            viewModel.onVerbClicked(verb)
        },
        onDismissDetails = viewModel::dismissDetails,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictionaryScreenContent(
    uiState: DictionaryUiState,
    searchQuery: String,
    onTabSelected: (DictionaryTab) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onClearSearch: () -> Unit,
    onSelectGrammarCategory: (GrammarCategory?) -> Unit,
    onWordClicked: (VocabularyWord) -> Unit,
    onSpeakWord: (VocabularyWord) -> Unit,
    onKanjiClicked: (Kanji) -> Unit,
    onSpeakKanji: (Kanji) -> Unit,
    onGrammarPointClicked: (GrammarPoint) -> Unit,
    onVerbClicked: (VerbEntry) -> Unit,
    onDismissDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
    ) {
        // Başlık
        Text(
            text = if (uiState.selectedTab == DictionaryTab.GRAMMAR) {
                L10n.grammarLibraryTitle
            } else {
                L10n.dictionaryTitle
            },
            style = Theme.display(34.sp),
            color = Theme.ink,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 8.dp)
        )

        // Sekme Seçici: Kelime | Kanji | Gramer
        DictionaryTabSelector(
            selectedTab = uiState.selectedTab,
            onTabSelected = onTabSelected,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 10.dp)
        )

        // Özel Brutalist Arama Çubuğu (iOS DictionaryView.swift custom search bar karşılığı)
        DictionarySearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChanged,
            onClear = onClearSearch,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 10.dp)
        )

        // Sekme İçeriği
        when (uiState.selectedTab) {
            DictionaryTab.VOCABULARY -> {
                VocabularyTabContent(
                    words = uiState.filteredWords,
                    query = searchQuery,
                    onWordClicked = onWordClicked,
                    onSpeakWord = onSpeakWord
                )
            }
            DictionaryTab.KANJI -> {
                KanjiTabContent(
                    kanjiList = uiState.filteredKanji,
                    query = searchQuery,
                    onKanjiClicked = onKanjiClicked,
                    onSpeakKanji = onSpeakKanji
                )
            }
            DictionaryTab.GRAMMAR -> {
                GrammarTabContent(
                    uiState = uiState,
                    query = searchQuery,
                    onSelectCategory = onSelectGrammarCategory,
                    onGrammarPointClicked = onGrammarPointClicked,
                    onVerbClicked = onVerbClicked
                )
            }
        }
    }

    // Detay Alt Pencereleri (ModalBottomSheet)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    uiState.selectedWord?.let { word ->
        ModalBottomSheet(
            onDismissRequest = onDismissDetails,
            sheetState = sheetState,
            containerColor = Theme.paper,
            contentColor = Theme.ink
        ) {
            VocabularyDetailSheet(
                word = word,
                onClose = onDismissDetails
            )
        }
    }

    uiState.selectedKanji?.let { kanji ->
        ModalBottomSheet(
            onDismissRequest = onDismissDetails,
            sheetState = sheetState,
            containerColor = Theme.paper,
            contentColor = Theme.ink
        ) {
            KanjiDetailSheet(
                kanji = kanji,
                onClose = onDismissDetails
            )
        }
    }

    uiState.selectedGrammarPoint?.let { point ->
        ModalBottomSheet(
            onDismissRequest = onDismissDetails,
            sheetState = sheetState,
            containerColor = Theme.paper,
            contentColor = Theme.ink
        ) {
            GrammarReferenceDetailSheet(
                point = point,
                onClose = onDismissDetails
            )
        }
    }

    uiState.selectedVerb?.let { verb ->
        ModalBottomSheet(
            onDismissRequest = onDismissDetails,
            sheetState = sheetState,
            containerColor = Theme.paper,
            contentColor = Theme.ink
        ) {
            VerbConjugationSheet(
                verb = verb,
                onClose = onDismissDetails
            )
        }
    }
}

@Composable
private fun DictionaryTabSelector(
    selectedTab: DictionaryTab,
    onTabSelected: (DictionaryTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerShape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = Theme.paper, shape = containerShape)
            .clip(containerShape)
            .border(width = 1.5.dp, color = Theme.ink, shape = containerShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DictionaryTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            val tabShape = RoundedCornerShape(9.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(tabShape)
                    .background(
                        color = if (isSelected) Theme.ink else Theme.paper,
                        shape = tabShape
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.localizedTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Theme.paper else Theme.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun DictionarySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .inkBordered(lineWidth = 1.5.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
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
            if (query.isEmpty()) {
                Text(
                    text = L10n.dictionarySearch,
                    fontSize = 16.sp,
                    color = Theme.secondaryInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = Theme.ink
                ),
                cursorBrush = SolidColor(Theme.accent),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Filled.Cancel,
                contentDescription = L10n.closeButton,
                tint = Theme.secondaryInk,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear)
            )
        }
    }
}

// MARK: - 1. Kelime Sekmesi

@Composable
private fun VocabularyTabContent(
    words: List<VocabularyWord>,
    query: String,
    onWordClicked: (VocabularyWord) -> Unit,
    onSpeakWord: (VocabularyWord) -> Unit
) {
    if (words.isEmpty()) {
        DictionaryEmptyState(query = query)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp)
    ) {
        items(words, key = { it.id }) { word ->
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onWordClicked(word) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = word.displayText,
                                style = Theme.heading(20.sp),
                                color = Theme.ink
                            )
                            if (word.kanji.isNotEmpty()) {
                                Text(
                                    text = word.hiragana,
                                    fontSize = 14.sp,
                                    color = Theme.secondaryInk
                                )
                            }
                            Text(
                                text = "· ${word.romaji}",
                                fontSize = 13.sp,
                                color = Theme.secondaryInk
                            )
                        }

                        Text(
                            text = word.turkishMeaning,
                            fontSize = 15.sp,
                            color = Theme.ink
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Theme.accent.copy(alpha = 0.12f))
                            .clickable { onSpeakWord(word) },
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

                HorizontalDivider(
                    color = Theme.secondaryInk.copy(alpha = 0.25f),
                    thickness = 1.dp
                )
            }
        }
    }
}

// MARK: - 2. Kanji Sekmesi

@Composable
private fun KanjiTabContent(
    kanjiList: List<Kanji>,
    query: String,
    onKanjiClicked: (Kanji) -> Unit,
    onSpeakKanji: (Kanji) -> Unit
) {
    if (kanjiList.isEmpty()) {
        DictionaryEmptyState(query = query)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(kanjiList, key = { it.id }) { kanji ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .inkBordered()
                    .clickable { onKanjiClicked(kanji) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            color = Theme.accent.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = kanji.character,
                        style = Theme.display(32.sp),
                        color = Theme.accent
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = kanji.meaning,
                            style = Theme.heading(17.sp),
                            color = Theme.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Theme.ink.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = L10n.strokeCountLabel(kanji.strokeCount),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Theme.secondaryInk
                            )
                        }
                    }

                    Text(
                        text = kanji.flipRecap,
                        fontSize = 13.sp,
                        color = Theme.secondaryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Theme.accent.copy(alpha = 0.12f))
                        .clickable { onSpeakKanji(kanji) },
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
        }
    }
}

// MARK: - 3. Gramer & Dilbilgisi Kütüphanesi Sekmesi

@Composable
private fun GrammarTabContent(
    uiState: DictionaryUiState,
    query: String,
    onSelectCategory: (GrammarCategory?) -> Unit,
    onGrammarPointClicked: (GrammarPoint) -> Unit,
    onVerbClicked: (VerbEntry) -> Unit
) {
    val categories = remember {
        listOf(
            GrammarCategory.VERB,
            GrammarCategory.PARTICLE,
            GrammarCategory.ADJECTIVE,
            GrammarCategory.CONJUNCTION,
            GrammarCategory.EXPRESSION
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Kategori Filtre Hapları (Tümü, Fiiller, Edatlar, Sıfatlar, Bağlaçlar, İfadeler)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryFilterChip(
                label = L10n.allFilterLabel,
                isSelected = uiState.selectedGrammarCategory == null,
                onClick = { onSelectCategory(null) }
            )
            categories.forEach { cat ->
                CategoryFilterChip(
                    label = cat.displayName,
                    isSelected = uiState.selectedGrammarCategory == cat,
                    onClick = { onSelectCategory(cat) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (uiState.selectedGrammarCategory == GrammarCategory.VERB) {
            // Fiil Çekim Kütüphanesi (iOS VerbCategoryListView karşılığı)
            VerbLibraryList(
                verbs = uiState.filteredVerbs,
                query = query,
                onVerbClicked = onVerbClicked
            )
        } else {
            val points = uiState.filteredGrammar
            if (points.isEmpty() && query.isNotBlank()) {
                DictionaryEmptyState(query = query)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Kategori seçilmemiş ve arama boşsa üstte iOS GrammarReferenceView kategori kartlarını göster
                    if (uiState.selectedGrammarCategory == null && query.isBlank()) {
                        item {
                            Text(
                                text = L10n.grammarLibrarySubtitle,
                                fontSize = 14.sp,
                                color = Theme.secondaryInk,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        items(categories, key = { "cat_${it.rawValue}" }) { cat ->
                            val subtitle = if (cat == GrammarCategory.VERB) {
                                L10n.verbLibrarySubtitle
                            } else {
                                val count = uiState.grammarCategoryCounts[cat] ?: 0
                                L10n.grammarRuleCountSubtitle(count, "N5")
                            }
                            GrammarCategoryReferenceCard(
                                category = cat,
                                subtitle = subtitle,
                                onClick = { onSelectCategory(cat) }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    items(points, key = { it.id }) { point ->
                        GrammarPointRow(
                            point = point,
                            onClick = { onGrammarPointClicked(point) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Theme.accent else Theme.paper
    val fgColor = if (isSelected) Theme.paper else Theme.ink

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(color = bgColor, shape = CircleShape)
            .border(
                width = 1.5.dp,
                color = if (isSelected) Theme.accent else Theme.ink,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = fgColor
        )
    }
}

@Composable
private fun GrammarCategoryReferenceCard(
    category: GrammarCategory,
    subtitle: String,
    onClick: () -> Unit
) {
    val icon: ImageVector = when (category) {
        GrammarCategory.VERB -> Icons.AutoMirrored.Filled.Redo
        GrammarCategory.PARTICLE -> Icons.Filled.Link
        GrammarCategory.ADJECTIVE -> Icons.Filled.Palette
        GrammarCategory.CONJUNCTION -> Icons.AutoMirrored.Filled.CallMerge
        GrammarCategory.EXPRESSION -> Icons.Filled.FormatQuote
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .inkBordered()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    color = Theme.accent.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Theme.accent,
                modifier = Modifier.size(24.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = category.displayName,
                style = Theme.heading(19.sp),
                color = Theme.ink
            )
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = Theme.secondaryInk
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Theme.secondaryInk,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun GrammarPointRow(
    point: GrammarPoint,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .inkBordered()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = point.pattern,
            style = Theme.heading(18.sp),
            color = Theme.accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(108.dp)
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

        // Zorluk noktaları (1..3)
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            for (i in 1..3) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            color = if (i <= point.difficulty) Theme.accent else Theme.accent.copy(alpha = 0.2f),
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

@Composable
private fun VerbLibraryList(
    verbs: List<VerbEntry>,
    query: String,
    onVerbClicked: (VerbEntry) -> Unit
) {
    if (verbs.isEmpty()) {
        DictionaryEmptyState(query = query)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    VerbLegendBadge(label = "Gr.1 Godan")
                    VerbLegendBadge(label = "Gr.2 Ichidan")
                    VerbLegendBadge(label = "する/くる")
                }
                Text(
                    text = L10n.verbCountLabel(verbs.size),
                    fontSize = 12.sp,
                    color = Theme.secondaryInk
                )
            }
        }

        items(verbs, key = { it.id }) { verb ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .inkBordered()
                    .clickable { onVerbClicked(verb) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tagLabel = when (verb.verbType) {
                        VerbType.ICHIDAN -> "Gr.2"
                        VerbType.SURU -> "する"
                        VerbType.KURU -> "くる"
                        VerbType.ARU -> "ある"
                        else -> "Gr.1"
                    }
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
}

@Composable
private fun VerbLegendBadge(label: String) {
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
private fun DictionaryEmptyState(query: String) {
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
            text = L10n.noSearchResults(query),
            style = Theme.heading(16.sp),
            color = Theme.secondaryInk,
            textAlign = TextAlign.Center
        )
    }
}

// MARK: - Detay Alt Sayfaları (Bottom Sheets)

@Composable
private fun VocabularyDetailSheet(
    word: VocabularyWord,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Başlık + Kapat
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = word.displayText,
                    style = Theme.display(38.sp),
                    color = Theme.ink
                )
                if (word.kanji.isNotEmpty()) {
                    Text(
                        text = "${word.hiragana} · ${word.romaji}",
                        fontSize = 16.sp,
                        color = Theme.secondaryInk
                    )
                } else {
                    Text(
                        text = word.romaji,
                        fontSize = 16.sp,
                        color = Theme.secondaryInk
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Theme.accent.copy(alpha = 0.14f))
                        .clickable { AudioService.shared.speak(word.speechText) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = L10n.listenPronunciation,
                        tint = Theme.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Theme.ink.copy(alpha = 0.08f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = L10n.closeButton,
                        tint = Theme.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Anlam Kutusu
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .inkBordered()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = L10n.meaningLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Theme.secondaryInk
            )
            Text(
                text = word.turkishMeaning,
                style = Theme.heading(20.sp),
                color = Theme.ink
            )
        }

        // Örnek Cümle (varsa)
        val hint = word.contextHint
        if (hint != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .inkBordered()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = L10n.exampleSentencesLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Theme.secondaryInk
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = L10n.listenPronunciation,
                        tint = Theme.accent,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { AudioService.shared.speak(hint.sentence) }
                    )
                }

                FuriganaText(
                    text = hint.sentence,
                    style = Theme.heading(18.sp),
                    color = Theme.ink
                )

                Text(
                    text = hint.translation,
                    fontSize = 14.sp,
                    color = Theme.secondaryInk
                )
            }
        }
    }
}

@Composable
private fun KanjiDetailSheet(
    kanji: Kanji,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .inkBordered(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = kanji.character,
                        style = Theme.display(44.sp),
                        color = Theme.accent
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = kanji.meaning,
                        style = Theme.display(24.sp),
                        color = Theme.ink
                    )
                    Text(
                        text = L10n.strokeCountLabel(kanji.strokeCount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Theme.secondaryInk
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Theme.accent.copy(alpha = 0.14f))
                        .clickable { AudioService.shared.speak(kanji.character) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = L10n.listenPronunciation,
                        tint = Theme.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Theme.ink.copy(alpha = 0.08f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = L10n.closeButton,
                        tint = Theme.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // On'yomi & Kun'yomi Kartları
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .inkBordered()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = L10n.onyomiLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Theme.secondaryInk
                )
                Text(
                    text = kanji.onyomi.ifEmpty { "—" },
                    style = Theme.heading(18.sp),
                    color = Theme.ink
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .inkBordered()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = L10n.kunyomiLabel,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Theme.secondaryInk
                )
                Text(
                    text = kanji.kunyomi.ifEmpty { "—" },
                    style = Theme.heading(18.sp),
                    color = Theme.ink
                )
            }
        }

        // Örnek Kelimeler
        if (kanji.exampleWords.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = L10n.exampleWordsLabel,
                    style = Theme.display(18.sp),
                    color = Theme.ink
                )

                kanji.exampleWords.forEach { ex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .inkBordered()
                            .clickable { AudioService.shared.speak(ex.hiragana) }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "${ex.hiragana} · ${ex.romaji}",
                                style = Theme.heading(17.sp),
                                color = Theme.ink
                            )
                            Text(
                                text = ex.turkishMeaning,
                                fontSize = 14.sp,
                                color = Theme.secondaryInk
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = L10n.listenPronunciation,
                            tint = Theme.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * iOS `GrammarReferenceDetailView.swift` bileşeninin birebir karşılığı.
 */
@Composable
private fun GrammarReferenceDetailSheet(
    point: GrammarPoint,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Üst Satır: Pattern + Zorluk Rozeti + Kapat
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
                    style = Theme.display(32.sp),
                    color = Theme.accent
                )
                Text(
                    text = point.romaji,
                    fontSize = 14.sp,
                    color = Theme.secondaryInk
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                                        color = if (i <= point.difficulty) Theme.accent else Theme.accent.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(3.dp)
                                    )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Theme.ink.copy(alpha = 0.08f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = L10n.closeButton,
                        tint = Theme.ink,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = point.title,
                style = Theme.heading(20.sp),
                color = Theme.ink
            )
            Text(
                text = point.explanation,
                fontSize = 15.sp,
                color = Theme.ink,
                lineHeight = 21.sp
            )
        }

        // Formül Kutusu
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
                color = Theme.ink
            )
        }

        // Örnek Cümleler
        if (point.examples.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = L10n.exampleSentencesLabel,
                    style = Theme.display(18.sp),
                    color = Theme.ink
                )

                point.examples.forEach { example ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .inkBordered()
                            .clickable {
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
                            Text(
                                text = example.japanese,
                                style = Theme.heading(18.sp),
                                color = Theme.ink,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = L10n.listenPronunciation,
                                tint = Theme.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (!example.hiragana.isNullOrEmpty()) {
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

        // Ders İpucu
        HorizontalDivider(color = Theme.secondaryInk.copy(alpha = 0.25f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lightbulb,
                contentDescription = null,
                tint = Theme.accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = L10n.grammarLessonHint(point.title),
                fontSize = 12.sp,
                color = Theme.secondaryInk
            )
        }
    }
}

/**
 * Fiil Çekim Tablosu Alt Sayfası (`VerbConjugationView.swift` karşılığı).
 */
@Composable
private fun VerbConjugationSheet(
    verb: VerbEntry,
    onClose: () -> Unit
) {
    val result = remember(verb) { verb.conjugationResult() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = verb.kanji ?: verb.hiragana,
                    style = Theme.display(34.sp),
                    color = Theme.ink
                )
                Text(
                    text = "${verb.hiragana} · ${verb.romaji}",
                    fontSize = 14.sp,
                    color = Theme.secondaryInk
                )
                Text(
                    text = verb.turkishMeaning,
                    style = Theme.heading(16.sp),
                    color = Theme.accent
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Theme.ink.copy(alpha = 0.08f))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = L10n.closeButton,
                    tint = Theme.ink,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Text(
            text = "${L10n.verbConjugationTitle} (${result.verbType.displayName})",
            style = Theme.display(20.sp),
            color = Theme.ink
        )

        result.forms.forEach { form ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .inkBordered()
                    .clickable { AudioService.shared.speak(form.japanese) }
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = form.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Theme.accent
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = L10n.listenPronunciation,
                        tint = Theme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = form.japanese,
                    style = Theme.heading(20.sp),
                    color = Theme.ink
                )
                Text(
                    text = form.romaji,
                    fontSize = 12.sp,
                    color = Theme.secondaryInk
                )
                Text(
                    text = form.explanation,
                    fontSize = 13.sp,
                    color = Theme.ink
                )
            }
        }
    }
}
