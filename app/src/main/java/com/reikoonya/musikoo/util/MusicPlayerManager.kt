package com.reikoonya.musikoo.util

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.reikoonya.musikoo.data.AlbumArtCache
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.scrobble.Scrobbler
import com.reikoonya.musikoo.service.MusikooMusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import kotlin.time.Duration.Companion.milliseconds

/**
 * Клиент плеера для экранов. Сам плеер (ExoPlayer + MediaSession) живёт в MusikooMusicService.
 * Все команды уходят через [controller]; состояние для Compose хранится здесь же.
 * Команды, пришедшие до подключения к сервису, ставятся в очередь и выполняются сразу после него.
 */
object MusicPlayerManager {
    /** Подключение к сервису. null, пока соединение не установлено (или оборвалось). */
    var controller: MediaController? = null
        private set

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val pendingCommands = mutableListOf<(MediaController) -> Unit>()
    private var appContext: Context? = null
    private var tickerStarted = false

    private var _songs by mutableStateOf<List<Song>>(emptyList())
    var songs: List<Song>
        get() = _songs
        set(value) {
            _songs = value
            songsById = value.associateBy { it.id }
        }

    private var songsById: Map<Long, Song> = emptyMap()
    var currentSongIndex by mutableIntStateOf(0)
    var isPlayingState by mutableStateOf(false)

    /** Set when the app is opened from the media notification: MainActivity then goes straight to the player. */
    var pendingOpenPlayer by mutableStateOf(false)
    const val EXTRA_OPEN_PLAYER = "open_player"
    var currentPosition by mutableFloatStateOf(0f)
    var trackDuration by mutableFloatStateOf(100f)

    /** Shuffle and repeat mode (saved between launches). */
    var shuffleOn by mutableStateOf(false)

    /** Playback speed, cycled from the Speed pill. */
    var playbackSpeed by mutableFloatStateOf(1f)
        private set

    /** Direction of the last track change, used by the cover animation. */
    var lastTransitionForward: Boolean = true
        private set
    private var lastTransitionIndex: Int = -1
    var repeatState by mutableIntStateOf(Player.REPEAT_MODE_OFF)

    private var resumeAfterTagEdit: Pair<Long, Boolean>? = null

    private const val MAX_ART_SIDE = 512
    private const val PREFS = "musikoo_prefs"
    private const val QUEUE_KEY = "queue_state"
    private const val ARTWORK_CACHE_BYTES = 8 * 1024 * 1024
    private var saveTicks = 0

    /** JPEG-обложки для сервиса (уведомление, экран блокировки). Ограничены по размеру, а не растут бесконечно. */
    private val artworkCache = object : LruCache<Long, ByteArray>(ARTWORK_CACHE_BYTES) {
        override fun sizeOf(key: Long, value: ByteArray): Int = value.size
    }

    private val artScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /** Слушатель состояния от сервиса. Выполняется в главном потоке. */
    private val stateListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            controller?.let { player ->
                if (player.duration > 0) {
                    trackDuration = player.duration.toFloat()
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            isPlayingState = false
        }

        override fun onIsPlayingChanged(playing: Boolean) {
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
            val player = controller ?: return
            val newIndex = player.currentMediaItemIndex
            if (newIndex in songs.indices) {
                // Real direction of the move, taken from the player (shuffle included)
                val old = lastTransitionIndex
                lastTransitionForward = when {
                    old < 0 -> true
                    old == songs.lastIndex && newIndex == 0 -> true
                    old == 0 && newIndex == songs.lastIndex -> false
                    else -> newIndex >= old
                }
                lastTransitionIndex = newIndex
                currentSongIndex = newIndex
                val song = songs[newIndex]
                trackDuration = if (song.duration > 0) song.duration.toFloat() else 100f
                preloadArtwork(newIndex)
            }
        }
    }

    /**
     * Вызывается при старте UI. Загружает настройки для экранов и подключается к сервису.
     * Плеер создаёт сам сервис, поэтому здесь ничего не инициализируется повторно.
     */
    fun initPlayer(context: Context) {
        val isFirstCall = appContext == null
        appContext = context.applicationContext
        Scrobbler.init(context)
        if (isFirstCall) {
            val prefs = appContext!!.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            shuffleOn = prefs.getBoolean("shuffle", false)
            repeatState = prefs.getInt("repeat", Player.REPEAT_MODE_OFF)
        }
        ensureServiceConnected()
    }

