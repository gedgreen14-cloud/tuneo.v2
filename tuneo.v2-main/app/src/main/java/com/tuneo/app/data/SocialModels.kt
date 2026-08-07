package com.tuneo.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val username: String,
    val avatar_url: String? = null,
    val bio: String? = null,
    val link_url: String? = null,
    val total_plays: Long = 0,
    val created_at: String? = null
)

/** Un artiste mis en avant sur le profil d'un utilisateur (ligne "Artistes préférés"). */
@Serializable
data class FavoriteArtist(
    val id: String? = null,
    val user_id: String,
    val artist_name: String,
    val avatar_url: String? = null,
    val position: Int = 0
)

/**
 * Compteurs agrégés du profil (Publications / Abonnés / Abonnements),
 * calculés en live via COUNT côté Supabase — pas de colonnes stockées.
 */
data class ProfileStats(
    val postCount: Long = 0,
    val followerCount: Long = 0,
    val followingCount: Long = 0
)

@Serializable
data class Story(
    val id: String? = null,
    val user_id: String,
    val song_title: String,
    val song_artist: String,
    val album_art_url: String? = null,
    val is_active: Boolean = true,
    val updated_at: String? = null
)

/** Story enrichie avec les infos du profil, pour l'affichage dans le bandeau. */
data class StoryWithProfile(
    val story: Story,
    val profile: Profile
)

@Serializable
data class NewPost(
    val user_id: String,
    val song_title: String,
    val song_artist: String,
    val song_genre: String? = null,
    val album_art_url: String? = null,
    val caption: String? = null
)

/** Correspond à la vue SQL `feed_posts` (post + profil + compteurs agrégés). */
@Serializable
data class FeedPost(
    val id: String,
    val user_id: String,
    val username: String,
    val user_avatar_url: String? = null,
    val song_title: String,
    val song_artist: String,
    val song_genre: String? = null,
    val album_art_url: String? = null,
    val caption: String? = null,
    val source_label: String,
    val created_at: String,
    val like_count: Long = 0,
    val comment_count: Long = 0,
    val share_count: Long = 0
)

@Serializable
data class Comment(
    val id: String? = null,
    val post_id: String,
    val user_id: String,
    val content: String,
    val created_at: String? = null
)

@Serializable
data class NewComment(
    val post_id: String,
    val user_id: String,
    val content: String
)

@Serializable
data class Follow(
    val follower_id: String,
    val following_id: String
)

@Serializable
data class Like(
    val post_id: String,
    val user_id: String
)

/** Variante de lecture de `likes`, avec created_at, pour trier les premiers likers d'un post. */
@Serializable
data class LikeWithTimestamp(
    val post_id: String,
    val user_id: String,
    val created_at: String
)
