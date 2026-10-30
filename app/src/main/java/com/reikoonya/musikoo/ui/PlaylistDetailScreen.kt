package com.reikoonya.musikoo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.reikoonya.musikoo.ui.components.GlassCircleButton
import com.reikoonya.musikoo.ui.components.InlineSearchField
import com.reikoonya.musikoo.ui.components.SearchNoResults
import com.reikoonya.musikoo.ui.components.matchesQuery
import com.reikoonya.musikoo.ui.components.rememberScreenSearchState
import com.reikoonya.musikoo.ui.components.CascadeText
import com.reikoonya.musikoo.ui.components.coverShapeOf
import com.reikoonya.musikoo.ui.components.PillTransition
import com.reikoonya.musikoo.ui.components.shapeOf
import com.reikoonya.musikoo.ui.components.rememberPersistentLazyListState
import androidx.compose.material.icons.rounded.*
import kotlin.math.roundToInt
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.data.Playlist
import com.reikoonya.musikoo.data.PlaylistManager
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.ui.components.ActionsDialog
import com.reikoonya.musikoo.ui.components.ArtBackdrop
import com.reikoonya.musikoo.ui.components.EditPlaylistDialog
import com.reikoonya.musikoo.util.ShareUtils
import com.reikoonya.musikoo.ui.components.MenuAction
import com.reikoonya.musikoo.ui.components.PlaylistArt
import com.reikoonya.musikoo.ui.components.TopFadeScrim
import com.reikoonya.musikoo.ui.components.topFadeBlur
import com.reikoonya.musikoo.ui.components.bounceClickable
import com.reikoonya.musikoo.ui.components.bounceCombinedClickable
import com.reikoonya.musikoo.ui.components.cardFill
import com.reikoonya.musikoo.ui.components.glassFill
import com.reikoonya.musikoo.ui.components.glassStroke
import com.reikoonya.musikoo.ui.components.nowPlayingArt
import com.reikoonya.musikoo.ui.components.rememberSongArt
import kotlinx.coroutines.launch
import com.reikoonya.musikoo.util.MusicPlayerManager
import com.reikoonya.musikoo.util.formatTime
import com.reikoonya.musikoo.ui.components.PlainTopTitle
import com.reikoonya.musikoo.ui.components.rememberMenuTopBarActive
import com.reikoonya.musikoo.ui.components.marqueeEdgeFade

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    allSongs: List<Song>,
    onSongClick: (List<Song>, Int) -> Unit,
    onQueueClick: () -> Unit = {},
    onBackClick: () -> Unit,
    onAddSongs: () -> Unit = {},
    onPlaylistDeleted: () -> Unit
) {
    val context = LocalContext.current
    val loadScope = rememberCoroutineScope()
    var playlists by remember { mutableStateOf<List<Playlist>?>(null) }
    fun reloadPlaylists() {
        loadScope.launch { playlists = PlaylistManager.loadPlaylists(context) }
    }
    LaunchedEffect(Unit) { reloadPlaylists() }

    // Until the first load finishes, show nothing: an empty list must not look like a deleted playlist
    val loaded = playlists ?: return
    val playlist = loaded.firstOrNull { it.id == playlistId }

    if (playlist == null) {
        LaunchedEffect(Unit) { onBackClick() }
        return
    }

    val playlistSongs = remember(playlist.songIds, allSongs) {
        val songMap = allSongs.associateBy { it.id }
        playlist.songIds.mapNotNull { songMap[it] }
    }

    // Displayed order. Starts from the saved playlist and changes while the user drags a song.
    var orderedSongs by remember(playlistSongs) { mutableStateOf(playlistSongs) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }

    // Save the new order once the drag ends
    fun finishReorder() {
        draggingId = null
        dragOffset = 0f
        PlaylistManager.updatePlaylistSongs(context, playlist.id, orderedSongs.map { it.id })
        reloadPlaylists()
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var actionSong by remember { mutableStateOf<Song?>(null) }
    var editSongs by remember { mutableStateOf<List<Song>?>(null) }
    var selectedSongIds by remember { mutableStateOf(emptySet<Long>()) }
    val isSelecting = selectedSongIds.isNotEmpty()
    val keyboard = LocalSoftwareKeyboardController.current
    val search = rememberScreenSearchState()
    val shownSongs = remember(orderedSongs, search.query) {
        orderedSongs.filter { matchesQuery(search.query, it.title, it.artist, it.album) }
    }
    // Search is a mode of its own: the system back button closes it first (selection mode has its own close button)
    BackHandler(enabled = search.open && !isSelecting) { search.close() }
    val selectedSongs = remember(selectedSongIds, orderedSongs) {
        orderedSongs.filter { it.id in selectedSongIds }
    }

    actionSong?.let { song ->
        ActionsDialog(
            title = song.title,
            actions = listOf(
                MenuAction(Icons.Rounded.Share, stringResource(R.string.share)) {
                    ShareUtils.shareSongs(context, listOf(song))
                    actionSong = null
                },
                MenuAction(Icons.Rounded.RemoveCircleOutline, "Remove from Playlist") {
                    PlaylistManager.removeSongFromPlaylist(context, playlist.id, song.id)
                    reloadPlaylists()
                    actionSong = null
                },
                MenuAction(Icons.Rounded.Edit, stringResource(R.string.edit_tags)) {
                    editSongs = listOf(song)
                    actionSong = null
                }
            ),
            onDismiss = { actionSong = null }
        )
    }

    editSongs?.let { list ->
        TagEditorDialog(
            songs = list,
            onDismiss = { editSongs = null },
            onSaved = { reloadPlaylists() }
        )
    }

    if (showEditDialog) {
        EditPlaylistDialog(
            playlist = playlist,
            allSongs = allSongs,
            onDismiss = { showEditDialog = false },
            onUpdated = { reloadPlaylists() }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = shapeOf(32.dp),
            title = { Text(stringResource(R.string.delete_playlist), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_playlist_confirm, playlist.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        PlaylistManager.deletePlaylist(context, playlist.id)
                        showDeleteConfirm = false
                        onPlaylistDeleted()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    val firstArt = rememberSongArt(playlistSongs.firstOrNull())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (draggingId == null && dragAmount > 30f) {
                        onBackClick()
                    }
                }
            }
    ) {
        ArtBackdrop(art = firstArt ?: nowPlayingArt(), topScrim = 0.55f, midScrim = 0.82f)

        LazyColumn(
            state = rememberPersistentLazyListState("playlist:$playlistId"),
            modifier = Modifier.fillMaxSize().topFadeBlur(),
            contentPadding = PaddingValues(top = 92.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Playlist Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shapeOf(36.dp),
                    colors = CardDefaults.cardColors(containerColor = cardFill()),
                    border = glassStroke()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PlaylistArt(
                            playlist = playlist,
                            allSongs = allSongs,
                            modifier = Modifier.size(90.dp)
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            CascadeText(
                                text = playlist.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.tracks_count, playlistSongs.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { ShareUtils.shareSongs(context, playlistSongs, playlist.name) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = stringResource(R.string.share),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = onQueueClick) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                        contentDescription = stringResource(R.string.queue),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        PlaylistManager.togglePinPlaylist(context, playlist.id)
                                        reloadPlaylists()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PushPin,
                                        contentDescription = null,
                                        tint = if (playlist.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = onAddSongs) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Add songs")
                                }
                                IconButton(onClick = { showEditDialog = true }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = null)
                                }
                                IconButton(onClick = { showDeleteConfirm = true }) {
                                    Icon(
                                        Icons.Rounded.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (shownSongs.isEmpty() && search.filtering) {
                item { SearchNoResults(search.query) }
            }
            itemsIndexed(shownSongs, key = { _, s -> s.id }) { index, song ->
                val songArt = rememberSongArt(song)
                val isDragging = draggingId == song.id
                val isSelected = song.id in selectedSongIds
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f)
                        .offset { IntOffset(0, if (isDragging) dragOffset.roundToInt() else 0) }
                        .onSizeChanged { rowHeightPx = it.height.toFloat() }
                        .clip(shapeOf(26.dp))
                        .bounceCombinedClickable(
                            onLongClick = {
                                selectedSongIds = if (isSelected) selectedSongIds - song.id else selectedSongIds + song.id
                            }
                        ) {
                            if (isSelecting) {
                                selectedSongIds = if (isSelected) selectedSongIds - song.id else selectedSongIds + song.id
                            } else {
                                onSongClick(shownSongs, index)
                            }
                        },
                    shape = shapeOf(26.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else cardFill(),
                    border = glassStroke(),
                    tonalElevation = if (isSelected) 4.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(coverShapeOf(12.dp)),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            if (isSelected) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            } else if (songArt != null) {
                                Image(
                                    bitmap = songArt.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                modifier = Modifier.marqueeEdgeFade(),
                                softWrap = false
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.marqueeEdgeFade(),
                                softWrap = false
                            )
                        }
                        Text(
                            text = formatTime(song.duration),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(40.dp)
                                .bounceClickable(pressedScale = 0.82f, shape = CircleShape) { actionSong = song }
                                .padding(6.dp)
                        )
                        // Drag handle: long-press and drag to move the song
                        Icon(
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = (if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                .copy(alpha = if (search.filtering) 0.3f else 1f),
                            modifier = Modifier
                                .size(28.dp)
                                .pointerInput(song.id, search.filtering) {
                                    // While a search narrows the list the visible rows are not the whole playlist: no reordering
                                    if (search.filtering) return@pointerInput
                                    detectDragGestures(
                                        onDragStart = {
                                            draggingId = song.id
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, amount ->
                                            change.consume()
                                            dragOffset += amount.y
                                            val step = rowHeightPx + 10.dp.toPx() // row height + list spacing
                                            if (step <= 0f) return@detectDragGestures
                                            var from = orderedSongs.indexOfFirst { it.id == draggingId }
                                            // Swap with neighbours once the drag passes half a row
                                            while (from >= 0 && dragOffset > step / 2 && from < orderedSongs.lastIndex) {
                                                orderedSongs = orderedSongs.toMutableList().apply { add(from + 1, removeAt(from)) }
                                                from++
                                                dragOffset -= step
                                            }
                                            while (from >= 0 && dragOffset < -step / 2 && from > 0) {
                                                orderedSongs = orderedSongs.toMutableList().apply { add(from - 1, removeAt(from)) }
                                                from--
                                                dragOffset += step
                                            }
                                        },
                                        onDragEnd = { finishReorder() },
                                        onDragCancel = { finishReorder() }
                                    )
                                }
                        )
                    }
                }
            }
        }

        // Top gradient scrim
        TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter), blurred = true)

        // Floating Top Bar Pill
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .bounceClickable(shape = CircleShape) {
                        if (isSelecting) {
                            selectedSongIds = emptySet()
                        } else {
                            onBackClick()
                        }
                    },
                shape = CircleShape,
                color = glassFill(),
                border = glassStroke(),
                tonalElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSelecting) Icons.Rounded.Close else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (isSelecting) "Close" else stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = search.open && !isSelecting,
                    transitionSpec = {
                        (fadeIn(tween(220, delayMillis = 60)) +
                            scaleIn(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.88f)) togetherWith
                            fadeOut(tween(110)) using
                            SizeTransform(clip = false) { _, _ ->
                                spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                            }
                    },
                    label = "topBarCenter"
                ) { isSearching ->
                    if (isSearching) {
                        InlineSearchField(
                            query = search.query,
                            onQueryChange = { search.query = it },
                            placeholder = "Search in playlist",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (rememberMenuTopBarActive() && !isSelecting) {
                        PlainTopTitle(text = playlist.name)
                    } else {
            Surface(
                shape = shapeOf(32.dp),
                color = glassFill(),
                border = glassStroke()
            ) {
                PillTransition(isSelecting) { selecting ->
                if (selecting) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "${selectedSongIds.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                selectedSongIds = if (selectedSongIds.size == orderedSongs.size) emptySet() else orderedSongs.map { it.id }.toSet()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SelectAll,
                                contentDescription = "Select All",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { ShareUtils.shareSongs(context, selectedSongs) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                selectedSongs.forEach { song ->
                                    PlaylistManager.removeSongFromPlaylist(context, playlist.id, song.id)
                                }
                                reloadPlaylists()
                                selectedSongIds = emptySet()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RemoveCircleOutline,
                                contentDescription = "Remove from Playlist",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = playlist.name.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            modifier = Modifier.marqueeEdgeFade(),
                            softWrap = false
                        )
                    }
                }
                }
            }
                    }
                }
            }

            if (isSelecting) {
                Spacer(modifier = Modifier.size(44.dp))
            } else {
                GlassCircleButton(
                    icon = if (search.open) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = if (search.open) "Close search" else "Search",
                    onClick = {
                        keyboard?.hide()
                        search.toggle()
                    }
                )
            }
        }
    }
}
