package com.reikoonya.musikoo.util

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import android.graphics.Color as AndroidColor

/**
 * Extracts an accent color from the cover (no third-party libraries).
 * Bright, saturated pixels weigh more than gray ones, so the color comes out vivid.
 * For gray covers it returns null, and the theme color is used instead.
 */
fun accentFromBitmap(bitmap: Bitmap, dark: Boolean): Color? {
    return try {
        val size = 24
        val small = Bitmap.createScaledBitmap(bitmap, size, size, true)
        val hsv = FloatArray(3)
        var r = 0.0
        var g = 0.0
        var b = 0.0
        var total = 0.0

        for (y in 0 until size) {
            for (x in 0 until size) {
                val c = small.getPixel(x, y)
                AndroidColor.colorToHSV(c, hsv)
                val sv = (hsv[1] * hsv[2]).toDouble()
                val w = sv * sv + 1e-6
                r += AndroidColor.red(c) * w
                g += AndroidColor.green(c) * w
                b += AndroidColor.blue(c) * w
                total += w
            }
        }

        val avg = AndroidColor.rgb((r / total).toInt(), (g / total).toInt(), (b / total).toInt())
        AndroidColor.colorToHSV(avg, hsv)
        if (hsv[1] < 0.12f) return null

        hsv[1] = hsv[1].coerceIn(0.45f, 0.85f)
        hsv[2] = if (dark) 0.95f else 0.55f
        Color(AndroidColor.HSVToColor(hsv))
    } catch (_: Exception) {
        null
    }
}

/** Picks white or dark text/icon color over [background] by contrast. */
fun readableOn(background: Color): Color {
    val l = background.luminance()
    val whiteContrast = 1.05f / (l + 0.05f)
    val blackContrast = (l + 0.05f) / 0.05f
    return if (blackContrast >= whiteContrast) Color(0xFF14121A) else Color.White
}
