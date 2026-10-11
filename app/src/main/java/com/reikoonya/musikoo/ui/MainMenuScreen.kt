package com.reikoonya.musikoo.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import com.reikoonya.musikoo.ui.components.glassStroke
import com.reikoonya.musikoo.util.MusicPlayerManager

@Composable
fun MainMenuScreen(
    songs: List<Song>,
    onSongClick: (Int) -> Unit,
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

    var isPinnedCollapsed by remember { mutableStateOf(PlaylistManager.isSectionCollapsed(context, "pinned")) }
    var isPlaylistsCollapsed by remember { mutableStateOf(PlaylistManager.isSectionCollapsed(context, "playlists")) }
    var isAlbumsCollapsed by remember { mutableStateOf(PlaylistManager.isSectionCollapsed(context, "albums")) }

    var isMiniPlayerHidden by rememberSaveable { mutableStateOf(false) }

    // Mini-player interactive gesture state
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val animOffsetX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "animOffsetX"
    )
    val animOffsetY by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "animOffsetY"
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

    val showSections = playlists.isNotEmpty() || pinnedPlaylists.isNotEmpty() || pinnedAlbumsList.isNotEmpty()
    val showTip = !showSections && !isTipDismissed

    actionsAlbum?.let { name ->
        val isPinned = pinnedAlbumNames.contains(name)
        ActionsDialog(
            title = name,
            actions = listOf(
                MenuAction(
                    icon = Icons.Default.PushPin,
                    label = if (isPinned) stringResource(R.string.unpin_from_top) else stringResource(R.string.pin_to_top)
                ) {
                    PlaylistManager.togglePinAlbum(context, name)
                    pinnedAlbumNames = PlaylistManager.getPinnedAlbums(context)
                    actionsAlbum = null
                },
                MenuAction(Icons.Default.Edit, stringResource(R.string.edit_tags)) {
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
                    icon = Icons.Default.PushPin,
                    label = if (playlist.isPinned) stringResource(R.string.unpin_from_top) else stringResource(R.string.pin_to_top)
                ) {
                    PlaylistManager.togglePinPlaylist(context, playlist.id)
                    playlists = PlaylistManager.getPlaylists(context)
                    actionsPlaylist = null
                },
                if (!isFav) {
                    MenuAction(Icons.Default.Edit, stringResource(R.string.edit_playlist)) {
                        selectedPlaylistForEdit = playlist
                        actionsPlaylist = null
                    }
                } else null,
                if (!isFav) {
                    MenuAction(Icons.Default.Delete, stringResource(R.string.delete_playlist)) {
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
    val currentSong = if (MusicPlayerManager.exoPlayer != null && MusicPlayerManager.songs.isNotEmpty() && MusicPlayerManager.currentSongIndex in MusicPlayerManager.songs.indices) {
        MusicPlayerManager.songs[MusicPlayerManager.currentSongIndex]
    } else null

    val isFavSong = currentSong?.let { PlaylistManager.isFavorite(context, it.id) } == true

    var mainMode by rememberSaveable { mutableStateOf(MainMode.Library) }

    val progress = if (MusicPlayerManager.trackDuration > 0f) {
        (MusicPlayerManager.currentPosition / MusicPlayerManager.trackDuration).coerceIn(0f, 1f)
    } else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Main Vertical Grid
        if (mainMode == MainMode.Library) LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 92.dp,
                bottom = if (currentSong != null && !isMiniPlayerHidden) 195.dp else 105.dp,
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
                            .bounceClickable { showGuideDialog = true },
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
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
                                    imageVector = Icons.Default.TipsAndUpdates,
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
                                    imageVector = Icons.Default.Close,
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClickable {
                                isPinnedCollapsed = PlaylistManager.toggleSectionCollapse(context, "pinned")
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.pinned),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        Icon(
                            imageVector = if (isPinnedCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                item(span = { GridItemSpan(3) }) {
                    AnimatedVisibility(
                        visible = !isPinnedCollapsed,
                        enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                        exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeOut()
                    ) {
                        SectionGrid(
                            items = pinnedPlaylists.map { SectionItem.Playlist(it) } +
                                    pinnedAlbumsList.entries.map { SectionItem.Album(it.key, it.value) },
                            columns = 3
                        ) { item ->
                            when (item) {
                                is SectionItem.Playlist -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bounceCombinedClickable(onLongClick = { actionsPlaylist = item.playlist }) {
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
                                                .clip(RoundedCornerShape(32.dp))
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
                                            text = stringResource(R.string.tracks_count, item.playlist.songIds.size),
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
                                            .bounceCombinedClickable(onLongClick = { actionsAlbum = item.name }) { onAlbumClick(item.name) },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(32.dp)),
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
                                                        imageVector = Icons.Default.MusicNote,
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
                                            text = stringResource(R.string.tracks_count, item.songs.size),
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

            // 2. PLAYLISTS SECTION
            if (playlists.isNotEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClickable {
                                isPlaylistsCollapsed = PlaylistManager.toggleSectionCollapse(context, "playlists")
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.playlists),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                        Icon(
                            imageVector = if (isPlaylistsCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                item(span = { GridItemSpan(3) }) {
                    AnimatedVisibility(
                        visible = !isPlaylistsCollapsed,
                        enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                        exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeOut()
                    ) {
                        SectionGrid(
                            items = playlists,
                            columns = 3
                        ) { playlist ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceCombinedClickable(onLongClick = { actionsPlaylist = playlist }) {
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
                                        .clip(RoundedCornerShape(32.dp))
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
                                    text = stringResource(R.string.tracks_count, playlist.songIds.size),
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

            // 3. ALBUMS SECTION
            if (showSections && albums.isNotEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClickable {
                                isAlbumsCollapsed = PlaylistManager.toggleSectionCollapse(context, "albums")
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.albums),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                        )
                        Icon(
                            imageVector = if (isAlbumsCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            item(span = { GridItemSpan(3) }) {
                AnimatedVisibility(
                    visible = !isAlbumsCollapsed,
                    enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
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
                                .bounceCombinedClickable(onLongClick = { actionsAlbum = albumName }) { onAlbumClick(albumName) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(32.dp)),
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
                                            imageVector = Icons.Default.MusicNote,
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

        if (mainMode == MainMode.Stats) {
            StatsPanel(
                contentPadding = PaddingValues(
                    top = 92.dp,
                    bottom = if (currentSong != null && !isMiniPlayerHidden) 195.dp else 105.dp,
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
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = glassStroke(),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BounceSurface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        onClick = onSettingsClick
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.settings),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    MainModeSwitch(mode = mainMode, onModeChange = { mainMode = it })

                    BounceSurface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        onClick = onAboutClick
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = stringResource(R.string.about),
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
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
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (currentSong != null && !isMiniPlayerHidden) {
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
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
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
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFavSong) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (isDraggingLeftPastThreshold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isDraggingLeftPastThreshold) {
                                        Text(
                                            text = if (isFavSong) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites),
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
                                translationY = animOffsetY * 0.4f
                            }
                            .pointerInput(currentSong.id) {
                                detectDragGestures(
                                    onDragEnd = {
                                        val passedLeftThreshold = dragOffsetX < -thresholdPx
                                        val passedRightThreshold = dragOffsetX > thresholdPx
                                        val passedUpThreshold = dragOffsetY < -thresholdPx

                                        if (passedUpThreshold) {
                                            onSongClick(MusicPlayerManager.currentSongIndex)
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
                            .bounceClickable {
                                onSongClick(MusicPlayerManager.currentSongIndex)
                            },
                        shape = RoundedCornerShape(36.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = glassStroke(),
                        tonalElevation = 2.dp
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(22.dp)),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        if (currentSong.albumArt != null) {
                                            Image(
                                                bitmap = currentSong.albumArt.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.MusicNote,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentSong.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = currentSong.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    BounceIconButton(onClick = { MusicPlayerManager.skipPrevious() }) {
                                        Icon(
                                            imageVector = Icons.Default.SkipPrevious,
                                            contentDescription = stringResource(R.string.previous),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    ExpressivePlayButton(
                                        isPlaying = MusicPlayerManager.isPlayingState,
                                        onClick = {
                                            MusicPlayerManager.togglePlayPause()
                                        },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        width = 44.dp,
                                        height = 44.dp,
                                        iconSize = 24.dp,
                                        playingCorner = 16.dp
                                    )
                                    BounceIconButton(onClick = { MusicPlayerManager.skipNext() }) {
                                        Icon(
                                            imageVector = Icons.Default.SkipNext,
                                            contentDescription = stringResource(R.string.next),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            WavyProgress(
                                progress = progress,
                                isPlaying = MusicPlayerManager.isPlayingState,
                                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                                activeColor = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Bottom Nav Pills (Artists / Songs / Random)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BounceSurface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = glassStroke(),
                    onClick = onArtistsClick
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.artists),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                BounceSurface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = glassStroke(),
                    onClick = onSongsClick
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.songs),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                BounceSurface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    border = glassStroke(),
                    onClick = onRandomizeClick,
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.random),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
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
