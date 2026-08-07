package com.tuneo.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Icône d'égaliseur pour le morceau en cours de lecture dans SongList.
 * Anime 3 barres en boucle tant que isPlaying == true ; s'arrête net et
 * reste figée à mi-hauteur en pause. Seul le morceau actif doit utiliser
 * ce composant : les autres gardent l'icône Equalizer statique classique.
 */
@Composable
fun EqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    barWidth: Dp = 3.dp,
    maxHeight: Dp = 16.dp
) {
    if (!isPlaying) {
        // En pause : icône égaliseur statique classique, pas d'animation.
        Icon(
            imageVector = Icons.Default.Equalizer,
            contentDescription = "En pause",
            tint = color,
            modifier = modifier
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "equalizer")

    val bar1: Float by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 420, delayMillis = 0, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2: Float by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 560, delayMillis = 90, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3: Float by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 480, delayMillis = 180, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        val fractions = listOf(bar1, bar2, bar3)
        fractions.forEachIndexed { index, fraction ->
            if (index > 0) {
                Spacer(modifier = Modifier.width(2.dp))
            }
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(maxHeight * fraction)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}
