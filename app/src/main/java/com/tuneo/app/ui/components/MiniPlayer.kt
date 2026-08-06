package com.tuneo.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.tuneo.app.data.Song

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    isShuffleEnabled: Boolean,
    repeatMode: Int,
    onTogglePlay: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onClick: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = song.title,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onToggleShuffle) {
            Icon(
                imageVector = Icons.Default.Shuffle,
                contentDescription = "Aléatoire",
                tint = if (isShuffleEnabled) contentColor else contentColor.copy(alpha = 0.4f)
            )
        }
        IconButton(onClick = onCycleRepeat) {
            Icon(
                imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                contentDescription = "Répéter",
                tint = if (repeatMode == Player.REPEAT_MODE_OFF) contentColor.copy(alpha = 0.4f) else contentColor
            )
        }
        IconButton(onClick = onTogglePlay) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Lecture",
                tint = contentColor
            )
        }
    }
}
