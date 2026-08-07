package com.tuneo.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Ajuste la couleur de la status bar (et si les icônes doivent être claires ou foncées)
 * pour correspondre au fond de l'écran actuellement affiché.
 * À appeler en tête de chaque écran plein écran (Library, Now Playing, etc.).
 */
@Composable
fun TuneoStatusBar(backgroundColor: Color, useDarkIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return

    SideEffect {
        val window = (view.context as android.app.Activity).window
        window.statusBarColor = backgroundColor.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useDarkIcons
    }
}
