package com.reikoonya.musikoo.util

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.util.Log
import android.os.Build
import com.reikoonya.musikoo.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.images.ArtworkFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

/** Fields the tag editor can edit. */
enum class TagField { TITLE, ARTIST, ALBUM, ALBUM_ARTIST, COMPOSER, LYRICIST, GENRE, YEAR, TRACK, DISC, COMMENT }

/** What to do with the cover. */
sealed interface CoverChange {
    object Keep : CoverChange
    object Remove : CoverChange
    class Replace(val jpeg: ByteArray, val preview: Bitmap) : CoverChange
}

sealed interface WriteResult {
    class Updated(val song: Song) : WriteResult

    /** The user must confirm in the system dialog (Android 10). */
    class NeedsPermission(val intentSender: IntentSender) : WriteResult

    class Failed(val reason: String) : WriteResult
}

/**
 * Reads and writes tags in audio files via jaudiotagger.
 *
 * How it works on modern Android (scoped storage): the file is copied to a temp folder,
 * tags are edited in the copy, then the finished file is written back via ContentResolver
 * (after the user allows writing in the system dialog).
 */
object TagWriter {

    init {
        // jaudiotagger is very chatty, so turn its logs off
        Logger.getLogger("org.jaudiotagger").level = Level.OFF
    }

    private val supportedExtensions = setOf("mp3", "m4a", "mp4", "m4b", "flac", "ogg", "oga", "wav", "wma", "aif", "aiff")

    // ---------------- reading ----------------

    /**
     * Initial values for the editor. For a single track we read all the tags,
     * for several tracks: quickly, and if values differ between tracks, return null.
     */
    suspend fun loadInitialValues(context: Context, songs: List<Song>): Map<TagField, String?> =
        withContext(Dispatchers.IO) {
            if (songs.size == 1) {
                readFull(context, songs[0]) ?: readQuick(context, songs[0])
            } else {
                val all = songs.map { readQuick(context, it) }
                TagField.values().associateWith { field ->
                    val first = all.firstOrNull()?.get(field).orEmpty()
                    if (all.all { it[field].orEmpty() == first }) first else null
                }
            }
        }

    private fun readFull(context: Context, song: Song): Map<TagField, String>? {
        val ext = extensionFor(song) ?: return null
        val temp = File(context.cacheDir, "tagread_${song.id}.$ext")
        return try {
            val input = context.contentResolver.openInputStream(song.uri) ?: return null
            input.use { src -> temp.outputStream().use { dst -> src.copyTo(dst) } }
            val tag = AudioFileIO.read(temp).tag
            TagField.values().associateWith { field ->
                if (tag == null) "" else safeGet(tag, fieldKey(field))
            }
        } catch (_: Throwable) {
            null
        } finally {
            temp.delete()
        }
    }

    private fun safeGet(tag: Tag, key: FieldKey): String =
        try {
            tag.getFirst(key) ?: ""
        } catch (_: Exception) {
            ""
        }

