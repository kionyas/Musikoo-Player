package com.reikoonya.musikoo.util

/**
 * Элемент нижней панели главного экрана. Хранится в настройках как строковый ключ
 * ("artists", "random", "genre:Rock"), поэтому порядок и состав панели переживают перезапуск.
 */
sealed class BottomBarItem(val key: String) {
    object Search : BottomBarItem("search")
    object Artists : BottomBarItem("artists")
    object Songs : BottomBarItem("songs")
    object Albums : BottomBarItem("albums")
    object Genres : BottomBarItem("genres")
    object Random : BottomBarItem("random")
    object Pins : BottomBarItem("pins")
    object Playlists : BottomBarItem("playlists")

    /** Быстрый переход сразу в один жанр (фильтр по тегу). */
    data class Genre(val name: String) : BottomBarItem("genre:$name")

    companion object {
        private const val GENRE_PREFIX = "genre:"

        /** Всё, что можно добавить на панель без привязки к библиотеке. */
        val builtIn: List<BottomBarItem> = listOf(Search, Artists, Songs, Albums, Genres, Random, Pins, Playlists)

        fun genreKey(name: String): String = GENRE_PREFIX + name

        /** Возвращает элемент по ключу или null, если ключ устарел или повреждён. */
        fun fromKey(key: String): BottomBarItem? = when {
            key.startsWith(GENRE_PREFIX) ->
                key.removePrefix(GENRE_PREFIX).trim().takeIf { it.isNotEmpty() }?.let { Genre(it) }
            else -> builtIn.firstOrNull { it.key == key }
        }
    }
}
