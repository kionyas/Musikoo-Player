package com.reikoonya.musikoo.util

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class TimeUtilsTest {

    @Before
    fun setLocale() {
        // Цифры в форматировании не должны зависеть от региона машины, на которой идут тесты
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `formats minutes and seconds`() {
        assertEquals("0:00", formatTime(0))
        assertEquals("1:05", formatTime(65_000))
        assertEquals("3:59", formatTime(239_999))
    }

    @Test
    fun `long tracks keep counting minutes`() {
        assertEquals("60:00", formatTime(3_600_000))
    }
}
