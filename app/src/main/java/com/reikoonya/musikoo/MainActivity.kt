package com.reikoonya.musikoo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.reikoonya.musikoo.data.loadAudioFiles
import com.reikoonya.musikoo.ui.*
import com.reikoonya.musikoo.ui.ArtistsScreen
import com.reikoonya.musikoo.ui.ArtistDetailScreen
import com.reikoonya.musikoo.ui.theme.MusikooTheme
import com.reikoonya.musikoo.util.MusicPlayerManager
import com.reikoonya.musikoo.util.normalizeArtistName
import kotlin.random.Random

sealed class Screen {
    object MainMenu : Screen()
    object Songs : Screen()
    object Albums : Screen()
    data class AlbumDetail(val albumName: String) : Screen()
    object Artists : Screen()
    data class ArtistDetail(val artistName: String) : Screen()
    object Settings : Screen()
    object About : Screen()
    data class Player(val songIndex: Int) : Screen()
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            recreate()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestAudioPermission()

        setContent {
            MusikooTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    val context = LocalContext.current
                    var songs by remember { mutableStateOf(loadAudioFiles(context)) }
                    val backStack = remember { mutableStateListOf<Screen>(Screen.MainMenu) }
                    val currentScreen = backStack.lastOrNull() ?: Screen.MainMenu

                    fun navigateTo(screen: Screen) {
                        if (currentScreen != screen) {
                            backStack.add(screen)
                        }
                    }

                    fun popBackStack() {
                        if (backStack.size > 1) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    }

                    BackHandler(enabled = backStack.size > 1) {
                        popBackStack()
                    }

                    LaunchedEffect(Unit) {
                        MusicPlayerManager.initPlayer(context)
                    }

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            val isBack = backStack.indexOf(targetState) < backStack.indexOf(initialState)
                            val isTopDropSheet = { s: Screen -> s is Screen.Settings || s is Screen.About || s is Screen.Albums || s is Screen.AlbumDetail }
                            val isBottomSlideSheet = { s: Screen -> s is Screen.Player || s is Screen.Artists || s is Screen.ArtistDetail || s is Screen.Songs }
                            when {
                                (initialState is Screen.MainMenu && isBottomSlideSheet(targetState)) -> {
                                    (slideInVertically { height -> height } + fadeIn()) togetherWith
                                            (slideOutVertically { height -> -height / 4 } + fadeOut())
                                }
                                (isBottomSlideSheet(initialState) && targetState is Screen.MainMenu) -> {
                                    (slideInVertically { height -> -height / 4 } + fadeIn()) togetherWith
                                            (slideOutVertically { height -> height } + fadeOut())
                                }
                                (initialState is Screen.MainMenu && isTopDropSheet(targetState)) -> {
                                    (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                            (slideOutVertically { height -> height / 4 } + fadeOut())
                                }
                                (isTopDropSheet(initialState) && targetState is Screen.MainMenu) -> {
                                    (slideInVertically { height -> height / 4 } + fadeIn()) togetherWith
                                            (slideOutVertically { height -> -height } + fadeOut())
                                }
                                (initialState is Screen.MainMenu && targetState is Screen.AlbumDetail) ||
                                (initialState is Screen.AlbumDetail && targetState is Screen.MainMenu && isBack) -> {
                                    if (targetState is Screen.AlbumDetail) {
                                        (fadeIn() + scaleIn(initialScale = 0.8f)) togetherWith
                                                (fadeOut() + scaleOut(targetScale = 1.1f))
                                    } else {
                                        (fadeIn() + scaleIn(initialScale = 1.1f)) togetherWith
                                                (fadeOut() + scaleOut(targetScale = 0.8f))
                                    }
                                }
                                isBack -> {
                                    (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                                            (slideOutHorizontally { width -> width } + fadeOut())
                                }
                                else -> {
                                    (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                                            (slideOutHorizontally { width -> -width } + fadeOut())
                                }
                            }
                        },
                        label = "ScreenTransition",
                    ) { screen ->
                        when (screen) {
                            is Screen.MainMenu -> {
                                MainMenuScreen(
                                    songs = songs,
                                    onSongClick = { index ->
                                        navigateTo(Screen.Player(index))
                                    },
                                    onAlbumClick = { albumName ->
                                        navigateTo(Screen.AlbumDetail(albumName))
                                    },
                                    onSettingsClick = { navigateTo(Screen.Settings) },
                                    onAboutClick = { navigateTo(Screen.About) },
                                    onSongsClick = { navigateTo(Screen.Songs) },
                                    onArtistsClick = { navigateTo(Screen.Artists) },
                                    onRandomizeClick = {
                                        if (songs.isNotEmpty()) {
                                            val randomIndex = Random.nextInt(songs.size)
                                            MusicPlayerManager.playSong(songs, randomIndex)
                                            navigateTo(Screen.Player(randomIndex))
                                        }
                                    },
                                )
                            }
                            is Screen.Songs -> {
                                SongsScreen(
                                    songs = songs,
                                    onSongClick = { index ->
                                        MusicPlayerManager.playSong(songs, index)
                                        navigateTo(Screen.Player(index))
                                    },
                                ) { popBackStack() }
                            }
                            is Screen.Albums -> {
                                AlbumsScreen(
                                    songs = songs,
                                    onAlbumClick = { albumName ->
                                        navigateTo(Screen.AlbumDetail(albumName))
                                    },
                                ) { backStack.removeAll { it !is Screen.MainMenu } }
                            }
                            is Screen.AlbumDetail -> {
                                AlbumDetailScreen(
                                    albumName = screen.albumName,
                                    songs = songs,
                                    onSongClick = { index ->
                                        val albumSongs = songs.filter { it.album == screen.albumName }
                                        val actualSong = albumSongs[index]
                                        val globalIndex = songs.indexOf(actualSong)
                                        if (globalIndex >= 0) {
                                            MusicPlayerManager.playSong(songs, globalIndex)
                                            navigateTo(Screen.Player(globalIndex))
                                        }
                                    },
                                ) { popBackStack() }
                            }
                            is Screen.Artists -> {
                                ArtistsScreen(
                                    songs = songs,
                                    onArtistClick = { artistName -> navigateTo(Screen.ArtistDetail(artistName)) },
                                ) { backStack.removeAll { it !is Screen.MainMenu } }
                            }
                            is Screen.ArtistDetail -> {
                                ArtistDetailScreen(
                                    artistName = screen.artistName,
                                    songs = songs,
                                    onSongClick = { localIndex ->
                                        val artistSongs = songs.filter { normalizeArtistName(it.artist).lowercase() == normalizeArtistName(screen.artistName).lowercase() }
                                            .sortedBy { it.title.lowercase() }
                                        if (localIndex in artistSongs.indices) {
                                            val actualSong = artistSongs[localIndex]
                                            val globalIndex = songs.indexOf(actualSong)
                                            if (globalIndex >= 0) {
                                                MusicPlayerManager.playSong(songs, globalIndex)
                                                navigateTo(Screen.Player(globalIndex))
                                            }
                                        }
                                    },
                                ) { popBackStack() }
                            }
                            is Screen.Player -> {
                                PlayerScreen(
                                    songs = songs,
                                    initialSongIndex = screen.songIndex,
                                ) {
                                    backStack.removeAll { it !is Screen.MainMenu }
                                }
                            }
                            is Screen.Settings -> {
                                SettingsScreen(
                                    onBackClick = { popBackStack() },
                                ) {
                                    songs = loadAudioFiles(context)
                                }
                            }
                            is Screen.About -> {
                                AboutScreen(
                                    onBackClick = { popBackStack() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permission)
        }
    }
}
