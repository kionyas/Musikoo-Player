package com.reikoonya.musikoo.navigation

/** Все экраны приложения. Стек из них хранит [AppNavigator]. */
sealed class Screen {
    object MainMenu : Screen()
    object Songs : Screen()
    object Albums : Screen()
    data class AlbumDetail(val albumName: String) : Screen()
    object Artists : Screen()
    object Genres : Screen()
    object Pins : Screen()
    object Playlists : Screen()
    /** Экран выбора песен для плейлиста. */
    data class PickSongs(val playlistId: String) : Screen()
    /** Шторка создания нового плейлиста. */
    object CreatePlaylist : Screen()
    data class GenreDetail(val genreName: String) : Screen()
    data class ArtistDetail(val artistName: String) : Screen()
    data class PlaylistDetail(val playlistId: String) : Screen()
    object Settings : Screen()
    object About : Screen()
    object Queue : Screen()
    data class Player(val songIndex: Int) : Screen()
    data class ComposerDetail(val composerName: String) : Screen()
}
