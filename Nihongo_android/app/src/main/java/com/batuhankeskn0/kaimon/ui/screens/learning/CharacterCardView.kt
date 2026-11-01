package com.batuhankeskn0.kaimon.ui.screens.learning

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batuhankeskn0.kaimon.data.model.JapaneseCharacter
import com.batuhankeskn0.kaimon.data.service.AlphabetAudioService
import com.batuhankeskn0.kaimon.ui.theme.Theme

/**
 * Tek bir Hiragana/Katakana kartı (`Views/Learning/CharacterCardView.swift` karşılığı).
 * Öne Japonca karakteri büyük gösterir, dokununca 3B Y-ekseni flip animasyonuyla (`graphicsLayer { rotationY }`)
 * arkaya döner ve romaji + Türkçe okunuşu gösterir.
 */
@Composable
fun CharacterCardView(
    character: JapaneseCharacter,
    modifier: Modifier = Modifier,
    accentColor: Color = Theme.accent,
    playAudioOnFlip: Boolean = true
) {
    var isFlipped by remember(character.id) { mutableStateOf(false) }

    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "CharacterCardFlip"
    )

    val showingBack = rotationY > 90f

    Box(
        modifier = modifier
            .size(width = 160.dp, height = 200.dp)
            .graphicsLayer {
                this.rotationY = rotationY
                this.cameraDistance = 12f * density
            }
            .background(color = Theme.paper, shape = RectangleShape)
            .border(width = 2.dp, color = accentColor, shape = RectangleShape)
            .clickable {
                isFlipped = !isFlipped
                if (playAudioOnFlip) {
                    AlphabetAudioService.shared.play(character.character)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (!showingBack) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = character.character,
                    style = Theme.display(72.sp),
                    color = Theme.ink
                )
            }
        } else {
            Column(
                modifier = Modifier.graphicsLayer {
                    this.rotationY = 180f
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = character.romaji,
                    style = Theme.heading(28.sp),
                    color = Theme.ink
                )
                Text(
                    text = character.turkishPronunciation,
                    fontSize = 15.sp,
                    color = Theme.secondaryInk
                )
            }
        }
    }
}
