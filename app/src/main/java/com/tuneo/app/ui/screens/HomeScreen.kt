package com.tuneo.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuneo.app.data.Song
import com.tuneo.app.ui.components.TabsRow
import com.tuneo.app.ui.components.TuneoTab
import com.tuneo.app.ui.theme.TuneoBackground
import com.tuneo.app.ui.theme.TuneoTextSecondary

@Composable
fun TuneoHeader(
    searchActive: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TuneoBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (searchActive) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Rechercher...", color = TuneoTextSecondary) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = TuneoTextSecondary,
                    unfocusedIndicatorColor = TuneoTextSecondary.copy(alpha = 0.4f),
                    cursorColor = Color.White
                )
            )
        } else {
            Text(
                text = "Tuneo",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleSearch) {
                Icon(
                    imageVector = if (searchActive) Icons.Default.Close else Icons.Default.Search,
                    contentDescription = if (searchActive) "Fermer la recherche" else "Rechercher",
                    tint = Color.White
                )
            }
            if (!searchActive) {
                IconButton(onClick = { }) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Trier", tint = Color.White)
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.GpsFixed, contentDescription = "Cible", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun SongListScreen(songs: List<Song>, onSongClick: (Int) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TuneoBackground)
    ) {
        items(songs.size) { index ->
            val song = songs[index]
            SongRow(song = song, onClick = { onSongClick(index) })
        }
        item { Spacer(modifier = Modifier.height(80.dp)) } // place pour le mini-player
    }
}

@Composable
private fun SongRow(song: Song, onClick: () -> Unit) {
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
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = song.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song.hasLyrics) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.Transparent)
                            .padding(end = 6.dp)
                    ) {
                        Text(
                            text = "PAROLES",
                            color = TuneoTextSecondary,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .background(
                                    Color.Transparent
                                )
                        )
                    }
                }
                Text(
                    text = song.artist,
                    color = TuneoTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
