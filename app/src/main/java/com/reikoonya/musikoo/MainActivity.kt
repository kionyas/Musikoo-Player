package com.reikoonya.musikoo

import androidx.compose.animation.scaleOut
import com.reikoonya.musikoo.ui.components.LoadingScreen
import com.reikoonya.musikoo.ui.components.rememberSongArt
import com.reikoonya.musikoo.ui.theme.FontChoice
import com.reikoonya.musikoo.ui.theme.ShapeFamily
import androidx.compose.material.icons.rounded.*
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.reikoonya.musikoo.library.LibraryViewModel
import com.reikoonya.musikoo.navigation.AppNavigator
import com.reikoonya.musikoo.navigation.Screen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.reikoonya.musikoo.scrobble.LastFmConfig
import com.reikoonya.musikoo.scrobble.LastFmSession
import com.reikoonya.musikoo.scrobble.Scrobbler
import com.reikoonya.musikoo.ui.LastFmAuthDialog
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.ui.*
import com.reikoonya.musikoo.ui.ArtistsScreen
import com.reikoonya.musikoo.ui.ArtistDetailScreen
import com.reikoonya.musikoo.ui.theme.MusikooTheme
import com.reikoonya.musikoo.ui.theme.ThemeBrightness
import com.reikoonya.musikoo.ui.components.UpdateDialog
import com.reikoonya.musikoo.util.UpdateManager
import com.reikoonya.musikoo.util.AppIconManager
import com.reikoonya.musikoo.util.AppIconMode
import com.reikoonya.musikoo.util.MusicPlayerManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.reikoonya.musikoo.util.UiStylePrefs
import com.reikoonya.musikoo.util.normalizeArtistName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

/** True when notifications may be shown: always on Android versions before 13, which need no permission. */
private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

class MainActivity : ComponentActivity() {

    /** Whether audio permission is granted. Compose observes it and reloads the library itself. */
    private var audioGranted by mutableStateOf(false)

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        audioGranted = hasAudioPermission()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Scrobbler.init(this)
        // Настройки анимаций читаются до первого кадра, чтобы экраны не мигали значением по умолчанию
        UiStylePrefs.ensureLoaded(applicationContext)
        com.reikoonya.musikoo.util.BottomBarPrefs.ensureLoaded(applicationContext)
        MusicPlayerManager.pendingOpenPlayer = intent?.getBooleanExtra(MusicPlayerManager.EXTRA_OPEN_PLAYER, false) == true
        checkAndRequestAudioPermission()
        audioGranted = hasAudioPermission()

        setContent {
            val styleContext = LocalContext.current
            var currentUiStyle by remember { mutableStateOf(UiStylePrefs.loadStyle(styleContext)) }
            var currentShapeFamily by remember { mutableStateOf(UiStylePrefs.loadShapeFamily(styleContext)) }
            var currentFontChoice by remember { mutableStateOf(UiStylePrefs.loadFontChoice(styleContext)) }
            var currentColorThemeMode by remember { mutableStateOf(UiStylePrefs.loadColorMode(styleContext)) }
            var currentBrightness by remember { mutableStateOf(UiStylePrefs.loadBrightness(styleContext)) }
            val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val useDarkTheme = when (currentBrightness) {
                ThemeBrightness.SYSTEM -> systemDark
                ThemeBrightness.LIGHT -> false
                ThemeBrightness.DARK -> true
            }
            var currentAppIconMode by remember { mutableStateOf(AppIconManager.loadIconMode(styleContext)) }

            val currentPlayingSong = MusicPlayerManager.songs.getOrNull(MusicPlayerManager.currentSongIndex)
            val currentPlayingArt = rememberSongArt(currentPlayingSong)

            MusikooTheme(
                darkTheme = useDarkTheme,
                colorThemeMode = currentColorThemeMode,
                uiStyle = currentUiStyle,
                shapeFamily = currentShapeFamily,
                fontChoice = currentFontChoice,
                coverArt = currentPlayingArt
            ) {
                // Check GitHub for a new release once per launch (at most every few hours) and show the dialog on top of any screen
                LaunchedEffect(Unit) { UpdateManager.autoCheck(applicationContext) }
                UpdateDialog()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    val context = LocalContext.current
                    // Library state lives in LibraryViewModel, so it survives rotation
                    val libraryVm: LibraryViewModel = viewModel()
                    val songs by libraryVm.songs.collectAsState()
                    // True until the first library scan finishes: shows the loading screen
                    val libraryLoading by libraryVm.loading.collectAsState()
                    var showNotificationPermissionDialog by remember { mutableStateOf(false) }

                    // Live state of the permission: it is re-read on every return to the app (the system dialog,
                    // or a change in the system settings), so the prompt never stays open once the permission is granted
                    var notificationGranted by remember { mutableStateOf(hasNotificationPermission(context)) }
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                notificationGranted = hasNotificationPermission(context)
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }
                    LaunchedEffect(notificationGranted) {
                        if (notificationGranted) showNotificationPermissionDialog = false
                    }


                    // Reload the library on every MediaStore change (new files, rescan, tag edits).
                    DisposableEffect(audioGranted) {
                        if (!audioGranted) return@DisposableEffect onDispose { }
                        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                            override fun onChange(selfChange: Boolean) {
                                libraryVm.rescan(debounce = true)
                            }
                        }
                        context.contentResolver.registerContentObserver(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer
                        )
                        onDispose { context.contentResolver.unregisterContentObserver(observer) }
                    }

