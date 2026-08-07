package com.tuneo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val TuneoDarkColors = darkColorScheme(
    background = TuneoBackgroundDark,
    surface = TuneoSurfaceDark,
    primary = TuneoAccentBlue,
    onBackground = TuneoTextPrimaryDark,
    onSurface = TuneoTextPrimaryDark
)

private val TuneoLightColors = lightColorScheme(
    background = TuneoBackgroundLight,
    surface = TuneoSurfaceLight,
    primary = TuneoAccentBlue,
    onBackground = TuneoTextPrimaryLight,
    onSurface = TuneoTextPrimaryLight
)

/**
 * Thème racine de l'app : suit automatiquement le mode clair/sombre du téléphone.
 * La couleur de la status bar est gérée séparément par écran via [TuneoStatusBar],
 * car l'écran Now Playing garde son propre fond (vert foncé) peu importe le thème système.
 */
@Composable
fun TuneoTheme(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val colors = if (isDark) TuneoDarkColors else TuneoLightColors

    MaterialTheme(
        colorScheme = colors,
        typography = TuneoTypography,
        shapes = TuneoShapes,
        content = content
    )
}
