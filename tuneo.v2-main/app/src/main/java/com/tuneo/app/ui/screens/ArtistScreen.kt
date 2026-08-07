package com.tuneo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import com.tuneo.app.data.Song
import com.tuneo.app.ui.theme.TuneoBackgroundDark
import com.tuneo.app.ui.theme.TuneoBackgroundLight
import com.tuneo.app.ui.theme.TuneoTextPrimaryDark
import com.tuneo.app.ui.theme.TuneoTextPrimaryLight
import com.tuneo.app.ui.theme.TuneoTextSecondaryDark
import com.tuneo.app.ui.theme.TuneoTextSecondaryLight

private const val UNKNOWN_ARTIST_LABEL = "Artistes inconnus"

@Composable
fun ArtistScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (List<Song>, Int) -> Unit
) {
    var selectedArtist by remember { mutableStateOf<String?>(null) }

    val grouped = remember(songs) {
        songs.groupBy { song ->
            val artist = song.artist.trim()
            if (artist.isBlank() || artist.equals("<unknown>", ignoreCase = true)) {
                UNKNOWN_ARTIST_LABEL
            } else {
                artist
            }
        }.toSortedMap(compareBy { if (it == UNKNOWN_ARTIST_LABEL) "\uFFFF" else it })
    }

    val artist = selectedArtist
    if (artist != null) {
        val artistSongs = grouped[artist].orEmpty()
        ArtistSongsScreen(
            artistName = artist,
            songs = artistSongs,
            currentSong = currentSong,
            isPlaying = isPlaying,
            onBack = { selectedArtist = null },
            onSongClick = { index -> onSongClick(artistSongs, index) }
        )
    } else {
        ArtistListScreen(
            grouped = grouped,
            onArtistClick = { selectedArtist = it }
        )
    }
}

@Composable
private fun ArtistListScreen(
    grouped: Map<String, List<Song>>,
    onArtistClick: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) TuneoBackgroundDark else TuneoBackgroundLight
    val textColor = if (isDark) TuneoTextPrimaryDark else TuneoTextPrimaryLight
    val secondaryColor = if (isDark) TuneoTextSecondaryDark else TuneoTextSecondaryLight

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        items(grouped.entries.toList()) { (artistName, artistSongs) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onArtistClick(artistName) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(secondaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = secondaryColor
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artistName,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${artistSongs.size} morceau${if (artistSongs.size > 1) "x" else ""}",
                        color = secondaryColor,
                        fontSize = 13.sp
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun ArtistSongsScreen(
    artistName: String,
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onSongClick: (Int) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) TuneoBackgroundDark else TuneoBackgroundLight
    val textColor = if (isDark) TuneoTextPrimaryDark else TuneoTextPrimaryLight

    Column(modifier = Modifier.fillMaxSize().background(background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour", tint = textColor)
            }
            Text(
                text = artistName,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        SongListScreen(
            songs = songs,
            currentSong = currentSong,
            isPlaying = isPlaying,
            onSongClick = onSongClick
        )
    }
}
