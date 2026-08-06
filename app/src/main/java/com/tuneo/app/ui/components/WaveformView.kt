package com.tuneo.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/**
 * Visualiseur d'ondes en pointillés colorés, animé en continu,
 * représentant "depuis la bibliothèque locale" (image 4) — sans timer ni barre de progression.
 */
@Composable
fun WaveformView(
    color: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 28
) {
    val heights = remember {
        List(barCount) { i ->
            val base = ((kotlin.math.sin(i * 0.9) + 1.0) / 2.0).toFloat()
            0.25f + 0.75f * base + Random(i).nextFloat() * 0.15f
        }
    }

    val transition = rememberInfiniteTransition(label = "waveform")
    val phase: Float by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    Canvas(modifier = modifier.width(96.dp).height(40.dp)) {
        val barWidth = size.width / (barCount * 1.6f)
        val gap = barWidth * 0.6f
        heights.forEachIndexed { index, baseHeight ->
            val wobbleRaw = kotlin.math.sin(((phase * 6.28f) + index * 0.5f).toDouble()).toFloat()
            val wobble = 0.55f + 0.45f * wobbleRaw
            val barHeight = (size.height * baseHeight * wobble).coerceIn(size.height * 0.12f, size.height)
            val x = index * (barWidth + gap)
            val alpha = 0.45f + 0.55f * baseHeight
            drawLine(
                color = color.copy(alpha = alpha),
                start = Offset(x, size.height / 2f - barHeight / 2f),
                end = Offset(x, size.height / 2f + barHeight / 2f),
                strokeWidth = barWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                pointMode = PointMode.Lines
            )
        }
    }
}
