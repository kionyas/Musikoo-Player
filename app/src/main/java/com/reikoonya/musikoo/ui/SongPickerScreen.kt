package com.reikoonya.musikoo.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.data.Playlist
import com.reikoonya.musikoo.data.PlaylistManager
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.ui.components.ArtBackdrop
import com.reikoonya.musikoo.ui.components.ScreenTopBar
import com.reikoonya.musikoo.ui.components.SearchNoResults
import com.reikoonya.musikoo.ui.components.TopFadeScrim
import com.reikoonya.musikoo.ui.components.topFadeBlur
import com.reikoonya.musikoo.ui.components.bounceClickable
import com.reikoonya.musikoo.ui.components.cardFill
import com.reikoonya.musikoo.ui.components.matchesQuery
import com.reikoonya.musikoo.ui.components.nowPlayingArt
import com.reikoonya.musikoo.ui.components.rememberScreenSearchState
import com.reikoonya.musikoo.ui.components.rememberSongArt
import com.reikoonya.musikoo.ui.components.shapeOf
import com.reikoonya.musikoo.ui.components.marqueeEdgeFade

/**
 * Экран выбора песен для плейлиста: поиск, отметки, «выбрать все найденные» и одна кнопка «Add».
 * Песни, которые уже лежат в плейлисте, в списке не показываются.
 */
@Composable
fun SongPickerScreen(
    playlistId: String,
    allSongs: List<Song>,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    var playlist by remember { mutableStateOf<Playlist?>(null) }
    LaunchedEffect(playlistId) {
        playlist = PlaylistManager.loadPlaylists(context).firstOrNull { it.id == playlistId }
    }
    val existing = remember(playlist) { playlist?.songIds?.toSet().orEmpty() }

    val search = rememberScreenSearchState()
    // Set с порядком вставки: песни попадут в плейлист в том порядке, в котором их отметили
    var selected by remember { mutableStateOf(emptySet<Long>()) }

    val candidates = remember(allSongs, existing, search.query) {
        allSongs
            .filter { it.id !in existing && matchesQuery(search.query, it.title, it.artist, it.album) }
            .sortedBy { it.title.lowercase() }
    }
    val allShownSelected = candidates.isNotEmpty() && candidates.all { it.id in selected }

    Box(modifier = Modifier.fillMaxSize()) {
        ArtBackdrop(art = nowPlayingArt(), topScrim = 0.55f, midScrim = 0.82f)

        LazyColumn(
            modifier = Modifier.fillMaxSize().topFadeBlur(),
            contentPadding = PaddingValues(top = 92.dp, bottom = 120.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (candidates.isEmpty()) {
                item {
                    if (search.filtering) SearchNoResults(search.query)
                    else Text(
                        text = "Every song is already in this playlist.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
            if (candidates.isNotEmpty()) {
                item(key = "select_all") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selected.size} selected",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        TextButton(
                            onClick = {
                                val ids = candidates.map { it.id }
                                selected = if (allShownSelected) selected - ids.toSet() else selected + ids
                            }
                        ) {
                            Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (allShownSelected) "Clear shown" else "Select all shown")
                        }
                    }
                }
            }
            items(candidates, key = { it.id }) { song ->
                val checked = song.id in selected
                val art = rememberSongArt(song)
                val rowColor by animateColorAsState(
                    targetValue = if (checked) MaterialTheme.colorScheme.primaryContainer else cardFill(),
                    label = "pickerRow"
                )
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .bounceClickable(shape = shapeOf(20.dp)) {
                            selected = if (checked) selected - song.id else selected + song.id
                        },
                    shape = shapeOf(20.dp),
                    color = rowColor
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(shapeOf(12.dp)),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            if (art != null) {
                                Image(
                                    bitmap = art.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
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
                        // Само нажатие обрабатывает строка, чекбокс только показывает состояние
                        Checkbox(checked = checked, onCheckedChange = null)
                    }
                }
            }
        }

        TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter), blurred = true)

        ScreenTopBar(
            title = playlist?.name?.let { "Add to $it" } ?: "Add songs",
            onBack = onBackClick,
            search = search,
            searchHint = "Search songs",
            modifier = Modifier.align(Alignment.TopCenter)
        )

        AnimatedVisibility(
            visible = selected.isNotEmpty(),
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it }
        ) {
            Button(
                onClick = {
                    keyboard?.hide()
                    PlaylistManager.addSongsToPlaylist(context, playlistId, selected.toList())
                    Toast.makeText(
                        context,
                        "Added ${selected.size} ${if (selected.size == 1) "song" else "songs"}",
                        Toast.LENGTH_SHORT
                    ).show()
                    onBackClick()
                },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Add ${selected.size} ${if (selected.size == 1) "song" else "songs"}",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
