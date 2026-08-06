package com.tuneo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import com.tuneo.app.ui.theme.FeedOnlineGreen
import com.tuneo.app.ui.theme.FeedPillBackground
import com.tuneo.app.ui.theme.FeedTextPrimary
import com.tuneo.app.ui.theme.FeedTextSecondary

private val ringGradients = listOf(
    listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)), // violet -> rose
    listOf(Color(0xFFF97316), Color(0xFFDB2777)), // orange -> rose foncé
    listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)), // bleu -> cyan
    listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)), // rose -> violet
    listOf(Color(0xFF22C55E), Color(0xFF16A34A))  // vert -> vert foncé
)

@Composable
fun StoriesRow(
    myUsername: String?,
    myAvatarUrl: String?,
    stories: List<StoryWithProfile>,
    onAddStoryClick: () -> Unit,
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
        items(stories) { item ->
            val ring = ringGradients[item.profile.username.hashCode().mod(ringGradients.size)]
            StoryItem(
                username = item.profile.username,
                songTitle = item.story.song_title,
                songArtist = item.story.song_artist,
                avatarUrl = item.profile.avatar_url,
                songArtUrl = item.story.album_art_url,
                ringColors = ring,
                onClick = { onStoryClick(item) }
            )
        }
    }
}

@Composable
private fun AddStoryItem(onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(FeedPillBackground)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Ta story", tint = FeedAccentPurple)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Ta story",
            color = FeedTextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StoryItem(
    username: String,
    songTitle: String,
    songArtist: String,
    avatarUrl: String?,
    songArtUrl: String?,
    ringColors: List<Color>,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).clickable { onClick() }
    ) {
        Box(modifier = Modifier.size(64.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(width = 2.dp, brush = Brush.linearGradient(ringColors), shape = CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(FeedPillBackground)
            ) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = username,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }

            // Vignette carrée de la pochette + pastille "en ligne", collées en bas-droite
            if (songArtUrl != null) {
                AsyncImage(
                    model = songArtUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd)
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, Color.Black, RoundedCornerShape(4.dp))
                )
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(FeedOnlineGreen)
                    .border(1.5.dp, Color.Black, CircleShape)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "@$username",
            color = FeedTextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "$songTitle · $songArtist",
            color = FeedTextSecondary,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
