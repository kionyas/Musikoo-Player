package com.reikoonya.musikoo.ui.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import com.reikoonya.musikoo.data.AlbumArtCache
import com.reikoonya.musikoo.model.Song

/**
 * Обложка песни. Если её уже нет в памяти, грузит в фоне и перерисовывает строку, когда она придёт.
 * Пока обложки нет, возвращает null, и экран показывает заглушку.
 */
@Composable
fun rememberSongArt(song: Song?): Bitmap? {
    val context = LocalContext.current.applicationContext
    val version = AlbumArtCache.overrideVersion
    // The key tells which song the loaded cover belongs to, so a recycled list row never shows the previous cover
    val ownerKey = song?.let { "${it.id}:${it.albumId}:$version" }
    val inMemory = song?.let { AlbumArtCache.peek(it) }
    val loaded by produceState<Pair<String?, Bitmap?>>(null to null, ownerKey) {
        if (song == null) return@produceState
        value = ownerKey to AlbumArtCache.load(context, song)
    }
    return inMemory ?: loaded.takeIf { it.first == ownerKey }?.second
}
