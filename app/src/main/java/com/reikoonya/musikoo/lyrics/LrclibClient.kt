package com.reikoonya.musikoo.lyrics

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

/**
 * LRCLIB (https://lrclib.net), a second lyrics source next to lrcmux.
 * Exact lookup via /api/get, fuzzy fallback via /api/search. No API key needed.
 */
object LrclibClient {
    private const val BASE_URL = "https://lrclib.net/api"
    // LRCLIB asks for an identifying User-Agent
    private const val USER_AGENT = "Musikoo/1.3.1 (https://github.com/reikoonya/musikoo)"

    /** Only the disk cache, no network. */
    fun cached(context: Context, artist: String, title: String, durationSec: Long): LyricsResult? {
        val file = cacheFile(context, artist, title, durationSec)
        if (!file.exists()) return null
        return LyricsRepository.parse(file.readText())?.takeIf { it.lines.isNotEmpty() }
    }

    /** Network lookup, then cache the normalised result. */
    suspend fun load(
        context: Context,
        artist: String,
        title: String,
        album: String?,
        durationSec: Long,
    ): LyricsResult? {
        val record = getRecord(artist, title, album, durationSec)
            ?: searchRecord(artist, title)
            ?: return null

        val result = toResult(record) ?: return null
        val file = cacheFile(context, artist, title, durationSec)
        file.parentFile?.mkdirs()
        file.writeText(toJson(result))
        return result
    }

    private suspend fun getRecord(artist: String, title: String, album: String?, durationSec: Long): JSONObject? {
        val params = mutableListOf("artist_name" to artist, "track_name" to title)
        if (!album.isNullOrBlank()) params.add("album_name" to album)
        if (durationSec > 0) params.add("duration" to durationSec.toString())
        return getJson("$BASE_URL/get?${query(params)}") as? JSONObject
    }

    private suspend fun searchRecord(artist: String, title: String): JSONObject? {
        val array = getJson("$BASE_URL/search?${query(listOf("artist_name" to artist, "track_name" to title))}") as? JSONArray
            ?: return null
        val records = List(array.length()) { array.getJSONObject(it) }
        // Prefer a record that has synced lyrics, otherwise take any with plain text
        return records.firstOrNull { !it.isNull("syncedLyrics") && it.optString("syncedLyrics").isNotBlank() }
            ?: records.firstOrNull { !it.isNull("plainLyrics") && it.optString("plainLyrics").isNotBlank() }
    }

    /** One GET with a single retry on 429. Returns null on 404 or any error. */
    private suspend fun getJson(url: String): Any? {
        for (attempt in 0..1) {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8_000
                connection.readTimeout = 12_000
                connection.setRequestProperty("User-Agent", USER_AGENT)
                connection.setRequestProperty("Accept", "application/json")
                val code = connection.responseCode
                if (code == 429 && attempt == 0) {
                    val wait = connection.getHeaderField("Retry-After")?.toLongOrNull() ?: 2L
                    connection.disconnect()
                    kotlinx.coroutines.delay(wait.coerceAtMost(6L) * 1000L)
                    continue
                }
                if (code !in 200..299) return null
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                return if (body.trimStart().startsWith("[")) JSONArray(body) else JSONObject(body)
            } catch (_: Exception) {
                return null
            } finally {
                connection.disconnect()
            }
        }
        return null
    }

    private fun toResult(record: JSONObject): LyricsResult? {
        val synced = record.optString("syncedLyrics").takeIf { !record.isNull("syncedLyrics") && it.isNotBlank() }
        if (synced != null) {
            val lines = parseLrc(synced)
            if (lines.isNotEmpty()) return LyricsResult(lines, "line", "LRCLIB")
        }
        val plain = record.optString("plainLyrics").takeIf { !record.isNull("plainLyrics") && it.isNotBlank() }
            ?: return null
        val lines = plain.lineSequence().map { LyricLine(0L, 0L, it.trim(), null) }.toList()
        return LyricsResult(lines, "none", "LRCLIB")
    }

    /** Standard LRC: [mm:ss.xx]text, several timestamps per line allowed. */
    internal fun parseLrc(lrc: String): List<LyricLine> {
        val tag = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
        val raw = mutableListOf<Pair<Long, String>>()
        lrc.lineSequence().forEach { line ->
            val matches = tag.findAll(line).toList()
            if (matches.isEmpty()) return@forEach
            val text = line.substring(matches.last().range.last + 1).trim()
            matches.forEach { m ->
                val minutes = m.groupValues[1].toLong()
                val seconds = m.groupValues[2].toLong()
                val fraction = m.groupValues[3]
                val ms = when (fraction.length) {
                    0 -> 0L
                    1 -> fraction.toLong() * 100L
                    2 -> fraction.toLong() * 10L
                    else -> fraction.toLong()
                }
                raw += (minutes * 60_000L + seconds * 1_000L + ms) to text
            }
        }
        raw.sortBy { it.first }
        return raw.mapIndexed { i, (start, text) ->
            LyricLine(start, raw.getOrNull(i + 1)?.first ?: (start + 4_000L), text, null)
        }
    }

    private fun toJson(result: LyricsResult): String {
        val lines = JSONArray()
        result.lines.forEach { l ->
            lines.put(JSONObject().put("start", l.startMs).put("end", l.endMs).put("text", l.text))
        }
        return JSONObject()
            .put("lines", lines)
            .put("meta", JSONObject()
                .put("level", result.level)
                .put("source", JSONObject().put("name", result.source)))
            .toString()
    }

    private fun cacheFile(context: Context, artist: String, title: String, durationSec: Long): File {
        val key = md5("$artist|$title|$durationSec")
        return File(context.filesDir, "lrclib/$key.json")
    }

    private fun query(params: List<Pair<String, String>>): String =
        params.joinToString("&") { "${URLEncoder.encode(it.first, "UTF-8")}=${URLEncoder.encode(it.second, "UTF-8")}" }

    private fun md5(s: String): String =
        MessageDigest.getInstance("MD5").digest(s.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
