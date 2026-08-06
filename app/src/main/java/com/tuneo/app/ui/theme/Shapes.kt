package com.tuneo.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formes plus généreusement arrondies que les valeurs Material3 par défaut,
 * pour se rapprocher du style "doux" (cartes, boutons, images) observé sur iOS.
 */
val TuneoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Arrondi standard à utiliser pour les pochettes d'album / vignettes carrées dans toute l'app. */
val AlbumArtCornerRadius = 14.dp
