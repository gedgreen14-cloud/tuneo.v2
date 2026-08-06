package com.tuneo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import coil.compose.AsyncImage
import com.tuneo.app.data.FeedRepository
import com.tuneo.app.data.NewPost
import com.tuneo.app.data.Song
import com.tuneo.app.ui.theme.FeedAccentPurple
import com.tuneo.app.ui.theme.FeedBackground
import com.tuneo.app.ui.theme.FeedPillBackground
import com.tuneo.app.ui.theme.FeedTextPrimary
import com.tuneo.app.ui.theme.FeedTextSecondary
import kotlinx.coroutines.launch

/**
 * Petit écran affiché quand un utilisateur déjà connecté appuie sur "Partager ce que j'écoute" :
 * juste une légende courte, puis publication dans le feed.
 */
@Composable
fun ShareCaptionScreen(
    song: Song,
    userId: String,
    onCancel: () -> Unit,
    onPublished: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val feedRepository = remember { FeedRepository() }

    var caption by remember { mutableStateOf("") }
    var isPublishing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FeedBackground)
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Annuler", tint = FeedTextPrimary)
            }
            Text("Partager", color = FeedTextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            TextButton(
                onClick = {
                    if (isPublishing) return@TextButton
                    isPublishing = true
                    scope.launch {
                        feedRepository.createPost(
                            NewPost(
                                user_id = userId,
                                song_title = song.title,
                                song_artist = song.artist,
                                song_genre = null,
                                album_art_url = song.albumArtUri?.toString(),
                                caption = caption.trim().ifBlank { null }
                            )
                        )
                        isPublishing = false
                        onPublished()
                    }
                }
            ) {
                Text(
                    if (isPublishing) "..." else "Publier",
                    color = FeedAccentPurple,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Aperçu de la chanson partagée
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(FeedPillBackground)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    song.title,
                    color = FeedTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(song.artist, color = FeedTextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = caption,
            onValueChange = { if (it.length <= 120) caption = it },
            placeholder = { Text("Écris une petite légende…", color = FeedTextSecondary) },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = FeedTextPrimary,
                unfocusedTextColor = FeedTextPrimary,
                focusedContainerColor = FeedPillBackground,
                unfocusedContainerColor = FeedPillBackground,
                focusedBorderColor = FeedAccentPurple,
                unfocusedBorderColor = Color.Transparent,
                cursorColor = FeedAccentPurple
            )
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "${caption.length}/120",
            color = FeedTextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.End)
        )
    }
}
