package com.reikoonya.musikoo.ui

import com.reikoonya.musikoo.ui.components.AnimatedFilterChips
import com.reikoonya.musikoo.ui.components.ChipOption
import com.reikoonya.musikoo.ui.components.PinBadge
import com.reikoonya.musikoo.ui.components.PinToggleButton
import com.reikoonya.musikoo.ui.components.PlayShuffleQueueRow
import com.reikoonya.musikoo.ui.components.ScreenTopBar
import com.reikoonya.musikoo.ui.components.SearchNoResults
import com.reikoonya.musikoo.ui.components.matchesQuery
import com.reikoonya.musikoo.ui.components.pinMenuAction
import com.reikoonya.musikoo.ui.components.rememberScreenSearchState
import com.reikoonya.musikoo.ui.components.swipeDownToBack
import com.reikoonya.musikoo.ui.components.toggleBarPin
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.data.PlaylistManager
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.ui.components.ActionsDialog
import com.reikoonya.musikoo.ui.components.AddToPlaylistDialog
import com.reikoonya.musikoo.ui.components.ArtBackdrop
import com.reikoonya.musikoo.ui.components.CascadeText
import com.reikoonya.musikoo.ui.components.MenuAction
import com.reikoonya.musikoo.ui.components.TopFadeScrim
import com.reikoonya.musikoo.ui.components.topFadeBlur
import com.reikoonya.musikoo.ui.components.TopPill
import com.reikoonya.musikoo.ui.components.bounceClickable
import com.reikoonya.musikoo.ui.components.bounceCombinedClickable
import com.reikoonya.musikoo.ui.components.cardFill
import com.reikoonya.musikoo.ui.components.coverShapeOf
import com.reikoonya.musikoo.ui.components.expressiveBadgeShape
import com.reikoonya.musikoo.ui.components.glassFill
import com.reikoonya.musikoo.ui.components.glassStroke
import com.reikoonya.musikoo.ui.components.nowPlayingArt
import com.reikoonya.musikoo.ui.components.rememberPersistentLazyGridState
import com.reikoonya.musikoo.ui.components.rememberPersistentLazyListState
import com.reikoonya.musikoo.ui.components.rememberSongArt
import com.reikoonya.musikoo.ui.components.shapeOf
import com.reikoonya.musikoo.ui.components.staggeredEntrance
import com.reikoonya.musikoo.util.BottomBarItem
import com.reikoonya.musikoo.util.BottomBarPrefs
import com.reikoonya.musikoo.util.ShareUtils
import com.reikoonya.musikoo.util.groupByGenre
import com.reikoonya.musikoo.util.hasGenre
import com.reikoonya.musikoo.ui.components.marqueeEdgeFade

