package com.batuhankeskn0.kaimon.ui.screens.grammar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.HorizontalDivider
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
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.ConjugationForm
import com.batuhankeskn0.kaimon.domain.engine.VerbConjugationEngine
import com.batuhankeskn0.kaimon.domain.engine.VerbConjugationResult
import com.batuhankeskn0.kaimon.domain.engine.VerbType
import com.batuhankeskn0.kaimon.ui.screens.dictionary.DictionaryViewModel
import com.batuhankeskn0.kaimon.ui.screens.dictionary.VerbEntry
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fiil çekim formlarının gruplandırılması (`Views/Grammar/VerbConjugationView.swift` karşılığı).
 */
enum class VerbFormGroup(val formIDs: List<String>) {
    POLITE(listOf("masu", "masen", "mashita", "masen_deshita", "mashou")),
    PLAIN(listOf("dict", "ta", "nai", "potential", "volitional", "imperative")),
    SPECIAL(listOf("te", "tai", "conditional"));

    val localizedTitle: String
        get() = when (this) {
            POLITE -> L10n.verbGroupPolite
            PLAIN -> L10n.verbGroupPlain
            SPECIAL -> L10n.verbGroupSpecial
        }
}

/**
 * Fiil Çekim Tablosu ve İnteraktif Fiil Çekimleyici Ekranı (`Views/Grammar/VerbConjugationView.swift` karşılığı).
 *
 * - `VerbConjugationEngine.conjugateExtended` kullanarak seçilen veya aranan herhangi bir Japonca fiilin
 *   tüm çekim formlarını (Masu, Te, Nai, Geçmiş, Potansiyel, Emir, İstek, İrade, Şart vb.) hesaplar.
 * - Her çekim formunu sesli okunuş (`AudioService.shared.speak`) ve Türkçe açıklamalarıyla sunar.
 */
@Composable
fun VerbConjugationScreen(
    hiragana: String = "たべる",
    kanji: String? = "食べる",
    turkishMeaning: String? = null,
    allowVerbSelection: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var activeHiragana by remember(hiragana) { mutableStateOf(hiragana) }
    var activeKanji by remember(kanji) { mutableStateOf(kanji) }
    var activeMeaning by remember(turkishMeaning) {
        mutableStateOf(turkishMeaning ?: L10n.grammarSampleVerbMeaning)
    }
    var selectedGroup by remember { mutableStateOf(VerbFormGroup.POLITE) }
    var searchQuery by remember { mutableStateOf("") }

    val allVerbs by produceState<List<VerbEntry>>(
        initialValue = emptyList(),
        key1 = allowVerbSelection
    ) {
        if (allowVerbSelection) {
            value = withContext(Dispatchers.Default) {
                val words = ContentStore.loadVocabulary("N5")
                DictionaryViewModel.extractVerbs(words)
            }
        }
    }

    val filteredVerbs = remember(allVerbs, searchQuery) {
        DictionaryViewModel.filterVerbs(allVerbs, searchQuery)
    }

    val result: VerbConjugationResult = remember(activeHiragana, activeKanji) {
        VerbConjugationEngine.conjugateExtended(
            hiragana = activeHiragana,
            kanji = activeKanji
        )
    }

    if (onDismiss != null) {
        BackHandler { onDismiss() }
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
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = L10n.grammarVerbConjugationCardTitle,
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // İnteraktif Fiil Arama & Seçim Bölümü (Fiil Çekimleyici Kısayolundan Açıldığında)
            if (allowVerbSelection && allVerbs.isNotEmpty()) {
                VerbQuickSelectorSection(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    verbs = filteredVerbs,
                    selectedHiragana = activeHiragana,
                    onSelectVerb = { verb ->
                        FeedbackManager.shared.playSelection()
                        activeHiragana = verb.hiragana
                        activeKanji = verb.kanji
                        activeMeaning = verb.turkishMeaning
                        AudioService.shared.speak(verb.hiragana)
                    }
                )
            }

            // 1. Fiil Başlık Kartı
            VerbHeaderCard(
                result = result,
                turkishMeaning = activeMeaning,
                onSpeak = {
                    FeedbackManager.shared.playSelection()
                    AudioService.shared.speak(result.dictionaryForm)
                }
            )

            // 2. Grup Seçici (Kibar | Plain | Özel)
            VerbGroupPicker(
                selectedGroup = selectedGroup,
                onGroupSelected = { group ->
                    FeedbackManager.shared.playSelection()
                    selectedGroup = group
                }
            )

            // 3. Seçili Grubun Çekim Kartları
            VerbFormGrid(
                result = result,
                selectedGroup = selectedGroup,
                onSpeakForm = { form ->
                    FeedbackManager.shared.playSelection()
                    AudioService.shared.speak(form.japanese)
                }
            )

            // 4. Tam Çekim Tablosu
            VerbAllFormsSection(
                result = result,
                onSpeakForm = { form ->
                    FeedbackManager.shared.playSelection()
                    AudioService.shared.speak(form.japanese)
                }
            )
        }
    }
}

