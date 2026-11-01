package com.batuhankeskn0.kaimon.ui.screens.learning

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.batuhankeskn0.kaimon.data.local.LearnableItemKind
import com.batuhankeskn0.kaimon.data.model.CharacterType
import com.batuhankeskn0.kaimon.data.model.JapaneseCharacter
import com.batuhankeskn0.kaimon.data.repository.ContentStore
import com.batuhankeskn0.kaimon.ui.navigation.LocalTabBarManager
import com.batuhankeskn0.kaimon.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hiragana/Katakana öğrenme ekranı (`Views/Learning/LearningView.swift` karşılığı).
 * Karakterleri `ContentStore` üzerinden yükler, gerçek flashcard akışını
 * (tek tek gelen kartlar, 4 şık, oturum devamlılığı) `FlashcardSessionScreen`'e bırakır.
 */
@Composable
fun LearningScreen(
    characterType: CharacterType,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val tabBarManager = LocalTabBarManager.current

    DisposableEffect(tabBarManager) {
        tabBarManager.hide()
        onDispose {
            tabBarManager.show()
        }
    }

    val characters by produceState<List<JapaneseCharacter>>(
        initialValue = emptyList(),
        key1 = characterType
    ) {
        value = withContext(Dispatchers.IO) {
            ContentStore.loadCharacters(characterType)
        }
    }

    if (characters.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Theme.paper),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Theme.accent)
        }
    } else {
        FlashcardSessionScreen(
            sessionKey = characterType.rawValue,
            itemKind = if (characterType == CharacterType.HIRAGANA) {
                LearnableItemKind.HIRAGANA_CHARACTER
            } else {
                LearnableItemKind.KATAKANA_CHARACTER
            },
            allItems = characters,
            distractorPool = characters,
            accentColor = Theme.accent,
            title = characterType.displayName,
            onBack = onBack,
            modifier = modifier
        )
    }
}
