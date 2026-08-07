package com.tuneo.app.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Persistance locale simple (SharedPreferences + JSON) pour les playlists
 * créées par l'utilisateur, les morceaux aimés, et l'historique de lecture
 * qui alimente les playlists automatiques (Chansons aimées, Lues récemment,
 * Les plus jouées). Pas de backend requis : ce sont des données propres à l'appareil.
 */
class PlaylistRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("tuneo_playlists", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    // ---- Playlists manuelles ----

    fun getPlaylists(): List<Playlist> {
        val raw = prefs.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<Playlist>>(raw) }.getOrDefault(emptyList())
    }

    private fun savePlaylists(playlists: List<Playlist>) {
        prefs.edit().putString(KEY_PLAYLISTS, json.encodeToString(playlists)).apply()
    }

    fun createPlaylist(name: String): Playlist {
        val playlist = Playlist(id = UUID.randomUUID().toString(), name = name)
        savePlaylists(getPlaylists() + playlist)
        return playlist
    }

    fun addSongToPlaylist(playlistId: String, songId: Long) {
        val updated = getPlaylists().map { playlist ->
            if (playlist.id == playlistId && !playlist.songIds.contains(songId)) {
                playlist.copy(songIds = playlist.songIds + songId)
            } else {
                playlist
            }
        }
        savePlaylists(updated)
    }

    fun removeSongFromPlaylist(playlistId: String, songId: Long) {
        val updated = getPlaylists().map { playlist ->
            if (playlist.id == playlistId) {
                playlist.copy(songIds = playlist.songIds.filterNot { it == songId })
            } else {
                playlist
            }
        }
        savePlaylists(updated)
    }

    fun deletePlaylist(playlistId: String) {
        savePlaylists(getPlaylists().filterNot { it.id == playlistId })
    }

    // ---- Morceaux aimés ----

    fun getLikedSongIds(): Set<Long> {
        val raw = prefs.getString(KEY_LIKED, null) ?: return emptySet()
        return runCatching { json.decodeFromString<LikedSongs>(raw).songIds }.getOrDefault(emptySet())
    }

    fun toggleLiked(songId: Long) {
        val current = getLikedSongIds()
        val updated = if (current.contains(songId)) current - songId else current + songId
        prefs.edit().putString(KEY_LIKED, json.encodeToString(LikedSongs(updated))).apply()
    }

    // ---- Historique de lecture (alimente Lues récemment / Les plus jouées) ----

    fun getHistory(): List<PlaybackHistoryEntry> {
        val raw = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<PlaybackHistoryEntry>>(raw)
        }.getOrDefault(emptyList())
    }

    fun recordPlayback(songId: Long) {
        val now = System.currentTimeMillis()
        val history = getHistory().toMutableList()
        val existingIndex = history.indexOfFirst { it.songId == songId }
        if (existingIndex >= 0) {
            val existing = history[existingIndex]
            history[existingIndex] = existing.copy(
                lastPlayedAtMs = now,
                playCount = existing.playCount + 1
            )
        } else {
            history.add(PlaybackHistoryEntry(songId = songId, lastPlayedAtMs = now))
        }
        // Borne raisonnable pour éviter une croissance illimitée du stockage local.
        val trimmed = history.sortedByDescending { it.lastPlayedAtMs }.take(500)
        prefs.edit().putString(KEY_HISTORY, json.encodeToString(trimmed)).apply()
    }

    private companion object {
        const val KEY_PLAYLISTS = "playlists"
        const val KEY_LIKED = "liked_songs"
        const val KEY_HISTORY = "playback_history"
    }
}
