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
import com.tuneo.app.data.MediaScanner
import com.tuneo.app.data.Song
import com.tuneo.app.data.VideoItem
import com.tuneo.app.player.PlayerController
import com.tuneo.app.ui.components.BottomNavBar
import com.tuneo.app.ui.components.MiniPlayer
import com.tuneo.app.ui.components.TabsRow
import com.tuneo.app.ui.components.TuneoDestination
import com.tuneo.app.ui.components.TuneoTab
import com.tuneo.app.ui.screens.*
import com.tuneo.app.ui.theme.PlayerBackground
import com.tuneo.app.ui.theme.TuneoBackgroundDark
import com.tuneo.app.ui.theme.TuneoBackgroundLight
import com.tuneo.app.ui.theme.TuneoStatusBar
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

private enum class Screen { MAIN, NOW_PLAYING }

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
    var screen by remember { mutableStateOf(Screen.MAIN) }
    var selectedDestination by remember { mutableStateOf(TuneoDestination.BIBLIOTHEQUE) }

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
        screen = Screen.MAIN
    }

    val isDark = isSystemInDarkTheme()
    val libraryBackground = if (isDark) TuneoBackgroundDark else TuneoBackgroundLight

    // La status bar suit le fond de l'écran actuellement affiché :
    // sombre + icônes claires pour la bibliothèque en mode dark,
    // claire + icônes foncées en mode light, et toujours le vert foncé
    // du lecteur sur l'écran Now Playing (peu importe le thème système).
    when (screen) {
        Screen.MAIN -> TuneoStatusBar(backgroundColor = libraryBackground, useDarkIcons = !isDark)
        Screen.NOW_PLAYING -> TuneoStatusBar(backgroundColor = PlayerBackground, useDarkIcons = false)
    }

    Box(modifier = Modifier.fillMaxSize().background(libraryBackground)) {
        when (screen) {
            Screen.MAIN -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Contenu de la destination sélectionnée
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedDestination) {
                            TuneoDestination.ACCUEIL -> PlaceholderScreen("Feed d'actualité")

                            TuneoDestination.DECOUVERTE -> PlaceholderScreen("Découverte")

                            TuneoDestination.BIBLIOTHEQUE -> Column(modifier = Modifier.fillMaxSize()) {
                                TuneoHeader()
                                TabsRow(selected = selectedTab, onSelect = { selectedTab = it })

                                Box(modifier = Modifier.fillMaxSize()) {
                                    when (selectedTab) {
                                        TuneoTab.SONGS -> SongListScreen(
                                            songs = songs,
                                            onSongClick = { index ->
                                                playerController.playQueue(songs, index)
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

                            TuneoDestination.NOTIFICATIONS -> PlaceholderScreen("Notifications")

                            TuneoDestination.PROFIL -> PlaceholderScreen("Profil")
                        }

                        // Mini-player persistant, flotte au-dessus du contenu, juste au-dessus de la nav bar
                        playerController.currentSong?.let { song ->
                            MiniPlayer(
                                song = song,
                                isPlaying = playerController.isPlaying,
                                onTogglePlay = { playerController.togglePlayPause() },
                                onClick = { screen = Screen.NOW_PLAYING },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }

                    // Barre de navigation, toujours visible en bas sur l'écran principal
                    BottomNavBar(
                        selected = selectedDestination,
                        onSelect = { selectedDestination = it }
                    )
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
