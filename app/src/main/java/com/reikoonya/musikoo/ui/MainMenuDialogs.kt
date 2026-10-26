package com.reikoonya.musikoo.ui

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

/** Search UI state, kept by MainActivity so it survives switching between tabs. */

/** Состояние открытых диалогов главного экрана. Создаётся в MainMenuScreen через remember. */
class MainMenuDialogState {
    var actionsAlbum by mutableStateOf<String?>(null)
    var actionsPlaylist by mutableStateOf<Playlist?>(null)
    var selectedPlaylistForEdit by mutableStateOf<Playlist?>(null)
    var editSongs by mutableStateOf<List<Song>?>(null)
    var showGuide by mutableStateOf(false)
}

@Composable
fun MainMenuDialogs(
    state: MainMenuDialogState,
    songs: List<Song>,
    albums: Map<String, List<Song>>,
    pinnedAlbumNames: Set<String>,
    playlists: List<Playlist>,
    onPinnedAlbumsChanged: () -> Unit,
    onPlaylistsChanged: () -> Unit,
    onSongsEdited: (List<Song>) -> Unit,
) {
    val context = LocalContext.current

    state.actionsAlbum?.let { name ->
        val isPinned = pinnedAlbumNames.contains(name)
        ActionsDialog(
            title = name,
            actions = listOf(
                MenuAction(
                    icon = Icons.Rounded.PushPin,
                    label = if (isPinned) stringResource(R.string.unpin_from_top) else stringResource(
                        R.string.pin_to_top
                    )
                ) {
                    PlaylistManager.togglePinAlbum(context, name)
                    onPinnedAlbumsChanged()
                    state.actionsAlbum = null
                },
                MenuAction(Icons.Rounded.Share, stringResource(R.string.share)) {
                    albums[name]?.let { songList -> ShareUtils.shareSongs(context, songList, name) }
                    state.actionsAlbum = null
                },
                MenuAction(Icons.Rounded.Edit, stringResource(R.string.edit_tags)) {
                    state.editSongs = albums[name].orEmpty()
                    state.actionsAlbum = null
                }
            ),
            onDismiss = { state.actionsAlbum = null }
        )
    }

    state.actionsPlaylist?.let { playlist ->
        val isFav = playlist.id == PlaylistManager.FAVORITES_PLAYLIST_ID
        ActionsDialog(
            title = playlist.name,
            actions = listOfNotNull(
                MenuAction(
                    icon = Icons.Rounded.PushPin,
                    label = if (playlist.isPinned) stringResource(R.string.unpin_from_top) else stringResource(
                        R.string.pin_to_top
                    )
                ) {
                    PlaylistManager.togglePinPlaylist(context, playlist.id)
                    onPlaylistsChanged()
                    state.actionsPlaylist = null
                },
                MenuAction(Icons.Rounded.Share, stringResource(R.string.share)) {
                    val songMap = songs.associateBy { it.id }
                    val songsToShare = playlist.songIds.mapNotNull { songMap[it] }
                    ShareUtils.shareSongs(context, songsToShare, playlist.name)
                    state.actionsPlaylist = null
                },
                if (!isFav) {
                    MenuAction(Icons.Rounded.Edit, stringResource(R.string.edit_playlist)) {
                        state.selectedPlaylistForEdit = playlist
                        state.actionsPlaylist = null
                    }
                } else null,
                if (!isFav) {
                    MenuAction(Icons.Rounded.Delete, stringResource(R.string.delete_playlist)) {
                        PlaylistManager.deletePlaylist(context, playlist.id)
                        onPlaylistsChanged()
                        state.actionsPlaylist = null
                    }
                } else null
            ),
            onDismiss = { state.actionsPlaylist = null }
        )
    }

    state.selectedPlaylistForEdit?.let { playlist ->
        EditPlaylistDialog(
            playlist = playlist,
            allSongs = songs,
            onDismiss = { state.selectedPlaylistForEdit = null },
            onUpdated = { onPlaylistsChanged() }
        )
    }

    state.editSongs?.let { list ->
        TagEditorDialog(
            songs = list,
            onDismiss = { state.editSongs = null },
            onSaved = onSongsEdited
        )
    }

    if (state.showGuide) {
        MarkdownGuideDialog(onDismiss = { state.showGuide = false })
    }
}
