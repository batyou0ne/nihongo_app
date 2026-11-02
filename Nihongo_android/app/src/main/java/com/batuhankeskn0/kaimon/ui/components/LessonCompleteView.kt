package com.batuhankeskn0.kaimon.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.batuhankeskn0.kaimon.data.service.AdManager
import com.batuhankeskn0.kaimon.data.service.FeedbackManager
import com.batuhankeskn0.kaimon.data.service.L10n
import com.batuhankeskn0.kaimon.ui.theme.Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * "Zen Ring" başarı ve ders tamamlama ekranı:
 * Halka dolar, checkmark çizilir, hafif parçacıklar yayılır, XP sayılır, seri rozeti ve CTA belirir.
 * iOS `LessonCompleteView.swift` (323 satır) bileşeninin birebir Jetpack Compose karşılığıdır.
 */
@Composable
fun LessonCompleteView(
    xpGained: Int = 20,
    streak: Int = 7,
    streakIncreased: Boolean = true,
    title: String = L10n.lessonCompleteTitle,
    ctaTitle: String = L10n.continueButton,
    ringColor: Color = Theme.accent,
    modifier: Modifier = Modifier,
    onContinue: () -> Unit
) {
    val hostActivity = LocalContext.current.findActivity()
    val scrim = remember { Animatable(0f) }
    val trackProgress = remember { Animatable(0f) }
    val ringProgress = remember { Animatable(0f) }
    val checkProgress = remember { Animatable(0f) }
    val glowProgress = remember { Animatable(0f) }
    val breathScale = remember { Animatable(1f) }
    val burstProgress = remember { Animatable(0f) }
    val titleProgress = remember { Animatable(0f) }
    val xpValue = remember { Animatable(0f) }
    val badgeProgress = remember { Animatable(0f) }
    val ctaProgress = remember { Animatable(0f) }

    LaunchedEffect(xpGained, streak, streakIncreased) {
        // 0 ms: scrim + track
        launch {
            scrim.animateTo(1f, tween(200, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)))
        }
        launch {
            trackProgress.animateTo(1f, tween(200, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)))
        }

        // 100 -> 700 ms: halka dolar
        launch {
            delay(100)
            ringProgress.animateTo(1f, tween(600, easing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)))
        }

        // 560 -> 860 ms: checkmark çizilir
        launch {
            delay(560)
            checkProgress.animateTo(1f, tween(300, easing = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)))
        }

        // 700 ms: halka kapanış anı (haptic + glow + nefes + parçacıklar)
        launch {
            delay(700)
            FeedbackManager.shared.playSuccess()
            launch {
                glowProgress.animateTo(1f, tween(450, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
            }
            breathScale.animateTo(1.05f, tween(120))
            breathScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)
            )
        }

        launch {
            delay(720)
            burstProgress.animateTo(1f, tween(460, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
        }

        // 900 ms: tebrik metni
        launch {
            delay(900)
            titleProgress.animateTo(1f, tween(350, easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)))
        }

        // 1000 -> 1500 ms: XP sayacı + hafif tikler
        launch {
            delay(1000)
            xpValue.animateTo(
                targetValue = xpGained.toFloat(),
                animationSpec = tween(500, easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f))
            )
        }
        launch {
            delay(1100)
            FeedbackManager.shared.playSelection()
            delay(150)
            FeedbackManager.shared.playSelection()
            delay(150)
            FeedbackManager.shared.playSelection()
        }

        // 1100 ms: streak rozeti
        launch {
            delay(1100)
            badgeProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)
            )
        }
        if (streakIncreased) {
            launch {
                delay(1450)
                FeedbackManager.shared.playSuccess()
            }
        }

        // 1300 ms: CTA
        launch {
            delay(1300)
            ctaProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.paper)
            .statusBarsPadding()
            .navigationBarsPadding()
            .alpha(scrim.value)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Zen Ring + GlowPulse + ParticleBurst + Checkmark
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .padding(bottom = 0.dp),
                contentAlignment = Alignment.Center
            ) {
                ZenRingCanvas(
                    ringColor = ringColor,
                    trackProgress = trackProgress.value,
                    ringProgress = ringProgress.value,
                    checkProgress = checkProgress.value,
                    glowProgress = glowProgress.value,
                    breathScale = breathScale.value,
                    burstProgress = burstProgress.value
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Başlık
            Text(
                text = title,
                style = Theme.heading(22.sp),
                color = Theme.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(titleProgress.value)
                    .offset(y = ((1f - titleProgress.value) * 12f).dp)
                    .padding(bottom = 8.dp)
            )

            // Sayan XP Metni
            Text(
                text = "+${xpValue.value.roundToInt()} XP",
                style = Theme.display(36.sp),
                color = Theme.ink,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Seri Rozeti
            val badgeScale = 0.85f + 0.15f * badgeProgress.value
            Box(
                modifier = Modifier
                    .alpha(badgeProgress.value.coerceIn(0f, 1f))
                    .scale(badgeScale)
                    .background(
                        color = ringColor.copy(alpha = 0.14f),
                        shape = CircleShape
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = L10n.streakText(streak),
                    style = Theme.heading(14.sp),
                    color = ringColor
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // CTA Butonu
            val ctaAlpha = ctaProgress.value.coerceIn(0f, 1f)
            val ctaScale = 0.96f + 0.04f * ctaAlpha
            val ctaShape = RoundedCornerShape(16.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .height(54.dp)
                    .alpha(ctaAlpha)
                    .offset(y = ((1f - ctaAlpha) * 16f).dp)
                    .scale(ctaScale)
                    .clip(ctaShape)
                    .background(color = Theme.ink, shape = ctaShape)
                    .clickable(enabled = ctaAlpha > 0.5f) {
                        FeedbackManager.shared.playSelection()
                        // Pro olmayan kullanıcılarda politika uygunsa geçiş reklamı gösterilir; Pro'da doğrudan devam edilir.
                        if (hostActivity != null) {
                            AdManager.shared.onSessionCompleted(hostActivity) { onContinue() }
                        } else {
                            onContinue()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ctaTitle,
                    style = Theme.heading(17.sp),
                    color = Theme.paper
                )
            }
        }
    }
}

@Composable
private fun ZenRingCanvas(
    ringColor: Color,
    trackProgress: Float,
    ringProgress: Float,
    checkProgress: Float,
    glowProgress: Float,
    breathScale: Float,
    burstProgress: Float
) {
    val softColor = ringColor.copy(alpha = 0.35f)

    Canvas(
        modifier = Modifier
            .size(160.dp)
            .scale(breathScale)
    ) {
        val strokePx = 6.dp.toPx()
        val ringDiameter = size.minDimension - strokePx
        val topLeft = Offset(strokePx / 2f, strokePx / 2f)
        val arcSize = Size(ringDiameter, ringDiameter)
        val center = Offset(size.width / 2f, size.height / 2f)

        // 1) GlowPulse (dışa genişleyip kaybolan halka)
        if (glowProgress > 0f && glowProgress < 1f) {
            val glowAlpha = if (glowProgress < 0.4f) {
                (glowProgress / 0.4f) * 0.45f
            } else {
                0.45f * (1f - (glowProgress - 0.4f) / 0.6f)
            }
            val glowRadius = (size.minDimension / 2f) * (1f + 0.35f * glowProgress)
            drawCircle(
                color = ringColor.copy(alpha = glowAlpha.coerceIn(0f, 1f)),
                radius = glowRadius,
                center = center
            )
        }

        // 2) ParticleBurst (10 mikro parçacık)
        if (burstProgress > 0f && burstProgress < 1f) {
            val particleAlpha = if (burstProgress < 0.3f) {
                (burstProgress / 0.3f) * 0.9f
            } else {
                0.9f * (1f - (burstProgress - 0.3f) / 0.7f)
            }
            val particleCount = 10
            val baseRadius = 3.dp.toPx() * (1f - 0.7f * burstProgress).coerceAtLeast(0.1f)
            for (i in 0 until particleCount) {
                val jitter = if (i % 2 == 0) -0.14 else 0.14
                val angle = (i.toDouble() / particleCount.toDouble()) * PI * 2.0 + jitter
                val distDp = (64f + (i % 3) * 10f).dp.toPx()
                val px = center.x + (cos(angle) * distDp * burstProgress).toFloat()
                val py = center.y + (sin(angle) * distDp * burstProgress).toFloat()
                drawCircle(
                    color = softColor.copy(alpha = particleAlpha.coerceIn(0f, 1f)),
                    radius = baseRadius,
                    center = Offset(px, py)
                )
            }
        }

        // 3) Track halkası
        if (trackProgress > 0f) {
            drawArc(
                color = ringColor.copy(alpha = (0.15f * trackProgress).coerceIn(0f, 1f)),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )
        }

        // 4) İlerleme halkası
        if (ringProgress > 0f) {
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * ringProgress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        // 5) Checkmark
        if (checkProgress > 0f) {
            val w = size.width
            val h = size.height
            val fullPath = Path().apply {
                moveTo(w * 0.350f, h * 0.512f)
                lineTo(w * 0.450f, h * 0.612f)
                lineTo(w * 0.662f, h * 0.388f)
            }
            val measure = PathMeasure()
            measure.setPath(fullPath, false)
            val trimmedPath = Path()
            measure.getSegment(
                startDistance = 0f,
                stopDistance = measure.length * checkProgress.coerceIn(0f, 1f),
                destination = trimmedPath,
                startWithMoveTo = true
            )
            val checkAlpha = (checkProgress * 12f).coerceIn(0f, 1f)
            drawPath(
                path = trimmedPath,
                color = ringColor.copy(alpha = checkAlpha),
                style = Stroke(
                    width = strokePx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
