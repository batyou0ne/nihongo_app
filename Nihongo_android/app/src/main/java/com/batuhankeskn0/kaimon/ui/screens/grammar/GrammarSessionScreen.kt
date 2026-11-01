package com.batuhankeskn0.kaimon.ui.screens.grammar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.local.UserDatabaseManager
import com.batuhankeskn0.kaimon.data.model.GrammarPoint
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.ui.navigation.LocalTabBarManager
import com.batuhankeskn0.kaimon.ui.theme.PrimaryButton
import com.batuhankeskn0.kaimon.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Otomatik Gramer Oturumu için öğrenilmemiş en fazla 3 konuyu seçen saf yardımcı fonksiyon.
 */
fun selectGrammarSessionPoints(
    allPoints: List<GrammarPoint>,
    learnedIDs: Set<String>,
    maxCount: Int = 3
): List<GrammarPoint> = allPoints
    .filter { !learnedIDs.contains(it.id) }
    .take(maxCount)

/**
 * Otomatik Gramer Oturumu Ekranı (`Views/Grammar/GrammarSessionView.swift` birebir karşılığı).
 *
 * Seçilen seviyedeki öğrenilmemiş gramer konularından en fazla 3 tanesini seçer.
 * Sırayla konuların derslerini (`GrammarLessonScreen(isSessionMode = true)`) gösterir,
 * ardından üçü için toplu pratik (`GrammarPracticeScreen`) yaptırır.
 */
@Composable
fun GrammarSessionScreen(
    level: String,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val tabBarManager = LocalTabBarManager.current

    var sessionPoints by remember { mutableStateOf<List<GrammarPoint>>(emptyList()) }
    var currentLessonIndex by remember { mutableIntStateOf(0) }
    var isPracticing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isAllLearned by remember { mutableStateOf(false) }

    DisposableEffect(tabBarManager) {
        val wasHidden = tabBarManager.isHidden
        tabBarManager.hide()
        onDispose {
            if (!wasHidden) {
                tabBarManager.show()
            }
        }
    }

    LaunchedEffect(level) {
        isLoading = true
        val (points, allDone) = withContext(Dispatchers.IO) {
            val allPoints = ContentStore.loadGrammar(level = level)
            val learnedIDs = runCatching {
                UserDatabaseManager.shared.progressDao
                    .getProgressByKind(LearnableItemKind.GRAMMAR)
                    .filter { it.isLearned || it.repetitionCount >= 1 }
                    .map { it.itemID }
                    .toSet()
            }.getOrDefault(emptySet())

            val unlearned = selectGrammarSessionPoints(allPoints, learnedIDs, maxCount = 3)
            unlearned to unlearned.isEmpty()
        }
        sessionPoints = points
        isAllLearned = allDone
        currentLessonIndex = 0
        isPracticing = false
        isLoading = false
    }

    if (onDismiss != null) {
        BackHandler { onDismiss() }
    }

    when {
        isLoading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Theme.paper),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Theme.accent)
            }
        }
        isAllLearned -> {
            GrammarAllLearnedContent(
                level = level,
                onDismiss = { onDismiss?.invoke() },
                modifier = modifier
            )
        }
        isPracticing -> {
            GrammarPracticeScreen(
                points = sessionPoints,
                title = L10n.grammarPracticeTitle,
                level = level,
                onFinish = { onDismiss?.invoke() },
                modifier = modifier
            )
        }
        currentLessonIndex in sessionPoints.indices -> {
            GrammarLessonScreen(
                point = sessionPoints[currentLessonIndex],
                level = level,
                isSessionMode = true,
                onNext = {
                    if (currentLessonIndex + 1 < sessionPoints.size) {
                        currentLessonIndex += 1
                    } else {
                        isPracticing = true
                    }
                },
                onBack = { onDismiss?.invoke() },
                modifier = modifier
            )
        }
    }
}

@Composable
private fun GrammarAllLearnedContent(
    level: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Celebration,
                contentDescription = null,
                tint = Theme.accent,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = L10n.grammarAllLearnedTitle,
                style = Theme.display(32.sp),
                color = Theme.ink
            )
            Text(
                text = L10n.grammarAllLearnedMessage(level),
                style = Theme.heading(18.sp),
                color = Theme.secondaryInk,
                textAlign = TextAlign.Center
            )
            PrimaryButton(
                text = L10n.grammarGoBackButton,
                onClick = {
                    FeedbackManager.shared.playSelection()
                    onDismiss()
                },
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
