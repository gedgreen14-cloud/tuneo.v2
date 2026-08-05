package com.tuneo.app.player

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.tuneo.app.data.Song

enum class RepeatMode { OFF, REPEAT_ONE, SHUFFLE }

/**
 * Pont entre l'UI Compose et le MediaController connecté au PlaybackService.
 * Un seul controller pour toute l'app -> état de lecture partagé entre
 * le mini-player persistant et l'écran "Now Playing".
 */
class PlayerController(private val context: Context) {

    private var controller: MediaController? = null

    var currentSong by mutableStateOf<Song?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var positionMs by mutableStateOf(0L)
        private set
    var repeatMode by mutableStateOf(RepeatMode.OFF)
        private set

    private var queue: List<Song> = emptyList()
    private var currentIndex: Int = -1

    private val positionHandler = Handler(Looper.getMainLooper())
    private val positionUpdater = object : Runnable {
        override fun run() {
            controller?.let { positionMs = it.currentPosition }
            positionHandler.postDelayed(this, 500L)
        }
    }

    fun connect(onReady: () -> Unit = {}) {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener({
            controller = controllerFuture.get()
            controller?.let { c ->
                // Synchronise l'état initial : si le controller est déjà connecté
                // à une session en cours de lecture, isPlaying doit le refléter
                // immédiatement plutôt qu'attendre un futur événement.
                isPlaying = c.isPlaying
            }
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    // Filet de sécurité : certains états (buffering -> ready,
                    // ended) ne déclenchent pas toujours onIsPlayingChanged.
                    controller?.let { isPlaying = it.isPlaying }
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val index = controller?.currentMediaItemIndex ?: return
                    if (index in queue.indices) {
                        currentIndex = index
                        currentSong = queue[index]
                    }
                }
            })
            positionHandler.post(positionUpdater)
            onReady()
        }, MoreExecutors.directExecutor())
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        queue = songs
        currentIndex = startIndex
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setUri(song.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setArtworkUri(song.albumArtUri)
                        .build()
                )
                .build()
        }
        controller?.setMediaItems(mediaItems, startIndex, 0L)
        controller?.prepare()
        controller?.play()
        currentSong = songs.getOrNull(startIndex)
        isPlaying = true
    }

    fun togglePlayPause() {
        controller?.let {
            if (it.isPlaying) it.pause() else it.play()
            // Mise à jour optimiste immédiate : ne pas attendre le listener,
            // pour que l'icône réagisse au clic sans latence perceptible.
            isPlaying = !it.isPlaying
        }
    }

    /**
     * Cycle : OFF -> REPEAT_ONE (🔂) -> SHUFFLE (🔀) -> OFF.
     * Traduit en réglages ExoPlayer réels (repeatMode / shuffleModeEnabled),
     * pas seulement en état visuel.
     */
    fun toggleRepeatMode() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.REPEAT_ONE
            RepeatMode.REPEAT_ONE -> RepeatMode.SHUFFLE
            RepeatMode.SHUFFLE -> RepeatMode.OFF
        }
        controller?.let { c ->
            when (repeatMode) {
                RepeatMode.OFF -> {
                    c.repeatMode = Player.REPEAT_MODE_OFF
                    c.shuffleModeEnabled = false
                }
                RepeatMode.REPEAT_ONE -> {
                    c.repeatMode = Player.REPEAT_MODE_ONE
                    c.shuffleModeEnabled = false
                }
                RepeatMode.SHUFFLE -> {
                    c.repeatMode = Player.REPEAT_MODE_ALL
                    c.shuffleModeEnabled = true
                }
            }
        }
    }

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        positionMs = ms
    }

    fun release() {
        positionHandler.removeCallbacks(positionUpdater)
        controller?.release()
        controller = null
    }
}
