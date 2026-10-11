package com.reikoonya.musikoo.model

import android.graphics.Bitmap
import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val composer: String,
    val genre: String,
    val lyricist: String,
    val trackNumber: String,
    val discNumber: String,
    val year: String,
    val comment: String,
    val uri: Uri,
    val dataPath: String,
    val duration: Long,
    val size: Long,
    val mimeType: String,
    val bitrate: String,
    val albumArt: Bitmap?,
    val albumId: Long = 0L
)
