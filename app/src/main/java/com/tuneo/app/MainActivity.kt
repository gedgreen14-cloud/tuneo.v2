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
import com.tuneo.app.data.AuthRepository
import com.tuneo.app.data.FeedRepository
import com.tuneo.app.data.MediaScanner
import com.tuneo.app.data.Profile
import com.tuneo.app.data.Song
import com.tuneo.app.data.VideoItem
import com.tuneo.app.player.PlayerController
import com.tuneo.app.ui.components.BottomNavBar
import com.tuneo.app.ui.components.MiniPlayer
import com.tuneo.app.ui.components.TabsRow
import com.tuneo.app.ui.components.TuneoDestination
import com.tuneo.app.ui.components.TuneoTab
import com.tuneo.app.ui.screens.*
import com.tuneo.app.ui.theme.FeedBackground
import com.tuneo.app.ui.theme.contentColorFor
import com.tuneo.app.ui.theme.rememberDominantColor
import com.tuneo.app.ui.theme.TuneoBackgroundDark
import com.tuneo.app.ui.theme.TuneoBackgroundLight
import com.tuneo.app.ui.theme.TuneoStatusBar
import com.tuneo.app.ui.theme.TuneoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

/**
 * Écrans plein écran de l'app. AUTH_GATE regroupe le flow de connexion/inscription,
 * déclenché soit par le bouton "Partager" du lecteur (si pas connecté),
 * soit directement par le "+" de story.
 */
private enum class Screen { MAIN, NOW_PLAYING, LOGIN, SIGN_UP, SHARE_CAPTION }

