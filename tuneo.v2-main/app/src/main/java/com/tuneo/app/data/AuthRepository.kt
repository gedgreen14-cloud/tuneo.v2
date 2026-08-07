package com.tuneo.app.data

import com.tuneo.app.data.remote.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Gère la création de compte (pseudo + mot de passe + photo de profil) et la connexion.
 * Supabase Auth attend un e-mail : on en dérive un en interne à partir du pseudo,
 * l'utilisateur ne voit et ne saisit jamais que son pseudo.
 */
class AuthRepository {

    private val client = SupabaseClientProvider.client
    private val auth get() = client.auth

    private fun emailFor(username: String) =
        "${username.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "")}@tuneo.local"

    val sessionStatus: Flow<Boolean> = auth.sessionStatus.map { it is SessionStatus.Authenticated }

    val currentUserId: String? get() = auth.currentUserOrNull()?.id

    suspend fun isUsernameTaken(username: String): Boolean {
        val existing = client.postgrest.from("profiles")
            .select { filter { eq("username", username) } }
            .decodeList<Profile>()
        return existing.isNotEmpty()
    }

    /**
     * Crée le compte, uploade la photo de profil si fournie, puis crée la ligne `profiles`.
     */
    suspend fun signUp(username: String, password: String, avatarBytes: ByteArray?): Result<Unit> {
        return try {
            if (isUsernameTaken(username)) {
                return Result.failure(IllegalStateException("Ce pseudo est déjà pris."))
            }

            auth.signUpWith(Email) {
                email = emailFor(username)
                this.password = password
            }

            val userId = auth.currentUserOrNull()?.id
                ?: return Result.failure(IllegalStateException("Compte créé mais session introuvable."))

            var avatarUrl: String? = null
            if (avatarBytes != null) {
                val path = "$userId/avatar.jpg"
                client.storage.from("avatars").upload(path, avatarBytes) {
                    upsert = true
                }
                avatarUrl = client.storage.from("avatars").publicUrl(path)
            }

            client.postgrest.from("profiles").insert(
                Profile(id = userId, username = username, avatar_url = avatarUrl)
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(username: String, password: String): Result<Unit> {
        return try {
            auth.signInWith(Email) {
                email = emailFor(username)
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun currentProfile(): Profile? {
        val userId = currentUserId ?: return null
        return client.postgrest.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<Profile>()
    }
}
