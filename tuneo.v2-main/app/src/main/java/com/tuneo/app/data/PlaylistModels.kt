package com.tuneo.app.data

import kotlinx.serialization.Serializable

/**
 * Playlist créée manuellement par l'utilisateur.
 * songIds préserve l'ordre d'ajout : le dernier élément est le dernier morceau ajouté,
 * utilisé pour déterminer la miniature de la playlist.
 */
@Serializable
data class Playlist(
    val id: String,
    val name: String,
    val songIds: List<Long> = emptyList()
)

/**
 * Historique minimal nécessaire pour calculer les playlists automatiques
 * (Chansons aimées, Lues récemment, Les plus jouées) sans dupliquer les données
 * déjà disponibles via MediaScanner.
 */
@Serializable
data class PlaybackHistoryEntry(
    val songId: Long,
    val lastPlayedAtMs: Long,
    val playCount: Int = 1
)

@Serializable
data class LikedSongs(
    val songIds: Set<Long> = emptySet()
)
