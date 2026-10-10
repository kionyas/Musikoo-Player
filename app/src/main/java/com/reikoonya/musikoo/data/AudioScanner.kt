package com.reikoonya.musikoo.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import com.reikoonya.musikoo.model.Song

fun loadAudioFiles(context: Context): List<Song> {
    val songList = mutableListOf<Song>()
    val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.YEAR,
        MediaStore.Audio.Media.COMPOSER,
        MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.DATA
    )

    val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

    context.contentResolver.query(
        collection,
        projection,
        "${MediaStore.Audio.Media.IS_MUSIC} != 0",
        null,
        sortOrder
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
        val composerCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPOSER)
        val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
        val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

        val retriever = MediaMetadataRetriever()

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
            val dataPath = cursor.getString(dataCol) ?: ""

            var title = cursor.getString(titleCol) ?: "Unknown Title"
            var artist = cursor.getString(artistCol) ?: "Unknown Artist"
            val album = cursor.getString(albumCol) ?: "Unknown Album"
            val year = cursor.getString(yearCol) ?: "0"
            val composer = cursor.getString(composerCol) ?: ""
            var trackNum = cursor.getString(trackCol) ?: "1"

            val duration = cursor.getLong(durationCol)
            val size = cursor.getLong(sizeCol)
            val mimeType = cursor.getString(mimeCol) ?: "audio/mpeg"

            var bitrate = "Unknown"
            var albumArt: Bitmap? = null

            try {
                retriever.setDataSource(context, contentUri)
                val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val metaArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val metaBitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                val metaTrack = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)

                if (!metaTitle.isNullOrBlank()) title = metaTitle
                if (!metaArtist.isNullOrBlank()) artist = metaArtist
                if (!metaTrack.isNullOrBlank()) trackNum = metaTrack
                if (!metaBitrate.isNullOrBlank()) {
                    val kbps = metaBitrate.toIntOrNull()?.let { it / 1000 } ?: 0
                    if (kbps > 0) bitrate = "$kbps kbps"
                }

                val artBytes = retriever.embeddedPicture
                if (artBytes != null) {
                    albumArt = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                }
            } catch (_: Exception) {}

            songList.add(
                Song(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtist = artist,
                    composer = composer,
                    genre = "Unknown",
                    lyricist = "",
                    trackNumber = trackNum,
                    discNumber = "1",
                    year = year,
                    comment = "",
                    uri = contentUri,
                    dataPath = dataPath,
                    duration = duration,
                    size = size,
                    mimeType = mimeType,
                    bitrate = bitrate,
                    albumArt = albumArt
                )
            )
        }
        try { retriever.release() } catch (_: Exception) {}
    }
    return songList
}
