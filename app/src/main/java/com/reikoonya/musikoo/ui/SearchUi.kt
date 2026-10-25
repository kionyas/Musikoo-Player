package com.reikoonya.musikoo.ui

import com.reikoonya.musikoo.ui.components.coverShapeOf
import com.reikoonya.musikoo.ui.components.shapeOf
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.Person
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.ui.components.cardFill
import com.reikoonya.musikoo.ui.components.glassStroke

/** What the search looks at. Songs is the default. */
enum class SearchMode(val label: String, val icon: ImageVector, val hint: String) {
    SONG("Songs", Icons.Rounded.MusicNote, "Search songs"),
    ALBUM("Albums", Icons.Rounded.Album, "Search albums"),
    ARTIST("Artist", Icons.Rounded.Person, "Search artists"),
    GENRE("Genre", Icons.Rounded.Category, "Search genres"),
}

/** Every genre that exists in the library tags, A to Z. */
fun genresOf(songs: List<Song>): List<String> =
    songs.map { it.genre.trim() }
        .filter { it.isNotBlank() && !it.equals("Unknown", ignoreCase = true) }
        .distinct()
        .sortedBy { it.lowercase() }

private fun relevanceOf(title: String, q: String): Int = when {
    title == q -> 100
    title.startsWith(q) -> 80
    title.split(' ', '-', '(', ')', '[', ']').any { it.startsWith(q) } -> 60
    title.contains(q) -> 40
    else -> 0
}

private sealed class SearchHit {
    class SongHit(val index: Int, val song: Song) : SearchHit()
    class AlbumHit(
        val name: String,
        val artist: String,
        val art: Bitmap?,
        val count: Int,
    ) : SearchHit()
}

private fun buildHits(songs: List<Song>, query: String, mode: SearchMode, genre: String?): List<SearchHit> {
    val q = query.trim().lowercase()
    val indexed = songs.withIndex()
    return when (mode) {
        SearchMode.SONG -> {
            if (q.isEmpty()) {
                emptyList()
            } else {
                indexed
                    .map { it to relevanceOf(it.value.title.lowercase(), q) }
                    .filter { it.second > 0 }
                    .sortedWith(
                        compareByDescending<Pair<IndexedValue<Song>, Int>> { it.second }
                            .thenBy { it.first.value.title.lowercase() }
                    )
                    .take(80)
                    .map { SearchHit.SongHit(it.first.index, it.first.value) }
            }
        }
        SearchMode.ALBUM -> {
            if (q.isEmpty()) {
                emptyList()
            } else {
                indexed
                    .groupBy { it.value.album }
                    .filter { (name, _) -> name.lowercase().contains(q) }
                    .map { (name, items) ->
                        SearchHit.AlbumHit(name, items.first().value.artist, items.first().value.albumArt, items.size)
                    }
                    .sortedWith(
                        compareByDescending<SearchHit.AlbumHit> { relevanceOf(it.name.lowercase(), q) }
                            .thenBy { it.name.lowercase() }
                    )
                    .take(60)
            }
        }
        SearchMode.ARTIST -> {
            if (q.isEmpty()) {
                emptyList()
            } else {
                indexed
                    .filter { item ->
                        val s = item.value
                        s.artist.lowercase().contains(q) || s.albumArtist.lowercase().contains(q)
                    }
                    .sortedWith(
                        compareBy<IndexedValue<Song>>(
                            { it.value.artist.lowercase() },
                            { it.value.title.lowercase() }
                        )
                    )
                    .take(120)
                    .map { SearchHit.SongHit(it.index, it.value) }
            }
        }
        SearchMode.GENRE -> {
            indexed
                .filter { item ->
                    val s = item.value
                    if (genre != null) {
                        s.genre.equals(genre, ignoreCase = true) &&
                                (q.isEmpty() || s.title.lowercase().contains(q))
                    } else {
                        q.isNotEmpty() && s.genre.lowercase().contains(q)
                    }
                }
                .sortedBy { it.value.title.lowercase() }
                .take(200)
                .map { SearchHit.SongHit(it.index, it.value) }
        }
    }
}

