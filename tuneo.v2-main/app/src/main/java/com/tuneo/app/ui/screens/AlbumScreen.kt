package com.tuneo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuneo.app.data.Song
import com.tuneo.app.ui.components.AlbumArtThumbnail
import com.tuneo.app.ui.theme.TuneoBackgroundDark
import com.tuneo.app.ui.theme.TuneoBackgroundLight
import com.tuneo.app.ui.theme.TuneoTextPrimaryDark
import com.tuneo.app.ui.theme.TuneoTextPrimaryLight
import com.tuneo.app.ui.theme.TuneoTextSecondaryDark
import com.tuneo.app.ui.theme.TuneoTextSecondaryLight

private const val UNKNOWN_ALBUM_LABEL = "Album inconnu"

@Composable
fun AlbumScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (List<Song>, Int) -> Unit
) {
    var selectedAlbum by remember { mutableStateOf<String?>(null) }

    val grouped = remember(songs) {
        songs.groupBy { song ->
            val album = song.album.trim()
            if (album.isBlank()) UNKNOWN_ALBUM_LABEL else album
        }.toSortedMap(compareBy { if (it == UNKNOWN_ALBUM_LABEL) "\uFFFF" else it })
    }

    val album = selectedAlbum
    if (album != null) {
        val albumSongs = grouped[album].orEmpty()
        AlbumSongsScreen(
            albumName = album,
            songs = albumSongs,
            currentSong = currentSong,
            isPlaying = isPlaying,
            onBack = { selectedAlbum = null },
            onSongClick = { index -> onSongClick(albumSongs, index) }
        )
    } else {
        AlbumListScreen(
            grouped = grouped,
            onAlbumClick = { selectedAlbum = it }
        )
    }
}

@Composable
private fun AlbumListScreen(
    grouped: Map<String, List<Song>>,
    onAlbumClick: (String) -> Unit
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
        items(grouped.entries.toList()) { (albumName, albumSongs) ->
            // La miniature de l'album utilise la pochette détectée sur ses morceaux,
            // s'il y en a une (première trouvée dans le groupe).
            val artUri = albumSongs.firstOrNull { it.albumArtUri != null }?.albumArtUri

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAlbumClick(albumName) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlbumArtThumbnail(albumArtUri = artUri, thumbnailSize = 56.dp, cornerRadius = 14.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = albumName,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = albumSongs.firstOrNull()?.artist ?: "",
                        color = secondaryColor,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun AlbumSongsScreen(
    albumName: String,
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
                text = albumName,
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
