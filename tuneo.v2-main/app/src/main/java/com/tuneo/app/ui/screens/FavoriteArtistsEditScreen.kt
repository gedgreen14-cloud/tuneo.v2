package com.tuneo.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuneo.app.data.FavoriteArtist
import com.tuneo.app.data.ProfileRepository
import com.tuneo.app.ui.theme.FeedAccentPurple
import com.tuneo.app.ui.theme.FeedBackgroundDark
import com.tuneo.app.ui.theme.FeedBackgroundLight
import com.tuneo.app.ui.theme.FeedTextPrimaryDark
import com.tuneo.app.ui.theme.FeedTextPrimaryLight
import com.tuneo.app.ui.theme.FeedTextSecondaryDark
import com.tuneo.app.ui.theme.FeedTextSecondaryLight
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Un artiste en cours d'édition, avant sauvegarde : soit une image déjà uploadée
 * (avatarUrl), soit une image tout juste choisie sur l'appareil (localUri) qui sera
 * uploadée à l'enregistrement.
 */
private data class EditableArtist(
    val localId: String = UUID.randomUUID().toString(),
    val name: String,
    val avatarUrl: String? = null,
    val localUri: Uri? = null
)

/**
 * Écran de choix des artistes préférés affichés sur le profil.
 * Chaque artiste a un nom et une photo ; la photo est affichée dans un cercle avec
 * ContentScale.Crop, qui recadre automatiquement l'image sur son centre pour remplir
 * le cercle, quel que soit le format d'origine de la photo choisie.
 */
@Composable
fun FavoriteArtistsEditScreen(
    userId: String,
    onCancel: () -> Unit,
    onSaved: (List<FavoriteArtist>) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) FeedBackgroundDark else FeedBackgroundLight
    val primaryColor = if (isDark) FeedTextPrimaryDark else FeedTextPrimaryLight
    val secondaryColor = if (isDark) FeedTextSecondaryDark else FeedTextSecondaryLight

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profileRepository = remember { ProfileRepository() }

    var artists by remember { mutableStateOf<List<EditableArtist>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var newArtistName by remember { mutableStateOf("") }
    var pendingPhotoForLocalId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(userId) {
        val existing = profileRepository.fetchFavoriteArtists(userId)
        artists = existing.map { EditableArtist(name = it.artist_name, avatarUrl = it.avatar_url) }
        isLoading = false
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val targetId = pendingPhotoForLocalId
        if (uri != null && targetId != null) {
            artists = artists.map { if (it.localId == targetId) it.copy(localUri = uri) else it }
        }
        pendingPhotoForLocalId = null
    }

    Column(modifier = Modifier.fillMaxSize().background(background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Annuler", tint = primaryColor)
            }
            Text("Artistes préférés", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            TextButton(
                enabled = !isSaving,
                onClick = {
                    scope.launch {
                        isSaving = true
                        try {
                            val saved = artists
                                .filter { it.name.isNotBlank() }
                                .map { artist ->
                                    var finalUrl = artist.avatarUrl
                                    artist.localUri?.let { uri ->
                                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                        if (bytes != null) {
                                            finalUrl = profileRepository.uploadFavoriteArtistPhoto(userId, artist.localId, bytes)
                                        }
                                    }
                                    FavoriteArtist(user_id = userId, artist_name = artist.name.trim(), avatar_url = finalUrl)
                                }
                            profileRepository.setFavoriteArtists(userId, saved)
                            onSaved(saved)
                        } finally {
                            isSaving = false
                        }
                    }
                }
            ) {
                Text(if (isSaving) "..." else "Enregistrer", color = FeedAccentPurple, fontWeight = FontWeight.Bold)
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FeedAccentPurple)
            }
            return@Column
        }

        // Ajout d'un nouvel artiste par nom, la photo se choisit ensuite dans la liste.
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newArtistName,
                onValueChange = { newArtistName = it },
                placeholder = { Text("Nom de l'artiste", color = secondaryColor) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = primaryColor,
                    unfocusedTextColor = primaryColor,
                    focusedBorderColor = FeedAccentPurple,
                    unfocusedBorderColor = secondaryColor.copy(alpha = 0.4f)
                ),
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(FeedAccentPurple)
                    .clickable(enabled = newArtistName.isNotBlank()) {
                        artists = artists + EditableArtist(name = newArtistName.trim())
                        newArtistName = ""
                    }
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Text("Ajouter", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        if (artists.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Ajoute tes artistes préférés pour qu'ils apparaissent sur ton profil.", color = secondaryColor, fontSize = 14.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(artists, key = { it.localId }) { artist ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Photo circulaire : ContentScale.Crop recadre automatiquement
                        // l'image sur son centre pour remplir le cercle, quel que soit
                        // le format d'origine (portrait, paysage, carré).
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color(0xFF555555), Color(0xFF222222))))
                                .clickable {
                                    pendingPhotoForLocalId = artist.localId
                                    pickImageLauncher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val displayModel = artist.localUri ?: artist.avatarUrl
                            if (displayModel != null) {
                                AsyncImage(
                                    model = displayModel,
                                    contentDescription = artist.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Icon(Icons.Default.AddAPhoto, contentDescription = "Ajouter une photo", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }

                        Text(artist.name, color = primaryColor, fontSize = 15.sp, modifier = Modifier.weight(1f))

                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Retirer",
                            tint = secondaryColor,
                            modifier = Modifier.clickable {
                                artists = artists.filterNot { it.localId == artist.localId }
                            }
                        )
                    }
                }
            }
        }
    }
}
