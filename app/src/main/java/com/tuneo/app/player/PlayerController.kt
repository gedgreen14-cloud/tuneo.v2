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
import com.tuneo.app.data.PlaylistRepository
import com.tuneo.app.data.Song

/**
 * Pont entre l'UI Compose et le MediaController connecté au PlaybackService.
 * Un seul controller pour toute l'app -> état de lecture partagé entre
 * le mini-player persistant et l'écran "Now Playing".
 */
class PlayerController(private val context: Context) {

    private var controller: MediaController? = null
    private val playlistRepository = PlaylistRepository(context)

    var currentSong by mutableStateOf<Song?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var positionMs by mutableStateOf(0L)
        private set
    var isShuffleEnabled by mutableStateOf(false)
        private set
    var repeatMode by mutableStateOf(Player.REPEAT_MODE_OFF)
        private set

    private var queue: List<Song> = emptyList()
    private var queueById: Map<String, Song> = emptyMap()
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
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                override fun onShuffleModeEnabledChanged(shuffleEnabled: Boolean) {
                    isShuffleEnabled = shuffleEnabled
                }
                override fun onRepeatModeChanged(mode: Int) {
                    repeatMode = mode
                }
                // Se déclenche pour TOUT changement de morceau : next()/previous() manuels,
                // fin de piste automatique, action depuis la notification système,
                // Bluetooth/Android Auto, etc. C'est la seule source fiable pour garder
                // currentSong synchronisé en temps réel avec ce qui joue réellement.
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val mediaId = mediaItem?.mediaId
                    currentSong = mediaId?.let { queueById[it] }
                    currentIndex = controller?.currentMediaItemIndex ?: currentIndex
                    // Alimente l'historique local pour les playlists automatiques
                    // "Lues récemment" et "Les plus jouées".
                    currentSong?.let { playlistRepository.recordPlayback(it.id) }
                }
            })
            isShuffleEnabled = controller?.shuffleModeEnabled ?: false
            repeatMode = controller?.repeatMode ?: Player.REPEAT_MODE_OFF
            positionHandler.post(positionUpdater)
            onReady()
        }, MoreExecutors.directExecutor())
    }

    fun playQueue(songs: List<Song>, startIndex: Int) {
        queue = songs
        queueById = songs.associateBy { it.id.toString() }
        currentIndex = startIndex
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
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
        currentSong?.let { playlistRepository.recordPlayback(it.id) }
    }

    fun togglePlayPause() {
        controller?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun next() {
        // currentSong/currentIndex sont mis à jour par onMediaItemTransition,
        // qui se déclenche de façon fiable après ce seek.
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(ms: Long) {
        controller?.seekTo(ms)
        positionMs = ms
    }

    /**
     * Un seul bouton cyclique, comme sur Lark Player (référence) :
     * Normal (lecture séquentielle) -> Répéter cette chanson (icône avec "1")
     * -> Aléatoire -> retour à Normal.
     * Shuffle et repeat-one sont mutuellement exclusifs : activer l'un désactive l'autre.
     */
    fun cyclePlaybackMode() {
        controller?.let {
            when {
                it.repeatMode == Player.REPEAT_MODE_OFF && !it.shuffleModeEnabled -> {
                    // Normal -> Répéter cette chanson
                    it.repeatMode = Player.REPEAT_MODE_ONE
                    repeatMode = Player.REPEAT_MODE_ONE
                }
                it.repeatMode == Player.REPEAT_MODE_ONE -> {
                    // Répéter cette chanson -> Aléatoire
                    it.repeatMode = Player.REPEAT_MODE_OFF
                    repeatMode = Player.REPEAT_MODE_OFF
                    it.shuffleModeEnabled = true
                    isShuffleEnabled = true
                }
                else -> {
                    // Aléatoire -> Normal
                    it.shuffleModeEnabled = false
                    isShuffleEnabled = false
                    it.repeatMode = Player.REPEAT_MODE_OFF
                    repeatMode = Player.REPEAT_MODE_OFF
                }
            }
        }
    }

    fun release() {
        positionHandler.removeCallbacks(positionUpdater)
        controller?.release()
        controller = null
    }
}