    private fun readQuick(context: Context, song: Song): Map<TagField, String> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, song.uri)
            fun meta(key: Int): String = retriever.extractMetadata(key).orEmpty()
            mapOf(
                TagField.TITLE to meta(MediaMetadataRetriever.METADATA_KEY_TITLE),
                TagField.ARTIST to meta(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                TagField.ALBUM to meta(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                TagField.ALBUM_ARTIST to meta(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                TagField.COMPOSER to meta(MediaMetadataRetriever.METADATA_KEY_COMPOSER),
                TagField.LYRICIST to meta(MediaMetadataRetriever.METADATA_KEY_WRITER),
                TagField.GENRE to meta(MediaMetadataRetriever.METADATA_KEY_GENRE),
                TagField.YEAR to meta(MediaMetadataRetriever.METADATA_KEY_YEAR),
                TagField.TRACK to meta(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER),
                TagField.DISC to meta(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER),
                TagField.COMMENT to ""
            )
        } catch (_: Exception) {
            fromSong(song)
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    private fun fromSong(song: Song): Map<TagField, String> {
        fun clean(value: String, vararg placeholders: String) =
            if (value in placeholders) "" else value
        return mapOf(
            TagField.TITLE to clean(song.title, "Unknown Title"),
            TagField.ARTIST to clean(song.artist, "Unknown Artist"),
            TagField.ALBUM to clean(song.album, "Unknown Album"),
            TagField.ALBUM_ARTIST to clean(song.albumArtist, "Unknown Artist"),
            TagField.COMPOSER to song.composer,
            TagField.LYRICIST to song.lyricist,
            TagField.GENRE to clean(song.genre, "Unknown"),
            TagField.YEAR to clean(song.year, "0"),
            TagField.TRACK to song.trackNumber,
            TagField.DISC to song.discNumber,
            TagField.COMMENT to song.comment
        )
    }

    // ---------------- writing ----------------

    /** Writes changes to one file. Returns the updated track model. */
    suspend fun writeSong(
        context: Context,
        song: Song,
        changes: Map<TagField, String>,
        cover: CoverChange,
    ): WriteResult = withContext(Dispatchers.IO) {
        val ext = extensionFor(song)
            ?: return@withContext WriteResult.Failed("file format not supported")
        val resolver = context.contentResolver
        val temp = File(context.cacheDir, "tagwrite_${song.id}.$ext")

        try {
            // 1. copy the file
            val input = resolver.openInputStream(song.uri)
                ?: return@withContext WriteResult.Failed("could not open the file")
            input.use { src -> temp.outputStream().use { dst -> src.copyTo(dst) } }

            // 2. edit the tags in the copy
            val audioFile = AudioFileIO.read(temp)
            val tag = audioFile.tagOrCreateAndSetDefault
            applyChanges(tag, changes, cover)
            audioFile.commit()

            // 3. write the finished file back
            try {
                val out = resolver.openOutputStream(song.uri, "wt")
                    ?: return@withContext WriteResult.Failed("no write access")
                out.use { dst -> temp.inputStream().use { src -> src.copyTo(dst) } }
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                    return@withContext WriteResult.NeedsPermission(e.userAction.actionIntent.intentSender)
                }
                return@withContext WriteResult.Failed("no write permission")
            }

            // 4. ask the system to re-read the tags (so they are up to date after a restart too)
            if (song.dataPath.isNotBlank()) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(song.dataPath),
                    arrayOf(song.mimeType),
                    null
                )
            }

            WriteResult.Updated(updatedSong(song, changes, cover, temp.length()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.e("TagWriter", "Tag write failed for ${song.dataPath}", e)
            WriteResult.Failed(e.message ?: e.javaClass.simpleName)
        } finally {
            temp.delete()
        }
    }

    private fun applyChanges(tag: Tag, changes: Map<TagField, String>, cover: CoverChange) {
        for ((field, raw) in changes) {
            val key = fieldKey(field)
            val value = raw.trim()
            if (value.isEmpty()) {
                try { tag.deleteField(key) } catch (_: Exception) {}
            } else {
                tag.setField(key, value)
            }
        }

        when (cover) {
            is CoverChange.Replace -> {
                try { tag.deleteArtworkField() } catch (_: Exception) {}
                val artwork = ArtworkFactory.getNew()
                artwork.setBinaryData(cover.jpeg)
                artwork.setMimeType("image/jpeg")
                artwork.setPictureType(3) // 3 = front cover
                artwork.setDescription("")
                tag.setField(artwork)
            }
            CoverChange.Remove -> {
                try { tag.deleteArtworkField() } catch (_: Exception) {}
            }
            CoverChange.Keep -> Unit
        }
    }

    /** New track model for the UI: the same values that were written to the file. */
    private fun updatedSong(song: Song, changes: Map<TagField, String>, cover: CoverChange, newSize: Long): Song {
        fun pick(field: TagField, current: String, whenEmpty: String): String {
            val changed = changes[field]?.trim() ?: return current
            return changed.ifEmpty { whenEmpty }
        }
        return song.copy(
            title = pick(TagField.TITLE, song.title, "Unknown Title"),
            artist = pick(TagField.ARTIST, song.artist, "Unknown Artist"),
            album = pick(TagField.ALBUM, song.album, "Unknown Album"),
            albumArtist = pick(TagField.ALBUM_ARTIST, song.albumArtist, ""),
            composer = pick(TagField.COMPOSER, song.composer, ""),
            lyricist = pick(TagField.LYRICIST, song.lyricist, ""),
            genre = pick(TagField.GENRE, song.genre, "Unknown"),
            year = pick(TagField.YEAR, song.year, "0"),
            trackNumber = pick(TagField.TRACK, song.trackNumber, "1"),
            discNumber = pick(TagField.DISC, song.discNumber, "1"),
            comment = pick(TagField.COMMENT, song.comment, ""),
            size = if (newSize > 0) newSize else song.size,
            albumArt = when (cover) {
                is CoverChange.Replace -> cover.preview
                CoverChange.Remove -> null
                CoverChange.Keep -> song.albumArt
            }
        )
    }

    private fun fieldKey(field: TagField): FieldKey = when (field) {
        TagField.TITLE -> FieldKey.TITLE
        TagField.ARTIST -> FieldKey.ARTIST
        TagField.ALBUM -> FieldKey.ALBUM
        TagField.ALBUM_ARTIST -> FieldKey.ALBUM_ARTIST
        TagField.COMPOSER -> FieldKey.COMPOSER
        TagField.LYRICIST -> FieldKey.LYRICIST
        TagField.GENRE -> FieldKey.GENRE
        TagField.YEAR -> FieldKey.YEAR
        TagField.TRACK -> FieldKey.TRACK
        TagField.DISC -> FieldKey.DISC_NO
        TagField.COMMENT -> FieldKey.COMMENT
    }

    /** jaudiotagger needs the extension to pick the format. Take it from the path, otherwise from the MIME type. */
    private fun extensionFor(song: Song): String? {
        val fromPath = song.dataPath.substringAfterLast('.', "").lowercase()
        if (fromPath in supportedExtensions) return fromPath
        return when (song.mimeType.lowercase()) {
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/mp4", "audio/x-m4a", "audio/m4a" -> "m4a"
            "audio/flac", "audio/x-flac" -> "flac"
            "audio/ogg", "application/ogg" -> "ogg"
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/x-ms-wma" -> "wma"
            else -> null
        }
    }

    // ---------------- cover ----------------

    /** Reads the image, downscales it to a reasonable size and prepares a JPEG to write to the file. */
    suspend fun loadCover(context: Context, uri: Uri): CoverChange.Replace? =
        withContext(Dispatchers.IO) {
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, bounds)
                }
                var sample = 1
                while (bounds.outWidth / sample > 1400 || bounds.outHeight / sample > 1400) sample *= 2

                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                } ?: return@withContext null

                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                CoverChange.Replace(out.toByteArray(), bitmap)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
        }
}
