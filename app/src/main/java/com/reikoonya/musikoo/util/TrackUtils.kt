package com.reikoonya.musikoo.util

fun parseTrackNumber(trackStr: String?): Pair<Int, Int> {
    if (trackStr.isNullOrBlank()) return Pair(1, Int.MAX_VALUE)
    val partBeforeSlash = trackStr.split("/")[0].trim()
    val num = partBeforeSlash.toIntOrNull() ?: return Pair(1, Int.MAX_VALUE)
    return if (num >= 1000) {
        Pair(num / 1000, num % 1000)
    } else {
        Pair(1, num)
    }
}

fun normalizeArtistName(rawArtist: String?): String {
    if (rawArtist.isNullOrBlank()) return "Unknown Artist"
    return rawArtist
        .replace('’', '\'')
        .replace('‘', '\'')
        .replace('`', '\'')
        .replace(Regex("\\s+"), " ")
        .trim()
}
