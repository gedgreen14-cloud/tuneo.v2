package com.tuneo.app.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Miniature de pochette d'album, style commun à SongList et Play Now :
 * card à coins arrondis. Si aucune pochette n'existe pour le morceau,
 * affiche une card transparente avec une icône de musique centrée,
 * plutôt qu'une image cassée ou vide.
 */
@Composable
fun AlbumArtThumbnail(
    albumArtUri: Uri?,
    thumbnailSize: Dp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp
) {
    var loadFailed by remember(albumArtUri) { mutableStateOf(false) }
    val showFallback = albumArtUri == null || loadFailed

    Box(
        modifier = modifier
            .size(thumbnailSize)
            .clip(RoundedCornerShape(cornerRadius))
            .background(if (showFallback) Color.Transparent else Color.Black.copy(alpha = 0.06f)),
        contentAlignment = Alignment.Center
    ) {
        if (showFallback) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.6f),
                modifier = Modifier.size(thumbnailSize / 2.2f)
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(albumArtUri)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                onError = { loadFailed = true }
            )
        }
    }
}
