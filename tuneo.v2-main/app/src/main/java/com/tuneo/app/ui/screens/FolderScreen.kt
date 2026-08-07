package com.tuneo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
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

/**
 * Onglet Dossiers : regroupe les morceaux déjà scannés (MediaScanner, via MediaStore)
 * par leur dossier réel sur le stockage (Song.folderPath), affichés verticalement
 * avec leur vrai nom (dernier segment du chemin).
 */
@Composable
fun FolderScreen(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (List<Song>, Int) -> Unit
) {
    var selectedFolder by remember { mutableStateOf<String?>(null) }

    val grouped = remember(songs) {
        songs.groupBy { it.folderPath.ifBlank { "Autres" } }
            .toSortedMap()
    }

    val folder = selectedFolder
    if (folder != null) {
        val folderSongs = grouped[folder].orEmpty()
        FolderSongsScreen(
            folderPath = folder,
            songs = folderSongs,
            currentSong = currentSong,
            isPlaying = isPlaying,
            onBack = { selectedFolder = null },
            onSongClick = { index -> onSongClick(folderSongs, index) }
        )
    } else {
        FolderListScreen(
            grouped = grouped,
            onFolderClick = { selectedFolder = it }
        )
    }
}

@Composable
private fun FolderListScreen(
    grouped: Map<String, List<Song>>,
    onFolderClick: (String) -> Unit
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
        items(grouped.entries.toList()) { (folderPath, folderSongs) ->
            // Vrai nom du dossier = dernier segment du chemin, pas le chemin complet.
            val folderName = folderPath.trimEnd('/').substringAfterLast('/')
                .ifBlank { folderPath }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onFolderClick(folderPath) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(secondaryColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = secondaryColor
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = folderName,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${folderSongs.size} morceau${if (folderSongs.size > 1) "x" else ""}",
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
private fun FolderSongsScreen(
    folderPath: String,
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onSongClick: (Int) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) TuneoBackgroundDark else TuneoBackgroundLight
    val textColor = if (isDark) TuneoTextPrimaryDark else TuneoTextPrimaryLight
    val folderName = folderPath.trimEnd('/').substringAfterLast('/').ifBlank { folderPath }

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
                text = folderName,
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
