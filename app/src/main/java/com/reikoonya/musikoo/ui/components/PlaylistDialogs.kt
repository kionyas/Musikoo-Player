package com.reikoonya.musikoo.ui.components

import androidx.compose.material.icons.rounded.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.data.Playlist
import com.reikoonya.musikoo.data.PlaylistManager
import com.reikoonya.musikoo.model.Song

@Composable
fun PlaylistArt(
    playlist: Playlist,
    allSongs: List<Song>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customBitmap = remember(playlist.customCoverPath) {
        playlist.customCoverPath?.let { path ->
            try {
                BitmapFactory.decodeFile(path)
            } catch (_: Exception) {
                null
            }
        }
    }

    val playlistSongs = remember(playlist.songIds, allSongs) {
        val songMap = allSongs.associateBy { it.id }
        playlist.songIds.mapNotNull { songMap[it] }
    }

    // Covers of the first songs for the mosaic, loaded per song (not held in the song list)
    val arts = playlistSongs.take(4).mapNotNull { song -> key(song.id) { rememberSongArt(song) } }

    Surface(
        modifier = modifier,
        shape = shapeOf(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        when {
            customBitmap != null -> {
                Image(
                    bitmap = customBitmap.asImageBitmap(),
                    contentDescription = playlist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            arts.size >= 4 -> {
                // 2x2 Collage
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        Image(
                            bitmap = arts[0].asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                        Image(
                            bitmap = arts[1].asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Row(modifier = Modifier.weight(1f)) {
                        Image(
                            bitmap = arts[2].asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                        Image(
                            bitmap = arts[3].asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            arts.isNotEmpty() -> {
                Image(
                    bitmap = arts[0].asImageBitmap(),
                    contentDescription = playlist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Rounded.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun AddToPlaylistDialog(
    songIds: List<Long>,
    allSongs: List<Song>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    LaunchedEffect(Unit) { playlists = PlaylistManager.loadPlaylists(context) }
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                val newPl = PlaylistManager.createPlaylist(context, name, songIds)
                Toast.makeText(context, context.getString(R.string.added_to_playlist, newPl.name), Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = shapeOf(32.dp),
        title = { Text(stringResource(R.string.add_to_playlist), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = shapeOf(20.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.create_playlist), fontWeight = FontWeight.Bold)
                }

                if (playlists.isNotEmpty()) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(playlists) { playlist ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClickable(shape = shapeOf(20.dp)) {
                                        PlaylistManager.addSongsToPlaylist(context, playlist.id, songIds)
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.added_to_playlist, playlist.name),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onDismiss()
                                    },
                                shape = shapeOf(20.dp),
                                color = cardFill()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    PlaylistArt(
                                        playlist = playlist,
                                        allSongs = allSongs,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playlist.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            modifier = Modifier.marqueeEdgeFade(),
                                            softWrap = false
                                        )
                                        Text(
                                            text = stringResource(R.string.tracks_count, playlist.songIds.size),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = shapeOf(32.dp),
        title = { Text(stringResource(R.string.create_playlist), fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.playlist_name)) },
                singleLine = true,
                shape = shapeOf(20.dp),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim())
                    }
                },
                enabled = name.isNotBlank(),
                shape = CircleShape
            ) {
                Text(stringResource(R.string.create), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun EditPlaylistDialog(
    playlist: Playlist,
    allSongs: List<Song>,
    onDismiss: () -> Unit,
    onUpdated: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(playlist.name) }
    var currentSongIds by remember { mutableStateOf(playlist.songIds) }

    val playlistSongs = remember(currentSongIds, allSongs) {
        val songMap = allSongs.associateBy { it.id }
        currentSongIds.mapNotNull { songMap[it] }
    }

    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = java.io.File(context.filesDir, "playlist_cover_${playlist.id}.jpg")
                file.outputStream().use { output ->
                    inputStream?.copyTo(output)
                }
                PlaylistManager.setCustomCover(context, playlist.id, file.absolutePath)
                onUpdated()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = shapeOf(32.dp),
        title = { Text(stringResource(R.string.edit_playlist), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.playlist_name)) },
                    singleLine = true,
                    shape = shapeOf(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { pickCover.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = shapeOf(20.dp)
                    ) {
                        Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.replace_cover))
                    }

                    if (playlist.customCoverPath != null) {
                        TextButton(
                            onClick = {
                                PlaylistManager.setCustomCover(context, playlist.id, null)
                                onUpdated()
                            }
                        ) {
                            Text(stringResource(R.string.remove_cover))
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.tracks_count, playlistSongs.size),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(playlistSongs) { index, song ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = shapeOf(16.dp),
                            color = cardFill()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        modifier = Modifier.marqueeEdgeFade(),
                                        softWrap = false
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.marqueeEdgeFade(),
                                        softWrap = false
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        val updatedIds = currentSongIds.toMutableList()
                                        updatedIds.removeAt(index)
                                        currentSongIds = updatedIds
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        PlaylistManager.renamePlaylist(context, playlist.id, name.trim())
                        PlaylistManager.updatePlaylistSongs(context, playlist.id, currentSongIds)
                        onUpdated()
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                shape = CircleShape
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun MarkdownGuideDialog(
    onDismiss: () -> Unit
) {
    val guideTitle = stringResource(R.string.guide_title)
    val guideText = stringResource(R.string.guide_text)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = shapeOf(32.dp),
        title = { Text(guideTitle, fontWeight = FontWeight.Bold) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = guideText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, shape = CircleShape) {
                Text(stringResource(R.string.ok), fontWeight = FontWeight.Bold)
            }
        }
    )
}
