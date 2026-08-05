package com.tuneo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuneo.app.ui.theme.TuneoBackground
import com.tuneo.app.ui.theme.TuneoTextSecondary

@Composable
fun PlaceholderScreen(label: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TuneoBackground)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Construction,
            contentDescription = null,
            tint = TuneoTextSecondary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "$label — bientôt disponible",
            color = TuneoTextSecondary,
            fontSize = 16.sp
        )
    }
}
