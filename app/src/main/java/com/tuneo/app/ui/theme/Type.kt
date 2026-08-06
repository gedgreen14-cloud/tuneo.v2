package com.tuneo.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Typographie du thème. Utilise la police système par défaut mais avec des poids
 * et un tracking resserré pour se rapprocher visuellement de la douceur de SF Pro (iOS),
 * en l'absence d'une police custom fournie par le design.
 * Si un fichier de police custom est ajouté plus tard (ex: dans res/font),
 * remplacer FontFamily.Default ci-dessous par FontFamily(Font(R.font.xxx)).
 */
val TuneoTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium)
)
