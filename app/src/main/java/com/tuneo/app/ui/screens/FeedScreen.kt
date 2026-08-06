package com.tuneo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.tuneo.app.ui.theme.FeedBackground
import com.tuneo.app.ui.theme.FeedPillBackground
import com.tuneo.app.ui.theme.FeedTextPrimary
import com.tuneo.app.ui.theme.FeedTextSecondary
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException

private enum class FeedFilter(val label: String) {
    TOUS("Tous"), AMIS("Amis"), PROCHE("Proche"), TENDANCES("Tendances")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    isAuthenticated: Boolean,
    myProfile: Profile?,
    onAddStoryClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val feedRepository = remember { FeedRepository() }

    var posts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var stories by remember { mutableStateOf<List<StoryWithProfile>>(emptyList()) }
    var likedPostIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedFilter by remember { mutableStateOf(FeedFilter.TOUS) }
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            posts = feedRepository.fetchFeed()
            stories = feedRepository.fetchStories()
            if (myProfile != null) {
                likedPostIds = feedRepository.likedPostIds(myProfile.id)
            }
        }
    }

    LaunchedEffect(isAuthenticated) { reload() }

    Column(modifier = Modifier.fillMaxSize().background(FeedBackground)) {
        FeedHeader(myAvatarUrl = myProfile?.avatar_url)

        StoriesRow(
            myUsername = myProfile?.username,
            myAvatarUrl = myProfile?.avatar_url,
            stories = stories,
            onAddStoryClick = onAddStoryClick,
            onStoryClick = { /* aperçu story à venir */ }
        )

        FilterRow(selected = selectedFilter, onSelect = { selectedFilter = it })

        Spacer(modifier = Modifier.height(4.dp))

        if (posts.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "Aucune publication pour le moment.\nPartage ce que tu écoutes depuis le lecteur !",
                    color = FeedTextSecondary,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        isLiked = likedPostIds.contains(post.id),
                        myAvatarUrl = myProfile?.avatar_url,
                        minutesAgoLabel = relativeTimeLabel(post.created_at),
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
                        onCommentClick = { activeCommentPostId = post.id }
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
private fun FeedHeader(myAvatarUrl: String?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Feed", color = FeedTextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(FeedAccentPurple))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = FeedTextPrimary)
            }
            AsyncImage(
                model = myAvatarUrl,
                contentDescription = "Profil",
                modifier = Modifier.size(32.dp).clip(CircleShape).background(FeedPillBackground)
            )
        }
    }
}

@Composable
private fun FilterRow(selected: FeedFilter, onSelect: (FeedFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FeedFilter.values().forEach { filter ->
            val isSelected = filter == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) FeedAccentPurple else FeedPillBackground)
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    filter.label,
                    color = if (isSelected) Color.White else FeedTextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(FeedPillBackground)
                .padding(10.dp)
        ) {
            Icon(Icons.Default.Tune, contentDescription = "Filtres", tint = FeedTextSecondary, modifier = Modifier.size(16.dp))
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
    var text by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = FeedBackground) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                "Avis sur \"${post.song_title}\"",
                color = FeedTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = myAvatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(FeedPillBackground)
                )
                Spacer(modifier = Modifier.width(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Ajoute ton avis sur son goût musical", color = FeedTextSecondary, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = FeedTextPrimary,
                        unfocusedTextColor = FeedTextPrimary,
                        focusedContainerColor = FeedPillBackground,
                        unfocusedContainerColor = FeedPillBackground,
                        focusedBorderColor = FeedAccentPurple,
                        unfocusedBorderColor = Color.Transparent
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
