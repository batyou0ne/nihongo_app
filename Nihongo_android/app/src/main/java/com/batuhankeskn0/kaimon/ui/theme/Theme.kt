package com.batuhankeskn0.kaimon.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Kaimon Brutalist renk şeması.
 * Açık ve koyu modda `paper`, `ink` ve `secondaryInk` değerlerini barındırır;
 * `accent` (vermilyon kırmızısı) her iki modda sabittir.
 */
@Immutable
data class KaimonColors(
    val accent: Color = KaimonAccentColor,
    val paper: Color,
    val ink: Color,
    val secondaryInk: Color,
    val success: Color = Color(0xFF34C759),
    val isDark: Boolean
)

/** Vermilyon: markanın tek vurgu rengi (`red: 0.92, green: 0.23, blue: 0.05` -> `0xFFEB3B0D`). */
val KaimonAccentColor = Color(0xFFEB3B0D)

val LightKaimonColors = KaimonColors(
    accent = KaimonAccentColor,
    paper = Color.White,
    ink = Color.Black,
    secondaryInk = Color(0xFF666666),
    isDark = false
)

val DarkKaimonColors = KaimonColors(
    accent = KaimonAccentColor,
    paper = Color(0xFF121212),
    ink = Color.White,
    secondaryInk = Color(0xFFA0A0A0),
    isDark = true
)

val LocalKaimonColors = staticCompositionLocalOf { LightKaimonColors }

private val LightMaterialColorScheme = lightColorScheme(
    primary = KaimonAccentColor,
    onPrimary = Color.White,
    background = LightKaimonColors.paper,
    onBackground = LightKaimonColors.ink,
    surface = LightKaimonColors.paper,
    onSurface = LightKaimonColors.ink,
    onSurfaceVariant = LightKaimonColors.secondaryInk
)

private val DarkMaterialColorScheme = darkColorScheme(
    primary = KaimonAccentColor,
    onPrimary = Color.White,
    background = DarkKaimonColors.paper,
    onBackground = DarkKaimonColors.ink,
    surface = DarkKaimonColors.paper,
    onSurface = DarkKaimonColors.ink,
    onSurfaceVariant = DarkKaimonColors.secondaryInk
)

/**
 * Uygulamanın tasarım sistemi: beyaz/koyu kağıt zemin, belirgin yuvarlatılmış tipografi
 * ve tek vurgu rengi olarak vermilyon kırmızısı (`0xFFEB3B0D`).
 * Hem `Theme.*` hem `KaimonTheme.*` üzerinden erişilebilir.
 */
object Theme {
    /** Vermilyon: markanın tek vurgu rengi (açık/koyu modda sabit). */
    val accent: Color = KaimonAccentColor

    /** Zemin rengi (açık modda `Color.White`, koyu modda `Color(0xFF121212)`). */
    val paper: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalKaimonColors.current.paper

    /** Ana metin ve kenarlık rengi (açık modda `Color.Black`, koyu modda `Color.White`). */
    val ink: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalKaimonColors.current.ink

    /** İkincil metin rengi (açık modda `0xFF666666`, koyu modda `0xFFA0A0A0`). */
    val secondaryInk: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalKaimonColors.current.secondaryInk

    /** Başarı/tamamlama durumları için semantik yeşil renk. */
    val success: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalKaimonColors.current.success

    /** Büyük başlıklar için kalın (`Bold`) ve yuvarlak (`SansSerif`) kesim. */
    fun display(size: TextUnit): TextStyle = TextStyle(
        fontSize = size,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif
    )

    fun display(size: Int): TextStyle = display(size.sp)

    fun display(size: Float): TextStyle = display(size.sp)

    /** Ara başlıklar için yarı kalın (`SemiBold`) ve yuvarlak (`SansSerif`) kesim. */
    fun heading(size: TextUnit): TextStyle = TextStyle(
        fontSize = size,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.SansSerif
    )

    fun heading(size: Int): TextStyle = heading(size.sp)

    fun heading(size: Float): TextStyle = heading(size.sp)
}

/**
 * `KaimonTheme` erişim nesnesi ve `@Composable` tema sarmalayıcısı.
 */
object KaimonTheme {
    val accent: Color = KaimonAccentColor

    val paper: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalKaimonColors.current.paper

    val ink: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalKaimonColors.current.ink

    val secondaryInk: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalKaimonColors.current.secondaryInk

    val success: Color
        @Composable
        @ReadOnlyComposable
        get() = LocalKaimonColors.current.success

    val colors: KaimonColors
        @Composable
        @ReadOnlyComposable
        get() = LocalKaimonColors.current

    fun display(size: TextUnit): TextStyle = Theme.display(size)
    fun display(size: Int): TextStyle = Theme.display(size)
    fun display(size: Float): TextStyle = Theme.display(size)

    fun heading(size: TextUnit): TextStyle = Theme.heading(size)
    fun heading(size: Int): TextStyle = Theme.heading(size)
    fun heading(size: Float): TextStyle = Theme.heading(size)
}

/**
 * Kaimon Brutalist Tema Sarmalayıcısı.
 */
@Composable
fun KaimonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val kaimonColors = if (darkTheme) DarkKaimonColors else LightKaimonColors
    val materialColorScheme = if (darkTheme) DarkMaterialColorScheme else LightMaterialColorScheme

    CompositionLocalProvider(
        LocalKaimonColors provides kaimonColors,
        LocalContentColor provides kaimonColors.ink
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content
        )
    }
}

/**
 * Yuvarlak köşeli (`16.dp`), `Theme.paper` dolgulu, `Theme.ink` kenarlıklı (`2.dp`) Brutalist kart/kutu görünümü.
 * iOS `InkBorder` / `.inkBordered(lineWidth:)` modifier'ının birebir karşılığıdır.
 */
fun Modifier.inkBordered(
    lineWidth: Dp = 2.dp,
    cornerRadius: Dp = 16.dp
): Modifier = composed {
    val shape = RoundedCornerShape(cornerRadius)
    this
        .background(color = Theme.paper, shape = shape)
        .clip(shape)
        .border(width = lineWidth, color = Theme.ink, shape = shape)
}

/**
 * Siyah (`Theme.ink`) zeminli, beyaz (`Theme.paper`) yazılı, `16.dp` yuvarlatılmış köşeli,
 * basıldığında `0.75f` opaklık veren birincil Brutalist buton bileşeni.
 * iOS `PrimaryButtonStyle` karşılığıdır.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = Theme.ink,
    contentColor: Color = Theme.paper
) {
    PrimaryButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        backgroundColor = backgroundColor,
        contentColor = contentColor
    ) {
        Text(
            text = text,
            style = Theme.display(17.sp),
            color = contentColor,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Özel içerik (`RowScope`) alan `PrimaryButton` varyantı.
 */
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = Theme.ink,
    contentColor: Color = Theme.paper,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(16.dp)
    val targetAlpha = when {
        !enabled -> 0.5f
        isPressed -> 0.75f
        else -> 1f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .alpha(targetAlpha)
            .clip(shape)
            .background(color = backgroundColor, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 16.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(value = Theme.display(17.sp)) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}
