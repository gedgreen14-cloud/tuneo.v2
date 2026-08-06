package com.tuneo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.tuneo.app.ui.theme.TuneoAccentBlue
import com.tuneo.app.ui.theme.TuneoBackgroundDark
import com.tuneo.app.ui.theme.TuneoBackgroundLight
import com.tuneo.app.ui.theme.TuneoTextSecondaryDark
import com.tuneo.app.ui.theme.TuneoTextSecondaryLight

enum class TuneoDestination(val label: String) {
    ACCUEIL("Accueil"),
    DECOUVERTE("Découverte"),
    BIBLIOTHEQUE("Bibliothèque"),
    NOTIFICATIONS("Notifications"),
    PROFIL("Profil")
}

@Composable
fun BottomNavBar(
    selected: TuneoDestination,
    onSelect: (TuneoDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    // Fond opaque qui suit le thème de l'app (pas de transparence,
    // contrairement à la barre Spotify du screenshot de référence)
    val background = if (isDark) TuneoBackgroundDark else TuneoBackgroundLight
    val unselectedColor = if (isDark) TuneoTextSecondaryDark else TuneoTextSecondaryLight
    val selectedColor = TuneoAccentBlue

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .padding(top = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TuneoDestination.values().forEach { destination ->
            val isSelected = destination == selected
            val tint = if (isSelected) selectedColor else unselectedColor

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onSelect(destination) }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = iconFor(destination, isSelected),
                    contentDescription = destination.label,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = destination.label,
                    color = tint,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

private fun iconFor(destination: TuneoDestination, isSelected: Boolean): ImageVector {
    return when (destination) {
        TuneoDestination.ACCUEIL ->
            if (isSelected) Icons.Filled.Home else Icons.Outlined.Home
        TuneoDestination.DECOUVERTE ->
            if (isSelected) Icons.Filled.Search else Icons.Outlined.Search
        TuneoDestination.BIBLIOTHEQUE ->
            if (isSelected) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic
        TuneoDestination.NOTIFICATIONS ->
            if (isSelected) Icons.Filled.Notifications else Icons.Outlined.Notifications
        TuneoDestination.PROFIL ->
            if (isSelected) Icons.Filled.Person else Icons.Outlined.Person
    }
}
