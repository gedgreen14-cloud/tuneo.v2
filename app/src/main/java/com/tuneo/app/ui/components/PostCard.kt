package com.tuneo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuneo.app.data.FeedPost
import com.tuneo.app.data.Profile
import com.tuneo.app.ui.theme.FeedAccentPurple
import com.tuneo.app.ui.theme.FeedLikeRed
import com.tuneo.app.ui.theme.FeedTextPrimaryDark
import com.tuneo.app.ui.theme.FeedTextPrimaryLight
import com.tuneo.app.ui.theme.FeedTextSecondaryDark
import com.tuneo.app.ui.theme.FeedTextSecondaryLight

/**
 * Card de post du feed, reproduite à l'identique de la maquette :
 * aucun fond de carte, aucun fond en pilule sur les actions — tout est posé
 * nu sur le fond de l'écran (noir en sombre, blanc en clair).
 */
@Composable
fun PostCard(
    post: FeedPost,
    isLiked: Boolean,
    isSaved: Boolean,
    minutesAgoLabel: String,
    isOwnPost: Boolean,
    isFollowing: Boolean,
    likedByProfiles: List<Profile>,
    onLikeToggle: () -> Unit,
    onCommentClick: () -> Unit,
    onRepostClick: () -> Unit,
    onSaveToggle: () -> Unit,
    onMoreClick: () -> Unit,
    onLikedByClick: () -> Unit,
    onFollowToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val primaryColor = if (isDark) FeedTextPrimaryDark else FeedTextPrimaryLight
    val secondaryColor = if (isDark) FeedTextSecondaryDark else FeedTextSecondaryLight

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {

        // Ligne d'en-tête : avatar + pastille en ligne, @pseudo, "Abonné (e)", •••
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.size(44.dp)) {
                AsyncImage(
                    model = post.user_avatar_url,
                    contentDescription = post.username,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(FeedAccentPurple)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "@${post.username}",
                    color = primaryColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(minutesAgoLabel, color = secondaryColor, fontSize = 12.sp)
            }
            if (!isOwnPost) {
                Text(
                    if (isFollowing) "Abonné (e)" else "S'abonner",
                    color = FeedAccentPurple,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onFollowToggle() }
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                Icons.Default.MoreHoriz,
                contentDescription = "Plus d'options",
                tint = secondaryColor,
                modifier = Modifier.clickable { onMoreClick() }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Pochette en plein format (ratio carré), titre/artiste/statut en overlay
        // sur un dégradé sombre en bas de l'image pour rester lisibles.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = post.album_art_url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                            startY = 0.4f
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EqualizerBars(
                        isPlaying = true,
                        color = FeedAccentPurple,
                        barWidth = 3.dp,
                        maxHeight = 12.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "ÉCOUTE MAINTENANT",
                        color = FeedAccentPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    post.song_title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    post.song_artist,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Headset,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(post.source_label, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                }
            }
        }

        post.caption?.let { caption ->
            Spacer(modifier = Modifier.height(14.dp))
            Text(caption, color = primaryColor, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Actions nues, sans fond en pilule : cœur, commentaire, repost à gauche ; enregistrer à droite.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                ActionStat(
                    icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    tint = if (isLiked) FeedLikeRed else secondaryColor,
                    label = formatCount(post.like_count),
                    textColor = secondaryColor,
                    onClick = onLikeToggle
                )
                ActionStat(
                    icon = Icons.AutoMirrored.Outlined.Chat,
                    tint = secondaryColor,
                    label = formatCount(post.comment_count),
                    textColor = secondaryColor,
                    onClick = onCommentClick
                )
                ActionStat(
                    icon = Icons.Outlined.Repeat,
                    tint = secondaryColor,
                    label = formatCount(post.share_count),
                    textColor = secondaryColor,
                    onClick = onRepostClick
                )
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Partager",
                    tint = secondaryColor,
                    modifier = Modifier.size(22.dp).clickable { /* partage à venir */ }
                )
            }
            Icon(
                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Enregistrer",
                tint = secondaryColor,
                modifier = Modifier.clickable { onSaveToggle() }
            )
        }

        // "Aimé par pseudo" (1 like) ou avatars empilés + "Aimé par p1, p2 et X autres" (2+ likes).
        if (post.like_count > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onLikedByClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (post.like_count > 1) {
                    StackedAvatars(profiles = likedByProfiles)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    likedByLabel(likedByProfiles, post.like_count),
                    color = secondaryColor,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ActionStat(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = textColor, fontSize = 13.sp)
    }
}

@Composable
private fun StackedAvatars(profiles: List<Profile>) {
    Row {
        profiles.take(3).forEachIndexed { index, profile ->
            Box(
                modifier = Modifier
                    .padding(start = if (index == 0) 0.dp else (-8).dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            ) {
                AsyncImage(
                    model = profile.avatar_url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
        }
    }
}

/**
 * "Aimé par pseudo" si un seul like ; "Aimé par pseudo1, pseudo2 et X autres" à partir
 * de 2 likes, exactement le format de la maquette.
 */
private fun likedByLabel(profiles: List<Profile>, likeCount: Long): String {
    if (likeCount <= 1) {
        val single = profiles.firstOrNull()?.username
        return if (single != null) "Aimé par $single" else "Aimé par 1 personne"
    }
    val firstTwo = profiles.take(2).map { it.username }
    val others = likeCount - firstTwo.size
    return when {
        firstTwo.size == 2 && others > 0 -> "Aimé par ${firstTwo[0]}, ${firstTwo[1]} et ${formatCount(others)} autres"
        firstTwo.size == 2 -> "Aimé par ${firstTwo[0]} et ${firstTwo[1]}"
        firstTwo.size == 1 -> "Aimé par ${firstTwo[0]} et ${formatCount(likeCount - 1)} autres"
        else -> "Aimé par ${formatCount(likeCount)} personnes"
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 1000 -> String.format("%.1fk", count / 1000.0)
        else -> count.toString()
    }
}
