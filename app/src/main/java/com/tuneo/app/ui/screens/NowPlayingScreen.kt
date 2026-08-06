package com.tuneo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuneo.app.data.Song
import com.tuneo.app.ui.theme.contentColorFor
import com.tuneo.app.ui.theme.rememberDominantColor
import java.util.concurrent.TimeUnit

@Composable
fun NowPlayingScreen(
    song: Song,
    isPlaying: Boolean,
    positionMs: Long,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onShareClick: () -> Unit = {}
) {
    val backgroundColor = rememberDominantColor(song.albumArtUri)
    val accentColor = contentColorFor(backgroundColor)
    val secondaryColor = accentColor.copy(alpha = 0.7f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        // Barre du haut : bouton "Partager ce que j'écoute" à droite
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Partager ce que j'écoute",
                    tint = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Pochette
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Titre + artiste + actions
        Text(
            text = song.title,
            color = accentColor,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = song.artist,
                color = secondaryColor,
                fontSize = 16.sp
            )
            Row {
                IconButton(onClick = { /* favori - à venir */ }) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = "Aimer",
                        tint = secondaryColor
                    )
                }
                IconButton(onClick = { /* menu - à venir */ }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Plus d'options",
                        tint = secondaryColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Barre de progression
        val durationMs = song.duration.coerceAtLeast(1L)
        Slider(
            value = positionMs.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..durationMs.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = accentColor.copy(alpha = 0.25f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatDuration(positionMs), color = secondaryColor, fontSize = 12.sp)
            Text(formatDuration(durationMs), color = secondaryColor, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Contrôles principaux
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val prevInteraction = remember { MutableInteractionSource() }
            Icon(
                imageVector = Icons.Default.FastRewind,
                contentDescription = "Précédent",
                tint = accentColor,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(interactionSource = prevInteraction, indication = null, onClick = onPrevious)
            )
            val playInteraction = remember { MutableInteractionSource() }
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Lecture / Pause",
                tint = accentColor,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(interactionSource = playInteraction, indication = null, onClick = onTogglePlay)
            )
            val nextInteraction = remember { MutableInteractionSource() }
            Icon(
                imageVector = Icons.Default.FastForward,
                contentDescription = "Suivant",
                tint = accentColor,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(interactionSource = nextInteraction, indication = null, onClick = onNext)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Ligne du bas : égaliseur / Paroles / queue
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Égaliseur",
                tint = secondaryColor
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(accentColor.copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = secondaryColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Paroles", color = secondaryColor, fontSize = 14.sp)
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.List,
                contentDescription = "File d'attente",
                tint = secondaryColor
            )
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
