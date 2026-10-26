package com.reikoonya.musikoo.scrobble

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalStatsTest {

    private val now = System.currentTimeMillis() / 1000
    private fun daysAgo(days: Long) = now - days * 86_400L

    private fun play(ts: Long, artist: String, title: String, album: String = "Album", ms: Long = 1_000L) =
        PlayRecord(timestamp = ts, artist = artist, title = title, album = album, listenedMs = ms)

    @Test
    fun `counts plays per artist and sorts by count`() {
        val records = listOf(
            play(now, "A", "x"),
            play(now, "B", "y"),
            play(now, "B", "z"),
        )
        val stats = LocalStats.build(records, emptyList(), StatsRange.ALL)

        assertEquals(listOf("B", "A"), stats.artists.map { it.title })
        assertEquals(listOf(2, 1), stats.artists.map { it.count })
        assertEquals(3, stats.totalPlays)
    }

    @Test
    fun `listened time is summed`() {
        val records = listOf(play(now, "A", "x", ms = 1_500L), play(now, "A", "y", ms = 2_500L))
        assertEquals(4_000L, LocalStats.build(records, emptyList(), StatsRange.ALL).listenedMs)
    }

    @Test
    fun `range drops plays older than the window`() {
        val records = listOf(
            play(daysAgo(3), "Recent", "r"),
            play(daysAgo(40), "Old", "o"),
        )
        val week = LocalStats.build(records, emptyList(), StatsRange.WEEK)
        val all = LocalStats.build(records, emptyList(), StatsRange.ALL)

        assertEquals(listOf("Recent"), week.artists.map { it.title })
        assertEquals(1, week.totalPlays)
        assertEquals(2, all.totalPlays)
    }

    @Test
    fun `albums without a name are not counted`() {
        val records = listOf(play(now, "A", "x", album = ""), play(now, "A", "y", album = "Real"))
        val stats = LocalStats.build(records, emptyList(), StatsRange.ALL)
        assertEquals(listOf("Real"), stats.albums.map { it.title })
    }

    @Test
    fun `recent list is newest first`() {
        val records = listOf(play(daysAgo(2), "A", "older"), play(now, "A", "newest"))
        val stats = LocalStats.build(records, emptyList(), StatsRange.ALL)
        assertEquals("newest", stats.recent.first().title)
    }
}
