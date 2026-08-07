package com.tuneo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuneo.app.data.FeedPost
import com.tuneo.app.data.FeedRepository
import com.tuneo.app.data.Profile
import com.tuneo.app.data.StoryWithProfile
import com.tuneo.app.ui.components.PostCard
import com.tuneo.app.ui.components.StoriesRow
import com.tuneo.app.ui.theme.FeedAccentPurple
import com.tuneo.app.ui.theme.FeedBackgroundDark
import com.tuneo.app.ui.theme.FeedBackgroundLight
import com.tuneo.app.ui.theme.FeedTextPrimaryDark
import com.tuneo.app.ui.theme.FeedTextPrimaryLight
import com.tuneo.app.ui.theme.FeedTextSecondaryDark
import com.tuneo.app.ui.theme.FeedTextSecondaryLight
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    isAuthenticated: Boolean,
    myProfile: Profile?,
    isListeningNow: Boolean,
    onAddStoryClick: () -> Unit,
    onMessagesClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) FeedBackgroundDark else FeedBackgroundLight

    val scope = rememberCoroutineScope()
    val feedRepository = remember { FeedRepository() }

    var posts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var stories by remember { mutableStateOf<List<StoryWithProfile>>(emptyList()) }
    var likedPostIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var savedPostIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var followingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var topLikers by remember { mutableStateOf<Map<String, List<Profile>>>(emptyMap()) }
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            posts = feedRepository.fetchFeed()
            stories = feedRepository.fetchStories()
            if (myProfile != null) {
                likedPostIds = feedRepository.likedPostIds(myProfile.id)
                followingIds = feedRepository.followingIds(myProfile.id)
            }
            topLikers = feedRepository.fetchTopLikers(posts.map { it.id })
        }
    }

    LaunchedEffect(isAuthenticated) { reload() }

    Column(modifier = Modifier.fillMaxSize().background(background)) {
        FeedHeader(myAvatarUrl = myProfile?.avatar_url, onMessagesClick = onMessagesClick)

        StoriesRow(
            myAvatarUrl = myProfile?.avatar_url,
            isListeningNow = isListeningNow,
            stories = stories,
            onAddStoryClick = onAddStoryClick,
            onMyStoryClick = { /* aperçu de ma propre story à venir */ },
            onStoryClick = { /* aperçu story à venir */ }
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (posts.isEmpty()) {
            val secondaryColor = if (isDark) FeedTextSecondaryDark else FeedTextSecondaryLight
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Aucune publication pour le moment.\nPartage ce que tu écoutes depuis le lecteur !",
                    color = secondaryColor,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        isLiked = likedPostIds.contains(post.id),
                        isSaved = savedPostIds.contains(post.id),
                        isOwnPost = myProfile?.id == post.user_id,
                        isFollowing = followingIds.contains(post.user_id),
                        minutesAgoLabel = relativeTimeLabel(post.created_at),
                        likedByProfiles = topLikers[post.id].orEmpty(),
                        onLikeToggle = {
                            val uid = myProfile?.id ?: return@PostCard
                            scope.launch {
                                if (likedPostIds.contains(post.id)) {
                                    feedRepository.unlike(post.id, uid)
                                    likedPostIds = likedPostIds - post.id
                                } else {
                                    feedRepository.like(post.id, uid)
                                    likedPostIds = likedPostIds + post.id
                                }
                            }
                        },
                        onCommentClick = { activeCommentPostId = post.id },
                        onRepostClick = { /* repost à venir */ },
                        onSaveToggle = {
                            savedPostIds = if (savedPostIds.contains(post.id)) {
                                savedPostIds - post.id
                            } else {
                                savedPostIds + post.id
                            }
                        },
                        onMoreClick = { /* menu options à venir */ },
                        onLikedByClick = { /* liste des personnes ayant aimé à venir */ },
                        onFollowToggle = {
                            val uid = myProfile?.id ?: return@PostCard
                            scope.launch {
                                if (followingIds.contains(post.user_id)) {
                                    feedRepository.unfollow(uid, post.user_id)
                                    followingIds = followingIds - post.user_id
                                } else {
                                    feedRepository.follow(uid, post.user_id)
                                    followingIds = followingIds + post.user_id
                                }
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(90.dp)) } // place pour le mini-player + nav bar
            }
        }
    }

    val commentPost = posts.find { it.id == activeCommentPostId }
    if (commentPost != null && myProfile != null) {
        CommentSheet(
            post = commentPost,
            myAvatarUrl = myProfile.avatar_url,
            onDismiss = { activeCommentPostId = null },
            onSubmit = { text ->
                scope.launch {
                    feedRepository.addComment(commentPost.id, myProfile.id, text)
                    activeCommentPostId = null
                    reload()
                }
            }
        )
    }
}

@Composable
private fun FeedHeader(myAvatarUrl: String?, onMessagesClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val primaryColor = if (isDark) FeedTextPrimaryDark else FeedTextPrimaryLight

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(modifier = Modifier.size(32.dp).align(Alignment.CenterStart)) {
            AsyncImage(
                model = myAvatarUrl,
                contentDescription = "Profil",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.Gray)
            )
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(FeedAccentPurple)
            )
        }

        Text(
            "TUNEO",
            color = primaryColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 3.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            IconButton(onClick = onMessagesClick) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Messages",
                    tint = primaryColor
                )
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(FeedAccentPurple)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentSheet(
    post: FeedPost,
    myAvatarUrl: String?,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) FeedBackgroundDark else FeedBackgroundLight
    val primaryColor = if (isDark) FeedTextPrimaryDark else FeedTextPrimaryLight
    val secondaryColor = if (isDark) FeedTextSecondaryDark else FeedTextSecondaryLight

    var text by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = background) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "Avis sur \"${post.song_title}\"",
                color = primaryColor,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = myAvatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Gray)
                )
                Spacer(modifier = Modifier.width(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Ajoute ton avis sur son goût musical", color = secondaryColor, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = primaryColor,
                        unfocusedTextColor = primaryColor,
                        focusedBorderColor = FeedAccentPurple
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { if (text.isNotBlank()) onSubmit(text.trim()) }) {
                    Text("Envoyer", color = FeedAccentPurple, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun relativeTimeLabel(isoTimestamp: String): String {
    return try {
        val instant = Instant.parse(isoTimestamp)
        val minutes = Duration.between(instant, Instant.now()).toMinutes()
        when {
            minutes < 1 -> "à l'instant"
            minutes < 60 -> "$minutes min"
            minutes < 1440 -> "${minutes / 60} h"
            else -> "${minutes / 1440} j"
        }
    } catch (e: DateTimeParseException) {
        ""
    }
}
