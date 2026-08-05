package com.tuneo.app.data

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long, // ms
    val uri: Uri,
    val albumArtUri: Uri?,
    val hasLyrics: Boolean = false
)

data class VideoItem(
    val id: Long,
    val displayName: String,
    val duration: Long, // ms
    val resolutionLabel: String, // ex "576P"
    val uri: Uri,
    val thumbnailUri: Uri?
)
