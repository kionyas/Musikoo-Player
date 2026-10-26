package com.reikoonya.musikoo.navigation

/** Все экраны приложения. Стек из них хранит [AppNavigator]. */
sealed class Screen {
    object MainMenu : Screen()
    object Songs : Screen()
    object Albums : Screen()
    data class AlbumDetail(val albumName: String) : Screen()
    object Artists : Screen()
    data class ArtistDetail(val artistName: String) : Screen()
    data class PlaylistDetail(val playlistId: String) : Screen()
    object Settings : Screen()
    object About : Screen()
    object Queue : Screen()
    data class Player(val songIndex: Int) : Screen()
    data class ComposerDetail(val composerName: String) : Screen()
}
