package com.reikoonya.musikoo.ui

import com.reikoonya.musikoo.ui.components.AppLoadingIndicator
import com.reikoonya.musikoo.ui.components.coverShapeOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import com.reikoonya.musikoo.ui.components.shapeOf
import androidx.compose.material.icons.rounded.*
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import com.reikoonya.musikoo.ui.components.CascadeText
import com.reikoonya.musikoo.ui.components.liveAnimationsEnabled
import com.reikoonya.musikoo.ui.components.staggeredEntrance
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.reikoonya.musikoo.ui.components.rememberSongArt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.scrobble.LastFmStats
import com.reikoonya.musikoo.scrobble.LocalStats
import com.reikoonya.musikoo.scrobble.Scrobbler
import com.reikoonya.musikoo.scrobble.StatItem
import com.reikoonya.musikoo.scrobble.StatsRange
import com.reikoonya.musikoo.scrobble.StatsSnapshot
import com.reikoonya.musikoo.ui.components.BounceSurface
import com.reikoonya.musikoo.ui.components.cardFill
import com.reikoonya.musikoo.ui.components.glassStroke
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.graphics.vector.ImageVector
import com.reikoonya.musikoo.ui.components.bounceClickable
import com.reikoonya.musikoo.ui.components.expressiveBadgeShape
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.reikoonya.musikoo.ui.components.topFadeBlur
import com.reikoonya.musikoo.ui.components.marqueeEdgeFade

/** What the main screen shows under the pill. */
enum class MainMode(val label: String) {
    Library("Home"),
    Stats("Statistics")
}

/**
 * Compact mode selector in the top pill: shows the current mode and an arrow.
 * Tapping it expands downward to show the other options.
 */
