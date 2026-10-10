package com.reikoonya.musikoo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.ui.components.SplitBottomPills
import com.reikoonya.musikoo.util.MusicPlayerManager
import com.reikoonya.musikoo.util.formatTime

@Composable
fun PlayerScreen(
    songs: List<Song>,
    initialSongIndex: Int,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current

    LaunchedEffect(initialSongIndex, songs) {
        MusicPlayerManager.initPlayer(context)
        if (MusicPlayerManager.exoPlayer?.isPlaying != true || MusicPlayerManager.currentSongIndex != initialSongIndex) {
            MusicPlayerManager.playSong(songs, initialSongIndex)
        }
    }

    var menuExpanded by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }

    val currentSong = if (MusicPlayerManager.songs.isNotEmpty() && MusicPlayerManager.currentSongIndex in MusicPlayerManager.songs.indices) {
        MusicPlayerManager.songs[MusicPlayerManager.currentSongIndex]
    } else if (songs.isNotEmpty()) {
        songs[initialSongIndex.coerceIn(0, songs.size - 1)]
    } else null

    if (showPropertiesDialog && currentSong != null) {
        AlertDialog(
            onDismissRequest = { showPropertiesDialog = false },
            shape = RoundedCornerShape(36.dp),
            title = { Text("Свойства трека", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PropertyRow("Название:", currentSong.title)
                    PropertyRow("Исполнитель:", currentSong.artist)
                    PropertyRow("Альбом:", currentSong.album)
                    PropertyRow("Формат:", currentSong.mimeType)
                    PropertyRow("Битрейт:", currentSong.bitrate)
                    PropertyRow("Длительность:", formatTime(currentSong.duration))
                    PropertyRow("Размер:", "${currentSong.size / (1024 * 1024)} MB")
                }
            },
            confirmButton = {
                TextButton(onClick = { showPropertiesDialog = false }) { Text("OK", fontWeight = FontWeight.Bold) }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 25f) {
                        onBackClick()
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header Row with Mini-Player styled Pill Collapse Button & Drag Handle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini-player styled collapse pill button
                Surface(
                    modifier = Modifier
                        .size(44.dp)
                        .clickable { onBackClick() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    tonalElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Свернуть",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Drag Indicator Handle
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = currentSong?.album ?: "Неизвестный альбом",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }

                Box {
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { menuExpanded = true },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        tonalElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Меню",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Свойства") },
                            onClick = { menuExpanded = false; showPropertiesDialog = true },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Hyper-rounded M3 Expressive Album Art Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(48.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = currentSong?.albumArt,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "AlbumArtCrossfade"
                ) { artBitmap ->
                    if (artBitmap != null) {
                        Image(
                            bitmap = artBitmap.asImageBitmap(),
                            contentDescription = "Обложка трека",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(96.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title and Artist Info
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(
                    text = currentSong?.title ?: "Трек не найден",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentSong?.artist ?: "Исполнитель",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Slider & Timers
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = MusicPlayerManager.currentPosition,
                    onValueChange = { newValue ->
                        MusicPlayerManager.seekTo(newValue.toLong())
                    },
                    valueRange = 0f..MusicPlayerManager.trackDuration.coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(MusicPlayerManager.currentPosition.toLong()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTime(MusicPlayerManager.trackDuration.toLong()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expressive Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    MusicPlayerManager.exoPlayer?.let { player ->
                        val newPos = (player.currentPosition - 10000).coerceAtLeast(0)
                        MusicPlayerManager.seekTo(newPos)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.FastRewind,
                        contentDescription = "-10 сек",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = {
                    MusicPlayerManager.skipPrevious()
                }) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Назад",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                FilledIconButton(
                    onClick = {
                        MusicPlayerManager.togglePlayPause()
                    },
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (MusicPlayerManager.isPlayingState) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Плей/Пауза",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                IconButton(onClick = {
                    MusicPlayerManager.skipNext()
                }) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Вперед",
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = {
                    MusicPlayerManager.exoPlayer?.let { player ->
                        val newPos = (player.currentPosition + 10000).coerceAtMost(player.duration)
                        MusicPlayerManager.seekTo(newPos)
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.FastForward,
                        contentDescription = "+10 сек",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            SplitBottomPills()
        }
    }
}

@Composable
fun PropertyRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
