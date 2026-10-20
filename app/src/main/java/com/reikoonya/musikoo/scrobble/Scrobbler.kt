package com.reikoonya.musikoo.scrobble

import android.net.ConnectivityManager
import android.net.Network
import android.content.Context
import android.os.SystemClock
import com.reikoonya.musikoo.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** A counted listen. Always recorded, even without Last.fm; this is the basis of the statistics. */
data class PlayRecord(
    val timestamp: Long,      // seconds, start time
    val artist: String,
    val title: String,
    val album: String,
    val listenedMs: Long,     // how long it actually played
)

/** Not yet sent to Last.fm (stored until the network is available). */
private data class ScrobbleItem(
    val artist: String,
    val title: String,
    val album: String,
    val timestamp: Long,
    val durationSec: Long,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("a", artist)
        .put("t", title)
        .put("al", album)
        .put("ts", timestamp)
        .put("d", durationSec)

    companion object {
        fun fromJson(o: JSONObject) = ScrobbleItem(
            artist = o.getString("a"),
            title = o.getString("t"),
            album = o.optString("al"),
            timestamp = o.getLong("ts"),
            durationSec = o.optLong("d"),
        )
    }
}

/**
 * Listen tracking per Last.fm rules: a track counts when half of it has played
 * (but at most 4 minutes) and the track is longer than 30 seconds. Time is counted from actual playback,
 * seeking and pausing do not inflate it.
 */
object Scrobbler {
    private const val PREFS = "lastfm_prefs"
    private const val KEY_SESSION = "session_key"
    private const val KEY_USER = "username"
    private const val KEY_QUEUE = "pending_scrobbles"
    private const val PLAY_LOG_FILE = "play_log.json"
    private const val MIN_TRACK_MS = 30_000L
    private const val MAX_THRESHOLD_MS = 240_000L
    private const val MAX_TICK_MS = 1_000L
    private const val MAX_QUEUE = 500

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val logLock = Mutex()
    private val queueLock = Mutex()
    private var appContext: Context? = null

    /** Incremented on every counted listen; the stats screen reloads its data on change. */
    private val playsCounter = MutableStateFlow(0)
    val playsVersion: StateFlow<Int> = playsCounter.asStateFlow()

    // current track state
    private var currentId = -1L
    private var startedAtSec = 0L
    private var listenedMs = 0L
    private var lastPosition = 0L
    private var lastElapsed = 0L
    private var nowPlayingSent = false
    private var scrobbled = false

    private var networkWatchStarted = false

    fun init(context: Context) {
        if (appContext == null) appContext = context.applicationContext
        watchNetwork()
    }