@Composable
fun MainModeSwitch(mode: MainMode, onModeChange: (MainMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "modeArrow",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BounceSurface(
            onClick = { expanded = !expanded },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Row(
                modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The chip grows/shrinks with the new label while the text slides in
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        (slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it } +
                            fadeIn(tween(240, delayMillis = 60))) togetherWith
                            (slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { -it } +
                                fadeOut(tween(120))) using
                            SizeTransform(clip = false) { _, _ ->
                                spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                            }
                    },
                    label = "modeLabel"
                ) { m ->
                    Text(
                        text = m.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp).rotate(arrowRotation)
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) +
                fadeIn(tween(220, delayMillis = 60)) + scaleIn(initialScale = 0.9f),
            exit = shrinkVertically(tween(260, easing = FastOutSlowInEasing)) +
                fadeOut(tween(140)) + scaleOut(targetScale = 0.9f)
        ) {
            Column(
                modifier = Modifier.padding(top = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MainMode.entries.filter { it != mode }.forEach { option ->
                    BounceSurface(
                        onClick = {
                            expanded = false
                            onModeChange(option)
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}


/** Where the numbers come from. */
enum class StatsSource(val label: String) {
    LOCAL("This device"),
    LASTFM("Last.fm"),
}

private const val COLLAPSED_ROWS = 5

/**
 * Statistics screen in the same look as Settings: grouped rounded cards, uppercase section labels,
 * expressive icon badges. Compact: a small control card, two stat tiles and short top lists
 * (5 rows, "Show all" expands the rest). Switch between this device and Last.fm.
 */
@Composable
fun StatsPanel(
    songs: List<Song>,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val version by Scrobbler.playsVersion.collectAsState()
    var source by rememberSaveable { mutableStateOf(StatsSource.LOCAL) }
    var range by rememberSaveable { mutableStateOf(StatsRange.WEEK) }
    var snapshot by remember { mutableStateOf<StatsSnapshot?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    val user = Scrobbler.username()
    // Covers are looked up once per snapshot, not once per row on every recomposition
    val covers = remember(snapshot, songs) {
        snapshot?.let { s ->
            (s.artists + s.albums + s.tracks + s.composers + s.recent).associateWith { artFor(it, songs) }
        } ?: emptyMap()
    }

    LaunchedEffect(source, range, version, songs.size) {
        loading = true
        val result = withContext(Dispatchers.IO) {
            runCatching {
                when (source) {
                    StatsSource.LOCAL -> LocalStats.build(Scrobbler.loadPlays(), songs, range)
                    StatsSource.LASTFM -> LastFmStats.fetch(
                        user ?: throw IllegalStateException("Connect Last.fm in Settings first"),
                        range
                    )
                }
            }
        }
        snapshot = result.getOrNull()
        error = result.exceptionOrNull()?.message
        loading = false
    }

    LazyColumn(
        modifier = modifier.topFadeBlur(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(modifier = Modifier.staggeredEntrance(0, once = true)) {
                ControlsCard(source, { source = it }, range, { range = it })
            }
        }
        item {
            StatTiles(
                source = source,
                snapshot = snapshot,
                loading = loading,
                error = error,
                user = user,
            )
        }
        snapshot?.let { s ->
            if (s.artists.isNotEmpty()) {
                item {
                    StatsSection("Top artists", s.artists, covers, "plays", Icons.Rounded.Person, refreshing = loading)
                }
            }
            if (s.albums.isNotEmpty()) {
                item {
                    StatsSection("Top albums", s.albums, covers, "plays", Icons.Rounded.Album, refreshing = loading)
                }
            }
            if (s.tracks.isNotEmpty()) {
                item {
                    StatsSection("Top tracks", s.tracks, covers, "plays", Icons.Rounded.MusicNote, refreshing = loading)
                }
            }
            if (s.composers.isNotEmpty()) {
                item {
                    StatsSection("Composers", s.composers, covers, "tracks", Icons.Rounded.Edit, refreshing = loading, letterFallback = true)
                }
            }
            if (s.recent.isNotEmpty()) {
                item {
                    StatsSection("Recently played", s.recent, covers, null, Icons.Rounded.History, refreshing = loading, showRank = false)
                }
            }
        }
    }
}

/** Finds a cover in the library for a statistics row. */
private fun artFor(item: StatItem, songs: List<Song>): Song? {
    val match = songs.firstOrNull { s ->
        when {
            item.matchComposer.isNotEmpty() -> s.composer.equals(item.matchComposer, ignoreCase = true)
            item.matchTitle.isNotEmpty() -> s.title.equals(item.matchTitle, ignoreCase = true) &&
                (item.matchArtist.isEmpty() || s.artist.equals(item.matchArtist, ignoreCase = true))
            item.matchAlbum.isNotEmpty() -> s.album.equals(item.matchAlbum, ignoreCase = true) &&
                (item.matchArtist.isEmpty() || s.artist.equals(item.matchArtist, ignoreCase = true))
            item.matchArtist.isNotEmpty() -> s.artist.equals(item.matchArtist, ignoreCase = true)
            else -> false
        }
    }
    return match
}

/** One grouped card with both selectors: source on top, period below. */
@Composable
private fun ControlsCard(
    source: StatsSource,
    onSource: (StatsSource) -> Unit,
    range: StatsRange,
    onRange: (StatsRange) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeOf(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SegmentRow(StatsSource.entries, source, { it.label }, onSource)
            SegmentRow(StatsRange.entries, range, { it.label }, onRange)
        }
    }
}

/**
 * Equal-width pill segments. One accent pill slides under the labels with a springy overshoot
 * and stretches a little while it travels.
 */
@Composable
private fun <T> SegmentRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val accent = MaterialTheme.colorScheme.primary
    val selIndex = options.indexOf(selected).coerceAtLeast(0)
    val pos by animateFloatAsState(
        targetValue = selIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
        label = "segPos"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val gap = 4.dp.toPx()
                val n = options.size
                val segW = (size.width - gap * (n - 1)) / n
                // The farther the pill is from its target, the longer it stretches
                val stretch = 1f + 0.18f * abs(pos - selIndex).coerceAtMost(1f)
                val w = segW * stretch
                val x = pos * (segW + gap) - (w - segW) / 2f
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(x, 0f),
                    size = Size(w, size.height),
                    cornerRadius = CornerRadius(size.height / 2f)
                )
            },
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { index, option ->
            val isSel = index == selIndex
            val fg by animateColorAsState(
                if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "segFg"
            )
            BounceSurface(
                onClick = {
                    if (!isSel) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect(option)
                },
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                color = Color.Transparent
            ) {
                Text(
                    text = label(option),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                    color = fg,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

/** Number that rolls up to [target] (and between values when it changes). */
@Composable
private fun rollingNumber(target: Long): Float {
    val live = liveAnimationsEnabled()
    var go by remember { mutableStateOf(!live) }
    LaunchedEffect(Unit) { go = true }
    val v by animateFloatAsState(
        targetValue = if (go) target.toFloat() else 0f,
        animationSpec = if (live) tween(durationMillis = 1000, easing = FastOutSlowInEasing) else snap(),
        label = "rolling"
    )
    return v
}

/** Two small tiles with the totals; they pop in one after another and the numbers roll up. */
@Composable
private fun StatTiles(
    source: StatsSource,
    snapshot: StatsSnapshot?,
    loading: Boolean,
    error: String?,
    user: String?,
) {
    if (!loading && error != null) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapeOf(28.dp),
            color = MaterialTheme.colorScheme.errorContainer
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(16.dp)
            )
        }
        return
    }

    val listened = rollingNumber(snapshot?.listenedMs ?: 0L)
    val plays = rollingNumber((snapshot?.totalPlays ?: 0).toLong())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (source == StatsSource.LOCAL) {
            StatTile(
                icon = Icons.Rounded.Schedule,
                label = "Listened",
                value = snapshot?.let { formatListened(listened.toLong()) },
                loading = loading,
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                delayMs = 80L,
            )
            StatTile(
                icon = Icons.Rounded.PlayArrow,
                label = "Plays",
                value = snapshot?.let { plays.toLong().toString() },
                loading = loading,
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
                delayMs = 180L,
            )
        } else {
            StatTile(
                icon = Icons.Rounded.GraphicEq,
                label = "Scrobbles",
                value = snapshot?.let { plays.toLong().toString() },
                loading = loading,
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                delayMs = 80L,
            )
            if (user != null) {
                StatTile(
                    icon = Icons.Rounded.Person,
                    label = "Last.fm",
                    value = "@$user",
                    loading = false,
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                    delayMs = 180L,
                )
            }
        }
    }
}

@Composable
private fun RowScope.StatTile(
    icon: ImageVector,
    label: String,
    value: String?,
    loading: Boolean,
    container: Color,
    content: Color,
    delayMs: Long,
) {
    // Springy pop-in with a small overshoot (without live animations the tile is just there)
    val live = liveAnimationsEnabled()
    var visible by remember { mutableStateOf(!live) }
    LaunchedEffect(Unit) {
        if (live) delay(delayMs)
        visible = true
    }
    val pop by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (live) spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow) else snap(),
        label = "tilePop"
    )
    // The badge shape slowly turns
    val spin by rememberInfiniteTransition(label = "tileSpin").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing)),
        label = "tileSpinAngle"
    )

    Surface(
        modifier = Modifier
            .weight(1f)
            .graphicsLayer {
                val s = 0.82f + 0.18f * pop
                scaleX = s
                scaleY = s
                alpha = pop.coerceIn(0f, 1f)
                translationY = (1f - pop) * 28f
            },
        shape = shapeOf(28.dp),
        color = container
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = if (live) spin else 0f }
                        .clip(expressiveBadgeShape(label.hashCode()))
                        .background(content.copy(alpha = 0.14f))
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            val s = 0.6f + 0.4f * pop
                            scaleX = s
                            scaleY = s
                            rotationZ = (1f - pop) * -90f
                        }
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (loading) {
                    Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.CenterStart) {
                        AppLoadingIndicator(size = 22.dp, color = content)
                    }
                } else {
                    Text(
                        text = value ?: "–",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = content,
                        maxLines = 1,
                        modifier = Modifier.marqueeEdgeFade(),
                        softWrap = false
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = content.copy(alpha = 0.8f),
                    maxLines = 1
                )
            }
        }
    }
}

