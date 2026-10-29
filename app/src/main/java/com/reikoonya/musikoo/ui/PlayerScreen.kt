package com.reikoonya.musikoo.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.widthIn
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import com.reikoonya.musikoo.ui.components.staggeredEntrance
import com.reikoonya.musikoo.ui.components.TrackTextSwap
import com.reikoonya.musikoo.ui.components.TrackLabel
import com.reikoonya.musikoo.ui.components.MagicArt
import com.reikoonya.musikoo.ui.components.CascadeText
import com.reikoonya.musikoo.ui.components.marqueeEdgeFade
import com.reikoonya.musikoo.ui.components.shapeOf
import com.reikoonya.musikoo.ui.components.expressiveBadgeShape
import com.reikoonya.musikoo.ui.components.ExpressiveShapes
import com.reikoonya.musikoo.ui.theme.ShapeFamily
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.foundation.clickable
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.SizeTransform
import androidx.compose.ui.unit.IntSize
import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.data.PlaylistManager
import com.reikoonya.musikoo.lyrics.LyricsRepository
import com.reikoonya.musikoo.lyrics.LyricsResult
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.ui.components.AddToPlaylistDialog
import com.reikoonya.musikoo.ui.components.ArtBackdrop
import com.reikoonya.musikoo.ui.components.GlassIconButton
import com.reikoonya.musikoo.ui.components.PlayerSlider
import com.reikoonya.musikoo.ui.components.bounceClickable
import com.reikoonya.musikoo.util.DeleteResult
import com.reikoonya.musikoo.util.MusicPlayerManager
import com.reikoonya.musikoo.util.ShareUtils
import com.reikoonya.musikoo.util.SongDeleter
import com.reikoonya.musikoo.util.formatTime
import com.reikoonya.musikoo.ui.components.PlayerProgress
import com.reikoonya.musikoo.ui.components.ScrubState
import com.reikoonya.musikoo.ui.components.scrubReact
import com.reikoonya.musikoo.ui.components.rememberSongArt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlayerScreen(
    onAlbumClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    songs: List<Song>,
    initialSongIndex: Int,
    onQueueClick: (() -> Unit)? = null,
    onSongDeleted: (Song) -> Unit = {},
    onSongsEdited: (List<Song>) -> Unit = {},
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current

    val strDeleted = stringResource(R.string.track_deleted)
    val strDeleteFailed = stringResource(R.string.delete_failed)
    val strSelectTrack = stringResource(R.string.select_track)
    val strUnknownArtist = stringResource(R.string.unknown_artist)

    LaunchedEffect(initialSongIndex) {
        MusicPlayerManager.initPlayer(context)
        if (MusicPlayerManager.songs.isEmpty() && songs.isNotEmpty()) {
            MusicPlayerManager.playSong(songs, initialSongIndex.coerceIn(songs.indices))
        }
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var showProperties by remember { mutableStateOf(false) }
    var showTagEditor by remember { mutableStateOf(false) }
    var showAddToPlaylist by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingNeedsRetry by rememberSaveable { mutableStateOf(false) }

    val prefs = remember(context) { context.getSharedPreferences("musikoo_prefs", Context.MODE_PRIVATE) }
    var showLyrics by rememberSaveable {
        mutableStateOf(prefs.getBoolean("show_lyrics_player", false))
    }
    var isScrubbing by remember { mutableStateOf(false) }
    val scrub = remember { ScrubState() }

    val scrubbingScale by animateFloatAsState(
        targetValue = if (isScrubbing) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scrubbingScale"
    )
    val scrubbingCorner by animateDpAsState(
        targetValue = if (isScrubbing) 24.dp else 16.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scrubbingCorner"
    )

    val coverScope = rememberCoroutineScope()
    val coverSqueeze = remember { Animatable(1f) }
    var coverWidthPx by remember { mutableFloatStateOf(1f) }
    var doubleTapFeedback by remember { mutableStateOf<Boolean?>(null) }
    var feedbackTrigger by remember { mutableLongStateOf(0L) }
    var coverDrag by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }

    val currentSong = MusicPlayerManager.songs.getOrNull(MusicPlayerManager.currentSongIndex)
        ?: songs.getOrNull(initialSongIndex.coerceIn(0, (songs.size - 1).coerceAtLeast(0)))

    val favIds by PlaylistManager.favorites.collectAsState()
    val isFav = currentSong?.let { it.id in favIds } == true
    val currentArt = rememberSongArt(currentSong)
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val isPlaying = MusicPlayerManager.isPlayingState

    // Track change: the cover gets a little push in the direction of travel and a springy squeeze
    val coverKick = remember { Animatable(0f) }
    var firstSongSeen by remember { mutableStateOf(false) }
    LaunchedEffect(currentSong?.id) {
        if (!firstSongSeen) {
            firstSongSeen = true
            return@LaunchedEffect
        }
        val dir = if (MusicPlayerManager.lastTransitionForward) 1f else -1f
        coverKick.snapTo(90f * dir)
        launch {
            coverSqueeze.animateTo(0.93f, tween(110))
            coverSqueeze.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
        }
        coverKick.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow))
    }
    // While playing, the cover slowly "breathes"
    val breathTransition = rememberInfiniteTransition(label = "coverBreath")
    val breathe by breathTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    val breatheAmount by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(),
        label = "breatheAmount"
    )

    val lyricsState by produceState<Pair<Boolean, LyricsResult?>>(initialValue = false to null, currentSong?.id) {
        value = false to null
        val song = currentSong ?: return@produceState
        val result = withContext(Dispatchers.IO) { LyricsRepository.load(context, song) }
        value = true to result
    }

    LaunchedEffect(feedbackTrigger) {
        if (feedbackTrigger > 0L) {
            kotlinx.coroutines.delay(650)
            doubleTapFeedback = null
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val id = pendingDeleteId ?: return@rememberLauncherForActivityResult
        pendingDeleteId = null
        val song = MusicPlayerManager.songs.firstOrNull { it.id == id } ?: songs.firstOrNull { it.id == id } ?: return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            if (pendingNeedsRetry) SongDeleter.deleteDirect(context, song)
            MusicPlayerManager.removeSong(song)
            onSongDeleted(song)
            Toast.makeText(context, strDeleted, Toast.LENGTH_SHORT).show()
            if (MusicPlayerManager.songs.isEmpty()) onBackClick()
        }
    }

    fun startDelete(song: Song) {
        when (val r = SongDeleter.requestDelete(context, song)) {
            is DeleteResult.Deleted -> {
                MusicPlayerManager.removeSong(song)
                onSongDeleted(song)
                Toast.makeText(context, strDeleted, Toast.LENGTH_SHORT).show()
                if (MusicPlayerManager.songs.isEmpty()) onBackClick()
            }
            is DeleteResult.NeedsConfirmation -> {
                pendingDeleteId = song.id
                pendingNeedsRetry = r.retryAfterApproval
                deleteLauncher.launch(IntentSenderRequest.Builder(r.intentSender).build())
            }
            is DeleteResult.Failed -> Toast.makeText(context, strDeleteFailed, Toast.LENGTH_LONG).show()
        }
    }

    if (showDeleteDialog && currentSong != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = shapeOf(32.dp),
            title = { Text(stringResource(R.string.delete_track), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_confirm, currentSong.title)) },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; startDelete(currentSong) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }

    if (showTagEditor && currentSong != null) {
        TagEditorDialog(songs = listOf(currentSong), onDismiss = { showTagEditor = false }, onSaved = onSongsEdited)
    }

    if (showAddToPlaylist && currentSong != null) {
        AddToPlaylistDialog(songIds = listOf(currentSong.id), allSongs = songs, onDismiss = { showAddToPlaylist = false })
    }

    if (showProperties && currentSong != null) {
        AlertDialog(
            onDismissRequest = { showProperties = false },
            shape = shapeOf(28.dp),
            title = { Text(stringResource(R.string.track_properties), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PropertyRow(stringResource(R.string.prop_title), currentSong.title)
                    PropertyRow(stringResource(R.string.prop_artist), currentSong.artist)
                    PropertyRow(stringResource(R.string.prop_album), currentSong.album)
                    PropertyRow(stringResource(R.string.prop_format), currentSong.mimeType)
                    PropertyRow(stringResource(R.string.prop_bitrate), currentSong.bitrate)
                    PropertyRow(stringResource(R.string.prop_duration), formatTime(currentSong.duration))
                    PropertyRow(stringResource(R.string.prop_size), "${currentSong.size / (1024 * 1024)} MB")
                }
            },
            confirmButton = { TextButton(onClick = { showProperties = false }) { Text(stringResource(R.string.ok)) } }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 25f) onBackClick()
                }
            }
    ) {
        ArtBackdrop(art = currentArt)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Верхний бар
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.collapse),
                    onClick = onBackClick,
                    width = 42.dp,
                    height = 42.dp,
                    iconSize = 24.dp,
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                        modifier = Modifier
                            .widthIn(min = 96.dp)
                            .bounceClickable(shape = CircleShape) {
                                currentSong?.album?.takeIf { it.isNotBlank() }?.let { album ->
                                    onAlbumClick(album)
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(34.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            )
                            // The pill grows and shrinks with the album name, which slides in on track change
                            AnimatedContent(
                                targetState = currentSong?.album?.takeIf { it.isNotBlank() } ?: "",
                                transitionSpec = {
                                    (slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it } +
                                            fadeIn(tween(280, delayMillis = 70))) togetherWith
                                            (slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { -it } +
                                                    fadeOut(tween(140))) using
                                            SizeTransform(clip = true) { _, _ ->
                                                spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                                            }
                                },
                                label = "playerPillAlbum"
                            ) { album ->
                                Text(
                                    text = album,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.marqueeEdgeFade()
                                )
                            }
                        }
                    }
                }

                Box {
                    GlassIconButton(
                        icon = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.menu),
                        onClick = { menuExpanded = true },
                        width = 42.dp,
                        height = 42.dp,
                        iconSize = 22.dp,
                        shape = CircleShape,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        shape = shapeOf(24.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isFav) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites)) },
                            onClick = {
                                menuExpanded = false
                                currentSong?.let { PlaylistManager.toggleFavorite(context, it.id) }
                            },
                            leadingIcon = { Icon(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.share)) },
                            onClick = { menuExpanded = false; currentSong?.let { ShareUtils.shareSongs(context, listOf(it)) } },
                            leadingIcon = { Icon(Icons.Rounded.Share, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_to_playlist)) },
                            onClick = { menuExpanded = false; showAddToPlaylist = true },
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.properties)) },
                            onClick = { menuExpanded = false; showProperties = true },
                            leadingIcon = { Icon(Icons.Rounded.Info, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_tags)) },
                            onClick = { menuExpanded = false; showTagEditor = true },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_from_device), color = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; showDeleteDialog = true },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            // 2. Центральная область
            AnimatedContent(
                targetState = showLyrics,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .staggeredEntrance(0),
                transitionSpec = {
                    val easing = FastOutSlowInEasing
                    val sizeSpec = tween<IntSize>(durationMillis = 480, easing = easing)
                    if (targetState) {
                        // Lyrics take over: they rise gently and appear just after the cover starts leaving
                        (fadeIn(animationSpec = tween(420, delayMillis = 120, easing = easing)) +
                                slideInVertically(animationSpec = tween(480, easing = easing)) { it / 10 })
                            .togetherWith(
                                fadeOut(animationSpec = tween(220, easing = easing)) +
                                        scaleOut(targetScale = 0.92f, animationSpec = tween(320, easing = easing))
                            )
                    } else {
                        // Back to the cover: the cover grows back in while the lyrics sink and fade
                        (fadeIn(animationSpec = tween(420, delayMillis = 120, easing = easing)) +
                                scaleIn(initialScale = 0.92f, animationSpec = tween(480, easing = easing)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(200, easing = easing)) +
                                        slideOutVertically(animationSpec = tween(360, easing = easing)) { it / 10 }
                            )
                    }.using(SizeTransform(clip = false) { _, _ -> sizeSpec })
                },
                label = "PlayerMorph"
            ) { isLyrics ->
                if (isLyrics) {
                    Column(modifier = Modifier.fillMaxSize().padding(top = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(52.dp)
                                    .graphicsLayer {
                                        scaleX = scrubbingScale
                                        scaleY = scrubbingScale
                                    }
                                    .scrubReact(scrub, shift = 6.dp, tiltDegrees = 8f)
                                    .clip(RoundedCornerShape(scrubbingCorner)),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                MagicArt(art = currentArt, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = currentSong?.title ?: strSelectTrack,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.marqueeEdgeFade()
                                )
                                Text(
                                    text = currentSong?.artist ?: strUnknownArtist,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .marqueeEdgeFade()
                                        .then(
                                            if (currentSong?.artist != null) {
                                                Modifier.clip(RoundedCornerShape(10.dp)).clickable { onArtistClick(currentSong.artist) }
                                            } else Modifier
                                        )
                                )
                            }
                        }
                        Box(Modifier.fillMaxWidth().weight(1f)) {
                            LyricsPanel(lyrics = lyricsState.second, loaded = lyricsState.first, visible = true, accent = accent, modifier = Modifier.fillMaxSize())
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(0.85f)
                                    .aspectRatio(1f)
                                    .graphicsLayer {
                                        val pulse = 1f + 0.014f * breathe * breatheAmount
                                        scaleX = coverSqueeze.value * scrubbingScale * pulse
                                        scaleY = coverSqueeze.value * scrubbingScale * pulse
                                        translationX = coverDrag * 0.35f + coverKick.value
                                    }
                                    .scrubReact(scrub, shift = 10.dp, tiltDegrees = 5f)
                                    .onSizeChanged { coverWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = { offset ->
                                                val isForward = offset.x >= coverWidthPx / 2f
                                                val player = MusicPlayerManager.controller
                                                if (player != null) {
                                                    val duration = player.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                                                    val target = (player.currentPosition + if (isForward) 10_000L else -10_000L).coerceIn(0L, duration)
                                                    MusicPlayerManager.seekTo(target)
                                                }
                                                doubleTapFeedback = isForward
                                                feedbackTrigger = System.currentTimeMillis()
                                                coverScope.launch {
                                                    coverSqueeze.animateTo(0.88f, tween(90))
                                                    coverSqueeze.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium))
                                                }
                                            }
                                        )
                                    }
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragStart = { coverDrag = 0f },
                                            onHorizontalDrag = { change, amount ->
                                                change.consume()
                                                coverDrag += amount
                                            },
                                            onDragEnd = {
                                                if (coverDrag < -swipeThresholdPx) MusicPlayerManager.skipNext()
                                                else if (coverDrag > swipeThresholdPx) MusicPlayerManager.skipPrevious()
                                                coverDrag = 0f
                                            },
                                            onDragCancel = { coverDrag = 0f }
                                        )
                                    }
                                    .clip(RoundedCornerShape(scrubbingCorner))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                MagicArt(art = currentArt, modifier = Modifier.fillMaxSize()) {
                                    Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(20.dp),
                                    contentAlignment = if (doubleTapFeedback == true) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    if (doubleTapFeedback != null) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                                            tonalElevation = 6.dp
                                        ) {
                                            Text(
                                                text = if (doubleTapFeedback == true) "+10s" else "-10s",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = accent,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrackTextSwap(
                                label = TrackLabel(
                                    id = currentSong?.id,
                                    title = currentSong?.title ?: strSelectTrack,
                                    artist = currentSong?.artist ?: strUnknownArtist
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .scrubReact(scrub, shift = 16.dp, fade = 0.35f)
                            ) { l ->
                                Column(Modifier.fillMaxWidth()) {
                                    CascadeText(l.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.marqueeEdgeFade(), playOnce = false)
                                    CascadeText(
                                        text = l.artist,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        delayMillis = 120,
                                        playOnce = false,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .marqueeEdgeFade()
                                            .then(
                                                if (l.id != null) {
                                                    Modifier.clip(RoundedCornerShape(10.dp)).clickable { onArtistClick(l.artist) }
                                                } else Modifier
                                            )
                                    )
                                }
                            }
                            if (currentSong != null) {
                                val favShape = remember(com.reikoonya.musikoo.ui.components.ShapeStyle.family) {
                                    if (com.reikoonya.musikoo.ui.components.ShapeStyle.family == ShapeFamily.MD3) CircleShape else ExpressiveShapes.sunny.toShape()
                                }
                                Surface(
                                    modifier = Modifier
                                        .scrubReact(scrub, shift = 8.dp, shrink = 0.12f, fade = 0.4f)
                                        .size(52.dp)
                                        .bounceClickable(shape = favShape) { PlaylistManager.toggleFavorite(context, currentSong.id) },
                                    shape = favShape,
                                    color = if (isFav) accent else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    tonalElevation = 3.dp
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            contentDescription = "Favorite",
                                            tint = if (isFav) onAccent else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Слайдер
            Column(Modifier.staggeredEntrance(1)) {
                Spacer(Modifier.height(4.dp))
                PlayerProgress(
                    isPlaying = isPlaying,
                    accent = accent,
                    onScrubbingChanged = { isScrubbing = it },
                    scrubState = scrub
                )
            }

            // 4. Транспортный ряд
            Spacer(Modifier.height(8.dp))
            Box(Modifier.staggeredEntrance(2).scrubReact(scrub, shift = 6.dp, shrink = 0.05f, fade = 0.35f)) {
                PlayerTransportRow(
                    isPlaying = isPlaying,
                    accent = accent,
                    onAccent = onAccent,
                    onPlayPause = {
                        if (MusicPlayerManager.controller == null && songs.isNotEmpty()) {
                            MusicPlayerManager.playSong(songs, initialSongIndex.coerceIn(songs.indices))
                        } else {
                            MusicPlayerManager.togglePlayPause()
                        }
                    }
                )
            }

            // 5. Нижние капсулы
            Spacer(Modifier.height(10.dp))
            Box(Modifier.staggeredEntrance(3).scrubReact(scrub, shift = 4.dp, fade = 0.25f)) {
                PlayerPills(
                    accent = accent,
                    onAccent = onAccent,
                    lyricsOn = showLyrics,
                    onToggleLyrics = {
                        val next = !showLyrics
                        showLyrics = next
                        prefs.edit().putBoolean("show_lyrics_player", next).apply()
                    },
                    onQueueClick = onQueueClick
                )
            }
        }
    }
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}