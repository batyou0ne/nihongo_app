package com.batuhankeskn0.kaimon.ui.screens.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.ui.components.ModuleCard
import com.batuhankeskn0.kaimon.ui.components.ProgressBar
import com.batuhankeskn0.kaimon.ui.components.SectionLabel
import com.batuhankeskn0.kaimon.ui.components.StreakStrip
import com.batuhankeskn0.kaimon.ui.components.UtilityCard
import com.batuhankeskn0.kaimon.ui.navigation.MainTab
import com.batuhankeskn0.kaimon.ui.screens.progress.ProgressScreen
import com.batuhankeskn0.kaimon.ui.screens.review.ReviewListScreen
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered

private enum class HomeDestination {
    DASHBOARD,
    REVIEW_LIST,
    PROGRESS
}

/**
 * Kaimon Ana Ekranı (`Views/Home/HomeView.swift` karşılığı).
 *
 * İçerdiği Bölümler:
 * 1. Üst Bar: PRO butonu, aktif seri rozeti, hesap/profil butonu ve uygulama başlığı (`開門` / `Kaimon`).
 * 2. 7 Günlük Seri Şeridi (`StreakStrip` - `UserProgress.didStudy(daysAgo)`).
 * 3. "Kaldığın Yer" Kartı (`ResumeCard` - `LearningSessionState` içinde en son açılan tamamlanmamış oturum).
 * 4. "Konuşma Pratiği" Bölümü: Sohbet Pratiği (Senaryolar) ve AI ile Sohbet kartları.
 * 5. "Tekrar & İlerleme" Bölümü: Bekleyen SRS tekrar sayısı ile `ReviewListScreen` ve `ProgressScreen` kartları.
 * 6. "Öğren" Bölümü: Hiragana, Katakana, Kanji, Kelime, Gramer, Okuma ilerleme çubuklu özet kartları.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
    onNavigateToTab: (MainTab) -> Unit = {},
    onResumeSession: ((ResumeTarget) -> Unit)? = null,
    onOpenPaywall: () -> Unit = {},
    onOpenAccount: () -> Unit = {},
    onOpenScenarios: () -> Unit = {},
    onOpenAiChat: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentDestination by rememberSaveable { mutableStateOf(HomeDestination.DASHBOARD) }

    LaunchedEffect(currentDestination) {
        if (currentDestination == HomeDestination.DASHBOARD) {
            viewModel.refresh()
        }
    }

    BackHandler(enabled = currentDestination != HomeDestination.DASHBOARD) {
        currentDestination = HomeDestination.DASHBOARD
    }

    when (currentDestination) {
        HomeDestination.REVIEW_LIST -> {
            ReviewListScreen(
                modifier = modifier,
                onBack = { currentDestination = HomeDestination.DASHBOARD }
            )
        }
        HomeDestination.PROGRESS -> {
            ProgressScreen(
                modifier = modifier,
                onBack = { currentDestination = HomeDestination.DASHBOARD }
            )
        }
        HomeDestination.DASHBOARD -> {
            HomeDashboardContent(
                uiState = uiState,
                onOpenPaywall = {
                    FeedbackManager.shared.playSelection()
                    onOpenPaywall()
                },
                onOpenAccount = {
                    FeedbackManager.shared.playSelection()
                    onOpenAccount()
                },
                onResumeClick = { target ->
                    FeedbackManager.shared.playSelection()
                    if (onResumeSession != null) {
                        onResumeSession(target)
                    } else {
                        when (target.module) {
                            is ResumeTarget.Module.Hiragana,
                            is ResumeTarget.Module.Katakana,
                            is ResumeTarget.Module.Kanji -> onNavigateToTab(MainTab.ALPHABET)
                            is ResumeTarget.Module.Vocabulary -> onNavigateToTab(MainTab.VOCABULARY)
                        }
                    }
                },
                onOpenReviewList = {
                    FeedbackManager.shared.playSelection()
                    currentDestination = HomeDestination.REVIEW_LIST
                },
                onOpenProgress = {
                    FeedbackManager.shared.playSelection()
                    currentDestination = HomeDestination.PROGRESS
                },
                onOpenScenarios = {
                    FeedbackManager.shared.playSelection()
                    onOpenScenarios()
                },
                onOpenAiChat = {
                    FeedbackManager.shared.playSelection()
                    onOpenAiChat()
                },
                onNavigateToTab = { tab ->
                    FeedbackManager.shared.playSelection()
                    onNavigateToTab(tab)
                },
                modifier = modifier
            )
        }
    }
}

@Composable
fun HomeDashboardContent(
    uiState: HomeUiState,
    onOpenPaywall: () -> Unit,
    onOpenAccount: () -> Unit,
    onResumeClick: (ResumeTarget) -> Unit,
    onOpenReviewList: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenScenarios: () -> Unit,
    onOpenAiChat: () -> Unit,
    onNavigateToTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // 1. Üst Toolbar + Başlık
        HomeTopToolbar(
            activeStreak = uiState.activeStreak,
            isPro = uiState.isPro,
            hasAccount = uiState.hasAccount,
            onOpenPaywall = onOpenPaywall,
            onOpenAccount = onOpenAccount
        )

        HomeHeader()

        // 2. 7 Günlük Seri Şeridi (son 7 gün: 6 gün önce -> bugün)
        StreakStrip(
            streak = uiState.activeStreak,
            studiedDaysAgo = { daysAgo -> uiState.didStudy(daysAgo) }
        )

        // 3. "Kaldığın Yer" Kartı (varsa)
        uiState.resumeTarget?.let { target ->
            ResumeCard(
                target = target,
                onClick = { onResumeClick(target) }
            )
        }

        // 4. Konuşma Pratiği Bölümü
        PracticeSection(
            onOpenScenarios = onOpenScenarios,
            onOpenAiChat = onOpenAiChat
        )

        // 5. Tekrar & İlerleme Bölümü
        ReviewAndProgressSection(
            reviewWaitingCount = uiState.reviewWaitingCount,
            onOpenReviewList = onOpenReviewList,
            onOpenProgress = onOpenProgress
        )

        // 6. Öğren Bölümü (Modül Özeti Kartları)
        LearnModulesSection(
            uiState = uiState,
            onNavigateToTab = onNavigateToTab
        )
    }
}

@Composable
private fun HomeTopToolbar(
    activeStreak: Int,
    isPro: Boolean,
    hasAccount: Boolean,
    onOpenPaywall: () -> Unit,
    onOpenAccount: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Sol: PRO Butonu (iOS HomeView ToolbarItem .topBarLeading)
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onOpenPaywall)
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = "PRO",
                tint = Theme.accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "PRO",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Theme.accent
            )
        }

        // Sağ: Aktif Seri Rozeti + Hesap/Profil Butonu (iOS HomeView ToolbarItem .topBarTrailing)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .background(
                        color = if (activeStreak > 0) Theme.accent.copy(alpha = 0.12f) else Theme.ink.copy(alpha = 0.06f),
                        shape = CircleShape
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalFireDepartment,
                    contentDescription = null,
                    tint = if (activeStreak > 0) Theme.accent else Theme.secondaryInk,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "$activeStreak",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (activeStreak > 0) Theme.accent else Theme.secondaryInk
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenAccount),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasAccount) Icons.Filled.VerifiedUser else Icons.Filled.AccountCircle,
                    contentDescription = L10n.accountTitle,
                    tint = Theme.ink,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "開門",
            style = Theme.display(56.sp),
            color = Theme.accent
        )
        Text(
            text = "Kaimon",
            style = Theme.heading(22.sp),
            color = Theme.ink
        )
    }
}

/**
 * iOS `HomeView.resumeCard(_:)` bileşeninin birebir karşılığı.
 */
