package com.tuneo.app.data

import com.tuneo.app.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order

class FeedRepository {

    private val client = SupabaseClientProvider.client

    suspend fun fetchFeed(): List<FeedPost> {
        return client.postgrest.from("feed_posts")
            .select {
                order("created_at", Order.DESCENDING)
            }
            .decodeList<FeedPost>()
    }

    suspend fun createPost(post: NewPost): Result<Unit> {
        return try {
            client.postgrest.from("posts").insert(post)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likedPostIds(userId: String): Set<String> {
        val likes = client.postgrest.from("likes")
            .select { filter { eq("user_id", userId) } }
            .decodeList<Like>()
        return likes.map { it.post_id }.toSet()
    }

    suspend fun like(postId: String, userId: String) {
        client.postgrest.from("likes").insert(Like(post_id = postId, user_id = userId))
    }

    suspend fun unlike(postId: String, userId: String) {
        client.postgrest.from("likes").delete {
            filter {
                eq("post_id", postId)
                eq("user_id", userId)
            }
        }
    }

    suspend fun addComment(postId: String, userId: String, content: String) {
        client.postgrest.from("comments").insert(
            NewComment(post_id = postId, user_id = userId, content = content)
        )
    }

    suspend fun comments(postId: String): List<Comment> {
        return client.postgrest.from("comments")
            .select { filter { eq("post_id", postId) } }
            .decodeList<Comment>()
    }

    /**
     * Crée ou met à jour la story "ce que j'écoute actuellement" de l'utilisateur.
     * Une seule story active par utilisateur (contrainte unique sur user_id) :
     * on upsert pour refléter tout changement de morceau local.
     */
    suspend fun upsertStory(userId: String, songTitle: String, songArtist: String, albumArtUrl: String?) {
        client.postgrest.from("stories").upsert(
            Story(
                user_id = userId,
                song_title = songTitle,
                song_artist = songArtist,
                album_art_url = albumArtUrl
            )
        ) {
            onConflict = "user_id"
        }
    }

    suspend fun fetchStories(): List<StoryWithProfile> {
        val stories = client.postgrest.from("stories")
            .select {
                filter { eq("is_active", true) }
                order("updated_at", Order.DESCENDING)
            }
            .decodeList<Story>()

        if (stories.isEmpty()) return emptyList()

        // On récupère tous les profils puis on filtre côté client : évite toute dépendance
        // à une méthode "IN" du DSL Postgrest dont le nom exact varie selon les versions du SDK.
        val userIds = stories.map { it.user_id }.toSet()
        val profiles = client.postgrest.from("profiles")
            .select()
            .decodeList<Profile>()
            .filter { it.id in userIds }
            .associateBy { it.id }

        return stories.mapNotNull { story ->
            profiles[story.user_id]?.let { profile -> StoryWithProfile(story, profile) }
        }
    }
}