/** Все жанры библиотеки сеткой, как альбомы. Долгое нажатие закрепляет жанр на нижней панели. */
@Composable
fun GenresScreen(
    songs: List<Song>,
    onGenreClick: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    remember(context) { BottomBarPrefs.ensureLoaded(context.applicationContext) }
    val groups = remember(songs) { groupByGenre(songs) }
    var sortByCount by rememberSaveable { mutableStateOf(false) }
    val search = rememberScreenSearchState()
    val sorted = remember(groups, sortByCount, search.query) {
        val ordered = if (sortByCount) groups.sortedByDescending { it.songs.size } else groups
        ordered.filter { matchesQuery(search.query, it.name) }
    }
    var actionsGenre by remember { mutableStateOf<String?>(null) }

    actionsGenre?.let { name ->
        val key = BottomBarItem.genreKey(name)
        ActionsDialog(
            title = name,
            actions = listOf(pinMenuAction(key) { actionsGenre = null }),
            onDismiss = { actionsGenre = null }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .swipeDownToBack(onBackClick)
    ) {
        ArtBackdrop(art = nowPlayingArt(), topScrim = 0.55f, midScrim = 0.82f)

        LazyVerticalGrid(
            state = rememberPersistentLazyGridState("genres"),
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().topFadeBlur(),
            contentPadding = PaddingValues(top = 92.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AnimatedFilterChips(
                    options = listOf(ChipOption(false, "A–Z"), ChipOption(true, "Most tracks")),
                    selected = sortByCount,
                    onSelect = { sortByCount = it }
                )
            }

            if (sorted.isEmpty() && search.filtering) {
                item(span = { GridItemSpan(maxLineSpan) }) { SearchNoResults(search.query) }
            } else if (sorted.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "No genre tags found. Add a genre in Edit Tags and it will show up here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            gridItemsIndexed(sorted, key = { _, group -> group.name }) { index, group ->
                val pinned = BottomBarItem.genreKey(group.name) in BottomBarPrefs.items
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(index, once = true)
                        .bounceCombinedClickable(
                            shape = shapeOf(32.dp),
                            onLongClick = { actionsGenre = group.name }
                        ) { onGenreClick(group.name) },
                    shape = shapeOf(32.dp),
                    colors = CardDefaults.cardColors(containerColor = cardFill()),
                    border = glassStroke()
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val art = rememberSongArt(group.songs.firstOrNull())
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = expressiveBadgeShape(group.name.hashCode()),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                if (art != null) {
                                    Image(
                                        bitmap = art.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Category,
                                            contentDescription = null,
                                            modifier = Modifier.size(40.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().marqueeEdgeFade(centerWhenFits = true)
                            )
                            Text(
                                text = stringResource(R.string.tracks_count, group.songs.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        // Значок «закреплено» выскакивает пружиной
                        PinBadge(visible = pinned)
                    }
                }
            }
        }

        TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter), blurred = true)

        ScreenTopBar(
            title = stringResource(R.string.genres_count, groups.size),
            onBack = onBackClick,
            search = search,
            searchHint = "Search genres",
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

private enum class GenreSort(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
}

/** Треки одного жанра: то же, что экран альбома, плюс сортировка, «играть всё» и закрепление на панели. */
@Composable
fun GenreDetailScreen(
    genreName: String,
    songs: List<Song>,
    onSongClick: (List<Song>, Int) -> Unit,
    onQueueClick: () -> Unit = {},
    onSongsEdited: (List<Song>) -> Unit = {},
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    remember(context) { BottomBarPrefs.ensureLoaded(context.applicationContext) }
    val genreSongs = remember(songs, genreName) { songs.filter { it.hasGenre(genreName) } }
    var sortName by rememberSaveable { mutableStateOf(GenreSort.TITLE.name) }
    val sort = GenreSort.valueOf(sortName)
    val sortedSongs = remember(genreSongs, sort) {
        when (sort) {
            GenreSort.TITLE -> genreSongs.sortedBy { it.title.lowercase() }
            GenreSort.ARTIST -> genreSongs.sortedWith(
                compareBy<Song> { it.artist.lowercase() }.thenBy { it.title.lowercase() }
            )
            GenreSort.ALBUM -> genreSongs.sortedWith(
                compareBy<Song> { it.album.lowercase() }.thenBy { it.title.lowercase() }
            )
        }
    }
    val search = rememberScreenSearchState()
    val shownSongs = remember(sortedSongs, search.query) {
        sortedSongs.filter { matchesQuery(search.query, it.title, it.artist, it.album) }
    }
    val genreArt = rememberSongArt(genreSongs.firstOrNull())
    val pinKey = BottomBarItem.genreKey(genreName)
    val pinned = pinKey in BottomBarPrefs.items

    var actionSong by remember { mutableStateOf<Song?>(null) }
    var editSongs by remember { mutableStateOf<List<Song>?>(null) }
    var addToPlaylistSongIds by remember { mutableStateOf<List<Long>?>(null) }

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
            onSaved = onSongsEdited
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .swipeDownToBack(onBackClick)
    ) {
        ArtBackdrop(art = genreArt ?: nowPlayingArt(), topScrim = 0.55f, midScrim = 0.82f)

        LazyColumn(
            state = rememberPersistentLazyListState("genre:$genreName"),
            modifier = Modifier.fillMaxSize().topFadeBlur(),
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
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(expressiveBadgeShape(genreName.hashCode())),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                if (genreArt != null) {
                                    Image(
                                        bitmap = genreArt.asImageBitmap(),
                                        contentDescription = genreName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Rounded.Category,
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
                                    text = genreName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.tracks_count, genreSongs.size),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            PinToggleButton(
                                pinned = pinned,
                                onClick = { toggleBarPin(context, pinKey) }
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        PlayShuffleQueueRow(
                            onPlay = { if (sortedSongs.isNotEmpty()) onSongClick(sortedSongs, 0) },
                            onShuffle = {
                                val shuffled = sortedSongs.shuffled()
                                if (shuffled.isNotEmpty()) onSongClick(shuffled, 0)
                            },
                            onQueue = onQueueClick
                        )
                    }
                }
            }

            item {
                AnimatedFilterChips(
                    options = GenreSort.values().map { ChipOption(it, it.label) },
                    selected = sort,
                    onSelect = { sortName = it.name }
                )
            }

            if (shownSongs.isEmpty() && search.filtering) {
                item { SearchNoResults(search.query) }
            }
            itemsIndexed(shownSongs, key = { _, song -> song.id }) { index, song ->
                val songArt = rememberSongArt(song)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(index, offsetY = 20.dp, once = true)
                        .clip(shapeOf(26.dp))
                        .bounceCombinedClickable(onLongClick = { actionSong = song }) {
                            onSongClick(shownSongs, index)
                        },
                    shape = shapeOf(26.dp),
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
                                .clip(coverShapeOf(12.dp)),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            if (songArt != null) {
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
                                text = "${song.artist} · ${song.album}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                modifier = Modifier.marqueeEdgeFade(),
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter), blurred = true)

        ScreenTopBar(
            title = genreName.uppercase(),
            onBack = onBackClick,
            search = search,
            searchHint = "Search in genre",
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}
