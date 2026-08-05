package com.tuneo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val TuneoDarkColors = darkColorScheme(
    background = TuneoBackground,
    surface = TuneoSurface,
    primary = TuneoAccentBlue,
    onBackground = TuneoTextPrimary,
    onSurface = TuneoTextPrimary
)

@Composable
fun TuneoTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as android.app.Activity).window
        window.statusBarColor = TuneoBackground.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
    }

    MaterialTheme(
        colorScheme = TuneoDarkColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
