package com.tuneo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuneo.app.data.StoryWithProfile
import com.tuneo.app.ui.theme.FeedAccentPurple
import com.tuneo.app.ui.theme.FeedTextPrimaryDark
import com.tuneo.app.ui.theme.FeedTextPrimaryLight
import com.tuneo.app.ui.theme.FeedTextSecondaryDark
import com.tuneo.app.ui.theme.FeedTextSecondaryLight

private val ringGradients = listOf(
    listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)), // violet -> rose
    listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)), // violet -> violet foncé
    listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)), // bleu
    listOf(Color(0xFFEF4444), Color(0xFFB91C1C)), // rouge
    listOf(Color(0xFFEC4899), Color(0xFFDB2777))  // rose
)

// Anneau dédié à "ma" story en cours d'écoute (violet -> rose, comme sur la maquette).
private val myListeningRing = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))

@Composable
fun StoriesRow(
    myAvatarUrl: String?,
    isListeningNow: Boolean,
    stories: List<StoryWithProfile>,
    onAddStoryClick: () -> Unit,
    onMyStoryClick: () -> Unit,
    onStoryClick: (StoryWithProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AddStoryItem(onClick = onAddStoryClick)
        }
        // "En écoute" : ma propre story, uniquement visible si je suis en train d'écouter.
        if (isListeningNow) {
            item {
                MyListeningStoryItem(avatarUrl = myAvatarUrl, onClick = onMyStoryClick)
            }
        }
        items(stories) { item ->
            val ring = ringGradients[item.profile.username.hashCode().mod(ringGradients.size)]
            StoryItem(
                username = item.profile.username,
                avatarUrl = item.profile.avatar_url,
                ringColors = ring,
                onClick = { onStoryClick(item) }
            )
        }
    }
}

@Composable
private fun AddStoryItem(onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val secondaryColor = if (isDark) FeedTextSecondaryDark else FeedTextSecondaryLight

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .border(width = 2.dp, color = FeedAccentPurple, shape = CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Votre story", tint = Color.White)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Votre story",
            color = secondaryColor,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Ma propre story, affichée avec le label "En écoute" (pas mon pseudo) + égaliseur en overlay. */
@Composable
private fun MyListeningStoryItem(avatarUrl: String?, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val primaryColor = if (isDark) FeedTextPrimaryDark else FeedTextPrimaryLight

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(width = 2.dp, brush = Brush.linearGradient(myListeningRing), shape = CircleShape)
                .padding(3.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "En écoute",
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Equalizer,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "En écoute",
            color = primaryColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StoryItem(
    username: String,
    avatarUrl: String?,
    ringColors: List<Color>,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val primaryColor = if (isDark) FeedTextPrimaryDark else FeedTextPrimaryLight

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .border(width = 2.dp, brush = Brush.linearGradient(ringColors), shape = CircleShape)
                .padding(3.dp)
                .clip(CircleShape)
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = username,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "@$username",
            color = primaryColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
