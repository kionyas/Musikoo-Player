package com.reikoonya.musikoo.util

import com.reikoonya.musikoo.scrobble.Scrobbler
import com.reikoonya.musikoo.widget.MusicWidget
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.reikoonya.musikoo.MainActivity
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.service.MusikooMusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.time.Duration.Companion.milliseconds

object MusicPlayerManager {
    var exoPlayer: ExoPlayer? = null
    var mediaSession: MediaSession? = null
    private var appContext: Context? = null

    private var controllerFuture: ListenableFuture<MediaController>? = null

    var songs by mutableStateOf<List<Song>>(emptyList())
    var currentSongIndex by mutableIntStateOf(0)
    var isPlayingState by mutableStateOf(false)
    var currentPosition by mutableFloatStateOf(0f)
    var trackDuration by mutableFloatStateOf(100f)

    /** Shuffle and repeat mode (saved between launches). */
    var shuffleOn by mutableStateOf(false)
    var repeatState by mutableIntStateOf(Player.REPEAT_MODE_OFF)

    private var resumeAfterTagEdit: Pair<Long, Boolean>? = null

    private const val MAX_ART_SIDE = 512
    private const val PREFS = "musikoo_prefs"
    private val artworkCache = mutableMapOf<Long, ByteArray>()

    fun initPlayer(context: Context) {
        appContext = context.applicationContext
        Scrobbler.init(context)
        if (exoPlayer == null) {
            val appCtx = appContext!!

            val prefs = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            shuffleOn = prefs.getBoolean("shuffle", false)
            repeatState = prefs.getInt("repeat", Player.REPEAT_MODE_OFF)

            val intent = Intent(appCtx, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                appCtx,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            exoPlayer = ExoPlayer.Builder(appCtx)
                .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
                .setHandleAudioBecomingNoisy(true)
                .build()
                .apply {
                    shuffleModeEnabled = shuffleOn
                    repeatMode = repeatState
                    addListener(
                        object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                if (duration > 0) {
                                    trackDuration = duration.toFloat()
                                }
                            }

                            override fun onIsPlayingChanged(playing: Boolean) {
                                MusicWidget.refresh(appCtx)
                                isPlayingState = playing
                            }

                            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                                shuffleOn = shuffleModeEnabled
                                savePlaybackPrefs()
                            }

                            override fun onRepeatModeChanged(repeatMode: Int) {
                                repeatState = repeatMode
                                savePlaybackPrefs()
                            }

                            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                                MusicWidget.refresh(appCtx)
                                exoPlayer?.let { player ->
                                    val newIndex = player.currentMediaItemIndex
                                    if (newIndex in songs.indices) {
                                        currentSongIndex = newIndex
                                        val song = songs[newIndex]
                                        trackDuration = if (song.duration > 0) song.duration.toFloat() else 100f
                                    }
                                }
                            }
                        }
                    )
                }

            mediaSession = MediaSession.Builder(appCtx, exoPlayer!!)
                .setSessionActivity(pendingIntent)
                .build()

            CoroutineScope(Dispatchers.Main).launch {
                while (true) {
                    exoPlayer?.let { player ->
                        Scrobbler.onPlayerState(songs.getOrNull(currentSongIndex), player.isPlaying, player.currentPosition)
                    }
                    exoPlayer?.let { player ->
                        if (player.isPlaying) {
                            currentPosition = player.currentPosition.toFloat()
                            if (player.duration > 0) {
                                trackDuration = player.duration.toFloat()
                            }
                        }
                    }
                    delay(300.milliseconds)
                }
            }
        }
    }

    /** Downscales the cover and compresses it to JPEG to avoid TransactionTooLargeException. */
    private fun bitmapToArtworkBytes(bitmap: Bitmap): ByteArray? {
        return try {
            val maxSide = maxOf(bitmap.width, bitmap.height)
            val scaled = if (maxSide > MAX_ART_SIDE) {
                val scale = MAX_ART_SIDE.toFloat() / maxSide
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true,
                )
            } else {
                bitmap
            }

            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            stream.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    private fun getArtworkBytes(song: Song): ByteArray? {
        if (song.albumArt == null) return null
        return artworkCache.getOrPut(song.id) {
            bitmapToArtworkBytes(song.albumArt) ?: return null
        }
    }

    private fun buildMediaItem(song: Song): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .apply {
                // The system needs the duration right away to show the progress bar in the notification
                if (song.duration > 0) setDurationMs(song.duration)
            }

        getArtworkBytes(song)?.let { bytes ->
            metadataBuilder.setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    /** Binds the service to the session (once). The service shows the foreground notification itself. */
    private fun ensureServiceConnected() {
        if (controllerFuture != null) return
        val ctx = appContext ?: return
        controllerFuture = try {
            val token = SessionToken(ctx, ComponentName(ctx, MusikooMusicService::class.java))
            MediaController.Builder(ctx, token).buildAsync()
        } catch (_: Exception) {
            null
        }
    }

    fun playSong(newSongs: List<Song>, index: Int) {
        songs = newSongs
        currentSongIndex = index
        if (songs.isNotEmpty() && (index in songs.indices)) {
            val mediaItems = songs.map { buildMediaItem(it) }

            exoPlayer?.apply {
                setMediaItems(mediaItems, index, 0L)
                prepare()
                playWhenReady = true
                play()
                isPlayingState = true
                val song = songs[index]
                trackDuration = if (song.duration > 0) song.duration.toFloat() else 100f
            }

            ensureServiceConnected()
        }
    }

    /**
     * Removes a track deleted from the device from the list and from the ExoPlayer playlist.
     * If the current track is deleted, the player moves to the next one (keeping the pause/play state).
     */
    fun removeSong(song: Song) {
        val removedIndex = songs.indexOfFirst { it.id == song.id }
        if (removedIndex < 0) return

        val wasCurrent = removedIndex == currentSongIndex
        artworkCache.remove(song.id)

        // Update the list BEFORE player.removeMediaItem(): the onMediaItemTransition listener reads songs.
        songs = songs.filterIndexed { index, _ -> index != removedIndex }

        val player = exoPlayer ?: return

        if (songs.isEmpty()) {
            player.stop()
            player.clearMediaItems()
            currentSongIndex = 0
            currentPosition = 0f
            isPlayingState = false
            return
        }

        val shouldPlay = player.playWhenReady
        player.removeMediaItem(removedIndex)

        if (wasCurrent) {
            val target = if (removedIndex >= songs.size) 0 else removedIndex
            currentSongIndex = target
            currentPosition = 0f
            val next = songs[target]
            trackDuration = if (next.duration > 0) next.duration.toFloat() else 100f
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.seekTo(target, 0L)
            player.playWhenReady = shouldPlay
            isPlayingState = shouldPlay
        } else if (removedIndex < currentSongIndex) {
            currentSongIndex -= 1
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            isPlayingState = if (player.isPlaying) {
                player.pause()
                false
            } else {
                player.play()
                true
            }
        }
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
        currentPosition = position.toFloat()
    }

    fun rewind10() {
        exoPlayer?.let { exo ->
            exo.seekTo((exo.currentPosition - 10000).coerceAtLeast(0L))
        }
    }

    fun forward10() {
        exoPlayer?.let { exo ->
            exo.seekTo((exo.currentPosition + 10000).coerceAtMost(exo.duration.coerceAtLeast(0L)))
        }
    }

    fun skipNext() {
        exoPlayer?.let { player ->
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
            } else if (songs.isNotEmpty()) {
                player.seekTo(0, 0L)
            }
        }
    }

    fun skipPrevious() {
        exoPlayer?.let { player ->
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
            } else if (songs.isNotEmpty()) {
                val lastIndex = songs.size - 1
                player.seekTo(lastIndex, 0L)
            }
        }
    }

    // ---------- shuffle and repeat ----------

    fun toggleShuffle() {
        shuffleOn = !shuffleOn
        exoPlayer?.shuffleModeEnabled = shuffleOn
        savePlaybackPrefs()
    }

    /** Repeat: off → all → one track → off. */
    fun cycleRepeat() {
        repeatState = when (repeatState) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        exoPlayer?.repeatMode = repeatState
        savePlaybackPrefs()
    }

    private fun savePlaybackPrefs() {
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()
            ?.putBoolean("shuffle", shuffleOn)
            ?.putInt("repeat", repeatState)
            ?.apply()
    }

    // ---------- tag editor ----------

    /** Before writing tags: if the playing track is being edited, pause it and remember the position. */
    fun beginTagEdit(songIds: Set<Long>) {
        val player = exoPlayer ?: return
        val current = songs.getOrNull(currentSongIndex) ?: return
        if (current.id in songIds && resumeAfterTagEdit == null) {
            resumeAfterTagEdit = player.currentPosition to player.playWhenReady
            player.pause()
        }
    }

    /** Write cancelled or failed: restore the player to its previous state. */
    fun cancelTagEdit() {
        val state = resumeAfterTagEdit ?: return
        resumeAfterTagEdit = null
        exoPlayer?.playWhenReady = state.second
    }

    /** Puts the updated tags into the playlist (the title and cover in the notification update too). */
    fun applyEditedSongs(updated: List<Song>) {
        if (updated.isEmpty()) {
            cancelTagEdit()
            return
        }
        val byId = updated.associateBy { it.id }
        updated.forEach { artworkCache.remove(it.id) }
        val newSongs = songs.map { byId[it.id] ?: it }
        songs = newSongs

        val player = exoPlayer ?: return
        val resume = resumeAfterTagEdit
        resumeAfterTagEdit = null

        val currentIndex = player.currentMediaItemIndex
        val count = player.mediaItemCount
        newSongs.forEachIndexed { index, song ->
            if (index < count && byId.containsKey(song.id)) {
                player.replaceMediaItem(index, buildMediaItem(song))
            }
        }
        if (resume != null) {
            player.seekTo(currentIndex, resume.first)
            player.playWhenReady = resume.second
        }
    }
}