@Composable
fun TuneoApp(
    playerController: PlayerController,
    permissions: Array<String>,
    hasPermission: (Array<String>) -> Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember { AuthRepository() }
    val feedRepository = remember { FeedRepository() }

    var permissionGranted by remember { mutableStateOf(hasPermission(permissions)) }
    var songs by remember { mutableStateOf<List<Song>>(emptyList()) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var selectedTab by remember { mutableStateOf(TuneoTab.SONGS) }
    var screen by remember { mutableStateOf(Screen.MAIN) }
    var selectedDestination by remember { mutableStateOf(TuneoDestination.BIBLIOTHEQUE) }

    // Session utilisateur (compte Tuneo, distinct des permissions Android)
    var isAuthenticated by remember { mutableStateOf(false) }
    var myProfile by remember { mutableStateOf<Profile?>(null) }
    // Quand vrai, après connexion/inscription réussie on enchaîne directement sur l'écran de partage
    var pendingShareAfterAuth by remember { mutableStateOf(false) }

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

    // Vérifie s'il existe déjà une session Tuneo active au lancement
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val userId = authRepository.currentUserId
            if (userId != null) {
                val profile = authRepository.currentProfile()
                isAuthenticated = profile != null
                myProfile = profile
            }
        }
    }

    // Chaque fois que la chanson en cours de lecture locale change, on met à jour
    // automatiquement la story "ce que j'écoute" si l'utilisateur est connecté.
    LaunchedEffect(playerController.currentSong, isAuthenticated) {
        val song = playerController.currentSong
        val profile = myProfile
        if (song != null && isAuthenticated && profile != null) {
            withContext(Dispatchers.IO) {
                feedRepository.upsertStory(
                    userId = profile.id,
                    songTitle = song.title,
                    songArtist = song.artist,
                    albumArtUrl = song.albumArtUri?.toString()
                )
            }
        }
    }

    if (!permissionGranted) {
        PermissionScreen(onRequestPermission = { permissionLauncher.launch(permissions) })
        return
    }

    BackHandler(enabled = screen != Screen.MAIN) {
        screen = if (screen == Screen.SIGN_UP || screen == Screen.LOGIN || screen == Screen.SHARE_CAPTION) {
            Screen.NOW_PLAYING
        } else {
            Screen.MAIN
        }
    }

    val isDark = isSystemInDarkTheme()
    val libraryBackground = if (isDark) TuneoBackgroundDark else TuneoBackgroundLight

    // La status bar suit le fond de l'écran actuellement affiché.
    // Sur Now Playing, ce fond est dynamique (couleur dominante de la pochette en cours).
    val nowPlayingBackground = playerController.currentSong?.let { rememberDominantColor(it.albumArtUri) }
    when (screen) {
        Screen.MAIN -> TuneoStatusBar(backgroundColor = libraryBackground, useDarkIcons = !isDark)
        Screen.NOW_PLAYING -> TuneoStatusBar(
            backgroundColor = nowPlayingBackground ?: libraryBackground,
            useDarkIcons = nowPlayingBackground?.let { contentColorFor(it) == androidx.compose.ui.graphics.Color(0xFF1A1A1A) } ?: !isDark
        )
        Screen.LOGIN, Screen.SIGN_UP, Screen.SHARE_CAPTION ->
            TuneoStatusBar(backgroundColor = FeedBackground, useDarkIcons = false)
    }

    Box(modifier = Modifier.fillMaxSize().background(libraryBackground)) {
        when (screen) {
            Screen.MAIN -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Contenu de la destination sélectionnée
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedDestination) {
                            TuneoDestination.ACCUEIL -> FeedScreen(
                                isAuthenticated = isAuthenticated,
                                myProfile = myProfile,
                                onAddStoryClick = {
                                    if (!isAuthenticated) {
                                        pendingShareAfterAuth = false
                                        screen = Screen.SIGN_UP
                                    }
                                    // Si déjà connecté : la story se met déjà à jour automatiquement
                                    // en fonction de la chanson en cours (voir LaunchedEffect ci-dessus).
                                }
                            )

                            TuneoDestination.DECOUVERTE -> PlaceholderScreen("Découverte")

                            TuneoDestination.BIBLIOTHEQUE -> Column(modifier = Modifier.fillMaxSize()) {
                                TuneoHeader()
                                TabsRow(selected = selectedTab, onSelect = { selectedTab = it })

                                Box(modifier = Modifier.fillMaxSize()) {
                                    when (selectedTab) {
                                        TuneoTab.SONGS -> SongListScreen(
                                            songs = songs,
                                            currentSong = playerController.currentSong,
                                            isPlaying = playerController.isPlaying,
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
                            val miniBackground = rememberDominantColor(song.albumArtUri)
                            val miniContent = contentColorFor(miniBackground)
                            MiniPlayer(
                                song = song,
                                isPlaying = playerController.isPlaying,
                                isShuffleEnabled = playerController.isShuffleEnabled,
                                repeatMode = playerController.repeatMode,
                                onTogglePlay = { playerController.togglePlayPause() },
                                onCyclePlaybackMode = { playerController.cyclePlaybackMode() },
                                onClick = { screen = Screen.NOW_PLAYING },
                                backgroundColor = miniBackground,
                                contentColor = miniContent,
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
                        onSeek = { playerController.seekTo(it) },
                        onShareClick = {
                            // Logique demandée : pas de compte -> écran de création de compte ;
                            // déjà connecté -> écran de légende directement.
                            screen = if (isAuthenticated) {
                                Screen.SHARE_CAPTION
                            } else {
                                pendingShareAfterAuth = true
                                Screen.SIGN_UP
                            }
                        }
                    )
                }
            }

            Screen.SIGN_UP -> {
                SignUpScreen(
                    onBack = { screen = if (pendingShareAfterAuth) Screen.NOW_PLAYING else Screen.MAIN },
                    onSignedUp = { profile ->
                        isAuthenticated = true
                        myProfile = profile
                        screen = if (pendingShareAfterAuth) Screen.SHARE_CAPTION else Screen.MAIN
                        pendingShareAfterAuth = false
                    },
                    onGoToLogin = { screen = Screen.LOGIN }
                )
            }

            Screen.LOGIN -> {
                LoginScreen(
                    onBack = { screen = if (pendingShareAfterAuth) Screen.NOW_PLAYING else Screen.MAIN },
                    onSignedIn = { profile ->
                        isAuthenticated = true
                        myProfile = profile
                        screen = if (pendingShareAfterAuth) Screen.SHARE_CAPTION else Screen.MAIN
                        pendingShareAfterAuth = false
                    },
                    onGoToSignUp = { screen = Screen.SIGN_UP }
                )
            }

            Screen.SHARE_CAPTION -> {
                val song = playerController.currentSong
                val profile = myProfile
                if (song != null && profile != null) {
                    ShareCaptionScreen(
                        song = song,
                        userId = profile.id,
                        onCancel = { screen = Screen.NOW_PLAYING },
                        onPublished = {
                            selectedDestination = TuneoDestination.ACCUEIL
                            screen = Screen.MAIN
                        }
                    )
                } else {
                    // Pas de morceau en cours ou pas de profil : retour au lecteur par sécurité
                    screen = Screen.NOW_PLAYING
                }
            }
        }
    }
}
