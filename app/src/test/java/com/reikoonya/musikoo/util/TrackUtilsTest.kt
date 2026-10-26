package com.reikoonya.musikoo.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackUtilsTest {

    @Test
    fun `plain track number`() {
        assertEquals(Pair(1, 3), parseTrackNumber("3"))
    }

    @Test
    fun `track number with total is read before the slash`() {
        assertEquals(Pair(1, 3), parseTrackNumber("3/12"))
    }

    @Test
    fun `disc and track packed into one number`() {
        // 2003 = диск 2, трек 3
        assertEquals(Pair(2, 3), parseTrackNumber("2003"))
    }

    @Test
    fun `missing or broken track number goes last`() {
        assertEquals(Pair(1, Int.MAX_VALUE), parseTrackNumber(null))
        assertEquals(Pair(1, Int.MAX_VALUE), parseTrackNumber("  "))
        assertEquals(Pair(1, Int.MAX_VALUE), parseTrackNumber("abc"))
    }

    @Test
    fun `artist names are normalized`() {
        assertEquals("Foo's Bar", normalizeArtistName("  Foo’s   Bar  "))
        assertEquals("Ain't", normalizeArtistName("Ain`t"))
    }

    @Test
    fun `blank artist becomes unknown`() {
        assertEquals("Unknown Artist", normalizeArtistName(null))
        assertEquals("Unknown Artist", normalizeArtistName("   "))
    }
}