    /** Подключается к MusikooMusicService. Вызывается повторно, если соединение оборвалось. */
    private fun ensureServiceConnected() {
        if (controllerFuture != null) return
        val ctx = appContext ?: return
        val future = try {
            MediaController.Builder(ctx, SessionToken(ctx, ComponentName(ctx, MusikooMusicService::class.java)))
                .setListener(object : MediaController.Listener {
                    override fun onDisconnected(controller: MediaController) {
                        // Сервис остановлен системой. Следующая команда снова подключит его.
                        if (this@MusicPlayerManager.controller === controller) {
                            this@MusicPlayerManager.controller = null
                        }
                        controllerFuture = null
                        isPlayingState = false
                    }
                })
                .buildAsync()
        } catch (_: Exception) {
            null
        } ?: return

        controllerFuture = future
        future.addListener({
            try {
                attachController(future.get())
            } catch (_: Exception) {
                controllerFuture = null
            }
        }, ContextCompat.getMainExecutor(ctx))
    }

    private fun attachController(player: MediaController) {
        controller = player
        player.addListener(stateListener)
        player.shuffleModeEnabled = shuffleOn
        player.repeatMode = repeatState
        isPlayingState = player.isPlaying

        // Сервис мог перезапуститься с пустым плеером, а очередь в памяти осталась. Передаём её заново.
        if (player.mediaItemCount == 0 && songs.isNotEmpty() && currentSongIndex in songs.indices) {
            player.setMediaItems(songs.map { buildMediaItem(it) }, currentSongIndex, 0L)
            player.prepare()
            player.playWhenReady = false
        }

        val queued = pendingCommands.toList()
        pendingCommands.clear()
        queued.forEach { it(player) }

        startTicker()
    }

    /**
     * Выполняет команду сейчас, если сервис подключён, иначе ставит её в очередь
     * и запускает переподключение.
     */
    private inline fun onController(crossinline block: (MediaController) -> Unit) {
        val player = controller
        if (player != null) {
            block(player)
        } else {
            pendingCommands.add { block(it) }
            ensureServiceConnected()
        }
    }

