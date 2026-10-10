package com.reikoonya.musikoo.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.reikoonya.musikoo.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

object MusicPlayerManager {
    var exoPlayer: ExoPlayer? = null
    var songs: List<Song> = emptyList()
    var currentSongIndex by mutableIntStateOf(0)
    var isPlayingState by mutableStateOf(false)
    var currentPosition by mutableFloatStateOf(0f)
    var trackDuration by mutableFloatStateOf(100f)

    fun initPlayer(context: Context) {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(context.applicationContext).build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (duration > 0) {
                            trackDuration = duration.toFloat()
                        }
                    }
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlayingState = playing
                    }
                })
            }
            CoroutineScope(Dispatchers.Main).launch {
                while (true) {
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

    fun playSong(newSongs: List<Song>, index: Int) {
        songs = newSongs
        currentSongIndex = index
        if (songs.isNotEmpty() && index in songs.indices) {
            val song = songs[index]
            exoPlayer?.apply {
                setMediaItem(MediaItem.fromUri(song.uri))
                prepare()
                play()
                isPlayingState = true
                trackDuration = if (song.duration > 0) song.duration.toFloat() else 100f
            }
        }
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                isPlayingState = false
            } else {
                player.play()
                isPlayingState = true
            }
        }
    }

    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
        currentPosition = position.toFloat()
    }

    fun skipNext() {
        if (songs.isNotEmpty()) {
            val nextIndex = (currentSongIndex + 1) % songs.size
            playSong(songs, nextIndex)
        }
    }

    fun skipPrevious() {
        if (songs.isNotEmpty()) {
            val prevIndex = if (currentSongIndex > 0) currentSongIndex - 1 else songs.size - 1
            playSong(songs, prevIndex)
        }
    }
}
