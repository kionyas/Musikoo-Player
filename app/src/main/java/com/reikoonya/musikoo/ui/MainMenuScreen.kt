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

/** Search UI state, kept by MainActivity so it survives switching between tabs. */
class MainSearchState {
    val open = mutableStateOf(false)
    val query = mutableStateOf("")
    val pillHidden = mutableStateOf(false)
    val mode = mutableStateOf(SearchMode.SONG)
    val genre = mutableStateOf<String?>(null)
}

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

    var playlists by remember { mutableStateOf(PlaylistManager.getPlaylists(context)) }
    var pinnedAlbumNames by remember { mutableStateOf(PlaylistManager.getPinnedAlbums(context)) }
    var isTipDismissed by remember { mutableStateOf(PlaylistManager.isTipDismissed(context)) }

    var searchOpen by search.open
    var searchQuery by search.query
    var isSearchPillHidden by search.pillHidden
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

    var isPinnedCollapsed by remember {
        mutableStateOf(
            PlaylistManager.isSectionCollapsed(
                context,
                "pinned"
            )
        )
    }
    var isPlaylistsCollapsed by remember {
        mutableStateOf(
            PlaylistManager.isSectionCollapsed(
                context,
                "playlists"
            )
        )
    }
    var isAlbumsCollapsed by remember {
        mutableStateOf(
            PlaylistManager.isSectionCollapsed(
                context,
                "albums"
            )
        )
    }

    var isMiniPlayerHidden by rememberSaveable { mutableStateOf(false) }

    // Mini-player interactive gesture state
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val animOffsetX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "animOffsetX"
    )

    val thresholdPx = remember(density) { with(density) { 100.dp.toPx() } }

    var showGuideDialog by remember { mutableStateOf(false) }
    var selectedPlaylistForEdit by remember { mutableStateOf<Playlist?>(null) }

    var actionsAlbum by remember { mutableStateOf<String?>(null) }
    var actionsPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var editSongs by remember { mutableStateOf<List<Song>?>(null) }

    val pinnedPlaylists = remember(playlists) { playlists.filter { it.isPinned } }
    val pinnedAlbumsList = remember(albums, pinnedAlbumNames) {
        albums.filterKeys { pinnedAlbumNames.contains(it) }
    }

    val showSections =
        playlists.isNotEmpty() || pinnedPlaylists.isNotEmpty() || pinnedAlbumsList.isNotEmpty()
    val showTip = !showSections && !isTipDismissed
    val hasPinnedSection = pinnedPlaylists.isNotEmpty() || pinnedAlbumsList.isNotEmpty()

    actionsAlbum?.let { name ->
        val isPinned = pinnedAlbumNames.contains(name)
        ActionsDialog(
            title = name,
            actions = listOf(
                MenuAction(
                    icon = Icons.Rounded.PushPin,
                    label = if (isPinned) stringResource(R.string.unpin_from_top) else stringResource(
                        R.string.pin_to_top
                    )
                ) {
                    PlaylistManager.togglePinAlbum(context, name)
                    pinnedAlbumNames = PlaylistManager.getPinnedAlbums(context)
                    actionsAlbum = null
                },
                MenuAction(Icons.Rounded.Share, stringResource(R.string.share)) {
                    albums[name]?.let { songList -> ShareUtils.shareSongs(context, songList, name) }
                    actionsAlbum = null
                },
                MenuAction(Icons.Rounded.Edit, stringResource(R.string.edit_tags)) {
                    editSongs = albums[name].orEmpty()
                    actionsAlbum = null
                }
            ),
            onDismiss = { actionsAlbum = null }
        )
    }

    actionsPlaylist?.let { playlist ->
        val isFav = playlist.id == PlaylistManager.FAVORITES_PLAYLIST_ID
        ActionsDialog(
            title = playlist.name,
            actions = listOfNotNull(
                MenuAction(
                    icon = Icons.Rounded.PushPin,
                    label = if (playlist.isPinned) stringResource(R.string.unpin_from_top) else stringResource(
                        R.string.pin_to_top
                    )
                ) {
                    PlaylistManager.togglePinPlaylist(context, playlist.id)
                    playlists = PlaylistManager.getPlaylists(context)
                    actionsPlaylist = null
                },
                MenuAction(Icons.Rounded.Share, stringResource(R.string.share)) {
                    val songMap = songs.associateBy { it.id }
                    val songsToShare = playlist.songIds.mapNotNull { songMap[it] }
                    ShareUtils.shareSongs(context, songsToShare, playlist.name)
                    actionsPlaylist = null
                },
                if (!isFav) {
                    MenuAction(Icons.Rounded.Edit, stringResource(R.string.edit_playlist)) {
                        selectedPlaylistForEdit = playlist
                        actionsPlaylist = null
                    }
                } else null,
                if (!isFav) {
                    MenuAction(Icons.Rounded.Delete, stringResource(R.string.delete_playlist)) {
                        PlaylistManager.deletePlaylist(context, playlist.id)
                        playlists = PlaylistManager.getPlaylists(context)
                        actionsPlaylist = null
                    }
                } else null
            ),
            onDismiss = { actionsPlaylist = null }
        )
    }

    selectedPlaylistForEdit?.let { playlist ->
        EditPlaylistDialog(
            playlist = playlist,
            allSongs = songs,
            onDismiss = { selectedPlaylistForEdit = null },
            onUpdated = { playlists = PlaylistManager.getPlaylists(context) }
        )
    }

    editSongs?.let { list ->
        TagEditorDialog(
            songs = list,
            onDismiss = { editSongs = null },
            onSaved = onSongsEdited
        )
    }

    if (showGuideDialog) {
        MarkdownGuideDialog(onDismiss = { showGuideDialog = false })
    }

    // Mini-player shows only when active playback session is running
    val currentSong =
        if (MusicPlayerManager.exoPlayer != null && MusicPlayerManager.songs.isNotEmpty() && MusicPlayerManager.currentSongIndex in MusicPlayerManager.songs.indices) {
            MusicPlayerManager.songs[MusicPlayerManager.currentSongIndex]
        } else null

    val isFavSong = currentSong?.let { PlaylistManager.isFavorite(context, it.id) } == true

    var mainMode by rememberSaveable { mutableStateOf(MainMode.Library) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Main Vertical Grid
        if (mainMode == MainMode.Library) LazyVerticalGrid(
            state = rememberPersistentLazyGridState("main"),
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 92.dp,
                bottom = if (currentSong != null && !isMiniPlayerHidden) 220.dp else 140.dp,
                start = 16.dp,
                end = 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dismissible Tip Banner
            if (showTip) {
                item(span = { GridItemSpan(3) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClickable(shape = shapeOf(28.dp)) { showGuideDialog = true },
                        shape = shapeOf(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = 0.4f
                            )
                        ),
                        border = glassStroke()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.TipsAndUpdates,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = stringResource(R.string.tip_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.tip_text),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    PlaylistManager.setTipDismissed(context, true)
                                    isTipDismissed = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.cancel)
                                )
                            }
                        }
                    }
                }
            }

            // 1. PINNED SECTION
            if (pinnedPlaylists.isNotEmpty() || pinnedAlbumsList.isNotEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = stringResource(R.string.pinned),
                            collapsed = isPinnedCollapsed,
                            topPadding = 8.dp,
                            onClick = {
                                isPinnedCollapsed =
                                    PlaylistManager.toggleSectionCollapse(context, "pinned")
                            }
                        )

                        AnimatedVisibility(
                            visible = !isPinnedCollapsed,
                            enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeOut()
                        ) {
                            SectionGrid(
                                items = pinnedPlaylists.map { SectionItem.Playlist(it) } +
                                        pinnedAlbumsList.entries.map {
                                            SectionItem.Album(
                                                it.key,
                                                it.value
                                            )
                                        },
                                columns = 3
                            ) { item ->
                                when (item) {
                                    is SectionItem.Playlist -> {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .bounceCombinedClickable(
                                                    shape = coverShapeOf(16.dp),
                                                    onLongClick = {
                                                        actionsPlaylist = item.playlist
                                                    }) {
                                                    onPlaylistClick(item.playlist.id)
                                                },
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            PlaylistArt(
                                                playlist = item.playlist,
                                                allSongs = songs,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                                    .clip(coverShapeOf(16.dp))
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = item.playlist.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(
                                                    R.string.tracks_count,
                                                    item.playlist.songIds.size
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    is SectionItem.Album -> {
                                        val firstArt = item.songs.firstOrNull()?.albumArt
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .bounceCombinedClickable(
                                                    shape = coverShapeOf(16.dp),
                                                    onLongClick = {
                                                        actionsAlbum = item.name
                                                    }) { onAlbumClick(item.name) },
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                                    .clip(coverShapeOf(16.dp)),
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                tonalElevation = 2.dp
                                            ) {
                                                if (firstArt != null) {
                                                    Image(
                                                        bitmap = firstArt.asImageBitmap(),
                                                        contentDescription = item.name,
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.MusicNote,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(36.dp),
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = stringResource(
                                                    R.string.tracks_count,
                                                    item.songs.size
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
            }

            // 2. PLAYLISTS SECTION
            if (playlists.isNotEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SectionHeader(
                            title = stringResource(R.string.playlists),
                            collapsed = isPlaylistsCollapsed,
                            topPadding = if (hasPinnedSection && isPinnedCollapsed) 0.dp else 12.dp,
                            onClick = {
                                isPlaylistsCollapsed =
                                    PlaylistManager.toggleSectionCollapse(context, "playlists")
                            }
                        )

                        AnimatedVisibility(
                            visible = !isPlaylistsCollapsed,
                            enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeOut()
                        ) {
                            SectionGrid(
                                items = playlists,
                                columns = 3
                            ) { playlist ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bounceCombinedClickable(
                                            shape = coverShapeOf(16.dp),
                                            onLongClick = { actionsPlaylist = playlist }) {
                                            onPlaylistClick(playlist.id)
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    PlaylistArt(
                                        playlist = playlist,
                                        allSongs = songs,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(coverShapeOf(16.dp))
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = playlist.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.tracks_count,
                                            playlist.songIds.size
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                    }
                }
            }

            // 3. ALBUMS SECTION
            item(span = { GridItemSpan(3) }) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (showSections && albums.isNotEmpty()) {
                        SectionHeader(
                            title = stringResource(R.string.albums),
                            collapsed = isAlbumsCollapsed,
                            topPadding = 12.dp,
                            onClick = {
                                isAlbumsCollapsed =
                                    PlaylistManager.toggleSectionCollapse(context, "albums")
                            }
                        )
                    }
                    AnimatedVisibility(
                        visible = !isAlbumsCollapsed,
                        enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) + fadeIn(),
                        exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeOut()
                    ) {
                        SectionGrid(
                            items = albums.entries.toList(),
                            columns = 3
                        ) { (albumName, albumSongs) ->
                            val firstArt = albumSongs.firstOrNull()?.albumArt
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceCombinedClickable(
                                        shape = coverShapeOf(16.dp),
                                        onLongClick = { actionsAlbum = albumName }) {
                                        onAlbumClick(
                                            albumName
                                        )
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(coverShapeOf(16.dp)),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 2.dp
                                ) {
                                    if (firstArt != null) {
                                        Image(
                                            bitmap = firstArt.asImageBitmap(),
                                            contentDescription = albumName,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.MusicNote,
                                                contentDescription = null,
                                                modifier = Modifier.size(36.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = albumName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.tracks_count, albumSongs.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                }
            }
        }

        if (mainMode == MainMode.Stats) {
            StatsPanel(
                songs = songs,
                contentPadding = PaddingValues(
                    top = 92.dp,
                    bottom = if (currentSong != null && !isMiniPlayerHidden) 220.dp else 140.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top & Bottom gradient scrims
        TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter))
        BottomFadeScrim(modifier = Modifier.align(Alignment.BottomCenter))

        // Floating Top Bar Pill
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = glassStroke(),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BounceSurface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        onClick = onSettingsClick
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.settings),
                                modifier = Modifier.size(19.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    MainModeSwitch(mode = mainMode, onModeChange = { mainMode = it })

                    BounceSurface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        onClick = onAboutClick
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = stringResource(R.string.about),
                                modifier = Modifier.size(19.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = searchOpen,
            enter = fadeIn(animationSpec = tween(250)) + slideInVertically(
                animationSpec = tween(
                    durationMillis = 380,
                    easing = FastOutSlowInEasing
                )
            ) { it / 12 },
            exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
                animationSpec = tween(
                    durationMillis = 380,
                    easing = FastOutSlowInEasing
                )
            ) { it / 12 }
        ) {
            SearchResultsList(
                songs = songs,
                query = searchQuery,
                mode = searchMode,
                genre = searchGenre,
                onSongClick = onSongClick,
                onAlbumClick = onAlbumClick,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .imePadding()
                    .navigationBarsPadding()
            )
        }

        AnimatedVisibility(
            visible = searchOpen,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(250)),
            exit = fadeOut(tween(180))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter))
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.animateEnterExit(
                            enter = scaleIn(
                                tween(340, easing = FastOutSlowInEasing),
                                initialScale = 0.7f
                            ) +
                                    slideInVertically(
                                        tween(
                                            340,
                                            easing = FastOutSlowInEasing
                                        )
                                    ) { -it },
                            exit = scaleOut(tween(200), targetScale = 0.8f) + slideOutVertically(
                                tween(200)
                            ) { -it }
                        ),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = glassStroke(),
                        tonalElevation = 3.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.search).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Floating Bottom Bar Pills (Mini Player + Nav Pills)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // The search field lives inside the bottom pill; here the mini player shows as usual
            if (!searchOpen && currentSong != null && !isMiniPlayerHidden) {
                // Drag Progress Thresholds
                val isDraggingLeftPastThreshold = dragOffsetX < -thresholdPx
                val isDraggingRightPastThreshold = dragOffsetX > thresholdPx

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Side Option Indicators (glowing badges visible behind the mini-player while dragging)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Indicator: Hide Mini Player
                        val rightRatio = (dragOffsetX / thresholdPx).coerceIn(0f, 1.2f)
                        if (dragOffsetX > 10f) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDraggingRightPastThreshold) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = glassStroke(),
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = 0.8f + (rightRatio * 0.35f)
                                        scaleY = 0.8f + (rightRatio * 0.35f)
                                        alpha = rightRatio.coerceIn(0.2f, 1f)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = null,
                                        tint = if (isDraggingRightPastThreshold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isDraggingRightPastThreshold) {
                                        Text(
                                            text = stringResource(R.string.hide_mini_player),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        // Right Indicator: Favorites Toggle
                        val leftRatio = (-dragOffsetX / thresholdPx).coerceIn(0f, 1.2f)
                        if (dragOffsetX < -10f) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDraggingLeftPastThreshold) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = glassStroke(),
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = 0.8f + (leftRatio * 0.35f)
                                        scaleY = 0.8f + (leftRatio * 0.35f)
                                        alpha = leftRatio.coerceIn(0.2f, 1f)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFavSong) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (isDraggingLeftPastThreshold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isDraggingLeftPastThreshold) {
                                        Text(
                                            text = if (isFavSong) stringResource(R.string.remove_from_favorites) else stringResource(
                                                R.string.add_to_favorites
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                    }

                    // Mini-Player Card with Smooth Drag Translation & Threshold Release
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationX = animOffsetX
                            }
                            .pointerInput(currentSong.id) {
                                detectDragGestures(
                                    onDragEnd = {
                                        val passedLeftThreshold = dragOffsetX < -thresholdPx
                                        val passedRightThreshold = dragOffsetX > thresholdPx
                                        val passedUpThreshold = dragOffsetY < -thresholdPx

                                        if (passedUpThreshold) {
                                            onOpenPlayer()
                                            dragOffsetX = 0f
                                            dragOffsetY = 0f
                                        } else if (passedLeftThreshold) {
                                            PlaylistManager.toggleFavorite(context, currentSong.id)
                                            playlists = PlaylistManager.getPlaylists(context)
                                            dragOffsetX = 0f
                                            dragOffsetY = 0f
                                        } else if (passedRightThreshold) {
                                            isMiniPlayerHidden = true
                                            dragOffsetX = 0f
                                            dragOffsetY = 0f
                                        } else {
                                            dragOffsetX = 0f
                                            dragOffsetY = 0f
                                        }
                                    },
                                    onDragCancel = {
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetX += dragAmount.x
                                        dragOffsetY = (dragOffsetY + dragAmount.y).coerceAtMost(0f)
                                    }
                                )
                            }
                            .bounceClickable(shape = shapeOf(28.dp)) {
                                onOpenPlayer()
                            },
                        shape = shapeOf(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = glassStroke(),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. Album Art
                                Surface(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(coverShapeOf(12.dp)),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    MagicArt(
                                        art = currentSong.albumArt,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MusicNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // 2. Title & Artist
                                TrackTextSwap(
                                    label = TrackLabel(
                                        currentSong.id,
                                        currentSong.title,
                                        currentSong.artist
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) { l ->
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = l.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            modifier = Modifier.marqueeEdgeFade()
                                        )
                                        Text(
                                            text = l.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            modifier = Modifier.marqueeEdgeFade()
                                        )
                                    }
                                }

                                // 3. Controls: Previous, Play/Pause, Next
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    BounceIconButton(onClick = { MusicPlayerManager.skipPrevious() }) {
                                        Icon(
                                            imageVector = Icons.Rounded.SkipPrevious,
                                            contentDescription = stringResource(R.string.previous),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    ExpressivePlayButton(
                                        isPlaying = MusicPlayerManager.isPlayingState,
                                        onClick = { MusicPlayerManager.togglePlayPause() },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        width = 44.dp,
                                        height = 44.dp,
                                        iconSize = 22.dp,
                                        playingCorner = 12.dp,
                                        idleCorner = 16.dp
                                    )
                                    BounceIconButton(onClick = { MusicPlayerManager.skipNext() }) {
                                        Icon(
                                            imageVector = Icons.Rounded.SkipNext,
                                            contentDescription = stringResource(R.string.next),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 4. Draggable Wavy Slider
                            val progress = if (MusicPlayerManager.trackDuration > 0f) {
                                (MusicPlayerManager.currentPosition / MusicPlayerManager.trackDuration).coerceIn(
                                    0f,
                                    1f
                                )
                            } else 0f

                            var miniBarWidthPx by remember { mutableFloatStateOf(1f) }
                            fun seekMiniBar(x: Float) {
                                val d = MusicPlayerManager.trackDuration
                                if (d > 0f) {
                                    MusicPlayerManager.seekTo(
                                        ((x / miniBarWidthPx).coerceIn(
                                            0f,
                                            1f
                                        ) * d).toLong()
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged {
                                        miniBarWidthPx = it.width.toFloat().coerceAtLeast(1f)
                                    }
                                    .pointerInput(Unit) { detectTapGestures { seekMiniBar(it.x) } }
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures { change, _ ->
                                            change.consume()
                                            seekMiniBar(change.position.x)
                                        }
                                    }
                            ) {
                                WavyProgress(
                                    progress = progress,
                                    isPlaying = MusicPlayerManager.isPlayingState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(16.dp),
                                    activeColor = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                                )
                            }
                        }
                    }
                }
            }

            // Unified Bottom Block (Search Bar + Bottom Nav in one single card)
            var searchDragOffsetY by remember { mutableFloatStateOf(0f) }
            val searchGap by animateDpAsState(
                targetValue = if (searchOpen) 0.dp else 6.dp,
                label = "searchGap"
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (searchDragOffsetY > 30f) {
                                        isSearchPillHidden = true
                                        searchOpen = false
                                    } else if (searchDragOffsetY < -30f) {
                                        isSearchPillHidden = false
                                    }
                                    searchDragOffsetY = 0f
                                },
                                onDragCancel = { searchDragOffsetY = 0f }
                            ) { _, dragAmount ->
                                searchDragOffsetY += dragAmount
                            }
                        },
                    // 32 dp = 26 dp (inner pill radius) + 6 dp (padding): corners stay concentric
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = glassStroke(),
                    tonalElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Top
                        ) {
                            AnimatedVisibility(
                                visible = !isSearchPillHidden,
                                enter = fadeIn(animationSpec = tween(200)) + expandVertically(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                                ),
                                exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = searchGap),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AnimatedContent(
                                        targetState = searchOpen,
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(250)) togetherWith
                                                    fadeOut(animationSpec = tween(200)))
                                                .using(SizeTransform(clip = false))
                                        },
                                        label = "searchSwap"
                                    ) { open ->
                                        if (open) {
                                            SearchPanel(
                                                query = searchQuery,
                                                onQueryChange = { searchQuery = it },
                                                onClose = { searchOpen = false },
                                                mode = searchMode,
                                                onModeChange = { searchMode = it },
                                                genres = searchGenres,
                                                selectedGenre = searchGenre,
                                                onGenreChange = { searchGenre = it }
                                            )
                                        } else {
                                            CollapsedSearchPill(
                                                onClick = { searchOpen = true },
                                                onHide = { isSearchPillHidden = true }
                                            )
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = !searchOpen,
                                enter = fadeIn(animationSpec = tween(200)) + expandVertically(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                                ),
                                exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                                )
                            ) {
                                BottomNavBar(
                                    onArtists = onArtistsClick,
                                    onSongs = onSongsClick,
                                    onRandom = onRandomizeClick
                                )
                            }
                        }

                    }
                }

                // Search button sits on top of the pill and sticks out past its edge
                // Bright peeking button peeking out slightly above Random when search is hidden
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(y = (-22).dp, x = (-14).dp)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isSearchPillHidden,
                        enter = fadeIn(animationSpec = tween(200)) + scaleIn(
                            animationSpec = tween(
                                200,
                                easing = FastOutSlowInEasing
                            ), initialScale = 0.7f
                        ),
                        exit = fadeOut(animationSpec = tween(150)) + scaleOut(
                            animationSpec = tween(
                                200,
                                easing = FastOutSlowInEasing
                            ), targetScale = 0.7f
                        )
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = shapeOf(14.dp),
                            color = MaterialTheme.colorScheme.primary,
                            tonalElevation = 8.dp,
                            shadowElevation = 10.dp,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                            onClick = { isSearchPillHidden = false }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = "Show search",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    collapsed: Boolean,
    onClick: () -> Unit,
    topPadding: Dp = 12.dp,
) {
    // The arrow flips with a small spring instead of switching between two icons.
    val arrow by animateFloatAsState(
        targetValue = if (collapsed) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "sectionArrow"
    )
    val top by animateDpAsState(
        targetValue = topPadding,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "sectionTop"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = top, bottom = 4.dp)
            .bounceClickable(shape = CircleShape, onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title sits in a stadium pill, the same family as the top pill and the bottom block.
        Surface(
            shape = CircleShape,
            color = glassFill()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(glassFill(), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(arrow)
            )
        }
    }
}

@Composable
private fun BottomNavBar(
    onArtists: () -> Unit,
    onSongs: () -> Unit,
    onRandom: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavSegment(
            stringResource(R.string.artists),
            Icons.Rounded.Person,
            Modifier.weight(1f),
            onArtists
        )
        NavSegment(
            stringResource(R.string.songs),
            Icons.Rounded.MusicNote,
            Modifier.weight(1f),
            onSongs
        )
        NavSegment(
            stringResource(R.string.random),
            Icons.Rounded.Shuffle,
            Modifier.weight(1f),
            onRandom
        )
    }
}

@Composable
private fun NavSegment(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    BounceSurface(
        modifier = modifier.fillMaxHeight(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CollapsedSearchPill(
    onClick: () -> Unit,
    onHide: () -> Unit,
) {
    // Stadium shape, 52 dp high (radius 26 dp). The hide button is 36 dp with an 8 dp inset
    // (26 - 18 = 8), so its round edge follows the pill's edge and both sides look even.
    BounceSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        pressedScale = 0.97f
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 18.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.search_hint),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            BounceIconButton(
                onClick = onHide,
                modifier = Modifier.size(36.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

private sealed class SectionItem {
    data class Playlist(val playlist: com.reikoonya.musikoo.data.Playlist) : SectionItem()
    data class Album(val name: String, val songs: List<Song>) : SectionItem()
}

@Composable
private fun <T> SectionGrid(
    items: List<T>,
    columns: Int = 3,
    spacing: Dp = 12.dp,
    itemContent: @Composable (T) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 4.dp)
    ) {
        val rows = items.chunked(columns)
        for (row in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (item in row) {
                    Box(modifier = Modifier.weight(1f)) {
                        itemContent(item)
                    }
                }
                if (row.size < columns) {
                    repeat(columns - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}