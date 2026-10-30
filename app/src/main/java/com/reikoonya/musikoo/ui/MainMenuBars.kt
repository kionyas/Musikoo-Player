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
import com.reikoonya.musikoo.ui.components.expressiveBadgeShape
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
import com.reikoonya.musikoo.util.BottomBarItem
import com.reikoonya.musikoo.util.BottomBarPrefs
import com.reikoonya.musikoo.ui.components.MiniPlayerCard
import com.reikoonya.musikoo.ui.components.rememberSongArt

/** Search UI state, kept by MainActivity so it survives switching between tabs. */

@Composable
fun BoxScope.MainMenuTopBar(
    search: MainSearchState,
    mainMode: MainMode,
    onModeChange: (MainMode) -> Unit,
    songs: List<Song>,
    onSongClick: (Int) -> Unit,
    onAlbumClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    libraryTabsMode: Boolean = false,
    titleState: LibraryTitleState? = null,
    onMenuClick: () -> Unit = {},
    menuOpen: Boolean = false,
) {
    val topBarContext = LocalContext.current.applicationContext
    remember(topBarContext) { com.reikoonya.musikoo.util.UiStylePrefs.ensureLoaded(topBarContext) }
    val menuStyle = com.reikoonya.musikoo.util.UiStylePrefs.menuTopBar && titleState != null

    if (menuStyle && titleState != null) {
        MenuTopBar(
            mainMode = mainMode,
            libraryTabsMode = libraryTabsMode,
            titleState = titleState,
            onMenuClick = onMenuClick,
            menuOpen = menuOpen
        )
    }

    if (!menuStyle) Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = glassStroke(),
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val settingsShape = expressiveBadgeShape("settings".hashCode())
                BounceSurface(
                    modifier = Modifier.size(36.dp),
                    shape = settingsShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    onClick = onSettingsClick
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.settings),
                            modifier = Modifier.size(19.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                MainModeSwitch(mode = mainMode, onModeChange = onModeChange)

                val aboutShape = expressiveBadgeShape("about".hashCode())
                BounceSurface(
                    modifier = Modifier.size(36.dp),
                    shape = aboutShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    onClick = onAboutClick
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = stringResource(R.string.about),
                            modifier = Modifier.size(19.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    AnimatedVisibility(
        visible = search.open.value,
        enter = fadeIn(animationSpec = tween(250)) + slideInVertically(
            animationSpec = tween(
                durationMillis = 380,
                easing = FastOutSlowInEasing
            )
        ) { it / 12 },
        exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
            animationSpec = tween(
                durationMillis = 380,
                easing = FastOutSlowInEasing
            )
        ) { it / 12 }
    ) {
        SearchResultsList(
            songs = songs,
            query = search.query.value,
            mode = search.mode.value,
            genre = search.genre.value,
            onSongClick = onSongClick,
            onAlbumClick = onAlbumClick,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .imePadding()
                .navigationBarsPadding()
        )
    }

    AnimatedVisibility(
        visible = search.open.value,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(180))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter), blurred = true)
            val searchTitleMenuStyle = com.reikoonya.musikoo.ui.components.rememberMenuTopBarActive()
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (searchTitleMenuStyle) Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .animateEnterExit(
                            enter = fadeIn(tween(250)) + slideInVertically(
                                tween(340, easing = FastOutSlowInEasing)
                            ) { -it },
                            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.search),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                } else Surface(
                    modifier = Modifier.animateEnterExit(
                        enter = scaleIn(
                            tween(340, easing = FastOutSlowInEasing),
                            initialScale = 0.7f
                        ) +
                                slideInVertically(
                                    tween(
                                        340,
                                        easing = FastOutSlowInEasing
                                    )
                                ) { -it },
                        exit = scaleOut(tween(200), targetScale = 0.8f) + slideOutVertically(
                            tween(200)
                        ) { -it }
                    ),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = glassStroke(),
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.search).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BoxScope.MainMenuBottomBar(
    search: MainSearchState,
    searchGenres: List<String>,
    currentSong: Song?,
    isMiniPlayerHidden: Boolean,
    onMiniPlayerHiddenChange: (Boolean) -> Unit,
    onPlaylistsChanged: () -> Unit,
    onOpenPlayer: () -> Unit,
    onSongsClick: () -> Unit,
    onArtistsClick: () -> Unit,
    onAlbumsClick: () -> Unit,
    onGenresClick: () -> Unit,
    onGenreClick: (String) -> Unit,
    onPinsClick: () -> Unit,
    onPlaylistsClick: () -> Unit,
    onRandomizeClick: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val favIds by PlaylistManager.favorites.collectAsState()
    val isFavSong = currentSong?.let { it.id in favIds } == true
    val currentArt = rememberSongArt(currentSong)

    // Состав нижней панели настраивается пользователем (Settings → Interface → Bottom Bar)
    val appContext = context.applicationContext
    remember(appContext) { BottomBarPrefs.ensureLoaded(appContext) }
    val searchVisible = BottomBarPrefs.searchVisible
    val barKeys = BottomBarPrefs.items
    val barItems = remember(barKeys, searchGenres) {
        barKeys.mapNotNull { BottomBarItem.fromKey(it) }
            // Закреплённый жанр, которого больше нет в библиотеке, на панели не показываем
            .filter { item ->
                item !is BottomBarItem.Genre || searchGenres.any { it.equals(item.name, ignoreCase = true) }
            }
    }
    val navShown = !search.open.value &&
            (!search.navHidden.value || !searchVisible) &&
            barItems.isNotEmpty()
    val showBlock = search.open.value || searchVisible || barItems.isNotEmpty()

    fun handleItemClick(item: BottomBarItem) {
        when (item) {
            BottomBarItem.Search -> search.open.value = true
            BottomBarItem.Artists -> onArtistsClick()
            BottomBarItem.Songs -> onSongsClick()
            BottomBarItem.Albums -> onAlbumsClick()
            BottomBarItem.Genres -> onGenresClick()
            BottomBarItem.Random -> onRandomizeClick()
            BottomBarItem.Pins -> onPinsClick()
            BottomBarItem.Playlists -> onPlaylistsClick()
            is BottomBarItem.Genre -> onGenreClick(item.name)
        }
    }

    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // The search field lives inside the bottom pill; here the mini player shows as usual
        if (!search.open.value && currentSong != null && !isMiniPlayerHidden) {
            MiniPlayerCard(
                song = currentSong,
                art = currentArt,
                isFavorite = isFavSong,
                onOpenPlayer = onOpenPlayer,
                onToggleFavorite = {
                    PlaylistManager.toggleFavorite(context, currentSong.id)
                    onPlaylistsChanged()
                },
                onHide = { onMiniPlayerHiddenChange(true) }
            )
        }

        // Нижний блок: необязательная строка поиска + прокручиваемая навигация в одной карточке
        var searchDragOffsetY by remember { mutableFloatStateOf(0f) }
        val searchGap by animateDpAsState(
            targetValue = if (search.open.value || !navShown || !searchVisible) 0.dp else 6.dp,
            label = "searchGap"
        )
        AnimatedVisibility(
            visible = showBlock,
            enter = fadeIn(tween(220)) + scaleIn(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                initialScale = 0.9f
            ),
            exit = fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.92f)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(searchVisible) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    // Свайп прячет и возвращает навигацию, только если есть строка поиска с кнопкой
                                    if (searchVisible) {
                                        if (searchDragOffsetY > 30f) {
                                            search.navHidden.value = true
                                        } else if (searchDragOffsetY < -30f) {
                                            search.navHidden.value = false
                                        }
                                    }
                                    searchDragOffsetY = 0f
                                },
                                onDragCancel = { searchDragOffsetY = 0f }
                            ) { _, dragAmount ->
                                searchDragOffsetY += dragAmount
                            }
                        },
                    // 32 dp = 26 dp (inner pill radius) + 6 dp (padding): corners stay concentric
                    shape = RoundedCornerShape(32.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = glassStroke(),
                    tonalElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.Top
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = searchGap),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AnimatedContent(
                                    targetState = search.open.value,
                                    transitionSpec = {
                                        (fadeIn(animationSpec = tween(250)) togetherWith
                                                fadeOut(animationSpec = tween(200)))
                                            .using(SizeTransform(clip = false))
                                    },
                                    label = "searchSwap"
                                ) { open ->
                                    if (open) {
                                        SearchPanel(
                                            query = search.query.value,
                                            onQueryChange = { search.query.value = it },
                                            onClose = { search.open.value = false },
                                            mode = search.mode.value,
                                            onModeChange = { search.mode.value = it },
                                            genres = searchGenres,
                                            selectedGenre = search.genre.value,
                                            onGenreChange = { search.genre.value = it }
                                        )
                                    } else if (searchVisible) {
                                        CollapsedSearchPill(
                                            onClick = { search.open.value = true },
                                            navHidden = search.navHidden.value,
                                            onToggleNav = { search.navHidden.value = !search.navHidden.value }
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = navShown,
                                enter = fadeIn(animationSpec = tween(200)) + expandVertically(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                                ),
                                exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                                )
                            ) {
                                BottomNavBar(
                                    items = barItems,
                                    onItemClick = { handleItemClick(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
