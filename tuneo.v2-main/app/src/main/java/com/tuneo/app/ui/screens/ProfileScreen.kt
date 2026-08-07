package com.tuneo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tuneo.app.data.FavoriteArtist
import com.tuneo.app.data.FeedPost
import com.tuneo.app.data.FeedRepository
import com.tuneo.app.data.Profile
import com.tuneo.app.data.ProfileRepository
import com.tuneo.app.data.ProfileStats
import com.tuneo.app.data.Song
import com.tuneo.app.data.Story
import com.tuneo.app.ui.components.EqualizerBars
import com.tuneo.app.ui.theme.FeedAccentPurple
import com.tuneo.app.ui.theme.FeedBackgroundDark
import com.tuneo.app.ui.theme.FeedBackgroundLight
import com.tuneo.app.ui.theme.FeedTextPrimaryDark
import com.tuneo.app.ui.theme.FeedTextPrimaryLight
import com.tuneo.app.ui.theme.FeedTextSecondaryDark
import com.tuneo.app.ui.theme.FeedTextSecondaryLight
import kotlinx.coroutines.launch

private enum class ProfileTab { POSTS, PLAYLISTS }

/**
 * Écran de profil, réutilisable pour le compte connecté (isOwnProfile = true,
 * boutons "Modifier le profil") ou pour le profil d'un autre utilisateur
 * (boutons Suivre/Message). Le morceau en cours ("Écoute maintenant") vient
 * du lecteur local uniquement quand on regarde son propre profil.
 */
