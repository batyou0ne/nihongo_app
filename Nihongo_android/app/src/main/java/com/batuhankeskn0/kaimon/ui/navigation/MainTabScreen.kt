package com.batuhankeskn0.kaimon.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.batuhankeskn0.kaimon.data.local.JLPTLevel
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.service.AudioService
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.domain.engine.LevelProgressionService
import com.batuhankeskn0.kaimon.domain.engine.TimeTrackingService
import com.batuhankeskn0.kaimon.ui.components.FuriganaText
import com.batuhankeskn0.kaimon.ui.components.LevelSelectionRow
import com.batuhankeskn0.kaimon.ui.components.LevelSelectorBar
import com.batuhankeskn0.kaimon.ui.components.LockedLevelInfoSheet
import com.batuhankeskn0.kaimon.ui.components.ModuleCard
import com.batuhankeskn0.kaimon.ui.components.SectionLabel
import com.batuhankeskn0.kaimon.ui.components.StreakStrip
import com.batuhankeskn0.kaimon.ui.components.UtilityCard
import com.batuhankeskn0.kaimon.data.model.CharacterType
import com.batuhankeskn0.kaimon.ui.screens.auth.AccountScreen
import com.batuhankeskn0.kaimon.ui.screens.auth.SignInScreen
import com.batuhankeskn0.kaimon.ui.screens.subscription.PaywallScreen
import com.batuhankeskn0.kaimon.ui.screens.alphabet.AlphabetDestination
import com.batuhankeskn0.kaimon.ui.screens.alphabet.AlphabetMainScreen
import com.batuhankeskn0.kaimon.ui.screens.dictionary.DictionaryScreen
import com.batuhankeskn0.kaimon.ui.screens.grammar.GrammarMainScreen
import com.batuhankeskn0.kaimon.ui.screens.home.HomeScreen
import com.batuhankeskn0.kaimon.ui.screens.home.ResumeTarget
import com.batuhankeskn0.kaimon.ui.screens.reading.ReadingMainScreen
import com.batuhankeskn0.kaimon.ui.screens.vocabulary.VocabularyDestination
import com.batuhankeskn0.kaimon.ui.screens.vocabulary.VocabularyMainScreen
import com.batuhankeskn0.kaimon.ui.theme.Theme
import com.batuhankeskn0.kaimon.ui.theme.inkBordered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Kaimon Ana Sekmeleri (iOS `MainTabView.swift` içindeki 6 sekme sırasıyla):
 * 1. Ana (`HOME`)
 * 2. Alfabe `あ` (`ALPHABET`)
 * 3. Gramer (`GRAMMAR`)
 * 4. Kelime (`VOCABULARY`)
 * 5. Okuma (`READING`)
 * 6. Sözlük (`DICTIONARY`)
 */
enum class MainTab(
    val index: Int,
    val icon: ImageVector? = null,
    val textIcon: String? = null
) {
    HOME(index = 0, icon = Icons.Filled.Home),
    ALPHABET(index = 1, textIcon = "あ"),
    GRAMMAR(index = 2, icon = Icons.AutoMirrored.Filled.Article),
    VOCABULARY(index = 3, icon = Icons.Filled.Book),
    READING(index = 4, icon = Icons.AutoMirrored.Filled.MenuBook),
    DICTIONARY(index = 5, icon = Icons.Filled.Search);

    val title: String
        get() = when (this) {
            HOME -> L10n.tabHome
            ALPHABET -> L10n.tabAlphabet
            GRAMMAR -> L10n.tabGrammar
            VOCABULARY -> L10n.tabVocabulary
            READING -> L10n.tabReading
            DICTIONARY -> L10n.tabDictionary
        }

    companion object {
        fun fromIndex(index: Int): MainTab =
            entries.firstOrNull { it.index == index } ?: HOME
    }
}

/**
 * Kaimon Ana Navigasyon Kabuğu ve Yüzen Alt Sekme Çubuğu (`FloatingTabBar`).
 * iOS `MainTabView.swift` bileşeninin birebir Jetpack Compose karşılığıdır.
 */
