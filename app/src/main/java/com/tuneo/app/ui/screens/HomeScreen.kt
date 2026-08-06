package com.tuneo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuneo.app.data.Song
import com.tuneo.app.ui.theme.TuneoAccentBlue
import com.tuneo.app.ui.theme.TuneoBackgroundDark
import com.tuneo.app.ui.theme.TuneoBackgroundLight
import com.tuneo.app.ui.theme.TuneoTextSecondaryDark
import com.tuneo.app.ui.theme.TuneoTextSecondaryLight
import com.tuneo.app.ui.theme.TuneoTextPrimaryDark
import com.tuneo.app.ui.theme.TuneoTextPrimaryLight

@Composable
fun TuneoHeader() {
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) TuneoBackgroundDark else TuneoBackgroundLight
    val textColor = if (isDark) TuneoTextPrimaryDark else TuneoTextPrimaryLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Tuneo",
            color = textColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Search, contentDescription = "Rechercher", tint = textColor)
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.SwapVert, contentDescription = "Trier", tint = textColor)
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.GpsFixed, contentDescription = "Cible", tint = textColor)
            }
        }
    }
}

@Composable
fun SongListScreen(
    songs: List<Song>,
    currentSong: Song? = null,
    isPlaying: Boolean = false,
    onSongClick: (Int) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) TuneoBackgroundDark else TuneoBackgroundLight

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        items(songs.size) { index ->
            val song = songs[index]
            val isCurrent = currentSong != null && song.id == currentSong.id
            SongRow(
                song = song,
                isCurrent = isCurrent,
                isPlaying = isCurrent && isPlaying,
                onClick = { onSongClick(index) }
            )
        }
        item { Spacer(modifier = Modifier.height(80.dp)) } // place pour le mini-player
    }
}

@Composable
private fun SongRow(song: Song, isCurrent: Boolean, isPlaying: Boolean, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val defaultTextColor = if (isDark) TuneoTextPrimaryDark else TuneoTextPrimaryLight
    val defaultSecondaryColor = if (isDark) TuneoTextSecondaryDark else TuneoTextSecondaryLight
    val textColor = if (isCurrent) TuneoAccentBlue else defaultTextColor
    val secondaryColor = if (isCurrent) TuneoAccentBlue.copy(alpha = 0.8f) else defaultSecondaryColor

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = song.albumArtUri,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song.hasLyrics) {
                    Text(
                        text = "PAROLES",
                        color = secondaryColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                Text(
                    text = song.artist,
                    color = secondaryColor,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (isCurrent) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Equalizer,
                contentDescription = if (isPlaying) "En cours de lecture" else "En pause",
                tint = TuneoAccentBlue,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