@Composable
private fun VerbQuickSelectorSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    verbs: List<VerbEntry>,
    selectedHiragana: String,
    onSelectVerb: (VerbEntry) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Arama Çubuğu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .inkBordered(lineWidth = 1.5.dp)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = Theme.secondaryInk,
                modifier = Modifier.size(18.dp)
            )
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = L10n.verbSearchPlaceholder,
                        fontSize = 14.sp,
                        color = Theme.secondaryInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 15.sp, color = Theme.ink),
                    cursorBrush = SolidColor(Theme.accent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (searchQuery.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Cancel,
                    contentDescription = L10n.closeButton,
                    tint = Theme.secondaryInk,
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .clickable { onSearchQueryChange("") }
                )
            }
        }

        // Yatay Fiil Seçim Hapları
        if (verbs.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                verbs.take(25).forEach { verb ->
                    val isSelected = verb.hiragana == selectedHiragana
                    val chipShape = RoundedCornerShape(10.dp)
                    Row(
                        modifier = Modifier
                            .clip(chipShape)
                            .background(
                                color = if (isSelected) Theme.accent else Theme.paper,
                                shape = chipShape
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) Theme.accent else Theme.ink,
                                shape = chipShape
                            )
                            .clickable { onSelectVerb(verb) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = verb.kanji ?: verb.hiragana,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Theme.paper else Theme.ink
                        )
                        Text(
                            text = verb.turkishMeaning.substringBefore(","),
                            fontSize = 12.sp,
                            color = if (isSelected) Theme.paper.copy(alpha = 0.9f) else Theme.secondaryInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VerbHeaderCard(
    result: VerbConjugationResult,
    turkishMeaning: String?,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .inkBordered()
            .padding(20.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = result.kanji ?: result.dictionaryForm,
                    style = Theme.display(36.sp),
                    color = Theme.ink
                )

                Box(
                    modifier = Modifier
                        .size(38.dp)
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

            if (result.kanji != null) {
                Text(
                    text = result.dictionaryForm,
                    style = Theme.heading(18.sp),
                    color = Theme.secondaryInk
                )
            }

            if (!turkishMeaning.isNullOrBlank()) {
                Text(
                    text = turkishMeaning,
                    fontSize = 15.sp,
                    color = Theme.secondaryInk,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        color = Theme.accent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = result.verbType.displayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Theme.accent
                )
            }

            if (result.verbType.isGodan) {
                Text(
                    text = L10n.verbGroup1Label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Theme.secondaryInk
                )
            } else if (result.verbType == VerbType.ICHIDAN) {
                Text(
                    text = L10n.verbGroup2Label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Theme.secondaryInk
                )
            }
        }
    }
}

@Composable
private fun VerbGroupPicker(
    selectedGroup: VerbFormGroup,
    onGroupSelected: (VerbFormGroup) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .inkBordered()
    ) {
        VerbFormGroup.entries.forEach { group ->
            val isSelected = selectedGroup == group
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(color = if (isSelected) Theme.ink else Theme.paper)
                    .clickable { onGroupSelected(group) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = group.localizedTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Theme.paper else Theme.secondaryInk
                )
            }
        }
    }
}

@Composable
private fun VerbFormGrid(
    result: VerbConjugationResult,
    selectedGroup: VerbFormGroup,
    onSpeakForm: (ConjugationForm) -> Unit
) {
    val ids = selectedGroup.formIDs
    val forms = remember(result, selectedGroup) {
        result.forms.filter { ids.contains(it.id) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        forms.forEach { form ->
            VerbFormRowCard(
                form = form,
                onSpeak = { onSpeakForm(form) }
            )
        }
    }
}

@Composable
fun VerbFormRowCard(
    form: ConjugationForm,
    onSpeak: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .inkBordered()
            .clickable(onClick = onSpeak)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sol Sütun: Form adı ve Türkçe açıklaması
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = form.label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.ink
            )
            Text(
                text = form.explanation,
                fontSize = 12.sp,
                color = Theme.secondaryInk,
                lineHeight = 16.sp
            )
        }

        // Sağ Sütun: Japonca Çekim + Romaji + Ses İkonu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = form.japanese,
                    style = Theme.heading(19.sp),
                    color = Theme.accent,
                    textAlign = TextAlign.End
                )
                Text(
                    text = form.romaji,
                    fontSize = 11.sp,
                    color = Theme.secondaryInk,
                    textAlign = TextAlign.End
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = L10n.listenPronunciation,
                tint = Theme.accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun VerbAllFormsSection(
    result: VerbConjugationResult,
    onSpeakForm: (ConjugationForm) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = L10n.verbFullTableTitle,
                style = Theme.display(18.sp),
                color = Theme.ink
            )
            Icon(
                imageVector = Icons.Filled.TableChart,
                contentDescription = null,
                tint = Theme.secondaryInk,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .inkBordered()
        ) {
            // Tablo Başlık Satırı
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Theme.ink.copy(alpha = 0.06f))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = L10n.verbTableFormColumn,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Theme.secondaryInk,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = L10n.verbTableJapaneseColumn,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Theme.secondaryInk,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(150.dp)
                )
            }

            HorizontalDivider(color = Theme.ink.copy(alpha = 0.15f))

            result.forms.forEachIndexed { idx, form ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = if (idx % 2 == 0) Theme.paper else Theme.ink.copy(alpha = 0.03f)
                        )
                        .clickable { onSpeakForm(form) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = form.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Theme.ink,
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = form.japanese,
                            style = Theme.heading(16.sp),
                            color = Theme.accent,
                            textAlign = TextAlign.End
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = L10n.listenPronunciation,
                            tint = Theme.accent.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (idx < result.forms.lastIndex) {
                    HorizontalDivider(
                        color = Theme.ink.copy(alpha = 0.1f),
                        modifier = Modifier.padding(start = 14.dp)
                    )
                }
            }
        }
    }
}
