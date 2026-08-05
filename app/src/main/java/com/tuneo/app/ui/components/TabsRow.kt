package com.tuneo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.tuneo.app.ui.theme.TuneoAccentBlue
import com.tuneo.app.ui.theme.TuneoSurface
import com.tuneo.app.ui.theme.TuneoTextSecondary

enum class TuneoTab(val label: String) {
    VIDEOS("Vidéos"),
    SONGS("Chansons"),
    PLAYLISTS("Playlists"),
    FOLDERS("Dossiers"),
    ARTISTS("Artists"),
    ALBUMS("Albums")
}

@Composable
fun TabsRow(
    selected: TuneoTab,
    onSelect: (TuneoTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TuneoTab.values().forEachIndexed { index, tab ->
            val isSelected = tab == selected

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) TuneoAccentBlue else TuneoSurface)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (tab == TuneoTab.VIDEOS) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleOutline,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else TuneoTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = tab.label,
                    color = if (isSelected) Color.White else TuneoTextSecondary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Séparateur vertical après l'onglet Vidéos, comme sur la capture
            if (tab == TuneoTab.VIDEOS) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(24.dp)
                        .background(TuneoTextSecondary.copy(alpha = 0.3f))
                )
            }
        }
    }
}
