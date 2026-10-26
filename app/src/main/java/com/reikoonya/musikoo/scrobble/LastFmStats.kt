package com.reikoonya.musikoo.scrobble

import com.reikoonya.musikoo.model.Song
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** One row of statistics. The match* fields tell the UI which library song provides its cover. */
data class StatItem(
    val title: String,
    val subtitle: String,
    val count: Int,
    val matchArtist: String = "",
    val matchAlbum: String = "",
    val matchTitle: String = "",
    val matchComposer: String = "",
)

data class StatsSnapshot(
    val artists: List<StatItem>,
    val albums: List<StatItem>,
    val tracks: List<StatItem>,
    val composers: List<StatItem>,
    val recent: List<StatItem>,
    val totalPlays: Int,
    val listenedMs: Long,
)

enum class StatsRange(val label: String, val localDays: Int?, val lastFm: String) {
    WEEK("7 days", 7, "7day"),
    MONTH("30 days", 30, "1month"),
    YEAR("1 year", 365, "12month"),
    ALL("All time", null, "overall"),
}

/** Statistics from the local play log (works without Last.fm). */
object LocalStats {
    fun build(records: List<PlayRecord>, songs: List<Song>, range: StatsRange): StatsSnapshot {
        val fromSec = range.localDays?.let { System.currentTimeMillis() / 1000 - it * 86_400L } ?: Long.MIN_VALUE
        val plays = records.filter { it.timestamp >= fromSec }

        // composer of each played track, looked up in the library
        val composerByKey = HashMap<String, String>()
        songs.forEach { s -> composerByKey["${s.artist.lowercase()}|${s.title.lowercase()}"] = s.composer.trim() }

        fun top(items: List<StatItem>) = items.sortedByDescending { it.count }.take(20)

        val artists = plays.groupingBy { it.artist }.eachCount().map { (name, count) ->
            StatItem(title = name, subtitle = "", count = count, matchArtist = name)
        }
        val albums = plays.filter { it.album.isNotBlank() }
            .groupingBy { it.album to it.artist }.eachCount().map { (key, count) ->
                StatItem(title = key.first, subtitle = key.second, count = count, matchArtist = key.second, matchAlbum = key.first)
            }
        val tracks = plays.groupingBy { it.artist to it.title }.eachCount().map { (key, count) ->
            StatItem(title = key.second, subtitle = key.first, count = count, matchArtist = key.first, matchTitle = key.second)
        }
        val composers = plays
            .mapNotNull { composerByKey["${it.artist.lowercase()}|${it.title.lowercase()}"]?.takeIf { c -> c.isNotBlank() } }
            .groupingBy { it }.eachCount().map { (name, count) ->
                StatItem(title = name, subtitle = "", count = count, matchComposer = name)
            }
        val recent = plays.sortedByDescending { it.timestamp }.take(20).map {
            StatItem(title = it.title, subtitle = it.artist, count = 0, matchArtist = it.artist, matchTitle = it.title)
        }

        return StatsSnapshot(
            artists = top(artists),
            albums = top(albums),
            tracks = top(tracks),
            composers = top(composers),
            recent = recent,
            totalPlays = plays.size,
            listenedMs = plays.sumOf { it.listenedMs },
        )
    }
}

/** Statistics from the Last.fm account. Reading needs only the API key and the username. */
object LastFmStats {
    private const val ENDPOINT = "https://ws.audioscrobbler.com/2.0/"
    private val cache = HashMap<String, StatsSnapshot>()

    /** Blocking call: run it on a background thread. */
    fun fetch(user: String, range: StatsRange): StatsSnapshot {
        val key = "$user|${range.name}"
        cache[key]?.let { return it }

        val period = range.lastFm
        val artists = get("user.gettopartists", mapOf("user" to user, "period" to period, "limit" to "20"))
            .getJSONObject("topartists").getJSONArray("artist")
            .items { o -> StatItem(o.getString("name"), "", o.optString("playcount").toIntOrNull() ?: 0, matchArtist = o.getString("name")) }

        val albums = get("user.gettopalbums", mapOf("user" to user, "period" to period, "limit" to "20"))
            .getJSONObject("topalbums").getJSONArray("album")
            .items { o ->
                val artist = o.getJSONObject("artist").getString("name")
                StatItem(o.getString("name"), artist, o.optString("playcount").toIntOrNull() ?: 0,
                    matchArtist = artist, matchAlbum = o.getString("name"))
            }

        val tracks = get("user.gettoptracks", mapOf("user" to user, "period" to period, "limit" to "20"))
            .getJSONObject("toptracks").getJSONArray("track")
            .items { o ->
                val artist = o.getJSONObject("artist").getString("name")
                StatItem(o.getString("name"), artist, o.optString("playcount").toIntOrNull() ?: 0,
                    matchArtist = artist, matchTitle = o.getString("name"))
            }

        // Last.fm has no composer data; the composer section is local only
        val recent = get("user.getrecenttracks", mapOf("user" to user, "limit" to "20"))
            .getJSONObject("recenttracks").getJSONArray("track")
            .items { o ->
                val artist = o.getJSONObject("artist").optString("#text")
                StatItem(o.getString("name"), artist, 0, matchArtist = artist, matchTitle = o.getString("name"))
            }

        val total = tracks.sumOf { it.count }
        val snapshot = StatsSnapshot(
            artists = artists,
            albums = albums,
            tracks = tracks,
            composers = emptyList(),
            recent = recent,
            totalPlays = total,
            listenedMs = 0L,
        )
        cache[key] = snapshot
        return snapshot
    }

    private fun get(method: String, params: Map<String, String>): JSONObject {
        check(LastFmConfig.isConfigured) { "Last.fm API key is not set" }
        val all = params + mapOf("method" to method, "api_key" to LastFmConfig.API_KEY, "format" to "json")
        val query = all.entries.joinToString("&") {
            URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
        }
        val connection = URL("$ENDPOINT?$query").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
            val json = JSONObject(stream?.bufferedReader()?.use { it.readText() }.orEmpty())
            if (json.has("error")) throw LastFmException(json.optInt("error"), json.optString("message"))
            return json
        } finally {
            connection.disconnect()
        }
    }

    private fun <T> JSONArray.items(map: (JSONObject) -> T): List<T> =
        List(length()) { map(getJSONObject(it)) }
}
