package com.tuneo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuneo.app.data.FeedPost
import com.tuneo.app.ui.theme.FeedAccentPurple
import com.tuneo.app.ui.theme.FeedCardBackground
import com.tuneo.app.ui.theme.FeedLikeRed
import com.tuneo.app.ui.theme.FeedOnlineGreen
import com.tuneo.app.ui.theme.FeedPillBackground
import com.tuneo.app.ui.theme.FeedTextPrimary
import com.tuneo.app.ui.theme.FeedTextSecondary

/** Couleur des ondes, variée par post (mêmes teintes que la maquette : violet / orange / rose). */
private val waveformColors = listOf(
    Color(0xFFB794F6), // violet clair
    Color(0xFFF6AD55), // orange
    Color(0xFFED64A6)  // rose
)

@Composable
fun PostCard(
    post: FeedPost,
    isLiked: Boolean,
    myAvatarUrl: String?,
    onLikeToggle: () -> Unit,
    onCommentClick: () -> Unit,
    minutesAgoLabel: String,
    modifier: Modifier = Modifier
) {
    val waveColor = waveformColors[post.id.hashCode().mod(waveformColors.size)]

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FeedCardBackground)
            .padding(14.dp)
    ) {
        // En-tête : avatar, pseudo + badge, "Écoute maintenant" + temps, menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = post.user_avatar_url,
                contentDescription = post.username,
                modifier = Modifier.size(38.dp).clip(CircleShape).background(FeedPillBackground)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "@${post.username}",
                        color = FeedTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = FeedAccentPurple,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Écoute maintenant", color = FeedTextSecondary, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier.size(6.dp).clip(CircleShape).background(FeedOnlineGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(minutesAgoLabel, color = FeedTextSecondary, fontSize = 12.sp)
                }
            }
            Icon(Icons.Default.MoreHoriz, contentDescription = "Plus d'options", tint = FeedTextSecondary)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bloc chanson : pochette + infos + waveform (pas de timer/barre de progression)
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = post.album_art_url,
                contentDescription = null,
                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(10.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    post.song_title,
                    color = FeedTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(post.song_artist, color = FeedTextSecondary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                post.song_genre?.let { genre ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(FeedAccentPurple.copy(alpha = 0.18f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(genre, color = FeedAccentPurple, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Headset, contentDescription = null, tint = FeedTextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(post.source_label, color = FeedTextSecondary, fontSize = 11.sp)
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            WaveformView(color = waveColor)
        }

        post.caption?.let { caption ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(caption, color = FeedTextPrimary, fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Rangée like / commentaire / partage + bookmark
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill(
                    icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    tint = if (isLiked) FeedLikeRed else FeedTextSecondary,
                    label = formatCount(post.like_count + if (isLiked) 1 else 0),
                    onClick = onLikeToggle
                )
                StatPill(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    tint = FeedTextSecondary,
                    label = formatCount(post.comment_count),
                    onClick = onCommentClick
                )
                StatPill(
                    icon = Icons.Outlined.Repeat,
                    tint = FeedTextSecondary,
                    label = formatCount(post.share_count),
                    onClick = {}
                )
            }
            Icon(Icons.Default.Bookmark, contentDescription = "Enregistrer", tint = FeedTextSecondary)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // "Aimé par X, Y et Z autres"
        if (post.like_count > 0) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Aimé par ${formatCount(post.like_count)} personnes",
                    color = FeedTextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = FeedTextSecondary, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Champ "Ajouter ton avis sur son goût musical" (remplace "Ajouter un commentaire")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(FeedPillBackground)
                .clickable { onCommentClick() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = myAvatarUrl,
                contentDescription = null,
                modifier = Modifier.size(24.dp).clip(CircleShape).background(FeedCardBackground)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Ajouter ton avis sur son goût musical",
                color = FeedTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Outlined.EmojiEmotions, contentDescription = null, tint = FeedTextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun StatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(FeedPillBackground)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = FeedTextSecondary, fontSize = 12.sp)
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 1000 -> String.format("%.1fk", count / 1000.0)
        else -> count.toString()
    }
}