/** Settings-style section: uppercase accent label + one rounded card with compact rows. */
@Composable
private fun StatsSection(
    title: String,
    items: List<StatItem>,
    covers: Map<StatItem, Song?>,
    unit: String?,
    badgeIcon: ImageVector,
    refreshing: Boolean = false,
    showRank: Boolean = true,
    letterFallback: Boolean = false,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(false) }
    val shown = if (expanded) items else items.take(COLLAPSED_ROWS)
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "moreChevron"
    )
    // While new numbers are loading, the old list softly dims instead of jumping
    val dim by animateFloatAsState(if (refreshing) 0.45f else 1f, tween(250), label = "sectionDim")
    val maxCount = remember(items) { items.maxOfOrNull { it.count } ?: 0 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = dim },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CascadeText(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp)
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapeOf(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .animateContentSize(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
                    .padding(vertical = 4.dp)
            ) {
                shown.forEachIndexed { index, item ->
                    StatRow(
                        rank = if (showRank) index + 1 else null,
                        item = item,
                        art = covers[item],
                        unit = unit,
                        badgeIcon = badgeIcon,
                        letterFallback = letterFallback,
                        fraction = if (maxCount > 0) item.count.toFloat() / maxCount else 0f,
                        // Rows reveal one after another; ones revealed by "Show all" start their own wave
                        staggerIndex = if (index < COLLAPSED_ROWS) index else index - COLLAPSED_ROWS
                    )
                }
                if (items.size > COLLAPSED_ROWS) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClickable(shape = shapeOf(20.dp)) { expanded = !expanded }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (expanded) "Show less" else "Show all ${items.size}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp).rotate(chevron)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(
    rank: Int?,
    item: StatItem,
    art: Song?,
    unit: String?,
    badgeIcon: ImageVector,
    letterFallback: Boolean,
    fraction: Float,
    staggerIndex: Int,
) {
    // Entrance: slides up and fades in with a stagger; replays when the row's data changes
    val live = liveAnimationsEnabled()
    var shownIn by remember(item) { mutableStateOf(!live) }
    LaunchedEffect(item) {
        if (live) delay(staggerIndex * 55L)
        shownIn = true
    }
    val appear by animateFloatAsState(
        targetValue = if (shownIn) 1f else 0f,
        animationSpec = if (live) spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow) else snap(),
        label = "rowAppear"
    )
    // The pill "pops", the bar grows to its share
    val pill by animateFloatAsState(
        targetValue = if (shownIn) 1f else 0f,
        animationSpec = if (live) spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium) else snap(),
        label = "rowPill"
    )
    val bar by animateFloatAsState(
        targetValue = if (shownIn) fraction else 0f,
        animationSpec = if (live) tween(durationMillis = 800, delayMillis = 120, easing = FastOutSlowInEasing) else snap(),
        label = "rowBar"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.coerceIn(0f, 1f)
                translationY = (1f - appear) * 22f
            }
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank != null) {
            Text(
                text = rank.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (rank == 1) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(22.dp)
            )
        }
        Box(
            modifier = Modifier.graphicsLayer {
                val s = 0.7f + 0.3f * appear
                scaleX = s
                scaleY = s
            }
        ) {
            RowCover(item, art, badgeIcon, letterFallback)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            CascadeText(
                text = item.title,
                delayMillis = staggerIndex * 55,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.marqueeEdgeFade(),
                softWrap = false
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.marqueeEdgeFade(),
                    softWrap = false
                )
            }
            if (unit != null && item.count > 0) {
                Box(
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(bar.coerceIn(0.03f, 1f))
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
        if (unit != null && item.count > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier.graphicsLayer {
                    scaleX = pill
                    scaleY = pill
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            ) {
                Text(
                    text = "${item.count} $unit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/** 40 dp cover; without art it becomes an expressive badge, like the icons in Settings. */
@Composable
private fun RowCover(item: StatItem, song: Song?, badgeIcon: ImageVector, letterFallback: Boolean) {
    val art = rememberSongArt(song)
    if (art != null) {
        Image(
            bitmap = art.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(coverShapeOf(10.dp))
        )
    } else {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(expressiveBadgeShape(item.title.hashCode()))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (letterFallback) {
                Text(
                    text = item.title.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

private fun formatListened(ms: Long): String {
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
