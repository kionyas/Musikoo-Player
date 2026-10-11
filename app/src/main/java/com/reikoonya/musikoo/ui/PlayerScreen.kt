package com.reikoonya.musikoo.ui

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.data.PlaylistManager
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.ui.components.AddToPlaylistDialog
import com.reikoonya.musikoo.ui.components.ArtBackdrop
import com.reikoonya.musikoo.ui.components.ExpressivePlayButton
import com.reikoonya.musikoo.ui.components.GlassIconButton
import com.reikoonya.musikoo.ui.components.SplitBottomPills
import com.reikoonya.musikoo.ui.components.PlayerSlider
import com.reikoonya.musikoo.ui.theme.LocalFancyStyle
import com.reikoonya.musikoo.util.DeleteResult
import com.reikoonya.musikoo.util.MusicPlayerManager
import com.reikoonya.musikoo.util.SongDeleter
import com.reikoonya.musikoo.util.accentFromBitmap
import com.reikoonya.musikoo.util.readableOn
import com.reikoonya.musikoo.util.formatTime

@Composable
fun PlayerScreen(
    songs: List<Song>,
    initialSongIndex: Int,
    onSongDeleted: (Song) -> Unit = {},
    onSongsEdited: (List<Song>) -> Unit = {},
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(initialSongIndex) {
        MusicPlayerManager.initPlayer(context)
        if (MusicPlayerManager.songs.isEmpty() && songs.isNotEmpty()) {
            MusicPlayerManager.playSong(songs, initialSongIndex.coerceIn(songs.indices))
        }
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var showTagEditor by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }

    val currentSong = if (MusicPlayerManager.songs.isNotEmpty() && MusicPlayerManager.currentSongIndex in MusicPlayerManager.songs.indices) {
        MusicPlayerManager.songs[MusicPlayerManager.currentSongIndex]
    } else if (songs.isNotEmpty()) {
        songs[initialSongIndex.coerceIn(0, songs.size - 1)]
    } else null

    val isFav = currentSong?.let { PlaylistManager.isFavorite(context, it.id) } == true

    val fancy = LocalFancyStyle.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val art = currentSong?.albumArt
    val rawAccent = remember(art, isDark, fancy) {
        if (fancy) art?.let { accentFromBitmap(it, isDark) } else null
    }
    val accent by animateColorAsState(
        targetValue = rawAccent ?: MaterialTheme.colorScheme.primary,
        animationSpec = tween(600),
        label = "accent"
    )
    val onAccent = readableOn(accent)

    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingNeedsRetry by rememberSaveable { mutableStateOf(false) }

    val strDeleted = stringResource(R.string.track_deleted)
    val strDeleteFailed = stringResource(R.string.delete_failed)
    val strAddedToFav = stringResource(R.string.added_to_favorites)
    val strRemovedFromFav = stringResource(R.string.removed_from_favorites)

    fun finishDelete(song: Song) {
        MusicPlayerManager.removeSong(song)
        onSongDeleted(song)
        Toast.makeText(context, strDeleted, Toast.LENGTH_SHORT).show()
        if (MusicPlayerManager.songs.isEmpty()) onBackClick()
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val id = pendingDeleteId
        pendingDeleteId = null
        val song = MusicPlayerManager.songs.firstOrNull { it.id == id }
            ?: songs.firstOrNull { it.id == id }
        if (song == null || result.resultCode != Activity.RESULT_OK) {
            return@rememberLauncherForActivityResult
        }
        if (pendingNeedsRetry) {
            when (SongDeleter.deleteDirect(context, song)) {
                is DeleteResult.Deleted -> finishDelete(song)
                else -> Toast.makeText(context, strDeleteFailed, Toast.LENGTH_LONG).show()
            }
        } else {
            finishDelete(song)
        }
    }

    fun startDelete(song: Song) {
        when (val r = SongDeleter.requestDelete(context, song)) {
            is DeleteResult.Deleted -> finishDelete(song)
            is DeleteResult.NeedsConfirmation -> {
                pendingDeleteId = song.id
                pendingNeedsRetry = r.retryAfterApproval
                deleteLauncher.launch(IntentSenderRequest.Builder(r.intentSender).build())
            }
            is DeleteResult.Failed -> Toast.makeText(context, strDeleteFailed, Toast.LENGTH_LONG).show()
        }
    }

    if (showDeleteDialog && currentSong != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(36.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.delete_track), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    stringResource(R.string.delete_confirm, currentSong.title),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        startDelete(currentSong)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text(stringResource(R.string.delete), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }

    if (showTagEditor && currentSong != null) {
        TagEditorDialog(
            songs = listOf(currentSong),
            onDismiss = { showTagEditor = false },
            onSaved = onSongsEdited
        )
    }

    if (showAddToPlaylist && currentSong != null) {
        AddToPlaylistDialog(
            songIds = listOf(currentSong.id),
            allSongs = songs,
            onDismiss = { showAddToPlaylist = false }
        )
    }

    if (showPropertiesDialog && currentSong != null) {
        AlertDialog(
            onDismissRequest = { showPropertiesDialog = false },
            shape = RoundedCornerShape(36.dp),
            title = { Text(stringResource(R.string.track_properties), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PropertyRow(stringResource(R.string.prop_title), currentSong.title)
                    PropertyRow(stringResource(R.string.prop_artist), currentSong.artist)
                    PropertyRow(stringResource(R.string.prop_album), currentSong.album)
                    PropertyRow(stringResource(R.string.prop_format), currentSong.mimeType)
                    PropertyRow(stringResource(R.string.prop_bitrate), currentSong.bitrate)
                    PropertyRow(stringResource(R.string.prop_duration), formatTime(currentSong.duration))
                    PropertyRow(stringResource(R.string.prop_size), "${currentSong.size / (1024 * 1024)} MB")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPropertiesDialog = false }) { Text(stringResource(R.string.ok), fontWeight = FontWeight.Bold) }
            }
        )
    }

    val isPlaying = MusicPlayerManager.isPlayingState
    val artScale by animateFloatAsState(
        targetValue = if (isPlaying || !fancy) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "artScale"
    )
    val artShape = RoundedCornerShape(36.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 25f) {
                        onBackClick()
                    }
                }
            }
    ) {
        ArtBackdrop(art = currentSong?.albumArt)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.collapse),
                    onClick = onBackClick,
                    width = 44.dp,
                    height = 44.dp,
                    iconSize = 24.dp,
                    shape = CircleShape
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.now_playing).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (fancy) null else FontWeight.Bold,
                        color = accent,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }

                Box {
                    GlassIconButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.menu),
                        onClick = { menuExpanded = true },
                        width = 44.dp,
                        height = 44.dp,
                        iconSize = 24.dp,
                        shape = CircleShape
                    )

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isFav) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites)) },
                            onClick = {
                                menuExpanded = false
                                currentSong?.let { song ->
                                    val newFav = PlaylistManager.toggleFavorite(context, song.id)
                                    val msg = if (newFav) strAddedToFav else strRemovedFromFav
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_to_playlist)) },
                            onClick = { menuExpanded = false; showAddToPlaylist = true },
                            leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.properties)) },
                            onClick = { menuExpanded = false; showPropertiesDialog = true },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_tags)) },
                            onClick = { menuExpanded = false; showTagEditor = true },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_from_device), color = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; showDeleteDialog = true },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .aspectRatio(1f)
                    .graphicsLayer { scaleX = artScale; scaleY = artScale }
                    .then(
                        if (fancy) Modifier.shadow(28.dp, artShape, ambientColor = accent, spotColor = accent)
                        else Modifier
                    )
                    .clip(artShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentSong?.albumArt,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "AlbumArtCrossfade"
                ) { artBitmap ->
                    if (artBitmap != null) {
                        Image(
                            bitmap = artBitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.now_playing),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(96.dp),
                            tint = accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedContent(
                    targetState = currentSong?.title ?: stringResource(R.string.select_track),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "TitleCrossfade"
                ) { title ->
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                AnimatedContent(
                    targetState = currentSong?.artist ?: stringResource(R.string.unknown_artist),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "ArtistCrossfade"
                ) { artistText ->
                    Text(
                        text = artistText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            PlayerSlider(
                value = MusicPlayerManager.currentPosition,
                onValueChange = { MusicPlayerManager.seekTo(it.toLong()) },
                valueRange = 0f..(MusicPlayerManager.trackDuration.takeIf { it > 0f } ?: 1f),
                isPlaying = isPlaying,
                accent = accent
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.Default.Replay10,
                    contentDescription = stringResource(R.string.rewind_10),
                    onClick = { MusicPlayerManager.rewind10() },
                    width = 44.dp,
                    height = 44.dp,
                    iconSize = 22.dp,
                    shape = CircleShape
                )

                GlassIconButton(
                    icon = Icons.Default.SkipPrevious,
                    contentDescription = stringResource(R.string.previous),
                    onClick = { MusicPlayerManager.skipPrevious() },
                    width = 56.dp,
                    height = 56.dp,
                    iconSize = 28.dp,
                    shape = CircleShape
                )

                ExpressivePlayButton(
                    isPlaying = isPlaying,
                    onClick = {
                        if (MusicPlayerManager.exoPlayer == null && songs.isNotEmpty()) {
                            MusicPlayerManager.playSong(songs, initialSongIndex.coerceIn(songs.indices))
                        } else {
                            MusicPlayerManager.togglePlayPause()
                        }
                    },
                    containerColor = accent,
                    contentColor = onAccent
                )

                GlassIconButton(
                    icon = Icons.Default.SkipNext,
                    contentDescription = stringResource(R.string.next),
                    onClick = { MusicPlayerManager.skipNext() },
                    width = 56.dp,
                    height = 56.dp,
                    iconSize = 28.dp,
                    shape = CircleShape
                )

                GlassIconButton(
                    icon = Icons.Default.Forward10,
                    contentDescription = stringResource(R.string.forward_10),
                    onClick = { MusicPlayerManager.forward10() },
                    width = 44.dp,
                    height = 44.dp,
                    iconSize = 22.dp,
                    shape = CircleShape
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SplitBottomPills(
                accent = accent
            )
        }
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