    /** Тикер раз в 300 мс: подсчёт прослушиваний, позиция для UI и периодическое сохранение очереди. */
    private fun startTicker() {
        if (tickerStarted) return
        tickerStarted = true
        CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                controller?.let { player ->
                    Scrobbler.onPlayerState(songs.getOrNull(currentSongIndex), player.isPlaying, player.currentPosition)
                    if (player.isPlaying) {
                        if (++saveTicks % 17 == 0) saveQueueState() // about every 5 seconds
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

    /** Обложка для сервиса: по id элемента очереди берём JPEG уменьшенной обложки. */
    fun artworkBytesFor(mediaId: String): ByteArray? {
        val id = mediaId.toLongOrNull() ?: return null
        val song = songsById[id] ?: return null
        return getArtworkBytes(song)
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
        artworkCache.get(song.id)?.let { return it }
        val bitmap = AlbumArtCache.peek(song) ?: return null
        val bytes = bitmapToArtworkBytes(bitmap) ?: return null
        artworkCache.put(song.id, bytes)
        return bytes
    }

    /**
     * Грузит обложку текущей и следующей песни и обновляет её элемент очереди, чтобы уведомление показало картинку.
     * Остальные песни очереди не грузятся: их обложки подтянутся, когда до них дойдёт очередь.
     */
    private fun preloadArtwork(index: Int) {
        val ctx = appContext ?: return
        for (i in listOf(index, index + 1)) {
            val song = songs.getOrNull(i) ?: continue
            if (artworkCache.get(song.id) != null || AlbumArtCache.peek(song) != null) continue
            artScope.launch {
                if (AlbumArtCache.load(ctx, song) != null) refreshArtwork(song.id)
            }
        }
    }

    /** Пересоздаёт элемент очереди с обложкой. Для текущей песни сохраняем позицию, как при правке тегов. */
    private fun refreshArtwork(songId: Long) {
        val index = songs.indexOfFirst { it.id == songId }
        if (index < 0) return
        onController { player ->
            if (index >= player.mediaItemCount) return@onController
            val isCurrent = index == player.currentMediaItemIndex
            val position = if (isCurrent) player.currentPosition else 0L
            player.replaceMediaItem(index, buildMediaItem(songs[index]))
            if (isCurrent) player.seekTo(index, position)
        }
    }

    /**
     * Элемент очереди без обложки: она добавляется уже на стороне сервиса (см. ArtworkPlayer).
     * Длительность нужна системе сразу, чтобы показать прогресс в уведомлении.
     */
    private fun buildMediaItem(song: Song): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .apply {
                if (song.duration > 0) setDurationMs(song.duration)
            }

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    fun playSong(newSongs: List<Song>, index: Int) {
        songs = newSongs
        currentSongIndex = index
        if (songs.isNotEmpty() && (index in songs.indices)) {
            val mediaItems = songs.map { buildMediaItem(it) }

            onController { player ->
                player.setMediaItems(mediaItems, index, 0L)
                player.prepare()
                player.playWhenReady = true
                player.play()
            }
            isPlayingState = true
            val song = songs[index]
            trackDuration = if (song.duration > 0) song.duration.toFloat() else 100f

            saveQueueState()
            preloadArtwork(index)
            ensureServiceConnected()
        }
    }

    /**
     * Removes a track deleted from the device from the list and from the player playlist.
     * If the current track is deleted, the player moves to the next one (keeping the pause/play state).
     */
    fun removeSong(song: Song) {
        val removedIndex = songs.indexOfFirst { it.id == song.id }
        if (removedIndex < 0) return

        val wasCurrent = removedIndex == currentSongIndex
        artworkCache.remove(song.id)

        // Update the list BEFORE player.removeMediaItem(): the onMediaItemTransition listener reads songs.
        songs = songs.filterIndexed { index, _ -> index != removedIndex }

        val player = controller
        if (songs.isEmpty()) {
            player?.stop()
            player?.clearMediaItems()
            currentSongIndex = 0
            currentPosition = 0f
            isPlayingState = false
            return
        }

        val shouldPlay = player?.playWhenReady ?: false
        player?.removeMediaItem(removedIndex)

        if (wasCurrent) {
            val target = if (removedIndex >= songs.size) 0 else removedIndex
            currentSongIndex = target
            currentPosition = 0f
            val next = songs[target]
            trackDuration = if (next.duration > 0) next.duration.toFloat() else 100f
            player?.let { p ->
                if (p.playbackState == Player.STATE_IDLE) p.prepare()
                p.seekTo(target, 0L)
                p.playWhenReady = shouldPlay
            }
            isPlayingState = shouldPlay
        } else if (removedIndex < currentSongIndex) {
            currentSongIndex -= 1
        }
        saveQueueState()
    }

    /** Reorders an item in the queue and updates the player playlist. */
    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in songs.indices || toIndex !in songs.indices || fromIndex == toIndex) return
        val list = songs.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        songs = list

        controller?.moveMediaItem(fromIndex, toIndex)

        if (currentSongIndex == fromIndex) {
            currentSongIndex = toIndex
        } else if (fromIndex < currentSongIndex && toIndex >= currentSongIndex) {
            currentSongIndex -= 1
        } else if (fromIndex > currentSongIndex && toIndex <= currentSongIndex) {
            currentSongIndex += 1
        }

        saveQueueState()
    }

    /** Removes an item at specific queue index. */
    fun removeQueueItemAt(index: Int) {
        if (index in songs.indices) {
            removeSong(songs[index])
        }
    }

    /** Plays the track at specific queue index immediately. */
    fun playQueueIndex(index: Int) {
        if (index !in songs.indices) return
        currentSongIndex = index
        onController { player ->
            player.seekTo(index, 0L)
            player.prepare()
            player.playWhenReady = true
            player.play()
        }
        isPlayingState = true
        val song = songs[index]
        trackDuration = if (song.duration > 0) song.duration.toFloat() else 100f
        currentPosition = 0f
        saveQueueState()
    }

    /** Clears the entire current queue. */
    fun clearQueue() {
        songs = emptyList()
        currentSongIndex = 0
        currentPosition = 0f
        isPlayingState = false
        controller?.apply {
            stop()
            clearMediaItems()
        }
        saveQueueState()
    }

    /** Saves the queue, the current track and the position, so playback can resume after an app restart. */
    fun saveQueueState() {
        val ctx = appContext ?: return
        if (songs.isEmpty()) return
        val json = JSONObject()
            .put("ids", JSONArray(songs.map { it.id }))
            .put("index", currentSongIndex)
            .put("pos", controller?.currentPosition ?: 0L)
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(QUEUE_KEY, json.toString()).apply()
    }

    /**
     * Restores the saved queue paused, once the library is loaded. Tracks that no longer exist are skipped.
     */
    fun restoreQueue(library: List<Song>) {
        val ctx = appContext ?: return
        // Если сервис уже играет очередь (процесс не перезапускался), ничего не трогаем.
        if ((controller?.mediaItemCount ?: 0) > 0) return
        val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(QUEUE_KEY, null) ?: return
        try {
            val json = JSONObject(raw)
            val ids = json.getJSONArray("ids")
            val savedIndex = json.optInt("index", 0)
            val currentId = if (savedIndex in 0 until ids.length()) ids.getLong(savedIndex) else -1L
            val byId = library.associateBy { it.id }
            val queue = (0 until ids.length()).mapNotNull { byId[ids.getLong(it)] }
            if (queue.isEmpty()) return
            val index = queue.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
            val position = json.optLong("pos", 0L)

            songs = queue
            currentSongIndex = index
            val items = queue.map { buildMediaItem(it) }
            onController { player ->
                if (player.mediaItemCount > 0) return@onController
                player.setMediaItems(items, index, position)
                player.prepare()
                player.playWhenReady = false
            }
            isPlayingState = false
            trackDuration = queue[index].duration.takeIf { it > 0 }?.toFloat() ?: 100f
            currentPosition = position.toFloat()
            preloadArtwork(index)
        } catch (_: Exception) {
            // a broken or old saved state is simply ignored
        }
    }

    fun togglePlayPause() {
        saveQueueState()
        onController { player ->
            if (player.isPlaying) {
                player.pause()
                isPlayingState = false
            } else {
                // Recover from an idle or failed player, and restart a finished track from the beginning
                if (player.playerError != null || player.playbackState == Player.STATE_IDLE) player.prepare()
                if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
                player.play()
                isPlayingState = true
            }
        }
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
        currentPosition = position.toFloat()
        saveQueueState()
    }

    fun rewind10() {
        controller?.let { player ->
            player.seekTo((player.currentPosition - 10000).coerceAtLeast(0L))
        }
        saveQueueState()
    }

    fun forward10() {
        controller?.let { player ->
            player.seekTo((player.currentPosition + 10000).coerceAtMost(player.duration.coerceAtLeast(0L)))
        }
        saveQueueState()
    }

    fun skipNext() {
        controller?.let { player ->
            if (player.hasNextMediaItem()) {
                player.seekToNextMediaItem()
            } else if (songs.isNotEmpty()) {
                player.seekTo(0, 0L)
            }
        }
        saveQueueState()
    }

    fun skipPrevious() {
        controller?.let { player ->
            if (player.hasPreviousMediaItem()) {
                player.seekToPreviousMediaItem()
            } else if (songs.isNotEmpty()) {
                val lastIndex = songs.size - 1
                player.seekTo(lastIndex, 0L)
            }
        }
        saveQueueState()
    }

    // ---------- shuffle and repeat ----------

    /** 1x -> 1.25x -> 1.5x -> 2x -> 0.75x -> 1x. */
    fun cycleSpeed() {
        val next = when (playbackSpeed) {
            1f -> 1.25f
            1.25f -> 1.5f
            1.5f -> 2f
            2f -> 0.75f
            else -> 1f
        }
        playbackSpeed = next
        controller?.setPlaybackSpeed(next)
    }

    fun toggleShuffle() {
        shuffleOn = !shuffleOn
        controller?.shuffleModeEnabled = shuffleOn
        savePlaybackPrefs()
    }

    /** Repeat: off → all → one track → off. */
    fun cycleRepeat() {
        repeatState = when (repeatState) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller?.repeatMode = repeatState
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
        val player = controller ?: return
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
        controller?.playWhenReady = state.second
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

        val resume = resumeAfterTagEdit
        resumeAfterTagEdit = null
        val player = controller ?: return

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