/** Results list: the best match sits at the bottom, right above the search panel. */
@Composable
fun SearchResultsList(
    songs: List<Song>,
    query: String,
    mode: SearchMode,
    genre: String?,
    onSongClick: (Int) -> Unit,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier,
) {
    val hits = remember(songs, query, mode, genre) { buildHits(songs, query, mode, genre) }
    val listState = rememberLazyListState()
    LaunchedEffect(hits) {
        if (hits.isNotEmpty()) listState.scrollToItem(hits.size - 1)
    }
    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 96.dp, bottom = 184.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom)
    ) {
        if (hits.isEmpty() && (query.isNotBlank() || genre != null)) {
            item {
                Text(
                    text = "Nothing found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        items(hits.reversed()) { hit ->
            when (hit) {
                is SearchHit.SongHit -> SongRow(hit.song, mode) { onSongClick(hit.index) }
                is SearchHit.AlbumHit -> AlbumRow(hit) { onAlbumClick(hit.name) }
            }
        }
    }
}

@Composable
private fun SongRow(song: Song, mode: SearchMode, onClick: () -> Unit) {
    val subtitle = "${song.artist} · ${song.album}"
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeOf(28.dp),
        color = cardFill(),
        border = glassStroke(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchThumb(song.albumArt)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AlbumRow(hit: SearchHit.AlbumHit, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeOf(28.dp),
        color = cardFill(),
        border = glassStroke(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SearchThumb(hit.art)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = hit.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${hit.artist} · ${hit.count} songs",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SearchThumb(art: Bitmap?) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(coverShapeOf(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (art != null) {
            Image(
                bitmap = art.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Compact search panel that replaces the bottom navigation while searching:
 * a search field on top and a horizontal slider of filters underneath.
 */
@Composable
fun SearchPanel(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    mode: SearchMode,
    onModeChange: (SearchMode) -> Unit,
    genres: List<String>,
    selectedGenre: String?,
    onGenreChange: (String?) -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    // No card of its own: it sits directly inside the bottom block (which already has the 6 dp padding),
    // so the field is as wide as the collapsed pill and the edges stay concentric.
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .focusRequester(focus),
            singleLine = true,
            shape = CircleShape,
            placeholder = { Text(mode.hint) },
            leadingIcon = {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            )
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp)
        ) {
            items(listOf(SearchMode.SONG, SearchMode.ALBUM, SearchMode.ARTIST)) { option ->
                FilterChip(
                    selected = mode == option,
                    onClick = { onModeChange(option) },
                    label = { Text(option.label, fontWeight = FontWeight.SemiBold) },
                    leadingIcon = {
                        Icon(option.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    shape = CircleShape
                )
            }
            item {
                GenreChip(
                    selected = mode == SearchMode.GENRE,
                    selectedGenre = selectedGenre,
                    genres = genres,
                    onOpen = { onModeChange(SearchMode.GENRE) },
                    onGenreChange = onGenreChange
                )
            }
        }
    }
}

/** Genre chip: tapping it opens the list of every genre found in the library. */
@Composable
private fun GenreChip(
    selected: Boolean,
    selectedGenre: String?,
    genres: List<String>,
    onOpen: () -> Unit,
    onGenreChange: (String?) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected,
            onClick = {
                onOpen()
                menuOpen = true
            },
            label = { Text(selectedGenre ?: SearchMode.GENRE.label, fontWeight = FontWeight.SemiBold) },
            leadingIcon = {
                Icon(SearchMode.GENRE.icon, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
            },
            shape = CircleShape
        )
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false }
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                DropdownMenuItem(
                    text = { Text("All genres") },
                    onClick = {
                        onGenreChange(null)
                        menuOpen = false
                    }
                )
                genres.forEach { genre ->
                    DropdownMenuItem(
                        text = { Text(genre) },
                        onClick = {
                            onGenreChange(genre)
                            menuOpen = false
                        }
                    )
                }
            }
        }
    }
}