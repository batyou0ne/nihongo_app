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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.model.GrammarCategory
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Dilbilgisi Başvuru Kütüphanesi Ana Ekranı (`Views/Reference/GrammarReferenceView.swift` karşılığı).
 *
 * Beş gramer kategorisini (`Fiiller`, `Edatlar`, `Sıfatlar`, `Bağlaçlar`, `İfadeler`) kilit olmadan listeler:
 * - `VERB` -> `VerbCategoryListScreen` (N5 sözcük listesinden fiiller ve çekim tabloları)
 * - Diğer kategoriler -> `GrammarCategoryListScreen` (ilgili kategorideki tüm gramer kuralları)
 */
@Composable
fun GrammarReferenceScreen(
    level: String = "N5",
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<GrammarCategory?>(null) }

    val categoryCounts by produceState<Map<GrammarCategory, Int>>(
        initialValue = emptyMap(),
        key1 = level
    ) {
        value = withContext(Dispatchers.Default) {
            val allPoints = ContentStore.loadGrammar(level = level)
            GrammarCategory.allCases.associateWith { cat ->
                allPoints.count { it.category == cat }
            }
        }
    }

    BackHandler(enabled = selectedCategory != null || onBack != null) {
        if (selectedCategory != null) {
            selectedCategory = null
        } else {
            onBack?.invoke()
        }
    }

    val currentCategory = selectedCategory
    if (currentCategory != null) {
        if (currentCategory == GrammarCategory.VERB) {
            VerbCategoryListScreen(
                onBack = { selectedCategory = null },
                modifier = modifier
            )
        } else {
            GrammarCategoryListScreen(
                category = currentCategory,
                accentColor = Theme.accent,
                level = level,
                onBack = { selectedCategory = null },
                modifier = modifier
            )
        }
        return
    }

    val categories = remember {
        listOf(
            GrammarCategory.VERB,
            GrammarCategory.PARTICLE,
            GrammarCategory.ADJECTIVE,
            GrammarCategory.CONJUNCTION,
            GrammarCategory.EXPRESSION
        )
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
                text = L10n.grammarLibraryTitle,
                style = Theme.heading(20.sp),
                color = Theme.ink
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .padding(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = L10n.grammarLibrarySubtitle,
                fontSize = 15.sp,
                color = Theme.secondaryInk
            )

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                categories.forEach { category ->
                    val subtitle = if (category == GrammarCategory.VERB) {
                        L10n.verbLibrarySubtitle
                    } else {
                        val count = categoryCounts[category] ?: 0
                        L10n.grammarRuleCountSubtitle(count, level)
                    }

                    GrammarReferenceCategoryCard(
                        category = category,
                        subtitle = subtitle,
                        onClick = {
                            FeedbackManager.shared.playSelection()
                            selectedCategory = category
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GrammarReferenceCategoryCard(
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
                modifier = Modifier.size(22.dp)
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
