package com.tuneo.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.tuneo.app.R

/**
 * Police Inter : gratuite, libre de droits, choisie car visuellement très proche
 * de SF Pro (la police système iOS) sans en être une copie — donc sans le risque
 * de licence lié à l'utilisation de la vraie police Apple sur Android.
 */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

val TuneoTypography = Typography(
    displayLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium)
)
