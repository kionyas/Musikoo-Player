package com.reikoonya.musikoo.ui

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.data.Playlist
import com.reikoonya.musikoo.data.PlaylistManager
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.ui.components.ArtBackdrop
import com.reikoonya.musikoo.ui.components.CreatePlaylistDialog
import com.reikoonya.musikoo.ui.components.PlaylistArt
import com.reikoonya.musikoo.ui.components.ScreenTopBar
import com.reikoonya.musikoo.ui.components.TopFadeScrim
import com.reikoonya.musikoo.ui.components.bounceClickable
import com.reikoonya.musikoo.ui.components.coverShapeOf
import com.reikoonya.musikoo.ui.components.nowPlayingArt
import com.reikoonya.musikoo.ui.components.rememberPersistentLazyGridState
import com.reikoonya.musikoo.ui.components.rememberSongArt
import com.reikoonya.musikoo.ui.components.staggeredEntrance
import com.reikoonya.musikoo.ui.components.swipeDownToBack

/** Плейлисты и закреплённые альбомы; перечитываются при каждом изменении в [PlaylistManager]. */
private data class LibraryCollections(val playlists: List<Playlist>, val pinnedAlbums: Set<String>)

@Composable
private fun rememberLibraryCollections(): LibraryCollections? {
    val context = LocalContext.current
    val version by PlaylistManager.changes.collectAsState()
    var data by remember { mutableStateOf<LibraryCollections?>(null) }
    LaunchedEffect(version) {
        data = LibraryCollections(
            playlists = PlaylistManager.loadPlaylists(context),
            pinnedAlbums = PlaylistManager.loadPinnedAlbums(context)
        )
    }
    return data
}

/** Экран «Pins»: закреплённые плейлисты и альбомы. Открывается из нижней панели. */
@Composable
fun PinsScreen(
    songs: List<Song>,
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onBackClick: () -> Unit,
) {
    val data = rememberLibraryCollections()
    val albums = remember(songs) { songs.groupBy { it.album } }
    val pinned = remember(data, albums) {
        if (data == null) emptyList() else {
            data.playlists.filter { it.isPinned }.map { SectionItem.Playlist(it) } +
                    albums.filterKeys { it in data.pinnedAlbums }.entries.map { SectionItem.Album(it.key, it.value) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .swipeDownToBack(onBackClick)
    ) {
        ArtBackdrop(art = nowPlayingArt(), topScrim = 0.55f, midScrim = 0.82f)

        LazyVerticalGrid(
            state = rememberPersistentLazyGridState("pins"),
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 92.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (data != null && pinned.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Nothing pinned yet. Long-press an album or a playlist on the main screen and choose “Pin to top”.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                    )
                }
            }
            itemsIndexed(
                pinned,
                key = { _, it -> if (it is SectionItem.Playlist) "p:${it.playlist.id}" else "a:${(it as SectionItem.Album).name}" }
            ) { index, item ->
                Box(modifier = Modifier.fillMaxWidth().animateItem().staggeredEntrance(index, offsetY = 20.dp, once = true)) {
                    when (item) {
                        is SectionItem.Playlist -> PlaylistCoverTile(item.playlist, songs) { onPlaylistClick(item.playlist.id) }
                        is SectionItem.Album -> AlbumCoverTile(item.name, item.songs) { onAlbumClick(item.name) }
                    }
                }
            }
        }

        TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter))
        ScreenTopBar(
            title = stringResource(R.string.nav_pins),
            onBack = onBackClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/** Экран «Playlists»: все плейлисты и плитка «+» для создания нового. */
@Composable
fun PlaylistsScreen(
    songs: List<Song>,
    onPlaylistClick: (String) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val data = rememberLibraryCollections()
    val playlists = data?.playlists.orEmpty()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .swipeDownToBack(onBackClick)
    ) {
        ArtBackdrop(art = nowPlayingArt(), topScrim = 0.55f, midScrim = 0.82f)

        LazyVerticalGrid(
            state = rememberPersistentLazyGridState("playlists"),
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 92.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(playlists, key = { _, it -> it.id }) { index, playlist ->
                Box(modifier = Modifier.fillMaxWidth().animateItem().staggeredEntrance(index, offsetY = 20.dp, once = true)) {
                    PlaylistCoverTile(playlist, songs) { onPlaylistClick(playlist.id) }
                }
            }
            item(key = "add") {
                Box(modifier = Modifier.fillMaxWidth().animateItem()) {
                    AddPlaylistTile(onClick = onCreatePlaylistClick)
                }
            }
        }

        TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter))
        ScreenTopBar(
            title = stringResource(R.string.nav_playlists),
            onBack = onBackClick,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun PlaylistCoverTile(playlist: Playlist, songs: List<Song>, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClickable(shape = coverShapeOf(16.dp), onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PlaylistArt(
            playlist = playlist,
            allSongs = songs,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(coverShapeOf(16.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        TileCaption(playlist.name, stringResource(R.string.tracks_count, playlist.songIds.size))
    }
}

@Composable
private fun AlbumCoverTile(name: String, albumSongs: List<Song>, onClick: () -> Unit) {
    val art = rememberSongArt(albumSongs.firstOrNull())
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClickable(shape = coverShapeOf(16.dp), onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(coverShapeOf(16.dp)),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp
        ) {
            if (art != null) {
                Image(
                    bitmap = art.asImageBitmap(),
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        TileCaption(name, stringResource(R.string.tracks_count, albumSongs.size))
    }
}

@Composable
private fun TileCaption(title: String, subtitle: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface
    )
    Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
