package com.reikoonya.musikoo.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.LruCache
import android.util.Size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.reikoonya.musikoo.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private const val ART_SIZE = 320

/**
 * Обложки по альбомам. Песни сами не хранят Bitmap: экран просит обложку по альбому,
 * и она грузится по требованию. Раньше каждая песня держала свою ссылку, поэтому
 * LRU-кэш не мог вытеснять старые обложки, и память росла с размером библиотеки.
 */
object AlbumArtCache {
    // No more than 1/16 of app memory and no more than 32 MB, so the UI keeps enough memory
    private val maxBytes = minOf(Runtime.getRuntime().maxMemory() / 16, 32L * 1024 * 1024).toInt()

    private val cache = object : LruCache<Long, Bitmap>(maxBytes) {
        override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount
    }

    /** Альбомы без обложки: их не пытаемся декодировать снова на каждой строке списка. */
    private val missing = ConcurrentHashMap.newKeySet<Long>()

    /** Обложки песен, которые меняли в редакторе тегов. Ограничены LRU-кэшем и уменьшены до 512px. */
    private object NoCover
    private val overrides = object : LruCache<Long, Any>(16) {
        override fun sizeOf(key: Long, value: Any): Int = if (value is Bitmap) value.byteCount / 1024 else 1
    }

    /** По одному декодеру за раз: не грузим много больших картинок одновременно. */
    private val loadLock = Mutex()

    /** Меняется при изменении ручных обложек, чтобы экраны перечитали их. */
    var overrideVersion by mutableIntStateOf(0)
        private set

    /** Обложка, если она уже в памяти. Ничего не грузит, поэтому безопасно вызывать откуда угодно. */
    fun peek(song: Song): Bitmap? {
        val override = overrides.get(song.id)
        if (override != null) return override as? Bitmap
        if (song.albumId == 0L) return null
        return cache.get(song.albumId)
    }

    /** Грузит обложку (с диска или из тегов) и кладёт в кэш. Вызывать из корутины. */
    suspend fun load(context: Context, song: Song): Bitmap? {
        peek(song)?.let { return it }
        if (overrides.get(song.id) != null) return null
        val albumId = song.albumId
        if (albumId == 0L || missing.contains(albumId)) return null
        return loadLock.withLock {
            cache.get(albumId) ?: run {
                val bitmap = withContext(Dispatchers.IO) { loadAlbumArt(context, song) }
                if (bitmap != null) {
                    cache.put(albumId, bitmap)
                } else {
                    missing.add(albumId)
                }
                bitmap
            }
        }
    }

    /** Ручная обложка после редактирования тегов: [bitmap] = null значит, что обложку убрали. */
    fun setOverride(songId: Long, bitmap: Bitmap?) {
        val stored: Any = if (bitmap == null) NoCover else {
            val maxSide = maxOf(bitmap.width, bitmap.height)
            if (maxSide > 512) {
                val scale = 512f / maxSide
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                bitmap
            }
        }
        overrides.put(songId, stored)
        overrideVersion++
    }

    /** Перед новым сканированием: альбом мог получить обложку (например, после правки файла). */
    fun forgetMissing() {
        missing.clear()
    }
}

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
