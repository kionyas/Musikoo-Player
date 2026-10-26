package com.reikoonya.musikoo.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun `compares numeric parts, not strings`() {
        // Строковое сравнение дало бы "1.9.2" > "1.10.0"
        assertTrue(isNewerVersion("1.10.0", "1.9.2"))
        assertFalse(isNewerVersion("1.9.2", "1.10.0"))
    }

    @Test
    fun `missing parts count as zero`() {
        assertFalse(isNewerVersion("1.4", "1.4.0"))
        assertFalse(isNewerVersion("1.4.0", "1.4"))
        assertTrue(isNewerVersion("1.4.1", "1.4"))
    }

    @Test
    fun `strips v prefix and suffixes`() {
        assertTrue(isNewerVersion("v1.5", "1.4.9"))
        assertTrue(isNewerVersion("2.0.0-beta", "1.9.9"))
        assertFalse(isNewerVersion("v1.4.2", "1.4.2"))
    }
}
