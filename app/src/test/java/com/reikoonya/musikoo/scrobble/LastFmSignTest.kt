package com.reikoonya.musikoo.scrobble

import org.junit.Assert.assertEquals
import org.junit.Test
import java.security.MessageDigest

class LastFmSignTest {

    private fun md5(s: String) = MessageDigest.getInstance("MD5")
        .digest(s.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    @Test
    fun `parameters are sorted by name before hashing`() {
        // По правилам Last.fm: имя+значение по алфавиту, затем секрет
        val expected = md5("a1b2" + LastFmConfig.API_SECRET)
        assertEquals(expected, LastFmClient.sign(mapOf("b" to "2", "a" to "1")))
    }

    @Test
    fun `same parameters in another order give the same signature`() {
        val first = LastFmClient.sign(mapOf("track" to "x", "artist" to "y", "method" to "m"))
        val second = LastFmClient.sign(mapOf("method" to "m", "artist" to "y", "track" to "x"))
        assertEquals(first, second)
    }
}