@Composable
fun MainTabScreen(
    modifier: Modifier = Modifier,
    tabBarManager: TabBarManager = TabBarManager.shared,
    homeContent: (@Composable () -> Unit)? = null,
    alphabetContent: (@Composable () -> Unit)? = null,
    grammarContent: (@Composable () -> Unit)? = null,
    vocabularyContent: (@Composable () -> Unit)? = null,
    readingContent: (@Composable () -> Unit)? = null,
    dictionaryContent: (@Composable () -> Unit)? = null
) {
    var selectedTabIndex by rememberSaveable { mutableStateOf(MainTab.HOME.index) }
    var alphabetInitialDestination by remember { mutableStateOf<AlphabetDestination>(AlphabetDestination.Root) }
    var vocabularyInitialDestination by remember { mutableStateOf<VocabularyDestination>(VocabularyDestination.Root) }
    var showPaywallSheet by rememberSaveable { mutableStateOf(false) }
    var showAccountSheet by rememberSaveable { mutableStateOf(false) }
    var showSignInSheet by rememberSaveable { mutableStateOf(false) }
    val selectedTab = MainTab.fromIndex(selectedTabIndex)
    val lifecycleOwner = LocalLifecycleOwner.current
    val ioScope = rememberCoroutineScope()

    // iOS MainTabView.onChange(of: scenePhase) -> TimeTrackingService bağlantısı
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    TimeTrackingService.shared.appDidBecomeActive()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    ioScope.launch(Dispatchers.IO) {
                        runCatching {
                            TimeTrackingService.shared.appWillResignActive()
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    CompositionLocalProvider(LocalTabBarManager provides tabBarManager) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Theme.paper)
                .statusBarsPadding()
        ) {
            // Sekme İçerikleri
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    MainTab.HOME -> {
                        if (homeContent != null) {
                            homeContent()
                        } else {
                            HomeScreen(
                                onNavigateToTab = { tab ->
                                    alphabetInitialDestination = AlphabetDestination.Root
                                    vocabularyInitialDestination = VocabularyDestination.Root
                                    selectedTabIndex = tab.index
                                },
                                onOpenPaywall = { showPaywallSheet = true },
                                onOpenAccount = { showAccountSheet = true },
                                onResumeSession = { target ->
                                    when (val mod = target.module) {
                                        is ResumeTarget.Module.Hiragana -> {
                                            alphabetInitialDestination = AlphabetDestination.Learning(CharacterType.HIRAGANA)
                                            selectedTabIndex = MainTab.ALPHABET.index
                                        }
                                        is ResumeTarget.Module.Katakana -> {
                                            alphabetInitialDestination = AlphabetDestination.Learning(CharacterType.KATAKANA)
                                            selectedTabIndex = MainTab.ALPHABET.index
                                        }
                                        is ResumeTarget.Module.Kanji -> {
                                            alphabetInitialDestination = AlphabetDestination.KanjiSession(
                                                level = mod.level,
                                                partIndex = mod.index
                                            )
                                            selectedTabIndex = MainTab.ALPHABET.index
                                        }
                                        is ResumeTarget.Module.Vocabulary -> {
                                            vocabularyInitialDestination = VocabularyDestination.PartSession(
                                                level = mod.level,
                                                partIndex = mod.index
                                            )
                                            selectedTabIndex = MainTab.VOCABULARY.index
                                        }
                                    }
                                }
                            )
                        }
                    }
                    MainTab.ALPHABET -> {
                        if (alphabetContent != null) {
                            alphabetContent()
                        } else {
                            AlphabetMainScreen(
                                initialDestination = alphabetInitialDestination
                            )
                        }
                    }
                    MainTab.GRAMMAR -> {
                        if (grammarContent != null) {
                            grammarContent()
                        } else {
                            GrammarMainScreen(onOpenPaywall = { showPaywallSheet = true })
                        }
                    }
                    MainTab.VOCABULARY -> {
                        if (vocabularyContent != null) {
                            vocabularyContent()
                        } else {
                            VocabularyMainScreen(
                                initialDestination = vocabularyInitialDestination,
                                onOpenPaywall = { showPaywallSheet = true }
                            )
                        }
                    }
                    MainTab.READING -> {
                        if (readingContent != null) {
                            readingContent()
                        } else {
                            ReadingMainScreen(onOpenPaywall = { showPaywallSheet = true })
                        }
                    }
                    MainTab.DICTIONARY -> {
                        if (dictionaryContent != null) {
                            dictionaryContent()
                        } else {
                            DictionaryScreen()
                        }
                    }
                }
            }

            // Yüzen Alt Sekme Çubuğu (Floating Tab Bar)
            AnimatedVisibility(
                visible = !tabBarManager.isHidden,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                    initialOffsetY = { it }
                ) + fadeIn(),
                exit = slideOutVertically(
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                    targetOffsetY = { it }
                ) + fadeOut()
            ) {
                FloatingTabBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        FeedbackManager.shared.playSelection()
                        selectedTabIndex = tab.index
                    }
                )
            }

            // Faz 10: Pro Paywall, Profil/Hesap ve Giriş/Kayıt Modal Ekranları
            if (showPaywallSheet) {
                PaywallScreen(
                    onDismiss = { showPaywallSheet = false }
                )
            }

            if (showAccountSheet) {
                AccountScreen(
                    onDismiss = { showAccountSheet = false },
                    onOpenSignIn = {
                        showAccountSheet = false
                        showSignInSheet = true
                    },
                    onOpenPaywall = {
                        showAccountSheet = false
                        showPaywallSheet = true
                    }
                )
            }

            if (showSignInSheet) {
                SignInScreen(
                    onDismiss = { showSignInSheet = false },
                    onSignedIn = {
                        showSignInSheet = false
                    }
                )
            }
        }
    }
}

/**
 * iOS `MainTabView.floatingTabBar` bileşeninin birebir karşılığı:
 * `RoundedCornerShape(30.dp)`, `Theme.paper` zemin, `1.5.dp` `Theme.ink` kenarlık, gölge ve 6 sekme.
 */
