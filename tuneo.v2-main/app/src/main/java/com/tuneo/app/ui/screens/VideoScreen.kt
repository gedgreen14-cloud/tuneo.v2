package com.tuneo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuneo.app.data.VideoItem
import com.tuneo.app.ui.theme.TuneoBackgroundDark
import com.tuneo.app.ui.theme.TuneoBackgroundLight
import com.tuneo.app.ui.theme.TuneoTextPrimaryDark
import com.tuneo.app.ui.theme.TuneoTextPrimaryLight
import com.tuneo.app.ui.theme.TuneoTextSecondaryDark
import com.tuneo.app.ui.theme.TuneoTextSecondaryLight
import java.util.concurrent.TimeUnit

@Composable
fun VideoScreen(videos: List<VideoItem>, onVideoClick: (VideoItem) -> Unit) {
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) TuneoBackgroundDark else TuneoBackgroundLight
    val textColor = if (isDark) TuneoTextPrimaryDark else TuneoTextPrimaryLight

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${videos.size} vidéos",
                color = textColor,
                fontSize = 15.sp
            )
            Icon(Icons.Default.Folder, contentDescription = "Dossiers", tint = textColor)
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(videos.size) { index ->
                val video = videos[index]
                VideoThumbnail(video = video, onClick = { onVideoClick(video) })
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun VideoThumbnail(video: VideoItem, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) TuneoTextPrimaryDark else TuneoTextPrimaryLight
    val secondaryColor = if (isDark) TuneoTextSecondaryDark else TuneoTextSecondaryLight

    Column(
        modifier = Modifier.clickable { onClick() }
    ) {
        AsyncImage(
            model = video.thumbnailUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = video.displayName,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${video.resolutionLabel} | ${formatDuration(video.duration)}",
            color = secondaryColor,
            fontSize = 11.sp
        )
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
