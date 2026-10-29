package com.reikoonya.musikoo.util

import com.reikoonya.musikoo.model.Song

/** Жанр-тег трека может содержать несколько значений через «;» (так MediaStore отдаёт многозначные теги). */
fun Song.genreTags(): List<String> =
    genre.split(';', '\u0000')
        .map { it.trim() }
        .filter {
            it.isNotBlank() &&
                !it.equals("Unknown", ignoreCase = true) &&
                !it.equals("<unknown>", ignoreCase = true)
        }
        .distinctBy { it.lowercase() }

/** Есть ли у трека этот жанр (без учёта регистра). */
fun Song.hasGenre(name: String): Boolean = genreTags().any { it.equals(name, ignoreCase = true) }

class GenreGroup(val name: String, val songs: List<Song>)

/** Все жанры библиотеки вместе с треками, A–Z. Трек с несколькими тегами попадает в каждый из них. */
fun groupByGenre(songs: List<Song>): List<GenreGroup> {
    val map = LinkedHashMap<String, Pair<String, MutableList<Song>>>()
    for (song in songs) {
        for (tag in song.genreTags()) {
            map.getOrPut(tag.lowercase()) { tag to mutableListOf() }.second.add(song)
        }
    }
    return map.values
        .map { GenreGroup(it.first, it.second) }
        .sortedBy { it.name.lowercase() }
}
