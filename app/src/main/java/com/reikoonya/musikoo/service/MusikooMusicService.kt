package com.reikoonya.musikoo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.reikoonya.musikoo.MainActivity
import com.reikoonya.musikoo.util.MusicPlayerManager
import com.reikoonya.musikoo.widget.MusicWidget

/**
 * Владелец плеера: ExoPlayer и MediaSession живут здесь, а не в синглтоне UI.
 * Экраны не трогают ExoPlayer напрямую, а шлют команды через MediaController.
 */
class MusikooMusicService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "musikoo_playback_channel"
        private const val PREFS = "musikoo_prefs"
    }

    private var player: ExoPlayer? = null
    private var session: MediaSession? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .build(),
        )

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(AudioAttributes.DEFAULT, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            // Keep the CPU awake while playing, so the track does not stall with the screen off
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        exo.shuffleModeEnabled = prefs.getBoolean("shuffle", false)
        exo.repeatMode = prefs.getInt("repeat", Player.REPEAT_MODE_OFF)
        var errorTrackIndex = -1
        var errorRetryCount = 0

        exo.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("Musikoo", "Playback error, recovering", error)
                val index = exo.currentMediaItemIndex.coerceAtLeast(0)
                if (index == errorTrackIndex) {
                    errorRetryCount++
                } else {
                    errorTrackIndex = index
                    errorRetryCount = 1
                }

                if (errorRetryCount <= 2) {
                    val position = exo.currentPosition
                    exo.prepare()
                    exo.seekTo(index, position)
                } else {
                    Log.w("Musikoo", "Too many playback errors on track index $index, skipping to next track")
                    errorRetryCount = 0
                    if (exo.hasNextMediaItem()) {
                        exo.seekToNextMediaItem()
                        exo.prepare()
                        exo.play()
                    } else {
                        exo.stop()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val mediaItem = exo.currentMediaItem
                val title = mediaItem?.mediaMetadata?.title?.toString()
                val artist = mediaItem?.mediaMetadata?.artist?.toString()
                MusicWidget.refresh(this@MusikooMusicService, title, artist, isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                errorTrackIndex = -1
                errorRetryCount = 0
                val title = mediaItem?.mediaMetadata?.title?.toString()
                val artist = mediaItem?.mediaMetadata?.artist?.toString()
                MusicWidget.refresh(this@MusikooMusicService, title, artist, exo.isPlaying)
            }
        })
        player = exo

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MusicPlayerManager.EXTRA_OPEN_PLAYER, true)
        }
        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val newSession = MediaSession.Builder(this, ArtworkPlayer(exo))
            .setSessionActivity(sessionActivity)
            .build()
        session = newSession
        addSession(newSession)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Musikoo Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Musikoo Media Playback Controls"
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val current = player
        if (current == null || !current.playWhenReady || current.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        session?.release()
        session = null
        player?.release()
        player = null
        super.onDestroy()
    }
}

/**
 * Обложки не передаются через IPC: при большом плейлисте Binder упёрся бы в TransactionTooLargeException.
 * Контроллер присылает элементы только с id и метаданными, а сервис сам подставляет JPEG-обложку
 * перед тем, как элементы попадут в ExoPlayer.
 */
@OptIn(UnstableApi::class)
private class ArtworkPlayer(exo: Player) : ForwardingPlayer(exo) {

    override fun setMediaItems(mediaItems: MutableList<MediaItem>) {
        super.setMediaItems(withArtwork(mediaItems))
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, resetPosition: Boolean) {
        super.setMediaItems(withArtwork(mediaItems), resetPosition)
    }

    override fun setMediaItems(mediaItems: MutableList<MediaItem>, startIndex: Int, startPositionMs: Long) {
        super.setMediaItems(withArtwork(mediaItems), startIndex, startPositionMs)
    }

    override fun addMediaItems(mediaItems: MutableList<MediaItem>) {
        super.addMediaItems(withArtwork(mediaItems))
    }

    override fun addMediaItems(index: Int, mediaItems: MutableList<MediaItem>) {
        super.addMediaItems(index, withArtwork(mediaItems))
    }

    override fun replaceMediaItem(index: Int, mediaItem: MediaItem) {
        super.replaceMediaItem(index, withArtwork(mutableListOf(mediaItem)).first())
    }

    private fun withArtwork(items: MutableList<MediaItem>): MutableList<MediaItem> =
        items.map { item ->
            val bytes = MusicPlayerManager.artworkBytesFor(item.mediaId) ?: return@map item
            val metadata = item.mediaMetadata.buildUpon()
                .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                .build()
            item.buildUpon().setMediaMetadata(metadata).build()
        }.toMutableList()
}
