package com.reikoonya.musikoo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BottomBarItemTest {
    @Test
    fun builtInKeysRoundTrip() {
        for (item in BottomBarItem.builtIn) {
            assertEquals(item, BottomBarItem.fromKey(item.key))
        }
    }

    @Test
    fun pinsAndPlaylistsAreBuiltIn() {
        assertEquals(BottomBarItem.Pins, BottomBarItem.fromKey("pins"))
        assertEquals(BottomBarItem.Playlists, BottomBarItem.fromKey("playlists"))
    }

    @Test
    fun genreKeyRoundTrip() {
        val item = BottomBarItem.fromKey(BottomBarItem.genreKey("Hip-Hop"))
        assertEquals(BottomBarItem.Genre("Hip-Hop"), item)
    }

    @Test
    fun unknownOrEmptyKeysAreRejected() {
        assertNull(BottomBarItem.fromKey("nope"))
        assertNull(BottomBarItem.fromKey("genre:"))
        assertNull(BottomBarItem.fromKey("genre:   "))
    }
}
