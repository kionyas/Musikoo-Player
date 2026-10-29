package com.reikoonya.musikoo.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.util.BottomBarItem

val BottomBarItem.barIcon: ImageVector
    get() = when (this) {
        BottomBarItem.Search -> Icons.Rounded.Search
        BottomBarItem.Artists -> Icons.Rounded.Person
        BottomBarItem.Songs -> Icons.Rounded.MusicNote
        BottomBarItem.Albums -> Icons.Rounded.Album
        BottomBarItem.Genres -> Icons.Rounded.Category
        BottomBarItem.Random -> Icons.Rounded.Shuffle
        BottomBarItem.Pins -> Icons.Rounded.PushPin
        BottomBarItem.Playlists -> Icons.Rounded.QueueMusic
        is BottomBarItem.Genre -> Icons.Rounded.LibraryMusic
    }

@Composable
fun BottomBarItem.barTitle(): String = when (this) {
    BottomBarItem.Search -> stringResource(R.string.search)
    BottomBarItem.Artists -> stringResource(R.string.artists)
    BottomBarItem.Songs -> stringResource(R.string.songs)
    BottomBarItem.Albums -> stringResource(R.string.nav_albums)
    BottomBarItem.Genres -> stringResource(R.string.nav_genres)
    BottomBarItem.Random -> stringResource(R.string.random)
    BottomBarItem.Pins -> stringResource(R.string.nav_pins)
    BottomBarItem.Playlists -> stringResource(R.string.nav_playlists)
    is BottomBarItem.Genre -> name
}

val BottomBarItem.barDescription: String
    get() = when (this) {
        BottomBarItem.Search -> "Opens the search panel"
        BottomBarItem.Artists -> "All artists in your library"
        BottomBarItem.Songs -> "Every track, A to Z"
        BottomBarItem.Albums -> "Browse the album grid"
        BottomBarItem.Genres -> "Browse tracks by genre tag"
        BottomBarItem.Random -> "Play a random track"
        BottomBarItem.Pins -> "Pinned albums and playlists"
        BottomBarItem.Playlists -> "All playlists, with a quick create button"
        is BottomBarItem.Genre -> "Opens the $name genre"
    }
