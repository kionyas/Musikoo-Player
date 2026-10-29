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
import com.reikoonya.musikoo.ui.components.swipeDownToBack
import com.reikoonya.musikoo.ui.components.CascadeText
import com.reikoonya.musikoo.ui.components.coverShapeOf
import com.reikoonya.musikoo.ui.components.PillTransition
import com.reikoonya.musikoo.ui.components.shapeOf
import com.reikoonya.musikoo.ui.components.rememberPersistentLazyListState
import androidx.compose.material.icons.rounded.*
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
import com.reikoonya.musikoo.data.PlaylistManager
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
import com.reikoonya.musikoo.ui.components.rememberSongArt
import com.reikoonya.musikoo.util.ShareUtils
import com.reikoonya.musikoo.util.formatTime
import com.reikoonya.musikoo.util.normalizeArtistName

@Composable
fun ArtistDetailScreen(
    artistName: String,
    songs: List<Song>,
    onSongClick: (List<Song>, Int) -> Unit,
    onQueueClick: () -> Unit = {},
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val artistSongs = remember(songs, artistName) {
        songs.filter { normalizeArtistName(it.artist).lowercase() == normalizeArtistName(artistName).lowercase() }
            .sortedBy { it.title.lowercase() }
    }
    val firstArt = rememberSongArt(artistSongs.firstOrNull())

    var actionSong by remember { mutableStateOf<Song?>(null) }
    var editSongs by remember { mutableStateOf<List<Song>?>(null) }
    var addToPlaylistSongIds by remember { mutableStateOf<List<Long>?>(null) }
    var selectedSongIds by remember { mutableStateOf(emptySet<Long>()) }
    val isSelecting = selectedSongIds.isNotEmpty()
    val keyboard = LocalSoftwareKeyboardController.current
    val search = rememberScreenSearchState()
    val shownSongs = remember(artistSongs, search.query) {
        artistSongs.filter { matchesQuery(search.query, it.title, it.artist, it.album) }
    }
    // Search is a mode of its own: the system back button closes it first (selection mode has its own close button)
    BackHandler(enabled = search.open && !isSelecting) { search.close() }
    val selectedSongs = remember(selectedSongIds, artistSongs) {
        artistSongs.filter { it.id in selectedSongIds }
    }

    val favIds by PlaylistManager.favorites.collectAsState()
    actionSong?.let { song ->
        val isFav = song.id in favIds
        ActionsDialog(
            title = song.title,
            actions = listOf(
                MenuAction(Icons.Rounded.PlaylistAdd, stringResource(R.string.add_to_playlist)) {
                    addToPlaylistSongIds = listOf(song.id)
                    actionSong = null
                },
                MenuAction(Icons.Rounded.Share, stringResource(R.string.share)) {
                    ShareUtils.shareSongs(context, listOf(song))
                    actionSong = null
                },
                MenuAction(
                    icon = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    label = if (isFav) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites)
                ) {
                    PlaylistManager.toggleFavorite(context, song.id)
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
            .swipeDownToBack(onBackClick)
    ) {
        ArtBackdrop(art = firstArt ?: nowPlayingArt(), topScrim = 0.55f, midScrim = 0.82f)

        // List fills entire screen and scrolls underneath floating top pill
        LazyColumn(
            state = rememberPersistentLazyListState("artist:$artistName"),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 92.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        Surface(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(shapeOf(28.dp)),
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
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CascadeText(
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
                        IconButton(onClick = onQueueClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = stringResource(R.string.queue),
                                tint = MaterialTheme.colorScheme.primary
                            )
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
                val isSelected = song.id in selectedSongIds
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
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
                    .bounceClickable(shape = CircleShape) {
                        if (isSelecting) {
                            selectedSongIds = emptySet()
                        } else {
                            onBackClick()
                        }
                    },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = glassStroke(),
                tonalElevation = 3.dp
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
                            placeholder = "Search artist's songs",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = glassStroke(),
                tonalElevation = 3.dp
            ) {
                PillTransition(isSelecting) { selecting ->
                if (selecting) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = "${selectedSongIds.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                selectedSongIds = if (selectedSongIds.size == artistSongs.size) emptySet() else artistSongs.map { it.id }.toSet()
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
                            onClick = { addToPlaylistSongIds = selectedSongIds.toList() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlaylistAdd,
                                contentDescription = "Add to Playlist",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { editSongs = selectedSongs },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit Tags",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        )
                        Spacer(modifier = Modifier.height(3.dp))
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
