package com.reikoonya.musikoo.ui

import androidx.compose.foundation.layout.BoxScope
import com.reikoonya.musikoo.ui.components.TrackTextSwap
import com.reikoonya.musikoo.ui.components.TrackLabel
import com.reikoonya.musikoo.ui.components.MagicArt
import com.reikoonya.musikoo.ui.components.coverShapeOf
import androidx.compose.animation.scaleOut
import androidx.compose.animation.scaleIn
import com.reikoonya.musikoo.ui.components.marqueeEdgeFade
import com.reikoonya.musikoo.ui.components.shapeOf
import com.reikoonya.musikoo.ui.components.rememberPersistentLazyGridState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.rounded.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import com.reikoonya.musikoo.util.ShareUtils
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.data.Playlist
import com.reikoonya.musikoo.data.PlaylistManager
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.ui.components.ActionsDialog
import com.reikoonya.musikoo.ui.components.BottomFadeScrim
import com.reikoonya.musikoo.ui.components.BounceIconButton
import com.reikoonya.musikoo.ui.components.BounceSurface
import com.reikoonya.musikoo.ui.components.EditPlaylistDialog
import com.reikoonya.musikoo.ui.components.ExpressivePlayButton
import com.reikoonya.musikoo.ui.components.MarkdownGuideDialog
import com.reikoonya.musikoo.ui.components.MenuAction
import com.reikoonya.musikoo.ui.components.PlaylistArt
import com.reikoonya.musikoo.ui.components.TopFadeScrim
import com.reikoonya.musikoo.ui.components.WavyProgress
import com.reikoonya.musikoo.ui.components.bounceClickable
import com.reikoonya.musikoo.ui.components.bounceCombinedClickable
import com.reikoonya.musikoo.ui.components.glassFill
import com.reikoonya.musikoo.ui.components.glassStroke
import com.reikoonya.musikoo.util.MusicPlayerManager
import com.reikoonya.musikoo.ui.components.rememberSongArt
import kotlinx.coroutines.launch

@Composable
fun LibraryTabsHeader(
    titles: List<String>,
    currentPage: Int,
    onPageSelected: (Int) -> Unit
) {
    val currentTitle = titles.getOrElse(currentPage) { "Albums" }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f),
        border = glassStroke(),
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Section title
            Text(
                text = currentTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Pagination dots below title
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                titles.forEachIndexed { index, _ ->
                    val selected = index == currentPage
                    val dotWidth by animateDpAsState(
                        targetValue = if (selected) 20.dp else 8.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                        label = "dotWidth"
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        label = "dotColor"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                            .bounceClickable(shape = CircleShape) {
                                onPageSelected(index)
                            }
                    )
                }
            }
        }
    }
}

/** Search UI state, kept by MainActivity so it survives switching between tabs. */

