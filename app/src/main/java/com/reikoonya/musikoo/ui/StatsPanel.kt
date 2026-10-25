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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

/**
 * Statistics screen: a hero card with the totals, then horizontal rows of covers
 * for top artists, albums, tracks and composers. Switch between this device and Last.fm.
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
    // Covers are looked up once per snapshot, not once per card on every recomposition
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
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { SourcePicker(source) { source = it } }
        item { RangePicker(range) { range = it } }
        item {
            HeroCard(
                source = source,
                range = range,
                snapshot = snapshot,
                loading = loading,
                error = error,
                user = user,
            )
        }
        snapshot?.let { s ->
            if (s.artists.isNotEmpty()) {
                item {
                    HorizontalSection("Top artists", s.artists) { item ->
                        ArtistCard(item, covers[item])
                    }
                }
            }
            if (s.albums.isNotEmpty()) {
                item {
                    HorizontalSection("Top albums", s.albums) { item ->
                        AlbumCard(item, covers[item])
                    }
                }
            }
            if (s.tracks.isNotEmpty()) {
                item {
                    HorizontalSection("Top tracks", s.tracks) { item ->
                        TrackCard(item, covers[item])
                    }
                }
            }
            if (s.composers.isNotEmpty()) {
                item {
                    HorizontalSection("Composers", s.composers) { item ->
                        ComposerCard(item, covers[item])
                    }
                }
            }
            if (s.recent.isNotEmpty()) {
                item {
                    HorizontalSection("Recently played", s.recent) { item ->
                        TrackCard(item, covers[item])
                    }
                }
            }
        }
    }
}

/** Finds a cover in the library for a statistics row. */
private fun artFor(item: StatItem, songs: List<Song>): Bitmap? {
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
    return match?.albumArt
}

@Composable
private fun SourcePicker(source: StatsSource, onChange: (StatsSource) -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = glassStroke(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatsSource.entries.forEach { option ->
                val isSel = source == option
                val bg by animateColorAsState(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent, label = "srcBg")
                val fg by animateColorAsState(if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, label = "srcFg")
                BounceSurface(
                    onClick = { onChange(option) },
                    shape = CircleShape,
                    color = bg
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                        color = fg,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RangePicker(range: StatsRange, onChange: (StatsRange) -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = glassStroke(),
        tonalElevation = 2.dp
    ) {
        LazyRow(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(StatsRange.entries) { option ->
                val isSel = range == option
                val bg by animateColorAsState(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent, label = "rngBg")
                val fg by animateColorAsState(if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, label = "rngFg")
                BounceSurface(
                    onClick = { onChange(option) },
                    shape = CircleShape,
                    color = bg
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                        color = fg,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    source: StatsSource,
    range: StatsRange,
    snapshot: StatsSnapshot?,
    loading: Boolean,
    error: String?,
    user: String?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeOf(36.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            when {
                loading -> AppLoadingIndicator(size = 40.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                error != null -> Text(
                    text = error,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                snapshot != null && source == StatsSource.LOCAL -> {
                    Text(
                        text = formatListened(snapshot.listenedMs),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${snapshot.totalPlays} plays · ${range.label}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                snapshot != null -> {
                    Text(
                        text = "${snapshot.totalPlays}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "scrobbles · ${range.label}" + (user?.let { " · @$it" } ?: ""),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> HorizontalSection(
    title: String,
    items: List<T>,
    itemContent: @Composable (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(items) { item -> itemContent(item) }
        }
    }
}

@Composable
private fun CoverBox(art: Bitmap?, size: androidx.compose.ui.unit.Dp, shape: androidx.compose.ui.graphics.Shape, fallback: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
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
            fallback()
        }
    }
}

@Composable
private fun ArtistCard(item: StatItem, art: Bitmap?) {
    Column(modifier = Modifier.width(128.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CoverBox(art, 128.dp, coverShapeOf(20.dp)) {
            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        if (item.count > 0) {
            Text("${item.count} plays", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AlbumCard(item: StatItem, art: Bitmap?) {
    Column(modifier = Modifier.width(150.dp)) {
        CoverBox(art, 150.dp, coverShapeOf(20.dp)) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(item.subtitle, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TrackCard(item: StatItem, art: Bitmap?) {
    Column(modifier = Modifier.width(140.dp)) {
        CoverBox(art, 140.dp, coverShapeOf(20.dp)) {
            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            text = if (item.count > 0) "${item.subtitle} · ${item.count}" else item.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ComposerCard(item: StatItem, art: Bitmap?) {
    Column(modifier = Modifier.width(118.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CoverBox(art, 104.dp, coverShapeOf(16.dp)) {
            Text(
                text = item.title.take(1).uppercase(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Text("${item.count} tracks", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatListened(ms: Long): String {
    val totalMinutes = ms / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "$hours h $minutes min" else "$minutes min"
}
