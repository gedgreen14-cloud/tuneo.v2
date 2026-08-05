package com.tuneo.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.tuneo.app.data.MediaScanner
import com.tuneo.app.data.Song
import com.tuneo.app.data.VideoItem
import com.tuneo.app.player.PlayerController
import com.tuneo.app.ui.components.MiniPlayer
import com.tuneo.app.ui.components.TabsRow
import com.tuneo.app.ui.components.TuneoTab
import com.tuneo.app.ui.screens.*
import com.tuneo.app.ui.theme.PlayerBackground
import com.tuneo.app.ui.theme.TuneoBackground
import com.tuneo.app.ui.theme.TuneoBackgroundLight
import com.tuneo.app.ui.theme.TuneoTheme

class MainActivity : ComponentActivity() {

    private lateinit var playerController: PlayerController

    private fun mediaPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        playerController = PlayerController(applicationContext)
        playerController.connect()

        setContent {
            TuneoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TuneoApp(
                        playerController = playerController,
                        permissions = mediaPermissions(),
                        hasPermission = { perms ->
                            perms.all {
                                checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        playerController.release()
        super.onDestroy()
    }
}

private enum class Screen { LIBRARY, NOW_PLAYING }

@Composable
fun TuneoApp(
    playerController: PlayerController,
    permissions: Array<String>,
    hasPermission: (Array<String>) -> Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var permissionGranted by remember { mutableStateOf(hasPermission(permissions)) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(TuneoTab.SONGS) }
    var screen by remember { mutableStateOf(Screen.LIBRARY) }
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Status bar : le mode clair/sombre est géré nativement par
    // values/themes.xml + values-night/themes.xml (fiable, piloté par l'OS).
    // On ne l'override manuellement que sur Now Playing, dont le fond
    // (vert foncé) est volontairement indépendant du thème clair/sombre.
    val darkTheme = isSystemInDarkTheme()
    val libraryBackground = if (darkTheme) TuneoBackground else TuneoBackgroundLight
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as android.app.Activity).window
        SideEffect {
            if (screen == Screen.NOW_PLAYING) {
                window.statusBarColor = PlayerBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            } else {
                // Retour à Library : on restaure la couleur native du thème XML
                // (values/themes.xml en clair, values-night/themes.xml en sombre)
                // plutôt que de continuer à l'imposer depuis Kotlin.
                window.statusBarColor = libraryBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionGranted = result.values.all { it }
    }

    // Une fois la permission accordée, on scanne la bibliothèque locale
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            val scanner = MediaScanner(context)
            songs = scanner.scanSongs()
            videos = scanner.scanVideos()
        }
    }

    if (!permissionGranted) {
        PermissionScreen(onRequestPermission = { permissionLauncher.launch(permissions) })
        return
    }

    BackHandler(enabled = screen == Screen.NOW_PLAYING) {
        screen = Screen.LIBRARY
    }

    Box(modifier = Modifier.fillMaxSize().background(libraryBackground)) {
        when (screen) {
            Screen.LIBRARY -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    TuneoHeader(
                        searchActive = searchActive,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onToggleSearch = {
                            searchActive = !searchActive
                            if (!searchActive) searchQuery = ""
                        }
                    )
                    TabsRow(selected = selectedTab, onSelect = { selectedTab = it })

                    val filteredSongs = remember(songs, searchQuery) {
                        if (searchQuery.isBlank()) songs
                        else songs.filter {
                            it.title.contains(searchQuery, ignoreCase = true) ||
                                it.artist.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (selectedTab) {
                            TuneoTab.SONGS -> SongListScreen(
                                songs = filteredSongs,
                                onSongClick = { index ->
                                    val song = filteredSongs[index]
                                    val realIndex = songs.indexOf(song)
                                    playerController.playQueue(songs, realIndex)
                                    screen = Screen.NOW_PLAYING
                                }
                            )
                            TuneoTab.VIDEOS -> VideoScreen(
                                videos = videos,
                                onVideoClick = { /* lecteur vidéo à venir */ }
                            )
                            TuneoTab.PLAYLISTS -> PlaceholderScreen("Playlists")
                            TuneoTab.FOLDERS -> PlaceholderScreen("Dossiers")
                            TuneoTab.ARTISTS -> PlaceholderScreen("Artists")
                            TuneoTab.ALBUMS -> PlaceholderScreen("Albums")
                        }
                    }
                }

                // Mini-player persistant en bas, visible si une chanson est chargée
                playerController.currentSong?.let { song ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        MiniPlayer(
                            song = song,
                            isPlaying = playerController.isPlaying,
                            repeatMode = playerController.repeatMode,
                            onTogglePlay = { playerController.togglePlayPause() },
                            onToggleRepeatMode = { playerController.toggleRepeatMode() },
                            onClick = { screen = Screen.NOW_PLAYING },
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }

            Screen.NOW_PLAYING -> {
                playerController.currentSong?.let { song ->
                    NowPlayingScreen(
                        song = song,
                        isPlaying = playerController.isPlaying,
                        positionMs = playerController.positionMs,
                        onTogglePlay = { playerController.togglePlayPause() },
                        onNext = { playerController.next() },
                        onPrevious = { playerController.previous() },
                        onSeek = { playerController.seekTo(it) }
                    )
                }
            }
        }
    }
}
