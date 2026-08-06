package com.tuneo.app.ui.theme

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest

/**
 * Couleur de secours utilisée tant que l'extraction n'a pas encore de résultat,
 * ou si la pochette est absente / illisible.
 */
private val FallbackPlayerColor = Color(0xFF1C2B23)

/**
 * Extrait la couleur dominante d'une pochette d'album (via Palette) et la renvoie
 * comme état Compose. Recalcule automatiquement quand [albumArtUri] change.
 *
 * Le fond de l'écran "En cours de lecture" et du mini-player doivent tous les deux
 * utiliser cette même fonction avec la même URI, pour rester visuellement cohérents.
 */
@Composable
fun rememberDominantColor(albumArtUri: Uri?): Color {
    val context = LocalContext.current
    val state = produceState(initialValue = FallbackPlayerColor, key1 = albumArtUri) {
        if (albumArtUri == null) {
            value = FallbackPlayerColor
            return@produceState
        }
        val request = ImageRequest.Builder(context)
            .data(albumArtUri)
            .allowHardware(false) // Palette a besoin d'un bitmap logiciel
            .build()
        val result = context.imageLoader.execute(request)
        val bitmap = result.drawable?.toBitmap()
        if (bitmap != null) {
            val palette = Palette.from(bitmap).generate()
            val swatch = palette.vibrantSwatch
                ?: palette.dominantSwatch
                ?: palette.mutedSwatch
            if (swatch != null) {
                value = Color(swatch.rgb)
            }
        }
    }
    return state.value
}

/** Couleur de texte/icônes lisible (blanc ou noir) selon la luminosité du fond donné. */
fun contentColorFor(background: Color): Color {
    val luminance = (0.299 * background.red + 0.587 * background.green + 0.114 * background.blue)
    return if (luminance > 0.5) Color(0xFF1A1A1A) else Color.White
}
