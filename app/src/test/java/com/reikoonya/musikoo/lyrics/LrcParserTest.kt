package com.reikoonya.musikoo.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun `reads timestamps with fractions`() {
        val lines = LrclibClient.parseLrc("[00:01.50]First line")
        assertEquals(1, lines.size)
        assertEquals(1_500L, lines[0].startMs)
        assertEquals("First line", lines[0].text)
    }

    @Test
    fun `one text line with several timestamps becomes several lines`() {
        val lines = LrclibClient.parseLrc("[00:05.00][00:10.00]Chorus")
        assertEquals(listOf(5_000L, 10_000L), lines.map { it.startMs })
        assertEquals(listOf("Chorus", "Chorus"), lines.map { it.text })
    }

    @Test
    fun `lines come out sorted by time and each ends where the next starts`() {
        val lrc = """
            [01:02.345]Third
            [00:01.00]First
            [00:05.00]Second
        """.trimIndent()
        val lines = LrclibClient.parseLrc(lrc)

        assertEquals(listOf("First", "Second", "Third"), lines.map { it.text })
        assertEquals(1_000L, lines[0].startMs)
        assertEquals(5_000L, lines[1].startMs)
        assertEquals(62_345L, lines[2].startMs)
        // Конец строки — начало следующей; у последней — +4 секунды
        assertEquals(lines[1].startMs, lines[0].endMs)
        assertEquals(lines[2].startMs + 4_000L, lines[2].endMs)
    }

    @Test
    fun `lines without timestamps are ignored`() {
        val lines = LrclibClient.parseLrc("[ar: Artist]\n[00:02.00]Only this")
        assertEquals(1, lines.size)
        assertTrue(lines[0].text == "Only this")
    }
}
