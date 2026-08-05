package com.tuneo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tuneo.app.data.Song
import com.tuneo.app.player.RepeatMode
import com.tuneo.app.ui.theme.MiniPlayerBackground
import com.tuneo.app.ui.theme.MiniPlayerText

@Composable
fun MiniPlayer(
    song: Song,
    isPlaying: Boolean,
    repeatMode: RepeatMode,
    onTogglePlay: () -> Unit,
    onToggleRepeatMode: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MiniPlayerBackground)
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
            color = MiniPlayerText,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Cycle : OFF -> REPEAT_ONE (🔂, badge "1") -> SHUFFLE (🔀) -> OFF
        IconButton(onClick = onToggleRepeatMode) {
            when (repeatMode) {
                RepeatMode.OFF -> Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Répétition désactivée",
                    tint = MiniPlayerText.copy(alpha = 0.5f)
                )
                RepeatMode.REPEAT_ONE -> Icon(
                    imageVector = Icons.Default.RepeatOne,
                    contentDescription = "Répéter la piste",
                    tint = MiniPlayerText
                )
                RepeatMode.SHUFFLE -> Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = "Lecture aléatoire",
                    tint = MiniPlayerText
                )
            }
        }

        IconButton(
            onClick = onTogglePlay,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.dp, MiniPlayerText, CircleShape)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Lecture",
                tint = MiniPlayerText
            )
        }
    }
}
