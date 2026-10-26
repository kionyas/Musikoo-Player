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
import com.reikoonya.musikoo.ui.components.MiniBarSeekBar
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
) {
        Box(
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
                TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter))
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
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
    onRandomizeClick: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val favIds by PlaylistManager.favorites.collectAsState()
    val isFavSong = currentSong?.let { it.id in favIds } == true
    val currentArt = rememberSongArt(currentSong)

    // Mini-player interactive gesture state
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val animOffsetX by animateFloatAsState(
        targetValue = dragOffsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "animOffsetX"
    )

    val thresholdPx = remember(density) { with(density) { 100.dp.toPx() } }

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
                // Drag Progress Thresholds
                val isDraggingLeftPastThreshold = dragOffsetX < -thresholdPx
                val isDraggingRightPastThreshold = dragOffsetX > thresholdPx

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Side Option Indicators (glowing badges visible behind the mini-player while dragging)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Indicator: Hide Mini Player
                        val rightRatio = (dragOffsetX / thresholdPx).coerceIn(0f, 1.2f)
                        if (dragOffsetX > 10f) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDraggingRightPastThreshold) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = glassStroke(),
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = 0.8f + (rightRatio * 0.35f)
                                        scaleY = 0.8f + (rightRatio * 0.35f)
                                        alpha = rightRatio.coerceIn(0.2f, 1f)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = null,
                                        tint = if (isDraggingRightPastThreshold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isDraggingRightPastThreshold) {
                                        Text(
                                            text = stringResource(R.string.hide_mini_player),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        // Right Indicator: Favorites Toggle
                        val leftRatio = (-dragOffsetX / thresholdPx).coerceIn(0f, 1.2f)
                        if (dragOffsetX < -10f) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDraggingLeftPastThreshold) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = glassStroke(),
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = 0.8f + (leftRatio * 0.35f)
                                        scaleY = 0.8f + (leftRatio * 0.35f)
                                        alpha = leftRatio.coerceIn(0.2f, 1f)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 8.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFavSong) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (isDraggingLeftPastThreshold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isDraggingLeftPastThreshold) {
                                        Text(
                                            text = if (isFavSong) stringResource(R.string.remove_from_favorites) else stringResource(
                                                R.string.add_to_favorites
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }
                    }

                    // Mini-Player Card with Smooth Drag Translation & Threshold Release
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                translationX = animOffsetX
                            }
                            .pointerInput(currentSong.id) {
                                detectDragGestures(
                                    onDragEnd = {
                                        val passedLeftThreshold = dragOffsetX < -thresholdPx
                                        val passedRightThreshold = dragOffsetX > thresholdPx
                                        val passedUpThreshold = dragOffsetY < -thresholdPx

                                        if (passedUpThreshold) {
                                            onOpenPlayer()
                                            dragOffsetX = 0f
                                            dragOffsetY = 0f
                                        } else if (passedLeftThreshold) {
                                            PlaylistManager.toggleFavorite(context, currentSong.id)
                                            onPlaylistsChanged()
                                            dragOffsetX = 0f
                                            dragOffsetY = 0f
                                        } else if (passedRightThreshold) {
                                            onMiniPlayerHiddenChange(true)
                                            dragOffsetX = 0f
                                            dragOffsetY = 0f
                                        } else {
                                            dragOffsetX = 0f
                                            dragOffsetY = 0f
                                        }
                                    },
                                    onDragCancel = {
                                        dragOffsetX = 0f
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetX += dragAmount.x
                                        dragOffsetY = (dragOffsetY + dragAmount.y).coerceAtMost(0f)
                                    }
                                )
                            }
                            .bounceClickable(shape = shapeOf(28.dp)) {
                                onOpenPlayer()
                            },
                        shape = shapeOf(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        border = glassStroke(),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 1. Album Art
                                Surface(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(coverShapeOf(12.dp)),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    MagicArt(
                                        art = currentArt,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.MusicNote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // 2. Title & Artist
                                TrackTextSwap(
                                    label = TrackLabel(
                                        currentSong.id,
                                        currentSong.title,
                                        currentSong.artist
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) { l ->
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = l.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            modifier = Modifier.marqueeEdgeFade()
                                        )
                                        Text(
                                            text = l.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            modifier = Modifier.marqueeEdgeFade()
                                        )
                                    }
                                }

                                // 3. Controls: Previous, Play/Pause, Next
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    BounceIconButton(onClick = { MusicPlayerManager.skipPrevious() }) {
                                        Icon(
                                            imageVector = Icons.Rounded.SkipPrevious,
                                            contentDescription = stringResource(R.string.previous),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    ExpressivePlayButton(
                                        isPlaying = MusicPlayerManager.isPlayingState,
                                        onClick = { MusicPlayerManager.togglePlayPause() },
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        width = 44.dp,
                                        height = 44.dp,
                                        iconSize = 22.dp,
                                        playingCorner = 12.dp,
                                        idleCorner = 16.dp
                                    )
                                    BounceIconButton(onClick = { MusicPlayerManager.skipNext() }) {
                                        Icon(
                                            imageVector = Icons.Rounded.SkipNext,
                                            contentDescription = stringResource(R.string.next),
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 4. Draggable Wavy Slider: reads the position itself, so the rest of the bar does not redraw on each tick
                            MiniBarSeekBar(isPlaying = MusicPlayerManager.isPlayingState)
                        }
                    }
                }
            }

            // Unified Bottom Block (Search Bar + Bottom Nav in one single card)
            var searchDragOffsetY by remember { mutableFloatStateOf(0f) }
            val searchGap by animateDpAsState(
                targetValue = if (search.open.value || search.navHidden.value) 0.dp else 6.dp,
                label = "searchGap"
            )
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (searchDragOffsetY > 30f) {
                                        search.navHidden.value = true
                                    } else if (searchDragOffsetY < -30f) {
                                        search.navHidden.value = false
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
                                    } else {
                                        CollapsedSearchPill(
                                            onClick = { search.open.value = true },
                                            navHidden = search.navHidden.value,
                                            onToggleNav = { search.navHidden.value = !search.navHidden.value }
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = !search.open.value && !search.navHidden.value,
                                enter = fadeIn(animationSpec = tween(200)) + expandVertically(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                                ),
                                exit = fadeOut(animationSpec = tween(150)) + shrinkVertically(
                                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                                )
                            ) {
                                BottomNavBar(
                                    onArtists = onArtistsClick,
                                    onSongs = onSongsClick,
                                    onRandom = onRandomizeClick
                                )
                            }
                        }

                    }
                }
            }
        }
}
