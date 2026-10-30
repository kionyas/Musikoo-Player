package com.reikoonya.musikoo.ui

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.reikoonya.musikoo.ui.components.AnimatedFilterChips
import com.reikoonya.musikoo.ui.components.ChipOption
import com.reikoonya.musikoo.ui.components.ScreenTopBar
import com.reikoonya.musikoo.ui.components.SearchNoResults
import com.reikoonya.musikoo.ui.components.matchesQuery
import com.reikoonya.musikoo.ui.components.rememberScreenSearchState
import com.reikoonya.musikoo.ui.components.staggeredEntrance
import com.reikoonya.musikoo.ui.components.swipeDownToBack
import com.reikoonya.musikoo.ui.components.TopPill
import com.reikoonya.musikoo.ui.components.shapeOf
import com.reikoonya.musikoo.ui.components.rememberPersistentLazyGridState
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.ui.components.ArtBackdrop
import com.reikoonya.musikoo.ui.components.TopFadeScrim
import com.reikoonya.musikoo.ui.components.topFadeBlur
import com.reikoonya.musikoo.ui.components.bounceClickable
import com.reikoonya.musikoo.ui.components.cardFill
import com.reikoonya.musikoo.ui.components.glassFill
import com.reikoonya.musikoo.ui.components.glassStroke
import com.reikoonya.musikoo.ui.components.listItemColors
import com.reikoonya.musikoo.ui.components.nowPlayingArt
import com.reikoonya.musikoo.ui.components.rememberSongArt
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.util.normalizeArtistName
import com.reikoonya.musikoo.ui.components.marqueeEdgeFade

@Composable
fun ArtistsScreen(
    songs: List<Song>,
    onArtistClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val allArtists = remember(songs) {
        songs.groupBy { normalizeArtistName(it.artist).lowercase() }
            .mapKeys { (_, groupSongs) -> normalizeArtistName(groupSongs.first().artist) }
    }
    var sortName by rememberSaveable { mutableStateOf(ArtistSort.AZ.name) }
    val sort = ArtistSort.valueOf(sortName)
    val search = rememberScreenSearchState()
    val artists = remember(allArtists, sort, search.query) {
        val filtered = allArtists.entries.filter { matchesQuery(search.query, it.key) }
        when (sort) {
            ArtistSort.AZ -> filtered.sortedBy { it.key.lowercase() }
            ArtistSort.MOST -> filtered.sortedByDescending { it.value.size }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .swipeDownToBack(onBackClick)
    ) {
        ArtBackdrop(art = nowPlayingArt(), topScrim = 0.55f, midScrim = 0.82f)

        LazyVerticalGrid(
            state = rememberPersistentLazyGridState("artists"),
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().topFadeBlur(),
            contentPadding = PaddingValues(top = 92.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AnimatedFilterChips(
                    options = listOf(
                        ChipOption(ArtistSort.AZ, "A–Z"),
                        ChipOption(ArtistSort.MOST, "Most tracks"),
                    ),
                    selected = sort,
                    onSelect = { sortName = it.name }
                )
            }

            if (artists.isEmpty() && search.filtering) {
                item(span = { GridItemSpan(maxLineSpan) }) { SearchNoResults(search.query) }
            }

            gridItemsIndexed(artists, key = { _, entry -> entry.key }) { index, (artistName, artistSongs) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredEntrance(index, once = true)
                        .bounceClickable(shape = shapeOf(32.dp)) { onArtistClick(artistName) },
                    shape = shapeOf(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cardFill()
                    ),
                    border = glassStroke()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            val artistArt = rememberSongArt(artistSongs.firstOrNull())
                            if (artistArt != null) {
                                Image(
                                    bitmap = artistArt.asImageBitmap(),
                                    contentDescription = null,
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
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().marqueeEdgeFade(centerWhenFits = true)
                        )
                        Text(
                            text = stringResource(R.string.tracks_count, artistSongs.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Top gradient scrim
        TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter), blurred = true)

        ScreenTopBar(
            title = stringResource(R.string.artists_count, allArtists.size),
            onBack = onBackClick,
            search = search,
            searchHint = "Search artists",
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

private enum class ArtistSort { AZ, MOST }