@Composable
fun BoxScope.MainMenuLibrary(
    mainMode: MainMode,
    songs: List<Song>,
    albums: Map<String, List<Song>>,
    playlists: List<Playlist>,
    pinnedPlaylists: List<Playlist>,
    pinnedAlbumsList: Map<String, List<Song>>,
    showSections: Boolean,
    hasPinnedSection: Boolean,
    currentSong: Song?,
    isMiniPlayerHidden: Boolean,
    dialogs: MainMenuDialogState,
    libraryTabsMode: Boolean,
    onAlbumClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onCreatePlaylist: () -> Unit,
) {
    val context = LocalContext.current

    // Состояние свёрнутых секций хранится здесь: родителю оно не нужно
    var isPinnedCollapsed by remember {
        mutableStateOf(
            PlaylistManager.isSectionCollapsed(
                context,
                "pinned"
            )
        )
    }
    var isPlaylistsCollapsed by remember {
        mutableStateOf(
            PlaylistManager.isSectionCollapsed(
                context,
                "playlists"
            )
        )
    }
    var isAlbumsCollapsed by remember {
        mutableStateOf(
            PlaylistManager.isSectionCollapsed(
                context,
                "albums"
            )
        )
    }

    if (mainMode == MainMode.Library) {
        if (libraryTabsMode) {
            val pages = com.reikoonya.musikoo.util.UiStylePrefs.libraryPages
            val pagerState = rememberPagerState(pageCount = { pages.size })
            val scope = rememberCoroutineScope()

            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    LazyVerticalGrid(
                        state = rememberPersistentLazyGridState("library_page_${pages.getOrElse(page) { page.toString() }}"),
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            // Соседняя страница чуть меньше и прозрачнее: свайп выглядит как плавный переход
                            if (!com.reikoonya.musikoo.util.UiStylePrefs.liveAnimations) return@graphicsLayer
                            val distance = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                                .let { if (it < 0f) -it else it }
                                .coerceIn(0f, 1f)
                            val s = 1f - 0.06f * distance
                            scaleX = s
                            scaleY = s
                            alpha = 1f - 0.5f * distance
                        },
                        contentPadding = PaddingValues(
                            top = 132.dp,
                            bottom = if (currentSong != null && !isMiniPlayerHidden) 246.dp else 140.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (pages.getOrNull(page)) {
                            "albums" -> {
                                if (albums.isNotEmpty()) {
                                    item(span = { GridItemSpan(3) }) {
                                        SectionGrid(
                                            items = albums.entries.toList(),
                                            columns = 3
                                        ) { (albumName, albumSongs) ->
                                            val firstArt = rememberSongArt(albumSongs.firstOrNull())
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .bounceCombinedClickable(
                                                        shape = coverShapeOf(16.dp),
                                                        onLongClick = { dialogs.actionsAlbum = albumName }) {
                                                        onAlbumClick(albumName)
                                                    },
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
                                                    if (firstArt != null) {
                                                        Image(
                                                            bitmap = firstArt.asImageBitmap(),
                                                            contentDescription = albumName,
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
                                                Text(
                                                    text = albumName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = stringResource(R.string.tracks_count, albumSongs.size),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            "pins" -> {
                                if (pinnedPlaylists.isNotEmpty() || pinnedAlbumsList.isNotEmpty()) {
                                    item(span = { GridItemSpan(3) }) {
                                        SectionGrid(
                                            items = pinnedPlaylists.map { SectionItem.Playlist(it) } +
                                                    pinnedAlbumsList.entries.map {
                                                        SectionItem.Album(it.key, it.value)
                                                    },
                                            columns = 3
                                        ) { item ->
                                            when (item) {
                                                is SectionItem.Playlist -> {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .bounceCombinedClickable(
                                                                shape = coverShapeOf(16.dp),
                                                                onLongClick = {
                                                                    dialogs.actionsPlaylist = item.playlist
                                                                }) {
                                                                onPlaylistClick(item.playlist.id)
                                                            },
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        PlaylistArt(
                                                            playlist = item.playlist,
                                                            allSongs = songs,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .aspectRatio(1f)
                                                                .clip(coverShapeOf(16.dp))
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(
                                                            text = item.playlist.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = stringResource(
                                                                R.string.tracks_count,
                                                                item.playlist.songIds.size
                                                            ),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                                is SectionItem.Album -> {
                                                    val firstArt = rememberSongArt(item.songs.firstOrNull())
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .bounceCombinedClickable(
                                                                shape = coverShapeOf(16.dp),
                                                                onLongClick = {
                                                                    dialogs.actionsAlbum = item.name
                                                                }) { onAlbumClick(item.name) },
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
                                                            if (firstArt != null) {
                                                                Image(
                                                                    bitmap = firstArt.asImageBitmap(),
                                                                    contentDescription = item.name,
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
                                                        Text(
                                                            text = item.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = stringResource(
                                                                R.string.tracks_count,
                                                                item.songs.size
                                                            ),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            "playlists" -> {
                                run {
                                    item(span = { GridItemSpan(3) }) {
                                        SectionGrid(
                                            items = playlists + null,
                                            columns = 3
                                        ) { playlist ->
                                            if (playlist == null) {
                                                AddPlaylistTile(onClick = onCreatePlaylist)
                                                return@SectionGrid
                                            }
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .bounceCombinedClickable(
                                                        shape = coverShapeOf(16.dp),
                                                        onLongClick = { dialogs.actionsPlaylist = playlist }) {
                                                        onPlaylistClick(playlist.id)
                                                    },
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
                                                Text(
                                                    text = playlist.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = stringResource(
                                                        R.string.tracks_count,
                                                        playlist.songIds.size
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }

                // Top Fade Scrim over pager content so scrolling items fade out
                TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter))

                // Floating Library Tabs Header drawn ON TOP of TopFadeScrim (fully bright, un-darkened)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 70.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LibraryTabsHeader(
                        titles = pages.map { libraryTitle(it) },
                        currentPage = pagerState.currentPage.coerceIn(0, pages.lastIndex),
                        onPageSelected = { page ->
                            scope.launch { pagerState.animateScrollToPage(page) }
                        }
                    )
                }
            }
        } else {
            // Standard combined view
            LazyVerticalGrid(
                state = rememberPersistentLazyGridState("main"),
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = 92.dp,
                    bottom = if (currentSong != null && !isMiniPlayerHidden) 246.dp else 140.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. PINNED SECTION
                val classic = com.reikoonya.musikoo.util.UiStylePrefs.classicSections
                if ("pins" in classic && (pinnedPlaylists.isNotEmpty() || pinnedAlbumsList.isNotEmpty())) {
                    item(span = { GridItemSpan(3) }) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SectionHeader(
                                title = stringResource(R.string.pinned),
                                collapsed = isPinnedCollapsed,
                                topPadding = 8.dp,
                                onClick = {
                                    isPinnedCollapsed =
                                        PlaylistManager.toggleSectionCollapse(context, "pinned")
                                }
                            )

                            AnimatedVisibility(
                                visible = !isPinnedCollapsed,
                                enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) + fadeIn(),
                                exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeOut()
                            ) {
                                SectionGrid(
                                    items = pinnedPlaylists.map { SectionItem.Playlist(it) } +
                                            pinnedAlbumsList.entries.map {
                                                SectionItem.Album(
                                                    it.key,
                                                    it.value
                                                )
                                            },
                                    columns = 3
                                ) { item ->
                                    when (item) {
                                        is SectionItem.Playlist -> {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .bounceCombinedClickable(
                                                        shape = coverShapeOf(16.dp),
                                                        onLongClick = {
                                                            dialogs.actionsPlaylist = item.playlist
                                                        }) {
                                                        onPlaylistClick(item.playlist.id)
                                                    },
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                PlaylistArt(
                                                    playlist = item.playlist,
                                                    allSongs = songs,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .aspectRatio(1f)
                                                        .clip(coverShapeOf(16.dp))
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = item.playlist.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = stringResource(
                                                        R.string.tracks_count,
                                                        item.playlist.songIds.size
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        is SectionItem.Album -> {
                                            val firstArt = rememberSongArt(item.songs.firstOrNull())
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .bounceCombinedClickable(
                                                        shape = coverShapeOf(16.dp),
                                                        onLongClick = {
                                                            dialogs.actionsAlbum = item.name
                                                        }) { onAlbumClick(item.name) },
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
                                                    if (firstArt != null) {
                                                        Image(
                                                            bitmap = firstArt.asImageBitmap(),
                                                            contentDescription = item.name,
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
                                                Text(
                                                    text = item.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = stringResource(
                                                        R.string.tracks_count,
                                                        item.songs.size
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                }

                // 2. PLAYLISTS SECTION
                if ("playlists" in classic) {
                    item(span = { GridItemSpan(3) }) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SectionHeader(
                                title = stringResource(R.string.playlists),
                                collapsed = isPlaylistsCollapsed,
                                topPadding = if (hasPinnedSection && isPinnedCollapsed) 0.dp else 12.dp,
                                onClick = {
                                    isPlaylistsCollapsed =
                                        PlaylistManager.toggleSectionCollapse(context, "playlists")
                                }
                            )

                            AnimatedVisibility(
                                visible = !isPlaylistsCollapsed,
                                enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) + fadeIn(),
                                exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeOut()
                            ) {
                                SectionGrid(
                                    items = playlists + null,
                                    columns = 3
                                ) { playlist ->
                                    if (playlist == null) {
                                        AddPlaylistTile(onClick = onCreatePlaylist)
                                        return@SectionGrid
                                    }
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .bounceCombinedClickable(
                                                shape = coverShapeOf(16.dp),
                                                onLongClick = { dialogs.actionsPlaylist = playlist }) {
                                                onPlaylistClick(playlist.id)
                                            },
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
                                        Text(
                                            text = playlist.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(
                                                R.string.tracks_count,
                                                playlist.songIds.size
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                        }
                    }
                }

                // 3. ALBUMS SECTION
                if ("albums" in classic) item(span = { GridItemSpan(3) }) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (showSections && albums.isNotEmpty()) {
                            SectionHeader(
                                title = stringResource(R.string.albums),
                                collapsed = isAlbumsCollapsed,
                                topPadding = 12.dp,
                                onClick = {
                                    isAlbumsCollapsed =
                                        PlaylistManager.toggleSectionCollapse(context, "albums")
                                }
                            )
                        }
                        AnimatedVisibility(
                            visible = !isAlbumsCollapsed,
                            enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)) + fadeIn(),
                            exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)) + fadeOut()
                        ) {
                            SectionGrid(
                                items = albums.entries.toList(),
                                columns = 3
                            ) { (albumName, albumSongs) ->
                                val firstArt = rememberSongArt(albumSongs.firstOrNull())
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .bounceCombinedClickable(
                                            shape = coverShapeOf(16.dp),
                                            onLongClick = { dialogs.actionsAlbum = albumName }) {
                                            onAlbumClick(
                                                albumName
                                            )
                                        },
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
                                        if (firstArt != null) {
                                            Image(
                                                bitmap = firstArt.asImageBitmap(),
                                                contentDescription = albumName,
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
                                    Text(
                                        text = albumName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.tracks_count, albumSongs.size),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    } else if (mainMode == MainMode.Stats) {
        StatsPanel(
            songs = songs,
            contentPadding = PaddingValues(
                top = 92.dp,
                bottom = if (currentSong != null && !isMiniPlayerHidden) 246.dp else 140.dp,
                start = 16.dp,
                end = 16.dp
            ),
            modifier = Modifier.fillMaxSize()
        )
        // Top fade scrim: list rows softly disappear under the top pill, like on the other screens
        TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter))
    }
}
/** Название страницы свайпов по ключу раздела. */
internal fun libraryTitle(key: String): String = when (key) {
    "albums" -> "Albums"
    "pins" -> "Pins"
    "playlists" -> "Playlists"
    else -> key
}