@Composable
fun FloatingTabBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val barShape = RoundedCornerShape(30.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = barShape,
                    ambientColor = Theme.ink.copy(alpha = 0.15f),
                    spotColor = Theme.ink.copy(alpha = 0.15f)
                )
                .clip(barShape)
                .background(color = Theme.paper, shape = barShape)
                .border(width = 1.5.dp, color = Theme.ink, shape = barShape)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val tintColor = if (isSelected) Theme.accent else Theme.secondaryInk
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onTabSelected(tab) }
                        )
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (tab.textIcon != null) {
                        Text(
                            text = tab.textIcon,
                            fontSize = 22.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = tintColor
                        )
                    } else if (tab.icon != null) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = tintColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Faz 4 Kabuk İçerikleri (Faz 5-8 Tam Ekranları Bağlanana Kadar Varsayılan Görünümler)

@Composable
private fun DefaultHomeShellTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
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

        StreakStrip(
            streak = 0,
            studiedDaysAgo = { false }
        )

        // Furigana & Ses Motoru Önizleme Kartı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .inkBordered()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FuriganaText(
                text = "日本語を勉強します",
                style = Theme.heading(20.sp),
                color = Theme.ink,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable {
                        AudioService.shared.speak("あ")
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = Theme.accent,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel(text = L10n.sectionReview)
            UtilityCard(
                title = L10n.reviewTitle,
                subtitle = L10n.reviewNoItems,
                icon = Icons.Filled.Refresh,
                isHighlighted = false
            )
        }
    }
}

@Composable
private fun DefaultAlphabetShellTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
            .padding(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = L10n.alphabetTitle,
            style = Theme.display(36.sp),
            color = Theme.ink
        )
        SectionLabel(text = L10n.sectionLearn)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModuleCard(
                kind = LearnableItemKind.HIRAGANA_CHARACTER,
                subtitle = "あいうえお",
                learned = 0,
                modifier = Modifier.weight(1f)
            )
            ModuleCard(
                kind = LearnableItemKind.KATAKANA_CHARACTER,
                subtitle = "アイウエオ",
                learned = 0,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ModuleCard(
                kind = LearnableItemKind.KANJI,
                subtitle = "N5 · 4 bölüm",
                learned = 0,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DefaultLevelModuleShellTab(
    title: String,
    levelTitle: (JLPTLevel) -> String,
    levelSubtitle: (JLPTLevel) -> String,
    icon: ImageVector
) {
    var selectedLockedLevel by remember { mutableStateOf<JLPTLevel?>(null) }
    val unlockedLevels by produceState(initialValue = setOf(JLPTLevel.N5)) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val records = LevelProgressionService.shared.ensureInitialProgress()
                JLPTLevel.allCases.filter {
                    LevelProgressionService.shared.isLevelUnlocked(it, records)
                }.toSet()
            }.getOrDefault(setOf(JLPTLevel.N5))
        }
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
            text = title,
            style = Theme.display(36.sp),
            color = Theme.ink
        )

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            JLPTLevel.allCases.forEach { level ->
                val isUnlocked = unlockedLevels.contains(level)
                LevelSelectionRow(
                    level = level,
                    title = levelTitle(level),
                    unlockedSubtitle = levelSubtitle(level),
                    isUnlocked = isUnlocked,
                    icon = icon,
                    onClick = {
                        if (!isUnlocked) {
                            selectedLockedLevel = level
                        }
                    }
                )
            }
        }
    }

    selectedLockedLevel?.let { locked ->
        LockedLevelInfoSheet(
            level = locked,
            onDismiss = { selectedLockedLevel = null },
            onStartTestOut = { selectedLockedLevel = null }
        )
    }
}

@Composable
private fun DefaultReadingShellTab() {
    var selectedLevel by remember { mutableStateOf(JLPTLevel.N5) }
    var selectedLockedLevel by remember { mutableStateOf<JLPTLevel?>(null) }
    val unlockedLevels by produceState(initialValue = setOf(JLPTLevel.N5)) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val records = LevelProgressionService.shared.ensureInitialProgress()
                JLPTLevel.allCases.filter {
                    LevelProgressionService.shared.isLevelUnlocked(it, records)
                }.toSet()
            }.getOrDefault(setOf(JLPTLevel.N5))
        }
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
            text = L10n.readingTitle,
            style = Theme.display(36.sp),
            color = Theme.ink
        )

        LevelSelectorBar(
            selectedLevel = selectedLevel,
            isLevelUnlocked = { unlockedLevels.contains(it) },
            onLevelSelected = { selectedLevel = it },
            onLockedLevelClicked = { selectedLockedLevel = it }
        )
    }

    selectedLockedLevel?.let { locked ->
        LockedLevelInfoSheet(
            level = locked,
            onDismiss = { selectedLockedLevel = null },
            onStartTestOut = { selectedLockedLevel = null }
        )
    }
}

@Composable
private fun DefaultDictionaryShellTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .padding(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = L10n.dictionaryTitle,
            style = Theme.display(36.sp),
            color = Theme.ink
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
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
            Text(
                text = L10n.dictionarySearch,
                fontSize = 16.sp,
                color = Theme.secondaryInk
            )


        }
    }
}
