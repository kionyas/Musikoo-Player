package com.reikoonya.musikoo.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.model.Song

object ShareUtils {
    /**
     * Shares one or multiple songs using their MediaStore URIs without copying files.
     */
    fun shareSongs(context: Context, songs: List<Song>, title: String? = null) {
        if (songs.isEmpty()) return

        if (songs.size == 1) {
            val song = songs.first()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = song.mimeType.ifEmpty { "audio/*" }
                putExtra(Intent.EXTRA_STREAM, song.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // ClipData carries the read grant through the chooser to the receiving app
                clipData = ClipData.newUri(context.contentResolver, song.title, song.uri)
            }
            val chooserTitle = title ?: song.title
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        } else {
            val uris = ArrayList(songs.map { it.uri })
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "audio/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                clipData = ClipData.newUri(context.contentResolver, "Music", uris.first()).apply {
                    uris.drop(1).forEach { addItem(ClipData.Item(it)) }
                }
            }
            val chooserTitle = title ?: context.getString(R.string.share)
            context.startActivity(Intent.createChooser(intent, chooserTitle))
        }
    }
}