    /** Sends queued scrobbles as soon as the device gets a network connection again. */
    private fun watchNetwork() {
        if (networkWatchStarted) return
        val ctx = appContext ?: return
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        networkWatchStarted = true
        cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (isConnected()) scope.launch { flushQueue() }
            }
        })
    }

    private fun prefs() = requireNotNull(appContext).getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun username(): String? = appContext?.let { prefs().getString(KEY_USER, null) }

    fun isConnected(): Boolean = appContext != null && prefs().getString(KEY_SESSION, null) != null

    fun saveSession(sessionKey: String, user: String) {
        prefs().edit().putString(KEY_SESSION, sessionKey).putString(KEY_USER, user).apply()
    }

    fun logout() {
        prefs().edit().remove(KEY_SESSION).remove(KEY_USER).remove(KEY_QUEUE).apply()
    }

    /**
     * Called by the player about every 300 ms on the main thread.
     * Only time is counted here; network and file work goes to the background.
     */
    fun onPlayerState(song: Song?, isPlaying: Boolean, positionMs: Long) {
        if (appContext == null || song == null) return
        val now = SystemClock.elapsedRealtime()

        // a new track, or the same track started again (repeat one)
        val isNewPlay = song.id != currentId || positionMs + 2_000 < lastPosition
        if (isNewPlay) {
            currentId = song.id
            startedAtSec = System.currentTimeMillis() / 1000
            listenedMs = 0L
            nowPlayingSent = false
            scrobbled = false
        }
        if (isNewPlay && isConnected()) {
            // Send anything queued while offline, now that the network is available
            scope.launch { flushQueue() }
        }
        if (isPlaying && lastElapsed > 0L) {
            listenedMs += (now - lastElapsed).coerceIn(0L, MAX_TICK_MS)
        }
        lastElapsed = now
        lastPosition = positionMs

        if (!isPlaying) return

        if (!nowPlayingSent) {
            nowPlayingSent = true
            if (isConnected()) sendNowPlaying(song)
        }

        val threshold = minOf(song.duration / 2, MAX_THRESHOLD_MS)
        if (!scrobbled && song.duration >= MIN_TRACK_MS && listenedMs >= threshold) {
            scrobbled = true
            recordPlay(song, startedAtSec, listenedMs)
        }
    }

    /** All counted listens from the local log. Call from a background thread. */
    fun loadPlays(): List<PlayRecord> {
        val ctx = appContext ?: return emptyList()
        val file = File(ctx.filesDir, PLAY_LOG_FILE)
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                PlayRecord(
                    timestamp = o.getLong("ts"),
                    artist = o.getString("a"),
                    title = o.getString("t"),
                    album = o.optString("al"),
                    listenedMs = o.optLong("ms"),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun recordPlay(song: Song, timestamp: Long, listened: Long) {
        val record = PlayRecord(timestamp, song.artist, song.title, song.album, listened)
        val item = ScrobbleItem(song.artist, song.title, song.album, timestamp, song.duration / 1000)
        scope.launch {
            logLock.withLock { appendPlay(record) }
            playsCounter.value = playsCounter.value + 1
            if (isConnected()) {
                enqueue(item)
                flushQueue()
            }
        }
    }

    private fun appendPlay(record: PlayRecord) {
        val file = File(requireNotNull(appContext).filesDir, PLAY_LOG_FILE)
        val array = try {
            if (file.exists()) JSONArray(file.readText()) else JSONArray()
        } catch (_: Exception) {
            JSONArray()
        }
        array.put(
            JSONObject()
                .put("ts", record.timestamp)
                .put("a", record.artist)
                .put("t", record.title)
                .put("al", record.album)
                .put("ms", record.listenedMs)
        )
        file.writeText(array.toString())
    }

    private fun sendNowPlaying(song: Song) {
        val session = prefs().getString(KEY_SESSION, null) ?: return
        scope.launch {
            try {
                LastFmClient.updateNowPlaying(session, song.artist, song.title, song.album, song.duration / 1000)
            } catch (_: Exception) {
                // "now playing" is not critical, skip it
            }
        }
    }

    private suspend fun enqueue(item: ScrobbleItem) {
        queueLock.withLock {
            val updated = (loadQueue() + item).takeLast(MAX_QUEUE)
            saveQueue(updated)
        }
    }

    /** Sends the queue in order. On a network error, keeps the rest for the next attempt. */
    private suspend fun flushQueue() {
        val session = prefs().getString(KEY_SESSION, null) ?: return
        queueLock.withLock {
            val remaining = ArrayList<ScrobbleItem>()
            var stop = false
            for (item in loadQueue()) {
                if (stop) {
                    remaining.add(item)
                    continue
                }
                try {
                    LastFmClient.scrobble(
                        session, item.artist, item.title, item.album, item.timestamp, item.durationSec
                    )
                } catch (e: LastFmException) {
                    when (e.code) {
                        9 -> { // session invalid: the user revoked access on the website
                            logout()
                            return
                        }
                        11, 16, 29 -> { // server error or rate limit: try again later
                            stop = true
                            remaining.add(item)
                        }
                        else -> Unit // invalid data: this track will never be accepted, drop it
                    }
                } catch (_: Exception) {
                    // no network: keep everything for the next attempt
                    stop = true
                    remaining.add(item)
                }
            }
            saveQueue(remaining)
        }
    }

    private fun loadQueue(): List<ScrobbleItem> {
        val raw = prefs().getString(KEY_QUEUE, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { ScrobbleItem.fromJson(array.getJSONObject(it)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveQueue(items: List<ScrobbleItem>) {
        val array = JSONArray()
        items.forEach { array.put(it.toJson()) }
        prefs().edit().putString(KEY_QUEUE, array.toString()).apply()
    }
}