                    // Scanning runs in the background. The list appears immediately, album art loads afterwards.
                    LaunchedEffect(audioGranted) {
                        if (audioGranted) libraryVm.rescan()
                    }

                    // Вход и выход из Last.fm: состояние и операции в LastFmSession
                    val lastFm = remember { LastFmSession() }
                    val scope = rememberCoroutineScope()

                    fun finishLastFmAuth(token: String) {
                        scope.launch {
                            try {
                                val name = lastFm.confirm(token)
                                Toast.makeText(context, "Last.fm connected: $name", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Log.e("LastFm", "Failed to confirm Last.fm login", e)
                                Toast.makeText(context, "Last.fm: ${e.message ?: e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }

                    if (lastFm.starting || lastFm.pendingToken != null) {
                        LastFmAuthDialog(
                            starting = lastFm.starting,
                            busy = lastFm.busy,
                            onConfirm = { lastFm.pendingToken?.let { finishLastFmAuth(it) } },
                            onDismiss = { lastFm.cancelPending() }
                        )
                    }

                    val mainSearch = remember { MainSearchState() }
                    val navigator = remember { AppNavigator() }
                    val backStack = navigator.backStack
                    val currentScreen = navigator.currentScreen

                    fun navigateTo(screen: Screen) = navigator.navigateTo(screen)

                    // Opened from the media notification: go straight to the player
                    LaunchedEffect(MusicPlayerManager.pendingOpenPlayer, songs.size) {
                        if (MusicPlayerManager.pendingOpenPlayer && songs.isNotEmpty()) {
                            navigateTo(Screen.Player(MusicPlayerManager.currentSongIndex.coerceIn(songs.indices)))
                            MusicPlayerManager.pendingOpenPlayer = false
                        }
                    }

                    fun popBackStack() = navigator.popBack()

                    BackHandler(enabled = navigator.canGoBack) {
                        popBackStack()
                    }

                    fun promptNotificationPermission() {
                        // Show the prompt only while the permission is missing
                        if (!hasNotificationPermission(context)) {
                            showNotificationPermissionDialog = true
                        }
                    }

                    LaunchedEffect(Unit) {
                        promptNotificationPermission()
                    }

                    if (showNotificationPermissionDialog) {
                        AlertDialog(
                            onDismissRequest = { showNotificationPermissionDialog = false },
                            shape = RoundedCornerShape(32.dp),
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            title = { Text(stringResource(R.string.notification_dialog_title), fontWeight = FontWeight.Bold) },
                            text = {
                                Text(
                                    stringResource(R.string.notification_dialog_text),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showNotificationPermissionDialog = false
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            requestMultiplePermissionsLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.READ_MEDIA_AUDIO,
                                                    Manifest.permission.POST_NOTIFICATIONS
                                                )
                                            )
                                        }
                                    },
                                    shape = CircleShape
                                ) {
                                    Text(stringResource(R.string.allow), fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showNotificationPermissionDialog = false }) {
                                    Text(stringResource(R.string.later))
                                }
                            }
                        )
                    }

                    LaunchedEffect(Unit) {
                        MusicPlayerManager.initPlayer(context)
                    }

                    // Screens that float over the previous one as a sheet (rounded top, blurred background behind)
                    val isSheetScreen = { sc: Screen ->
                        sc is Screen.Songs || sc is Screen.Albums || sc is Screen.AlbumDetail ||
                                sc is Screen.Artists || sc is Screen.ArtistDetail ||
                                sc is Screen.Genres || sc is Screen.GenreDetail ||
                                sc is Screen.Pins || sc is Screen.Playlists || sc is Screen.PickSongs || sc is Screen.CreatePlaylist ||
                                sc is Screen.PlaylistDetail || sc is Screen.ComposerDetail
                    }

                    @Composable
                    fun ScreenContent(screen: Screen) {
                        when (screen) {
                            is Screen.MainMenu -> {
                                MainMenuScreen(
                                    search = mainSearch,
                                    songs = songs,
                                    onSongClick = { index ->
                                        MusicPlayerManager.playSong(songs, index)
                                        navigateTo(Screen.Player(index))
                                    },
                                    onOpenPlayer = {
                                        if (MusicPlayerManager.currentSongIndex in songs.indices) {
                                            navigateTo(Screen.Player(MusicPlayerManager.currentSongIndex))
                                        }
                                    },
                                    onAlbumClick = { albumName ->
                                        navigateTo(Screen.AlbumDetail(albumName))
                                    },
                                    onPlaylistClick = { playlistId ->
                                        navigateTo(Screen.PlaylistDetail(playlistId))
                                    },
                                    onSettingsClick = { navigateTo(Screen.Settings) },
                                    onAboutClick = { navigateTo(Screen.About) },
                                    onSongsClick = { navigateTo(Screen.Songs) },
                                    onArtistsClick = { navigateTo(Screen.Artists) },
                                    onAlbumsClick = { navigateTo(Screen.Albums) },
                                    onGenresClick = { navigateTo(Screen.Genres) },
                                    onGenreClick = { genreName -> navigateTo(Screen.GenreDetail(genreName)) },
                                    onPinsClick = { navigateTo(Screen.Pins) },
                                    onPlaylistsClick = { navigateTo(Screen.Playlists) },
                                    onCreatePlaylist = { navigateTo(Screen.CreatePlaylist) },
                                    onRandomizeClick = {
                                        if (songs.isNotEmpty()) {
                                            val randomIndex = Random.nextInt(songs.size)
                                            MusicPlayerManager.playSong(songs, randomIndex)
                                            navigateTo(Screen.Player(randomIndex))
                                        }
                                    },
                                    onSongsEdited = { edited ->
                                        libraryVm.setSongs(songs.map { song -> edited.firstOrNull { it.id == song.id } ?: song })
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
                                ) { navigator.popToMainMenu() }
                            }
                            is Screen.AlbumDetail -> {
                                AlbumDetailScreen(
                                    albumName = screen.albumName,
                                    songs = songs,
                                    onSongClick = { albumSongs, localIndex ->
                                        MusicPlayerManager.playSong(albumSongs, localIndex)
                                        navigateTo(Screen.Player(localIndex))
                                    },
                                    onQueueClick = { navigateTo(Screen.Queue) },
                                ) { popBackStack() }
                            }
                            is Screen.Artists -> {
                                ArtistsScreen(
                                    songs = songs,
                                    onArtistClick = { artistName -> navigateTo(Screen.ArtistDetail(artistName)) },
                                ) { navigator.popToMainMenu() }
                            }
                            is Screen.Pins -> {
                                PinsScreen(
                                    songs = songs,
                                    onAlbumClick = { albumName -> navigateTo(Screen.AlbumDetail(albumName)) },
                                    onPlaylistClick = { id -> navigateTo(Screen.PlaylistDetail(id)) },
                                ) { navigator.popToMainMenu() }
                            }
                            is Screen.Playlists -> {
                                PlaylistsScreen(
                                    songs = songs,
                                    onPlaylistClick = { id -> navigateTo(Screen.PlaylistDetail(id)) },
                                    onCreatePlaylistClick = { navigateTo(Screen.CreatePlaylist) },
                                ) { navigator.popToMainMenu() }
                            }
                            is Screen.CreatePlaylist -> {
                                CreatePlaylistScreen(
                                    onCreated = { id ->
                                        // Шторка создания заменяется шторкой выбора песен
                                        popBackStack()
                                        navigateTo(Screen.PickSongs(id))
                                    },
                                    onBackClick = { popBackStack() },
                                )
                            }
                            is Screen.PickSongs -> {
                                SongPickerScreen(
                                    playlistId = screen.playlistId,
                                    allSongs = songs,
                                ) { popBackStack() }
                            }
                            is Screen.Genres -> {
                                GenresScreen(
                                    songs = songs,
                                    onGenreClick = { genreName -> navigateTo(Screen.GenreDetail(genreName)) },
                                ) { navigator.popToMainMenu() }
                            }
                            is Screen.GenreDetail -> {
                                GenreDetailScreen(
                                    genreName = screen.genreName,
                                    songs = songs,
                                    onSongClick = { genreSongs, localIndex ->
                                        MusicPlayerManager.playSong(genreSongs, localIndex)
                                        navigateTo(Screen.Player(localIndex))
                                    },
                                    onQueueClick = { navigateTo(Screen.Queue) },
                                    onSongsEdited = { edited ->
                                        libraryVm.setSongs(songs.map { song -> edited.firstOrNull { it.id == song.id } ?: song })
                                    },
                                ) { popBackStack() }
                            }
                            is Screen.ArtistDetail -> {
                                ArtistDetailScreen(
                                    artistName = screen.artistName,
                                    songs = songs,
                                    onSongClick = { artistSongs, localIndex ->
                                        MusicPlayerManager.playSong(artistSongs, localIndex)
                                        navigateTo(Screen.Player(localIndex))
                                    },
                                    onQueueClick = { navigateTo(Screen.Queue) },
                                ) { popBackStack() }
                            }
                            is Screen.PlaylistDetail -> {
                                PlaylistDetailScreen(
                                    playlistId = screen.playlistId,
                                    allSongs = songs,
                                    onSongClick = { playlistSongs, localIndex ->
                                        MusicPlayerManager.playSong(playlistSongs, localIndex)
                                        navigateTo(Screen.Player(localIndex))
                                    },
                                    onQueueClick = { navigateTo(Screen.Queue) },
                                    onBackClick = { popBackStack() },
                                    onAddSongs = { navigateTo(Screen.PickSongs(screen.playlistId)) },
                                    onPlaylistDeleted = { popBackStack() }
                                )
                            }
                            is Screen.Player -> {
                                PlayerScreen(
                                    songs = songs,
                                    initialSongIndex = screen.songIndex,
                                    onArtistClick = { artistName -> navigateTo(Screen.ArtistDetail(artistName)) },
                                    onAlbumClick = { albumName -> navigateTo(Screen.AlbumDetail(albumName)) },
                                    onQueueClick = { navigateTo(Screen.Queue) },
                                    onSongDeleted = { deleted ->
                                        com.reikoonya.musikoo.data.PlaylistManager.onSongDeleted(context, deleted.id)
                                        libraryVm.setSongs(songs.filter { it.id != deleted.id })
                                    },
                                    onSongsEdited = { edited ->
                                        libraryVm.setSongs(songs.map { song -> edited.firstOrNull { it.id == song.id } ?: song })
                                    },
                                ) {
                                    navigator.popToMainMenu()
                                }
                            }
                            is Screen.ComposerDetail -> {
                                val composerSongs = songs.filter { it.composer.equals(screen.composerName, ignoreCase = true) }
                                SongsScreen(
                                    songs = composerSongs,
                                    memoryKey = "composer:${screen.composerName}",
                                    onSongClick = { index ->
                                        MusicPlayerManager.playSong(composerSongs, index)
                                        navigateTo(Screen.Player(index))
                                    },
                                ) { popBackStack() }
                            }
                            is Screen.Queue -> {
                                QueueScreen(
                                    onBackClick = { popBackStack() }
                                )
                            }
                            is Screen.Settings -> {
                                SettingsScreen(
                                    onBackClick = { popBackStack() },
                                    onRequestNotificationPermission = { promptNotificationPermission() },
                                    lastFmUser = lastFm.user,
                                    lastFmAvatarUrl = lastFm.avatarUrl,
                                    onLastFmConnect = {
                                        if (!LastFmConfig.isConfigured) {
                                            Toast.makeText(
                                                context,
                                                "Last.fm API key is not set (see LastFmConfig)",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            scope.launch {
                                                try {
                                                    val url = lastFm.beginLogin()
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                } catch (_: Exception) {
                                                    Toast.makeText(context, "No connection to Last.fm", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    songs = songs,
                                    onLastFmDisconnect = {
                                        lastFm.disconnect()
                                        Toast.makeText(context, "Last.fm disconnected", Toast.LENGTH_SHORT).show()
                                    },
                                    onRescanLibrary = {
                                        libraryVm.rescan(debounce = true)
                                    },
                                    uiStyle = currentUiStyle,
                                    fontChoice = currentFontChoice,
                                    onFontChoiceChange = { choice ->
                                        currentFontChoice = choice
                                        UiStylePrefs.saveFontChoice(context, choice)
                                    },
                                    shapeFamily = currentShapeFamily,
                                    onShapeFamilyChange = { family ->
                                        currentShapeFamily = family
                                        UiStylePrefs.saveShapeFamily(context, family)
                                    },
                                    onUiStyleChange = { newStyle ->
                                        currentUiStyle = newStyle
                                        UiStylePrefs.saveStyle(context, newStyle)
                                    },
                                    themeBrightness = currentBrightness,
                                    onThemeBrightnessChange = { newBrightness ->
                                        currentBrightness = newBrightness
                                        UiStylePrefs.saveBrightness(context, newBrightness)
                                        com.reikoonya.musikoo.widget.MusicWidget.updateAppearance(context)
                                    },
                                    colorThemeMode = currentColorThemeMode,
                                    onColorThemeModeChange = { newMode ->
                                        currentColorThemeMode = newMode
                                        UiStylePrefs.saveColorMode(context, newMode)
                                        com.reikoonya.musikoo.widget.MusicWidget.updateAppearance(context)
                                    },
                                    appIconMode = currentAppIconMode,
                                    onAppIconModeChange = { newIconMode ->
                                        currentAppIconMode = newIconMode
                                        AppIconManager.setIconMode(context, newIconMode)
                                    }
                                )
                            }
                            is Screen.About -> {
                                AboutScreen(
                                    onBackClick = { popBackStack() },
                                )
                            }
                        }
                    }

                    val baseScreen = backStack.lastOrNull { !isSheetScreen(it) } ?: Screen.MainMenu
                    val sheetScreen = currentScreen.takeIf { isSheetScreen(it) }
                    val sheetHolder = remember { arrayOfNulls<Screen>(1) }
                    if (sheetScreen != null) sheetHolder[0] = sheetScreen
                    val renderedSheet = sheetScreen ?: sheetHolder[0]

                    fun dismissSheets() {
                        while (backStack.size > 1 && isSheetScreen(backStack.last())) {
                            backStack.removeAt(backStack.lastIndex)
                        }
                    }

                    // Everything under a sheet gets blurred smoothly
                    val sheetBlur by animateDpAsState(
                        targetValue = if (sheetScreen != null && UiStylePrefs.blurEnabled) 14.dp else 0.dp,
                        animationSpec = if (sheetScreen != null) {
                            tween(320, easing = FastOutSlowInEasing)
                        } else {
                            tween(140, easing = FastOutSlowInEasing)
                        },
                        label = "sheetBackgroundBlur"
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(sheetBlur, BlurredEdgeTreatment.Rectangle)
                        ) {
                            AnimatedContent(
                                targetState = baseScreen,
                                transitionSpec = {
                                    val isBack = backStack.indexOf(initialState) < 0 ||
                                            backStack.indexOf(targetState) < backStack.indexOf(initialState)
                                    val isTopDropSheet = { s: Screen -> s is Screen.Settings || s is Screen.About }
                                    val isBottomSlideSheet = { s: Screen -> s is Screen.Player || s is Screen.Queue }
                                    when {
                                        (initialState is Screen.MainMenu && isBottomSlideSheet(targetState)) -> {
                                            (slideInVertically(tween(420, easing = FastOutSlowInEasing)) { height -> height } + fadeIn()) togetherWith
                                                    (slideOutVertically(tween(420, easing = FastOutSlowInEasing)) { height -> -height / 4 } + fadeOut())
                                        }
                                        (isBottomSlideSheet(initialState) && targetState is Screen.MainMenu) -> {
                                            (slideInVertically(tween(420, easing = FastOutSlowInEasing)) { height -> -height / 4 } + fadeIn()) togetherWith
                                                    (slideOutVertically(tween(420, easing = FastOutSlowInEasing)) { height -> height } + fadeOut())
                                        }
                                        (initialState is Screen.MainMenu && isTopDropSheet(targetState)) -> {
                                            (slideInVertically { height -> -height } + fadeIn()) togetherWith
                                                    (slideOutVertically { height -> height / 4 } + fadeOut())
                                        }
                                        (isTopDropSheet(initialState) && targetState is Screen.MainMenu) -> {
                                            (slideInVertically { height -> height / 4 } + fadeIn()) togetherWith
                                                    (slideOutVertically { height -> -height } + fadeOut())
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
                                // The screen that gets covered (e.g. main menu under the full-screen player) blurs while it leaves
                                val coverBlur by transition.animateDp(
                                    transitionSpec = { tween(420, easing = FastOutSlowInEasing) },
                                    label = "coveredScreenBlur"
                                ) { state ->
                                    when (state) {
                                        EnterExitState.Visible -> 0.dp
                                        EnterExitState.PostExit -> if (UiStylePrefs.blurEnabled) 18.dp else 0.dp
                                        EnterExitState.PreEnter -> if (screen is Screen.MainMenu && UiStylePrefs.blurEnabled) 18.dp else 0.dp
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .blur(coverBlur, BlurredEdgeTreatment.Rectangle)
                                ) {
                                    ScreenContent(screen)
                                }
                            }
                        }

                        // Dimming behind the sheet, tap to close
                        AnimatedVisibility(
                            visible = sheetScreen != null,
                            enter = fadeIn(tween(300)),
                            exit = fadeOut(tween(160)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.32f))
                                    .pointerInput(Unit) { detectTapGestures { dismissSheets() } }
                            )
                        }

                        // The sheet itself: full width, not full height, rounded top corners
                        AnimatedVisibility(
                            visible = sheetScreen != null,
                            modifier = Modifier.fillMaxSize(),
                            enter = slideInVertically(tween(440, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(200)),
                            exit = slideOutVertically(tween(300, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(260)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .padding(top = 44.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(Unit) { detectTapGestures { } },
                                    shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 12.dp
                                ) {
                                    if (renderedSheet != null) {
                                        AnimatedContent(
                                            targetState = renderedSheet,
                                            transitionSpec = {
                                                val isBack = backStack.indexOf(initialState) < 0 ||
                                                        backStack.indexOf(targetState) < backStack.indexOf(initialState)
                                                if (isBack) {
                                                    (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { w -> -w / 3 } + fadeIn(tween(300))) togetherWith
                                                            (slideOutHorizontally(tween(380, easing = FastOutSlowInEasing)) { w -> w / 3 } + fadeOut(tween(200)))
                                                } else {
                                                    (slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { w -> w / 3 } + fadeIn(tween(300))) togetherWith
                                                            (slideOutHorizontally(tween(380, easing = FastOutSlowInEasing)) { w -> -w / 3 } + fadeOut(tween(200)))
                                                }
                                            },
                                            label = "SheetContentTransition"
                                        ) { sc ->
                                            ScreenContent(sc)
                                        }
                                    }
                                }
                            }
                        }

                        // Loading screen: morphing expressive shapes (MD3E) or the standard indicator (MD3)
                        AnimatedVisibility(
                            visible = libraryLoading && audioGranted,
                            modifier = Modifier.fillMaxSize(),
                            enter = fadeIn(tween(200)),
                            exit = fadeOut(tween(380, easing = FastOutSlowInEasing)) +
                                    scaleOut(tween(380, easing = FastOutSlowInEasing), targetScale = 1.04f)
                        ) {
                            LoadingScreen(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .pointerInput(Unit) { detectTapGestures { } },
                                label = "Loading library…"
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(MusicPlayerManager.EXTRA_OPEN_PLAYER, false)) {
            MusicPlayerManager.pendingOpenPlayer = true
        }
    }

    override fun onResume() {
        super.onResume()
        // permission may have been granted in system settings while the app was in the background
        audioGranted = hasAudioPermission()
    }

    override fun onPause() {
        super.onPause()
        MusicPlayerManager.saveQueueState()
    }

    override fun onStop() {
        super.onStop()
        MusicPlayerManager.saveQueueState()
    }

    private fun hasAudioPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAndRequestAudioPermission() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        val ungranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungranted.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(ungranted.toTypedArray())
        }
    }
}