package com.tuneo.app.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

/**
 * Scanne la bibliothèque locale du téléphone via MediaStore.
 * Nécessite que la permission READ_MEDIA_AUDIO / READ_MEDIA_VIDEO
 * (ou READ_EXTERNAL_STORAGE sur API < 33) soit déjà accordée.
 */
class MediaScanner(private val context: Context) {

    fun scanSongs(): List<Song> {
        val songs = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                )
                val artUri = ContentUris.withAppendedId(
                    android.net.Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )

                val rawTitle = cursor.getString(titleCol)
                val displayName = cursor.getString(displayNameCol)
                val resolvedTitle = rawTitle
                    ?.takeIf { it.isNotBlank() && !it.equals("Audio", ignoreCase = true) }
                    ?: displayName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() }
                    ?: "Inconnu"

                songs.add(
                    Song(
                        id = id,
                        title = resolvedTitle,
                        artist = cursor.getString(artistCol) ?: "Artiste inconnu",
                        album = cursor.getString(albumCol) ?: "",
                        duration = cursor.getLong(durationCol),
                        uri = contentUri,
                        albumArtUri = artUri
                    )
                )
            }
        }
        return songs
    }

    fun scanVideos(): List<VideoItem> {
        val videos = mutableListOf<VideoItem>()

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.HEIGHT
        )
        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                )
                val height = cursor.getInt(heightCol)

                videos.add(
                    VideoItem(
                        id = id,
                        displayName = cursor.getString(nameCol) ?: "Vidéo",
                        duration = cursor.getLong(durationCol),
                        resolutionLabel = if (height > 0) "${height}P" else "",
                        uri = contentUri,
                        thumbnailUri = contentUri
                    )
                )
            }
        }
        return videos
    }
}
