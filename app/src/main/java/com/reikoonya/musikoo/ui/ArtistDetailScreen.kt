package com.reikoonya.musikoo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.ui.components.ActionsDialog
import com.reikoonya.musikoo.ui.components.AddToPlaylistDialog
import com.reikoonya.musikoo.ui.components.ArtBackdrop
import com.reikoonya.musikoo.ui.components.MenuAction
import com.reikoonya.musikoo.ui.components.TopFadeScrim
import com.reikoonya.musikoo.ui.components.bounceClickable
import com.reikoonya.musikoo.ui.components.bounceCombinedClickable
import com.reikoonya.musikoo.ui.components.cardFill
import com.reikoonya.musikoo.ui.components.glassFill
import com.reikoonya.musikoo.ui.components.glassStroke
import com.reikoonya.musikoo.ui.components.nowPlayingArt
import com.reikoonya.musikoo.util.formatTime
import com.reikoonya.musikoo.util.normalizeArtistName

@Composable
fun ArtistDetailScreen(
    artistName: String,
    songs: List<Song>,
    onSongClick: (List<Song>, Int) -> Unit,
    onBackClick: () -> Unit
) {
    val artistSongs = remember(songs, artistName) {
        songs.filter { normalizeArtistName(it.artist).lowercase() == normalizeArtistName(artistName).lowercase() }
            .sortedBy { it.title.lowercase() }
    }
    val firstArt = artistSongs.firstOrNull()?.albumArt

    var actionSong by remember { mutableStateOf<Song?>(null) }
    var editSongs by remember { mutableStateOf<List<Song>?>(null) }
    var addToPlaylistSongIds by remember { mutableStateOf<List<Long>?>(null) }

    actionSong?.let { song ->
        ActionsDialog(
            title = song.title,
            actions = listOf(
                MenuAction(Icons.Default.PlaylistAdd, stringResource(R.string.add_to_playlist)) {
                    addToPlaylistSongIds = listOf(song.id)
                    actionSong = null
                },
                MenuAction(Icons.Default.Edit, stringResource(R.string.edit_tags)) {
                    editSongs = listOf(song)
                    actionSong = null
                }
            ),
            onDismiss = { actionSong = null }
        )
    }

    addToPlaylistSongIds?.let { ids ->
        AddToPlaylistDialog(
            songIds = ids,
            allSongs = songs,
            onDismiss = { addToPlaylistSongIds = null }
        )
    }

    editSongs?.let { list ->
        TagEditorDialog(
            songs = list,
            onDismiss = { editSongs = null },
            onSaved = {}
        )
    }

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
        ArtBackdrop(art = firstArt ?: nowPlayingArt(), topScrim = 0.55f, midScrim = 0.82f)

        // List fills entire screen and scrolls underneath floating top pill
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 92.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        Surface(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(28.dp)),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            if (firstArt != null) {
                                Image(
                                    bitmap = firstArt.asImageBitmap(),
                                    contentDescription = artistName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = artistName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.artist_tracks_count, artistSongs.size),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            itemsIndexed(artistSongs) { index, _ ->
                val song = artistSongs[index]
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .bounceCombinedClickable(
                            onLongClick = { actionSong = song }
                        ) {
                            onSongClick(artistSongs, index)
                        },
                    shape = RoundedCornerShape(26.dp),
                    color = cardFill(),
                    border = glassStroke(),
                    tonalElevation = 1.dp
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
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            if (song.albumArt != null) {
                                Image(
                                    bitmap = song.albumArt.asImageBitmap(),
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
                                text = song.album,
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
                    .bounceClickable { onBackClick() },
                shape = CircleShape,
                color = glassFill(),
                border = glassStroke(),
                tonalElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(32.dp),
                color = glassFill(),
                border = glassStroke()
            ) {
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
                        text = artistName.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.size(44.dp))
        }
    }
}