@Composable
fun ProfileScreen(
    userId: String,
    isOwnProfile: Boolean,
    viewerUserId: String?,
    currentlyPlayingSong: Song?,
    isCurrentlyPlaying: Boolean,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onEditFavoriteArtists: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onMessageClick: (Profile) -> Unit = {}
) {
    val isDark = isSystemInDarkTheme()
    val background = if (isDark) FeedBackgroundDark else FeedBackgroundLight
    val primaryColor = if (isDark) FeedTextPrimaryDark else FeedTextPrimaryLight
    val secondaryColor = if (isDark) FeedTextSecondaryDark else FeedTextSecondaryLight

    val scope = rememberCoroutineScope()
    val profileRepository = remember { ProfileRepository() }
    val feedRepository = remember { FeedRepository() }

    var profile by remember { mutableStateOf<Profile?>(null) }
    var stats by remember { mutableStateOf(ProfileStats()) }
    var favoriteArtists by remember { mutableStateOf<List<FavoriteArtist>>(emptyList()) }
    var posts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var activeStory by remember { mutableStateOf<Story?>(null) }
    var isFollowing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(ProfileTab.POSTS) }
    var showMenu by remember { mutableStateOf(false) }
    var showSignOutConfirm by remember { mutableStateOf(false) }

    suspend fun reload() {
        profile = profileRepository.fetchProfile(userId)
        stats = profileRepository.fetchStats(userId)
        favoriteArtists = profileRepository.fetchFavoriteArtists(userId)
        posts = profileRepository.fetchUserPosts(userId)
        activeStory = profileRepository.fetchActiveStory(userId)
        if (!isOwnProfile && viewerUserId != null) {
            isFollowing = feedRepository.followingIds(viewerUserId).contains(userId)
        }
        isLoading = false
    }

    LaunchedEffect(userId) {
        isLoading = true
        reload()
    }

    // Sur son propre profil, la story Supabase se met déjà à jour automatiquement à chaque
    // changement de piste (upsertStory dans MainActivity) : on rafraîchit ici pour refléter
    // ça sans attendre une navigation complète de l'écran.
    LaunchedEffect(currentlyPlayingSong, isOwnProfile) {
        if (isOwnProfile) {
            activeStory = profileRepository.fetchActiveStory(userId)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(background)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Retour",
                tint = primaryColor,
                modifier = Modifier.clickable { onBack() }
            )
            Box {
                Icon(
                    Icons.Default.MoreHoriz,
                    contentDescription = "Plus d'options",
                    tint = primaryColor,
                    modifier = Modifier.clickable { showMenu = true }
                )
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (isOwnProfile) {
                        DropdownMenuItem(
                            text = { Text("Modifier les artistes préférés") },
                            onClick = {
                                showMenu = false
                                onEditFavoriteArtists()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Déconnexion", color = Color(0xFFE0455F)) },
                            onClick = {
                                showMenu = false
                                showSignOutConfirm = true
                            }
                        )
                    }
                }
            }
        }

        if (showSignOutConfirm) {
            AlertDialog(
                onDismissRequest = { showSignOutConfirm = false },
                title = { Text("Se déconnecter ?") },
                text = { Text("Tu pourras te reconnecter ou créer un autre compte à tout moment.") },
                confirmButton = {
                    TextButton(onClick = {
                        showSignOutConfirm = false
                        onSignOut()
                    }) { Text("Déconnexion", color = Color(0xFFE0455F)) }
                },
                dismissButton = {
                    TextButton(onClick = { showSignOutConfirm = false }) { Text("Annuler") }
                }
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FeedAccentPurple)
            }
            return@Column
        }

        val currentProfile = profile
        if (currentProfile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Profil introuvable", color = secondaryColor)
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                ProfileHeaderSection(
                    profile = currentProfile,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor
                )

                StatsRow(stats = stats, totalPlays = currentProfile.total_plays, secondaryColor = secondaryColor, primaryColor = primaryColor)

                ActionsRow(
                    isOwnProfile = isOwnProfile,
                    isFollowing = isFollowing,
                    secondaryColor = secondaryColor,
                    onEditProfile = onEditProfile,
                    onFollowToggle = {
                        if (viewerUserId != null) {
                            scope.launch {
                                if (isFollowing) {
                                    feedRepository.unfollow(viewerUserId, userId)
                                } else {
                                    feedRepository.follow(viewerUserId, userId)
                                }
                                isFollowing = !isFollowing
                                stats = profileRepository.fetchStats(userId)
                            }
                        }
                    },
                    onMessageClick = { onMessageClick(currentProfile) }
                )

                if (favoriteArtists.isNotEmpty()) {
                    Text(
                        "Artistes préférés",
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    FavoriteArtistsRow(artists = favoriteArtists)
                    Spacer(modifier = Modifier.height(20.dp))
                }

                if (activeStory != null) {
                    NowPlayingCard(
                        story = activeStory,
                        isPlaying = if (isOwnProfile) isCurrentlyPlaying else true,
                        secondaryColor = secondaryColor,
                        primaryColor = primaryColor
                    )
                }

                ProfileTabsRow(
                    selected = selectedTab,
                    onSelect = { selectedTab = it },
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor
                )
            }

            when (selectedTab) {
                ProfileTab.POSTS -> {
                    if (posts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Aucune publication pour le moment", color = secondaryColor, fontSize = 14.sp)
                            }
                        }
                    } else {
                        items(posts) { post ->
                            ProfilePostPreview(post = post, primaryColor = primaryColor, secondaryColor = secondaryColor)
                        }
                    }
                }
                ProfileTab.PLAYLISTS -> {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Aucune playlist publique pour le moment", color = secondaryColor, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderSection(profile: Profile, primaryColor: Color, secondaryColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(FeedAccentPurple, Color(0xFF2B1A5E))))
        ) {
            if (profile.avatar_url != null) {
                AsyncImage(
                    model = profile.avatar_url,
                    contentDescription = profile.username,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                profile.username,
                color = primaryColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
            profile.bio?.let { bio ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(bio, color = if (primaryColor == FeedTextPrimaryDark) Color(0xFFE5E5E5) else Color(0xFF2A2A2A), fontSize = 14.sp, lineHeight = 19.sp)
            }
            profile.link_url?.let { link ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    link,
                    color = Color(0xFFA78BFA),
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline
                )
            }
        }
    }
}

