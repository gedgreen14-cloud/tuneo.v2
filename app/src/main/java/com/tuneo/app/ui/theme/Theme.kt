package com.tuneo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val TuneoDarkColors = darkColorScheme(
    background = TuneoBackground,
    surface = TuneoSurface,
    primary = TuneoAccentBlue,
    onBackground = TuneoTextPrimary,
    onSurface = TuneoTextPrimary
)

private val TuneoLightColors = lightColorScheme(
    background = TuneoBackgroundLight,
    surface = TuneoSurfaceLight,
    primary = TuneoAccentBlue,
    onBackground = TuneoTextPrimaryLight,
    onSurface = TuneoTextPrimaryLight
)

/**
 * Le thème suit désormais le mode système (clair/sombre).
 * La couleur de la status bar n'est plus fixée ici : elle est
 * pilotée dynamiquement depuis TuneoApp (MainActivity.kt) car elle
 * doit changer selon l'écran affiché (Library vs Now Playing), pas
 * une seule fois au lancement de l'Activity.
 */
@Composable
fun TuneoTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) TuneoDarkColors else TuneoLightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