@Composable
fun ResumeCard(
    target: ResumeTarget,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .inkBordered(lineWidth = 3.dp)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = L10n.resumeLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Theme.secondaryInk
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = target.title,
                style = Theme.heading(20.sp),
                color = Theme.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${target.done}/${target.total}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.secondaryInk
            )
        }

        ProgressBar(fraction = target.fraction)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .background(color = Theme.ink)
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = L10n.resumeButton,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.paper,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PracticeSection(
    onOpenScenarios: () -> Unit,
    onOpenAiChat: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(text = L10n.sectionPractice)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            UtilityCard(
                title = L10n.scenarioPractice,
                subtitle = null,
                icon = Icons.Filled.Groups,
                isHighlighted = false,
                onClick = onOpenScenarios
            )

            UtilityCard(
                title = L10n.aiChat,
                subtitle = null,
                icon = Icons.Filled.ChatBubble,
                isHighlighted = false,
                onClick = onOpenAiChat
            )
        }
    }
}

@Composable
private fun ReviewAndProgressSection(
    reviewWaitingCount: Int,
    onOpenReviewList: () -> Unit,
    onOpenProgress: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(text = L10n.sectionReview)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            UtilityCard(
                title = L10n.reviewTitle,
                subtitle = if (reviewWaitingCount == 0) {
                    L10n.reviewNoItems
                } else {
                    L10n.reviewItemsWaiting(reviewWaitingCount)
                },
                icon = Icons.Filled.Refresh,
                isHighlighted = reviewWaitingCount > 0,
                onClick = onOpenReviewList
            )

            UtilityCard(
                title = L10n.progressTitle,
                subtitle = L10n.progressSubtitle,
                icon = Icons.Filled.BarChart,
                isHighlighted = false,
                onClick = onOpenProgress
            )
        }
    }
}

