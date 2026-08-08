package com.tuneo.app.data

import com.tuneo.app.data.remote.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.Serializable

class ProfileRepository {

    private val client = SupabaseClientProvider.client

    suspend fun fetchProfile(userId: String): Profile? {
        return client.postgrest.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<Profile>()
    }

    /**
     * Compteurs Publications / Abonnés / Abonnements, calculés en live (COUNT),
     * pas de colonnes dénormalisées à maintenir.
     */
    suspend fun fetchStats(userId: String): ProfileStats {
        val postCount = client.postgrest.from("posts")
            .select { filter { eq("user_id", userId) }; count(Count.EXACT) }
            .countOrNull() ?: 0

        val followerCount = client.postgrest.from("follows")
            .select { filter { eq("following_id", userId) }; count(Count.EXACT) }
            .countOrNull() ?: 0

        val followingCount = client.postgrest.from("follows")
            .select { filter { eq("follower_id", userId) }; count(Count.EXACT) }
            .countOrNull() ?: 0

        return ProfileStats(postCount = postCount, followerCount = followerCount, followingCount = followingCount)
    }

    suspend fun fetchFavoriteArtists(userId: String): List<FavoriteArtist> {
        return client.postgrest.from("favorite_artists")
            .select {
                filter { eq("user_id", userId) }
                order("position", Order.ASCENDING)
            }
            .decodeList<FavoriteArtist>()
    }

    /**
     * Story active de l'utilisateur ("ce qu'il écoute en ce moment"), utilisée pour
     * afficher la carte "Écoute maintenant" sur N'IMPORTE QUEL profil (pas seulement
     * le sien) : la même table que le bandeau de stories du feed, source unique de vérité.
     */
    suspend fun fetchActiveStory(userId: String): Story? {
        return client.postgrest.from("stories")
            .select { filter { eq("user_id", userId); eq("is_active", true) } }
            .decodeSingleOrNull<Story>()
    }

    suspend fun setFavoriteArtists(userId: String, artists: List<FavoriteArtist>) {
        client.postgrest.from("favorite_artists").delete { filter { eq("user_id", userId) } }
        if (artists.isNotEmpty()) {
            client.postgrest.from("favorite_artists").insert(
                artists.mapIndexed { index, artist -> artist.copy(user_id = userId, position = index) }
            )
        }
    }

    /** Posts d'un utilisateur pour l'onglet "Posts" du profil, du plus récent au plus ancien. */
    suspend fun fetchUserPosts(userId: String): List<FeedPost> {
        return client.postgrest.from("feed_posts")
            .select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<FeedPost>()
    }

    suspend fun updateProfile(userId: String, username: String, bio: String?, linkUrl: String?) {
        client.postgrest.from("profiles").update(
            mapOf(
                "username" to username,
                "bio" to bio,
                "link_url" to linkUrl
            )
        ) {
            filter { eq("id", userId) }
        }
    }

    suspend fun uploadAvatar(userId: String, bytes: ByteArray): String {
        val path = "$userId/avatar.jpg"
        client.storage.from("avatars").upload(path, bytes) { upsert = true }
        return client.storage.from("avatars").publicUrl(path)
    }

    /**
     * Photo d'un artiste préféré, stockée dans le même bucket "avatars" sous un chemin
     * dédié par utilisateur pour éviter toute collision entre deux personnes ayant
     * choisi le même artiste.
     */
    suspend fun uploadFavoriteArtistPhoto(userId: String, localArtistId: String, bytes: ByteArray): String {
        val path = "$userId/favorite-artists/$localArtistId.jpg"
        client.storage.from("avatars").upload(path, bytes) { upsert = true }
        return client.storage.from("avatars").publicUrl(path)
    }

    suspend fun updateAvatarUrl(userId: String, avatarUrl: String) {
        client.postgrest.from("profiles").update(
            mapOf("avatar_url" to avatarUrl)
        ) {
            filter { eq("id", userId) }
        }
    }

    /**
     * Incrémente le compteur d'écoutes cumulées depuis la création du compte.
     * Appelé à chaque changement de piste dans PlayerController, uniquement
     * pour l'utilisateur connecté. Passe silencieusement en cas d'échec réseau :
     * ne doit jamais interrompre la lecture locale.
     */
    suspend fun incrementTotalPlays(userId: String) {
        try {
            client.postgrest.rpc("increment_total_plays", IncrementTotalPlaysParams(uid = userId))
        } catch (e: Exception) {
            // Silencieux : la lecture locale ne doit jamais dépendre du réseau.
        }
    }
}

@Serializable
private data class IncrementTotalPlaysParams(val uid: String)
