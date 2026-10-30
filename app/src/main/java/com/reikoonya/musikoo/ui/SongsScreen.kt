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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.reikoonya.musikoo.ui.components.AnimatedFilterChips
import com.reikoonya.musikoo.ui.components.ChipOption
import com.reikoonya.musikoo.ui.components.GlassCircleButton
import com.reikoonya.musikoo.ui.components.InlineSearchField
import com.reikoonya.musikoo.ui.components.SearchNoResults
import com.reikoonya.musikoo.ui.components.matchesQuery
import com.reikoonya.musikoo.ui.components.rememberScreenSearchState
import com.reikoonya.musikoo.ui.components.staggeredEntrance
import com.reikoonya.musikoo.ui.components.swipeDownToBack
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
import com.reikoonya.musikoo.ui.components.topFadeBlur
import com.reikoonya.musikoo.ui.components.bounceClickable
import com.reikoonya.musikoo.ui.components.bounceCombinedClickable
import com.reikoonya.musikoo.ui.components.cardFill
import com.reikoonya.musikoo.ui.components.glassFill
import com.reikoonya.musikoo.ui.components.glassStroke
import com.reikoonya.musikoo.ui.components.nowPlayingArt
import com.reikoonya.musikoo.ui.components.rememberSongArt
import com.reikoonya.musikoo.util.ShareUtils
import com.reikoonya.musikoo.util.formatTime
import com.reikoonya.musikoo.ui.components.PlainTopTitle
import com.reikoonya.musikoo.ui.components.rememberMenuTopBarActive
import com.reikoonya.musikoo.ui.components.marqueeEdgeFade

@Composable
fun SongsScreen(
    songs: List<Song>,
    onSongClick: (Int) -> Unit,
    memoryKey: String = "songs",
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    var sortName by rememberSaveable { mutableStateOf(SongSort.TITLE.name) }
    var descending by rememberSaveable { mutableStateOf(false) }
    val sort = SongSort.valueOf(sortName)
    val search = rememberScreenSearchState()
    val sortedSongs = remember(songs, sort, descending, search.query) {
        val filtered = songs.filter { matchesQuery(search.query, it.title, it.artist, it.album) }
        val ordered = when (sort) {
            SongSort.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SongSort.ARTIST -> filtered.sortedWith(
                compareBy<Song> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
            )
            SongSort.ALBUM -> filtered.sortedWith(
                compareBy<Song> { it.album.lowercase() }.thenBy { it.title.lowercase() }
            )
            SongSort.LENGTH -> filtered.sortedBy { it.duration }
        }
        if (descending) ordered.asReversed() else ordered
    }

    var actionSong by remember { mutableStateOf<Song?>(null) }
    var editSongs by remember { mutableStateOf<List<Song>?>(null) }
    var addToPlaylistSongIds by remember { mutableStateOf<List<Long>?>(null) }
    var selectedSongIds by remember { mutableStateOf(emptySet<Long>()) }
    val isSelecting = selectedSongIds.isNotEmpty()
    // Search is a mode of its own: the system back button closes it first (selection mode has its own close button)
    BackHandler(enabled = search.open && !isSelecting) { search.close() }
    val selectedSongs = remember(selectedSongIds, sortedSongs) {
        sortedSongs.filter { it.id in selectedSongIds }
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
        ArtBackdrop(art = nowPlayingArt(), topScrim = 0.55f, midScrim = 0.82f)

        // List fills entire screen and scrolls underneath floating top pill
        LazyColumn(
            state = rememberPersistentLazyListState(memoryKey),
            modifier = Modifier.fillMaxSize().topFadeBlur(),
            contentPadding = PaddingValues(top = 92.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                AnimatedFilterChips(
                    options = listOf(
                        ChipOption(SongSort.TITLE, "Title"),
                        ChipOption(SongSort.ARTIST, "Artist"),
                        ChipOption(SongSort.ALBUM, "Album"),
                        ChipOption(SongSort.LENGTH, "Length"),
                    ),
                    selected = sort,
                    descending = descending,
                    // Tapping the selected chip again flips the direction
                    onSelect = { picked ->
                        if (picked == sort) {
                            descending = !descending
                        } else {
                            sortName = picked.name
                            descending = false
                        }
                    }
                )
            }
            if (sortedSongs.isEmpty() && search.filtering) {
                item { SearchNoResults(search.query) }
            }
            itemsIndexed(sortedSongs, key = { _, song -> song.id }) { index, song ->
                val songArt = rememberSongArt(song)
                val isSelected = song.id in selectedSongIds
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(index, offsetY = 20.dp, once = true)
                        .clip(shapeOf(26.dp))
                        .bounceCombinedClickable(
                            onLongClick = {
                                selectedSongIds = if (isSelected) selectedSongIds - song.id else selectedSongIds + song.id
                            }
                        ) {
                            if (isSelecting) {
                                selectedSongIds = if (isSelected) selectedSongIds - song.id else selectedSongIds + song.id
                            } else {
                                val globalIndex = songs.indexOf(song)
                                if (globalIndex >= 0) {
                                    onSongClick(globalIndex)
                                }
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
                    label = "songsTopBarCenter"
                ) { isSearching ->
                    if (isSearching) {
                        InlineSearchField(
                            query = search.query,
                            onQueryChange = { search.query = it },
                            placeholder = "Search songs",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (rememberMenuTopBarActive() && !isSelecting) {
                        PlainTopTitle(text = stringResource(R.string.all_tracks_count, songs.size))
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
                                selectedSongIds = if (selectedSongIds.size == sortedSongs.size) emptySet() else sortedSongs.map { it.id }.toSet()
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
                            text = stringResource(R.string.all_tracks_count, songs.size),
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

private enum class SongSort { TITLE, ARTIST, ALBUM, LENGTH }
