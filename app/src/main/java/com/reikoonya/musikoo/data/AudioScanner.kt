package com.reikoonya.musikoo.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import com.reikoonya.musikoo.model.Song
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

private const val UNKNOWN_TITLE = "Unknown Title"
private const val UNKNOWN_ARTIST = "Unknown Artist"
private const val UNKNOWN_ALBUM = "Unknown Album"
private const val ART_SIZE = 320

/**
 * Fast library scan: a single MediaStore query, without opening each file.
 * MediaStore already stores tags (title, artist, album, year, track), so MediaMetadataRetriever
 * is no longer needed per track. Covers are loaded separately, per album, see attachAlbumArt.
 * Call only from a background thread.
 */
fun scanLibrary(context: Context): List<Song> {
    val resolver = context.contentResolver
    val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    val genres = loadGenres(context)

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.YEAR,
        MediaStore.Audio.Media.COMPOSER,
        MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.DATA
    )

    val songs = ArrayList<Song>(1024)
    resolver.query(
        collection,
        projection,
        "${MediaStore.Audio.Media.IS_MUSIC} != 0",
        null,
        "${MediaStore.Audio.Media.ALBUM} COLLATE NOCASE ASC, ${MediaStore.Audio.Media.TRACK} ASC"
    )?.use { c ->
        val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
        val composerCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPOSER)
        val trackCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
        val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

        while (c.moveToNext()) {
            val id = c.getLong(idCol)
            val artist = clean(c.getString(artistCol), UNKNOWN_ARTIST)
            val year = c.getInt(yearCol)
            songs.add(
                Song(
                    id = id,
                    title = clean(c.getString(titleCol), UNKNOWN_TITLE),
                    artist = artist,
                    album = clean(c.getString(albumCol), UNKNOWN_ALBUM),
                    albumArtist = artist,
                    composer = c.getString(composerCol)?.trim().orEmpty(),
                    genre = genres[id] ?: "Unknown",
                    lyricist = "",
                    trackNumber = normalizeTrack(c.getInt(trackCol)),
                    discNumber = "1",
                    year = if (year > 0) year.toString() else "0",
                    comment = "",
                    uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                    dataPath = c.getString(dataCol).orEmpty(),
                    duration = c.getLong(durationCol),
                    size = c.getLong(sizeCol),
                    mimeType = c.getString(mimeCol) ?: "audio/mpeg",
                    bitrate = "Unknown",
                    albumArt = null,
                    albumId = c.getLong(albumIdCol)
                )
            )
        }
    }
    return songs
}

private fun clean(value: String?, fallback: String): String =
    value?.trim()?.takeIf { it.isNotEmpty() && it != "<unknown>" } ?: fallback

/** MediaStore sometimes returns the track number as disc*1000 + track (e.g. 1003); we take only the track. */
private fun normalizeTrack(raw: Int): String {
    val number = if (raw >= 1000) raw % 1000 else raw
    return if (number > 0) number.toString() else "1"
}

/** Genres live in a separate table in MediaStore; we collect them once per scan. */
private fun loadGenres(context: Context): Map<Long, String> {
    val result = HashMap<Long, String>()
    try {
        val resolver = context.contentResolver
        val genresUri = MediaStore.Audio.Genres.getContentUri(MediaStore.VOLUME_EXTERNAL)
        resolver.query(
            genresUri,
            arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME),
            null, null, null
        )?.use { genres ->
            while (genres.moveToNext()) {
                val genreId = genres.getLong(0)
                val name = genres.getString(1)?.trim().orEmpty()
                if (name.isEmpty()) continue
                val membersUri = MediaStore.Audio.Genres.Members.getContentUri(MediaStore.VOLUME_EXTERNAL, genreId)
                resolver.query(
                    membersUri,
                    arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
                    null, null, null
                )?.use { members ->
                    while (members.moveToNext()) result.putIfAbsent(members.getLong(0), name)
                }
            }
        }
    } catch (_: Exception) {
        // genres are optional: if the table is unavailable, keep "Unknown"
    }
    return result
}

/** Album art cache: LRU, limited to a share of app memory. */
object AlbumArtCache {
    // No more than 1/16 of app memory and no more than 32 MB, so covers take less space and the UI keeps enough memory
    private val maxBytes = minOf(Runtime.getRuntime().maxMemory() / 16, 32L * 1024 * 1024).toInt()

    private val cache = object : LruCache<Long, Bitmap>(maxBytes) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount
    }

    fun get(albumId: Long): Bitmap? = cache.get(albumId)

    fun put(albumId: Long, bitmap: Bitmap) {
        cache.put(albumId, bitmap)
    }
}

/**
 * Loads album art in the background: one request per album, not per track.
 * [onUpdate] is called in batches, so the list shows immediately and covers appear as they load.
 * Cancelled together with the coroutine (for example, on a repeated scan).
 */
suspend fun attachAlbumArt(
    context: Context,
    songs: List<Song>,
    onUpdate: suspend (List<Song>) -> Unit,
) {
    val resolved = HashMap<Long, Bitmap?>()
    var handled = 0
    for ((albumId, albumSongs) in songs.groupBy { it.albumId }) {
        currentCoroutineContext().ensureActive()
        if (albumId == 0L) continue // tracks without an album get no cover
        val bitmap = AlbumArtCache.get(albumId)
            ?: loadAlbumArt(context, albumSongs.first())?.also { AlbumArtCache.put(albumId, it) }
        resolved[albumId] = bitmap
        handled++
        if (handled % 40 == 0) onUpdate(withArt(songs, resolved))
    }
    currentCoroutineContext().ensureActive()
    onUpdate(withArt(songs, resolved))
}

private fun withArt(songs: List<Song>, resolved: Map<Long, Bitmap?>): List<Song> =
    songs.map { song -> if (song.albumArt != null) song else song.copy(albumArt = resolved[song.albumId]) }

private fun loadAlbumArt(context: Context, song: Song): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            return context.contentResolver.loadThumbnail(song.uri, Size(ART_SIZE, ART_SIZE), null)
        } catch (_: Exception) {
            // no system thumbnail, trying the embedded picture below
        }
    }
    return try {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, song.uri)
            retriever.embeddedPicture?.let { decodeDownsampled(it) }
        } finally {
            retriever.release()
        }
    } catch (_: Exception) {
        null
    }
}

private fun decodeDownsampled(bytes: ByteArray): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    val largest = maxOf(bounds.outWidth, bounds.outHeight)
    while (largest / (sample * 2) >= ART_SIZE) sample *= 2
    BitmapFactory.decodeByteArray(
        bytes, 0, bytes.size,
        BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
    )
} catch (_: Exception) {
    null
}
