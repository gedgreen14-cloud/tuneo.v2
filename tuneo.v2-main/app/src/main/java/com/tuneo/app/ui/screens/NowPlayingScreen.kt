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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuneo.app.data.PlaylistRepository
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

    // Bouton "Aimer" : alimente la playlist automatique "Chansons aimées".
    // Recalculé à chaque changement de morceau puisque l'état aimé est par morceau.
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { PlaylistRepository(context) }
    var isLiked by remember(song.id) { mutableStateOf(repository.getLikedSongIds().contains(song.id)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        // Barre du haut : card "Partager avec mes amis" façon iOS, alignée à droite
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(accentColor.copy(alpha = 0.12f))
                    .clickable(onClick = onShareClick)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Partager avec mes amis",
                    color = accentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
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
                .aspectRatio(0.85f)
                .clip(RoundedCornerShape(28.dp))
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
                IconButton(onClick = {
                    repository.toggleLiked(song.id)
                    isLiked = !isLiked
                }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isLiked) "Retirer des chansons aimées" else "Aimer",
                        tint = if (isLiked) accentColor else secondaryColor
                    )
                }
                IconButton(onClick = { /* menu - à venir */ }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Plus d'options",
                        tint = secondaryColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Barre de progression
        val durationMs = song.duration.coerceAtLeast(1L)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(18.dp)
                    .background(accentColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                modifier = Modifier.weight(1f),
                value = positionMs.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..durationMs.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = accentColor.copy(alpha = 0.25f)
                )
            )
        }
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
                imageVector = Icons.Outlined.Tune,
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
