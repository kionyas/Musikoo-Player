package com.reikoonya.musikoo.lyrics

import android.content.Context
import com.reikoonya.musikoo.model.Song
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

data class LyricWord(val startMs: Long, val endMs: Long, val text: String)

data class LyricLine(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val words: List<LyricWord>?,
)

data class LyricsResult(
    val lines: List<LyricLine>,
    val level: String,   // "word", "line" or "none"
    val source: String,  // provider name, e.g. "YouTube Music", "KuGou", "LRCLIB"
)

/**
 * Репозиторий загрузки текстов через lrcmux API (https://api.lrcmux.dev).
 * Поддерживает каскадный поиск (полный -> очищенный -> принудительный) для максимального
 * процента нахождения треков в YouTube Music, LRCLIB и KuGou.
 */
object LyricsRepository {
    private const val BASE_URL = "https://api.lrcmux.dev"
    private const val USER_AGENT = "Musikoo/1.3.3 (https://github.com/reikoonya/musikoo)"

    private val noiseRegex = Regex("""(?i)\s*([\(\[](feat\.|ft\.|featuring|official|lyric|remastered|deluxe|bonus|live)[^)\]]*[\)\]]|\b(feat\.|ft\.|featuring)\b.+$)""")
    private val topicRegex = Regex("""(?i)\s*-\s*Topic$""")

    suspend fun load(context: Context, song: Song): LyricsResult? {
        val cleanT = song.title.replace(noiseRegex, "").trim()
        val cleanA = song.artist.replace(topicRegex, "").replace(noiseRegex, "").trim()

        val key = md5("$cleanA|$cleanT|${song.duration / 1000}")
        val cache = File(context.filesDir, "lyrics/$key.json")

        if (cache.exists()) {
            val parsed = parse(cache.readText())
            if (parsed != null && parsed.lines.isNotEmpty()) return parsed
            cache.delete()
        }

        // LRCLIB cache first: a track already known there costs no lrcmux request
        LrclibClient.cached(context, cleanA, cleanT, song.duration / 1000)?.let { return it }

        var fetched = fetch(
            artist = cleanA,
            title = cleanT,
            album = song.album.takeIf { !it.equals("Unknown Album", ignoreCase = true) && it.isNotBlank() },
            durationSec = (song.duration / 1000).takeIf { it > 0 },
            fetchStrategy = "default"
        )

        if (fetched.isNullOrBlank() || isEmpty(fetched)) {
            fetched = fetch(
                artist = cleanA,
                title = cleanT,
                album = null,
                durationSec = null,
                fetchStrategy = "force"
            )
        }

        if ((fetched == null || isEmpty(fetched)) && (cleanT != song.title || cleanA != song.artist)) {
            fetched = fetch(
                artist = song.artist,
                title = song.title,
                album = null,
                durationSec = null,
                fetchStrategy = "force"
            )
        }

        // lrcmux has nothing: try LRCLIB before giving up
        if (fetched == null || isEmpty(fetched)) {
            val album = song.album.takeIf { !it.equals("Unknown Album", ignoreCase = true) && it.isNotBlank() }
            return LrclibClient.load(context, cleanA, cleanT, album, song.duration / 1000)
        }

        return parse(fetched)?.takeIf { it.lines.isNotEmpty() }?.also {
            cache.parentFile?.mkdirs()
            cache.writeText(fetched)
        }
    }

    private suspend fun fetch(
        artist: String,
        title: String,
        album: String?,
        durationSec: Long?,
        fetchStrategy: String
    ): String? {
        val params = mutableListOf(
            "artist" to artist,
            "title" to title,
            "level" to "word",
            "strict" to "false",
            "format" to "json",
            "fetch" to fetchStrategy
        )
        if (!album.isNullOrBlank()) params.add("album" to album)
        if (durationSec != null && durationSec > 0) params.add("duration" to durationSec.toString())

        val query = params.joinToString("&") { "${enc(it.first)}=${enc(it.second)}" }

        for (attempt in 0..1) {
            val connection = URL("$BASE_URL/get?$query").openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8_000
                connection.readTimeout = 12_000
                connection.setRequestProperty("User-Agent", USER_AGENT)
                val code = connection.responseCode

                if (code == 429 && attempt == 0) {
                    val wait = connection.getHeaderField("Retry-After")?.toLongOrNull() ?: 2L
                    connection.disconnect()
                    delay(wait.coerceAtMost(6L) * 1000L)
                    continue
                }
                if (code !in 200..299) return null
                return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } catch (_: Exception) {
                return null
            } finally {
                connection.disconnect()
            }
        }
        return null
    }

    private fun isEmpty(json: String) = runCatching {
        (JSONObject(json).optJSONArray("lines")?.length() ?: 0) == 0
    }.getOrDefault(true)

    internal fun parse(json: String): LyricsResult? = try {
        val root = JSONObject(json)
        val meta = root.optJSONObject("meta")
        val array = root.optJSONArray("lines")
        val lines = if (array == null) emptyList() else List(array.length()) { i ->
            val o = array.getJSONObject(i)
            val wArr = o.optJSONArray("words")
            val words = wArr?.let { List(it.length()) { j -> it.getJSONObject(j).let { w -> LyricWord(w.optLong("start", 0L), w.optLong("end", 0L), w.optString("text")) } } }
            LyricLine(o.optLong("start", 0L), o.optLong("end", 0L), o.optString("text"), words)
        }

        val sourceName = meta?.optJSONObject("source")?.optString("name")
            ?: meta?.optString("source", "")
            ?: ""

        LyricsResult(lines, meta?.optString("level", "none") ?: "none", sourceName)
    } catch (_: Exception) {
        null
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    private fun md5(s: String): String = MessageDigest.getInstance("MD5").digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