@Composable
private fun StatsRow(stats: ProfileStats, totalPlays: Long, secondaryColor: Color, primaryColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatItem(formatStatCount(stats.postCount), "Publications", primaryColor, secondaryColor)
        StatItem(formatStatCount(stats.followerCount), "Abonnés", primaryColor, secondaryColor)
        StatItem(formatStatCount(stats.followingCount), "Abonnements", primaryColor, secondaryColor)
        StatItem(formatStatCount(totalPlays), "Écoutes", primaryColor, secondaryColor)
    }
}

@Composable
private fun StatItem(value: String, label: String, primaryColor: Color, secondaryColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = primaryColor, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, color = secondaryColor, fontSize = 12.sp)
    }
}

@Composable
private fun ActionsRow(
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    secondaryColor: Color,
    onEditProfile: () -> Unit,
    onFollowToggle: () -> Unit,
    onMessageClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isOwnProfile) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(FeedAccentPurple)
                    .clickable { onEditProfile() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Modifier le profil", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isFollowing) Color.Transparent else FeedAccentPurple)
                    .border(
                        width = if (isFollowing) 1.dp else 0.dp,
                        color = Color(0xFF444444),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onFollowToggle() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isFollowing) "Abonné(e)" else "Suivre",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF444444), RoundedCornerShape(10.dp))
                    .clickable { onMessageClick() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Message", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun FavoriteArtistsRow(artists: List<FavoriteArtist>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        artists.forEach { artist ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF555555), Color(0xFF222222))))
                ) {
                    if (artist.avatar_url != null) {
                        AsyncImage(
                            model = artist.avatar_url,
                            contentDescription = artist.artist_name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(artist.artist_name, color = Color(0xFFD4D4D4), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun NowPlayingCard(story: Story?, isPlaying: Boolean, secondaryColor: Color, primaryColor: Color) {
    if (story == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF131313))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF150A30))
        ) {
            story.album_art_url?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EqualizerBars(isPlaying = isPlaying, color = Color(0xFFA78BFA), barWidth = 2.dp, maxHeight = 10.dp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Écoute maintenant", color = Color(0xFFA78BFA), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(story.song_title, color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(story.song_artist, color = secondaryColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .border(1.5.dp, primaryColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Lecture", tint = primaryColor, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ProfileTabsRow(
    selected: ProfileTab,
    onSelect: (ProfileTab) -> Unit,
    primaryColor: Color,
    secondaryColor: Color
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        ProfileTabItem("Posts", selected == ProfileTab.POSTS, primaryColor, secondaryColor) { onSelect(ProfileTab.POSTS) }
        ProfileTabItem("Playlists", selected == ProfileTab.PLAYLISTS, primaryColor, secondaryColor) { onSelect(ProfileTab.PLAYLISTS) }
    }
    HorizontalDivider(color = Color(0xFF1F1F1F))
}

@Composable
private fun RowScope.ProfileTabItem(
    label: String,
    isSelected: Boolean,
    primaryColor: Color,
    secondaryColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            color = if (isSelected) primaryColor else secondaryColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(if (isSelected) FeedAccentPurple else Color.Transparent)
        )
    }
}

@Composable
private fun ProfilePostPreview(post: FeedPost, primaryColor: Color, secondaryColor: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = Color(0xFF111111))
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f)) {
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
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EqualizerBars(isPlaying = true, color = FeedAccentPurple, barWidth = 3.dp, maxHeight = 12.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ÉCOUTE ACTUELLE", color = FeedAccentPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(post.song_title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(post.song_artist, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun formatStatCount(count: Long): String {
    return when {
        count >= 1000 -> String.format("%.1fK", count / 1000.0).replace(".0K", "K")
        else -> count.toString()
    }
}
