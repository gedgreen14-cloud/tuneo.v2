package com.tuneo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuneo.app.data.PlaylistRepository
import com.tuneo.app.data.Song
import com.tuneo.app.ui.components.AlbumArtThumbnail
import com.tuneo.app.ui.theme.TuneoAccentBlue
import com.tuneo.app.ui.theme.TuneoBackgroundDark
import com.tuneo.app.ui.theme.TuneoBackgroundLight
import com.tuneo.app.ui.theme.TuneoTextPrimaryDark
import com.tuneo.app.ui.theme.TuneoTextPrimaryLight
import com.tuneo.app.ui.theme.TuneoTextSecondaryDark
import com.tuneo.app.ui.theme.TuneoTextSecondaryLight

private const val AUTO_LIKED_ID = "auto_liked"
private const val AUTO_RECENT_ID = "auto_recent"
private const val AUTO_MOST_PLAYED_ID = "auto_most_played"

private sealed class PlaylistEntry(
    val id: String,
    val name: String,
    val songs: List<Song>,
    val isAutomatic: Boolean
) {
    class Auto(id: String, name: String, songs: List<Song>) : PlaylistEntry(id, name, songs, true)
    class Manual(id: String, name: String, songs: List<Song>) : PlaylistEntry(id, name, songs, false)
}

@Composable
fun PlaylistScreen(
    allSongs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    onSongClick: (List<Song>, Int) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { PlaylistRepository(context) }

    // Compteur utilisé pour forcer un recalcul des données persistées après une mutation
    // (création de playlist, ajout de morceau...), puisque le repository n'est pas observable.
    var refreshTick by remember { mutableIntStateOf(0) }

    val songsById = remember(allSongs) { allSongs.associateBy { it.id } }

    val likedSongs = remember(refreshTick, allSongs) {
        repository.getLikedSongIds().mapNotNull { songsById[it] }
    }
    val recentSongs = remember(refreshTick, allSongs) {
        repository.getHistory()
            .sortedByDescending { it.lastPlayedAtMs }
            .mapNotNull { songsById[it.songId] }
    }
    val mostPlayedSongs = remember(refreshTick, allSongs) {
        repository.getHistory()
            .sortedByDescending { it.playCount }
            .mapNotNull { songsById[it.songId] }
    }
    val manualPlaylists = remember(refreshTick, allSongs) {
        repository.getPlaylists().map { playlist ->
            PlaylistEntry.Manual(
                id = playlist.id,
                name = playlist.name,
                songs = playlist.songIds.mapNotNull { songsById[it] }
            )
        }
    }

    val entries = remember(likedSongs, recentSongs, mostPlayedSongs, manualPlaylists) {
        listOf(
            PlaylistEntry.Auto(AUTO_LIKED_ID, "Chansons aimées", likedSongs),
            PlaylistEntry.Auto(AUTO_RECENT_ID, "Lues récemment", recentSongs),
            PlaylistEntry.Auto(AUTO_MOST_PLAYED_ID, "Les plus jouées", mostPlayedSongs)
        ) + manualPlaylists
    }

    var selectedEntryId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddSongsForPlaylistId by remember { mutableStateOf<String?>(null) }

    val selectedEntry = entries.firstOrNull { it.id == selectedEntryId }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                repository.createPlaylist(name)
                refreshTick++
                showCreateDialog = false
            }
        )
    }

    val addSongsPlaylistId = showAddSongsForPlaylistId
    if (addSongsPlaylistId != null) {
        AddSongsDialog(
            allSongs = allSongs,
            alreadyInPlaylist = manualPlaylists.firstOrNull { it.id == addSongsPlaylistId }
                ?.songs?.map { it.id }?.toSet() ?: emptySet(),
            onDismiss = { showAddSongsForPlaylistId = null },
            onAddSong = { songId ->
                repository.addSongToPlaylist(addSongsPlaylistId, songId)
                refreshTick++
            }
        )
    }

    if (selectedEntry != null) {
        PlaylistDetailScreen(
            entry = selectedEntry,
            currentSong = currentSong,
            isPlaying = isPlaying,
            onBack = { selectedEntryId = null },
            onSongClick = { index -> onSongClick(selectedEntry.songs, index) },
            onAddSongsClick = { showAddSongsForPlaylistId = selectedEntry.id }
        )
    } else {
        PlaylistListScreen(
            entries = entries,
            onEntryClick = { selectedEntryId = it.id },
            onCreateClick = { showCreateDialog = true }
        )
    }
}

@Composable
private fun PlaylistListScreen(
    entries: List<PlaylistEntry>,
    onEntryClick: (PlaylistEntry) -> Unit,
    onCreateClick: () -> Unit
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
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCreateClick() }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(TuneoAccentBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = TuneoAccentBlue)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Créer une playlist",
                    color = TuneoAccentBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        items(entries) { entry ->
            val lastAddedSong = entry.songs.lastOrNull()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEntryClick(entry) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (entry is PlaylistEntry.Auto) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(secondaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (entry.id) {
                                AUTO_LIKED_ID -> Icons.Default.Favorite
                                AUTO_RECENT_ID -> Icons.Default.History
                                else -> Icons.Default.TrendingUp
                            },
                            contentDescription = null,
                            tint = secondaryColor
                        )
                    }
                } else {
                    // Miniature de playlist manuelle = pochette du dernier morceau ajouté.
                    AlbumArtThumbnail(
                        albumArtUri = lastAddedSong?.albumArtUri,
                        thumbnailSize = 56.dp,
                        cornerRadius = 14.dp
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.name,
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${entry.songs.size} morceau${if (entry.songs.size > 1) "x" else ""}",
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
private fun PlaylistDetailScreen(
    entry: PlaylistEntry,
    currentSong: Song?,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onSongClick: (Int) -> Unit,
    onAddSongsClick: () -> Unit
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
                text = entry.name,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (!entry.isAutomatic) {
                IconButton(onClick = onAddSongsClick) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "Ajouter des morceaux", tint = textColor)
                }
            }
        }
        SongListScreen(
            songs = entry.songs,
            currentSong = currentSong,
            isPlaying = isPlaying,
            onSongClick = onSongClick
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Nom de la playlist") }
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Créer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}

@Composable
private fun AddSongsDialog(
    allSongs: List<Song>,
    alreadyInPlaylist: Set<Long>,
    onDismiss: () -> Unit,
    onAddSong: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajouter des morceaux") },
        text = {
            LazyColumn(modifier = Modifier.height(360.dp)) {
                items(allSongs) { song ->
                    val added = alreadyInPlaylist.contains(song.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !added) { onAddSong(song.id) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AlbumArtThumbnail(albumArtUri = song.albumArtUri, thumbnailSize = 40.dp, cornerRadius = 10.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = song.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (added) {
                            Icon(Icons.Default.Check, contentDescription = "Déjà ajouté", tint = TuneoAccentBlue)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Terminé") }
        }
    )
}