@Composable
private fun LearnModulesSection(
    uiState: HomeUiState,
    onNavigateToTab: (MainTab) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel(text = L10n.sectionLearn)

        // Satır 1: Hiragana & Katakana
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModuleCard(
                kind = LearnableItemKind.HIRAGANA_CHARACTER,
                subtitle = "あいうえお",
                learned = uiState.learnedCount(LearnableItemKind.HIRAGANA_CHARACTER),
                onClick = { onNavigateToTab(MainTab.ALPHABET) },
                modifier = Modifier.weight(1f)
            )
            ModuleCard(
                kind = LearnableItemKind.KATAKANA_CHARACTER,
                subtitle = "アイウエオ",
                learned = uiState.learnedCount(LearnableItemKind.KATAKANA_CHARACTER),
                onClick = { onNavigateToTab(MainTab.ALPHABET) },
                modifier = Modifier.weight(1f)
            )
        }

        // Satır 2: Kanji & Kelime
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModuleCard(
                kind = LearnableItemKind.KANJI,
                subtitle = L10n.kanjiModuleSubtitle(4),
                learned = uiState.learnedCount(LearnableItemKind.KANJI),
                onClick = { onNavigateToTab(MainTab.ALPHABET) },
                modifier = Modifier.weight(1f)
            )
            ModuleCard(
                kind = LearnableItemKind.VOCABULARY_WORD,
                subtitle = L10n.vocabModuleSubtitle(27),
                learned = uiState.learnedCount(LearnableItemKind.VOCABULARY_WORD),
                onClick = { onNavigateToTab(MainTab.VOCABULARY) },
                modifier = Modifier.weight(1f)
            )
        }

        // Satır 3: Gramer & Okuma
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModuleCard(
                kind = LearnableItemKind.GRAMMAR,
                subtitle = L10n.grammarModuleSubtitle(5),
                learned = uiState.learnedCount(LearnableItemKind.GRAMMAR),
                onClick = { onNavigateToTab(MainTab.GRAMMAR) },
                modifier = Modifier.weight(1f)
            )
            ReadingModuleSummaryCard(
                storiesCount = uiState.totalStoriesCount,
                onClick = { onNavigateToTab(MainTab.READING) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ReadingModuleSummaryCard(
    storiesCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val total = storiesCount.coerceAtLeast(1)

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .inkBordered()
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "読",
                style = Theme.display(34.sp),
                color = Theme.accent
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CallMade,
                contentDescription = null,
                tint = Theme.ink,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = L10n.readingTitle,
                style = Theme.heading(17.sp),
                color = Theme.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = L10n.readingModuleSubtitle(storiesCount),
                fontSize = 11.sp,
                color = Theme.secondaryInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "$storiesCount/$total",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Theme.secondaryInk
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        ProgressBar(fraction = if (storiesCount > 0) 1.0 else 0.0)
    }
}
