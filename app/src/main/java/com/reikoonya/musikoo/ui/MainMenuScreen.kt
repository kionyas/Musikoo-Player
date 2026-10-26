package com.reikoonya.musikoo.ui

import com.reikoonya.musikoo.ui.components.TrackTextSwap
import com.reikoonya.musikoo.ui.components.TrackLabel
import com.reikoonya.musikoo.ui.components.MagicArt
import com.reikoonya.musikoo.ui.components.coverShapeOf
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import com.reikoonya.musikoo.ui.components.marqueeEdgeFade
import com.reikoonya.musikoo.ui.components.shapeOf
import com.reikoonya.musikoo.ui.components.rememberPersistentLazyGridState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.rounded.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import com.reikoonya.musikoo.util.ShareUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.data.Playlist
import com.reikoonya.musikoo.data.PlaylistManager
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.ui.components.ActionsDialog
import com.reikoonya.musikoo.ui.components.BottomFadeScrim
import com.reikoonya.musikoo.ui.components.BounceIconButton
import com.reikoonya.musikoo.ui.components.BounceSurface
import com.reikoonya.musikoo.ui.components.EditPlaylistDialog
import com.reikoonya.musikoo.ui.components.ExpressivePlayButton
import com.reikoonya.musikoo.ui.components.MarkdownGuideDialog
import com.reikoonya.musikoo.ui.components.MenuAction
import com.reikoonya.musikoo.ui.components.PlaylistArt
import com.reikoonya.musikoo.ui.components.TopFadeScrim
import com.reikoonya.musikoo.ui.components.WavyProgress
import com.reikoonya.musikoo.ui.components.bounceClickable
import com.reikoonya.musikoo.ui.components.bounceCombinedClickable
import com.reikoonya.musikoo.ui.components.glassFill
import com.reikoonya.musikoo.ui.components.glassStroke
import com.reikoonya.musikoo.util.MusicPlayerManager
import kotlinx.coroutines.launch

/** Search UI state, kept by MainActivity so it survives switching between tabs. */

@Composable
fun MainMenuScreen(
    search: MainSearchState,
    songs: List<Song>,
    onSongClick: (Int) -> Unit,
    onOpenPlayer: () -> Unit = { onSongClick(MusicPlayerManager.currentSongIndex) },
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onSongsClick: () -> Unit,
    onArtistsClick: () -> Unit,
    onRandomizeClick: () -> Unit,
    onSongsEdited: (List<Song>) -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val albums = remember(songs) { songs.groupBy { it.album } }

    // Playlists and pinned albums are read from the database in the background, never during composition
    val loadScope = rememberCoroutineScope()
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var pinnedAlbumNames by remember { mutableStateOf<Set<String>>(emptySet()) }
    fun reloadPlaylists() {
        loadScope.launch { playlists = PlaylistManager.loadPlaylists(context) }
    }
    fun reloadPinnedAlbums() {
        loadScope.launch { pinnedAlbumNames = PlaylistManager.loadPinnedAlbums(context) }
    }
    LaunchedEffect(Unit) {
        reloadPlaylists()
        reloadPinnedAlbums()
    }

    var searchOpen by search.open
    var searchQuery by search.query
    var isNavHidden by search.navHidden
    var searchMode by search.mode
    var searchGenre by search.genre
    val searchGenres = remember(songs) { genresOf(songs) }

    // Back gesture closes the search instead of leaving the screen
    BackHandler(enabled = searchOpen) {
        searchOpen = false
    }

    // Clear the text only after the panel has finished collapsing, so it does not vanish before the panel
    LaunchedEffect(searchOpen) {
        if (!searchOpen && searchQuery.isNotEmpty()) {
            delay(450.milliseconds)
            searchQuery = ""
        }
    }

    var isMiniPlayerHidden by rememberSaveable { mutableStateOf(false) }


    val dialogs = remember { MainMenuDialogState() }


    val pinnedPlaylists = remember(playlists) { playlists.filter { it.isPinned } }
    val pinnedAlbumsList = remember(albums, pinnedAlbumNames) {
        albums.filterKeys { pinnedAlbumNames.contains(it) }
    }

    val showSections =
        playlists.isNotEmpty() || pinnedPlaylists.isNotEmpty() || pinnedAlbumsList.isNotEmpty()
    val hasPinnedSection = pinnedPlaylists.isNotEmpty() || pinnedAlbumsList.isNotEmpty()

    MainMenuDialogs(
        state = dialogs,
        songs = songs,
        albums = albums,
        pinnedAlbumNames = pinnedAlbumNames,
        playlists = playlists,
        onPinnedAlbumsChanged = { reloadPinnedAlbums() },
        onPlaylistsChanged = { reloadPlaylists() },
        onSongsEdited = onSongsEdited,
    )

    // Mini-player shows only when active playback session is running
    val currentSong =
        if (MusicPlayerManager.controller != null && MusicPlayerManager.songs.isNotEmpty() && MusicPlayerManager.currentSongIndex in MusicPlayerManager.songs.indices) {
            MusicPlayerManager.songs[MusicPlayerManager.currentSongIndex]
        } else null


    var libraryTabsMode by remember { mutableStateOf(com.reikoonya.musikoo.util.UiStylePrefs.loadLibraryTabsMode(context)) }
    LaunchedEffect(Unit) {
        libraryTabsMode = com.reikoonya.musikoo.util.UiStylePrefs.loadLibraryTabsMode(context)
    }
    var mainMode by rememberSaveable { mutableStateOf(MainMode.Library) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        MainMenuLibrary(
            mainMode = mainMode,
            songs = songs,
            albums = albums,
            playlists = playlists,
            pinnedPlaylists = pinnedPlaylists,
            pinnedAlbumsList = pinnedAlbumsList,
            showSections = showSections,
            hasPinnedSection = hasPinnedSection,
            currentSong = currentSong,
            isMiniPlayerHidden = isMiniPlayerHidden,
            dialogs = dialogs,
            libraryTabsMode = libraryTabsMode,
            onAlbumClick = onAlbumClick,
            onPlaylistClick = onPlaylistClick,
        )

        BottomFadeScrim(modifier = Modifier.align(Alignment.BottomCenter))

        MainMenuTopBar(
            search = search,
            mainMode = mainMode,
            onModeChange = { mainMode = it },
            songs = songs,
            onSongClick = onSongClick,
            onAlbumClick = onAlbumClick,
            onSettingsClick = onSettingsClick,
            onAboutClick = onAboutClick,
        )


        MainMenuBottomBar(
            search = search,
            searchGenres = searchGenres,
            currentSong = currentSong,
            isMiniPlayerHidden = isMiniPlayerHidden,
            onMiniPlayerHiddenChange = { isMiniPlayerHidden = it },
            onPlaylistsChanged = { reloadPlaylists() },
            onOpenPlayer = onOpenPlayer,
            onSongsClick = onSongsClick,
            onArtistsClick = onArtistsClick,
            onRandomizeClick = onRandomizeClick,
        )

    }
}
