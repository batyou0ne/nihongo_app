package com.batuhankeskn0.kaimon.ui.screens.scenarios

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.model.ConversationScenario
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gerçek Hayat Diyalog Senaryoları Listesi Ekranı (`Views/Scenarios/ScenarioListView.swift` karşılığı).
 * `ScenariosData.json` içindeki senaryoları (Tanışma, Restoranda sipariş, Tren istasyonu, Alışveriş vb.)
 * JLPT seviye etiketleriyle listeler ve seçilen senaryoyu `ScenarioChatScreen` içinde açar.
 */
@Composable
fun ScenarioListScreen(
    modifier: Modifier = Modifier,
    contentStore: ContentStore = ContentStore.shared,
    onBack: (() -> Unit)? = null
) {
    var selectedScenario by remember { mutableStateOf<ConversationScenario?>(null) }

    val scenarios by produceState<List<ConversationScenario>?>(initialValue = null, contentStore) {
        value = withContext(Dispatchers.IO) {
            contentStore.loadScenarios()
        }
    }

    BackHandler(enabled = selectedScenario != null || onBack != null) {
        if (selectedScenario != null) {
            selectedScenario = null
        } else {
            onBack?.invoke()
        }
    }

    val activeScenario = selectedScenario
    if (activeScenario != null) {
        ScenarioChatScreen(
            scenario = activeScenario,
            onDismiss = { selectedScenario = null },
            modifier = modifier
        )
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(onClick = {
                    FeedbackManager.shared.playSelection()
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = L10n.backButton,
                        tint = Theme.ink
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            Text(
                text = L10n.scenariosTitle,
                style = Theme.heading(20.sp),
                color = Theme.ink,
                modifier = Modifier.weight(1f)
            )
        }

        val loadedScenarios = scenarios
        if (loadedScenarios == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Theme.accent)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = loadedScenarios,
                    key = { it.id }
                ) { scenario ->
                    ScenarioCard(
                        scenario = scenario,
                        onClick = {
                            FeedbackManager.shared.playSelection()
                            selectedScenario = scenario
                        }
                    )
                }
            }
        }
    }
}

/**
 * Tek bir konuşma senaryosu kartı (`ScenarioListView.scenarioCard` karşılığı).
 */
@Composable
fun ScenarioCard(
    scenario: ConversationScenario,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .inkBordered()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Theme.accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = scenario.level,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.accent
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = scenario.title,
                style = Theme.heading(20.sp),
                color = Theme.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = scenario.description,
                fontSize = 14.sp,
                color = Theme.secondaryInk,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Theme.secondaryInk,
            modifier = Modifier.size(22.dp)
        )
    }
}
