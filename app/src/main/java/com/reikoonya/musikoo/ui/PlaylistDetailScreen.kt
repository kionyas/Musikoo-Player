package com.reikoonya.musikoo.ui

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
import com.reikoonya.musikoo.ui.components.bounceClickable
import com.reikoonya.musikoo.ui.components.bounceCombinedClickable
import com.reikoonya.musikoo.ui.components.cardFill
import com.reikoonya.musikoo.ui.components.glassFill
import com.reikoonya.musikoo.ui.components.glassStroke
import com.reikoonya.musikoo.ui.components.nowPlayingArt
import com.reikoonya.musikoo.util.MusicPlayerManager
import com.reikoonya.musikoo.util.formatTime

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    allSongs: List<Song>,
    onSongClick: (List<Song>, Int) -> Unit,
    onQueueClick: () -> Unit = {},
    onBackClick: () -> Unit,
    onPlaylistDeleted: () -> Unit
) {
    val context = LocalContext.current
    var playlists by remember { mutableStateOf(PlaylistManager.getPlaylists(context)) }
    val playlist = playlists.firstOrNull { it.id == playlistId }

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
        playlists = PlaylistManager.getPlaylists(context)
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var actionSong by remember { mutableStateOf<Song?>(null) }
    var editSongs by remember { mutableStateOf<List<Song>?>(null) }
    var selectedSongIds by remember { mutableStateOf(emptySet<Long>()) }
    val isSelecting = selectedSongIds.isNotEmpty()
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
                    playlists = PlaylistManager.getPlaylists(context)
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
            onSaved = { playlists = PlaylistManager.getPlaylists(context) }
        )
    }

    if (showEditDialog) {
        EditPlaylistDialog(
            playlist = playlist,
            allSongs = allSongs,
            onDismiss = { showEditDialog = false },
            onUpdated = { playlists = PlaylistManager.getPlaylists(context) }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(32.dp),
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

    val firstArt = playlistSongs.firstOrNull()?.albumArt

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
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 92.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Playlist Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(36.dp),
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
                            Text(
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
                                        playlists = PlaylistManager.getPlaylists(context)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PushPin,
                                        contentDescription = null,
                                        tint = if (playlist.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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

            itemsIndexed(orderedSongs, key = { _, s -> s.id }) { index, song ->
                val isDragging = draggingId == song.id
                val isSelected = song.id in selectedSongIds
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f)
                        .offset { IntOffset(0, if (isDragging) dragOffset.roundToInt() else 0) }
                        .onSizeChanged { rowHeightPx = it.height.toFloat() }
                        .clip(RoundedCornerShape(26.dp))
                        .bounceCombinedClickable(
                            onLongClick = {
                                selectedSongIds = if (isSelected) selectedSongIds - song.id else selectedSongIds + song.id
                            }
                        ) {
                            if (isSelecting) {
                                selectedSongIds = if (isSelected) selectedSongIds - song.id else selectedSongIds + song.id
                            } else {
                                onSongClick(orderedSongs, index)
                            }
                        },
                    shape = RoundedCornerShape(26.dp),
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
                                .clip(RoundedCornerShape(20.dp)),
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
                            } else if (song.albumArt != null) {
                                Image(
                                    bitmap = song.albumArt.asImageBitmap(),
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
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                .size(28.dp)
                                .clickable { actionSong = song }
                        )
                        // Drag handle: long-press and drag to move the song
                        Icon(
                            imageVector = Icons.Rounded.DragHandle,
                            contentDescription = "Drag to reorder",
                            tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(28.dp)
                                .pointerInput(song.id) {
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
        TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter))

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
                    .bounceClickable {
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

            Surface(
                shape = RoundedCornerShape(32.dp),
                color = glassFill(),
                border = glassStroke()
            ) {
                if (isSelecting) {
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
                                playlists = PlaylistManager.getPlaylists(context)
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
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .bounceClickable { onQueueClick() },
                shape = CircleShape,
                color = glassFill(),
                border = glassStroke(),
                tonalElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = stringResource(R.string.queue),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
