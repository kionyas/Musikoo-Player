package com.reikoonya.musikoo.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.reikoonya.musikoo.util.MusicPlayerManager

class MusikooMusicService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "musikoo_playback_channel"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(CHANNEL_ID)
                .build(),
        )

        if (MusicPlayerManager.mediaSession == null) {
            MusicPlayerManager.initPlayer(applicationContext)
        }
        MusicPlayerManager.mediaSession?.let { addSession(it) }
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        if (MusicPlayerManager.mediaSession == null) {
            MusicPlayerManager.initPlayer(applicationContext)
        }
        return MusicPlayerManager.mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = MusicPlayerManager.exoPlayer
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }
}
