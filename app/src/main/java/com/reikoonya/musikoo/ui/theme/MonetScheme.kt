package com.reikoonya.musikoo.ui.theme

import android.graphics.Bitmap
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin

/**
 * Material You style ("Monet") color scheme built from a seed color, without third-party libraries.
 *
 * Works like Material's TonalSpot scheme: one seed hue produces five tonal palettes
 * (primary, secondary, tertiary, neutral, neutral variant) and every color role is picked
 * from a palette by tone (lightness), so contrast between pairs (color / onColor) is guaranteed
 * the same way as in the system dynamic scheme. Tones are computed in OKLab, which is close
 * to HCT and has no dependencies.
 */
private class TonalPalette(private val hue: Double, private val chroma: Double) {
    private val cache = HashMap<Int, Color>()
    fun tone(t: Int): Color = cache.getOrPut(t) { OkColor.fromTone(hue, chroma, t.toDouble()) }
}

private object OkColor {
    private fun toLinear(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    private fun toSrgb(c: Double) = if (c <= 0.0031308) c * 12.92 else 1.055 * c.pow(1 / 2.4) - 0.055

    /** sRGB int -> (L, C, hueDegrees) in OKLCH. */
    fun lch(argb: Int): DoubleArray {
        val r = toLinear(((argb shr 16) and 0xFF) / 255.0)
        val g = toLinear(((argb shr 8) and 0xFF) / 255.0)
        val b = toLinear((argb and 0xFF) / 255.0)
        val l = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
        val m = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
        val s = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
        val L = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
        val a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
        val bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
        var h = Math.toDegrees(atan2(bb, a))
        if (h < 0) h += 360.0
        return doubleArrayOf(L, hypot(a, bb), h)
    }

    /** Linear RGB for OKLCH, or null when outside the sRGB gamut. */
    private fun rgbOrNull(L: Double, C: Double, hueDeg: Double): DoubleArray? {
        val hr = Math.toRadians(hueDeg)
        val a = C * cos(hr)
        val b = C * sin(hr)
        val l = (L + 0.3963377774 * a + 0.2158037573 * b).let { it * it * it }
        val m = (L - 0.1055613458 * a - 0.0638541728 * b).let { it * it * it }
        val s = (L - 0.0894841775 * a - 1.2914855480 * b).let { it * it * it }
        val r = 4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s
        val g = -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s
        val bl = -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
        val e = 0.0005
        if (r < -e || r > 1 + e || g < -e || g > 1 + e || bl < -e || bl > 1 + e) return null
        return doubleArrayOf(r.coerceIn(0.0, 1.0), g.coerceIn(0.0, 1.0), bl.coerceIn(0.0, 1.0))
    }

    /**
     * Color of the given tone (0 = black, 100 = white) with as much of [chroma] as fits in sRGB.
     * Lightness stays exactly on the tone, only chroma is reduced for out-of-gamut colors.
     */
    fun fromTone(hue: Double, chroma: Double, tone: Double): Color {
        if (tone <= 0.0) return Color.Black
        if (tone >= 100.0) return Color.White
        val y = if (tone > 8.0) ((tone + 16.0) / 116.0).pow(3.0) else tone / 903.2963
        val L = cbrt(y)
        var rgb = rgbOrNull(L, chroma, hue)
        if (rgb == null) {
            var lo = 0.0
            var hi = chroma
            var best = rgbOrNull(L, 0.0, hue)!!
            repeat(18) {
                val mid = (lo + hi) / 2
                val c = rgbOrNull(L, mid, hue)
                if (c != null) { best = c; lo = mid } else hi = mid
            }
            rgb = best
        }
        return Color(toSrgb(rgb[0]).toFloat(), toSrgb(rgb[1]).toFloat(), toSrgb(rgb[2]).toFloat())
    }
}

/** Seed picked from the cover: the most "colorful" hue group, not the muddy average of all pixels. */
private class Seed(val hue: Double, val chroma: Double, val colorful: Boolean)

private fun seedFromBitmap(bitmap: Bitmap): Seed? = try {
    val size = 32
    val small = Bitmap.createScaledBitmap(bitmap, size, size, true)
    val bins = 24
    val binW = DoubleArray(bins)
    val binX = DoubleArray(bins)
    val binY = DoubleArray(bins)
    val binC = DoubleArray(bins)
    var totalC = 0.0
    var n = 0
    var sumX = 0.0
    var sumY = 0.0
    for (y in 0 until size) for (x in 0 until size) {
        val px = small.getPixel(x, y)
        if ((px ushr 24) < 128) continue
        val lch = OkColor.lch(px)
        n++
        totalC += lch[1]
        val hr = Math.toRadians(lch[2])
        // Almost black / almost white pixels carry no useful hue
        if (lch[0] < 0.18 || lch[0] > 0.97) continue
        val w = lch[1] * lch[1]
        sumX += cos(hr) * lch[1]; sumY += sin(hr) * lch[1]
        val bin = (floor(lch[2] / 360.0 * bins).toInt()).coerceIn(0, bins - 1)
        binW[bin] += w
        binX[bin] += cos(hr) * w
        binY[bin] += sin(hr) * w
        binC[bin] += lch[1] * w
    }
    if (n == 0) null else {
        // Neighbouring bins support each other, so one hue spread over two bins is not underestimated
        var best = 0
        var bestScore = -1.0
        for (i in 0 until bins) {
            val s = binW[i] + 0.5 * (binW[(i + 1) % bins] + binW[(i + bins - 1) % bins])
            if (s > bestScore) { bestScore = s; best = i }
        }
        var w = 0.0; var x = 0.0; var y = 0.0; var c = 0.0
        for (k in -1..1) {
            val i = (best + k + bins) % bins
            w += binW[i]; x += binX[i]; y += binY[i]; c += binC[i]
        }
        val meanChroma = if (w > 1e-9) c / w else 0.0
        if (w < 1e-9 || meanChroma < 0.035) {
            // Gray cover: keep a faint tint of its overall hue instead of a random color
            var h = Math.toDegrees(atan2(sumY, sumX)); if (h < 0) h += 360.0
            Seed(h, totalC / n, false)
        } else {
            var h = Math.toDegrees(atan2(y, x)); if (h < 0) h += 360.0
            Seed(h, meanChroma, true)
        }
    }
} catch (_: Exception) {
    null
}

private fun wrapHue(h: Double): Double = ((h % 360.0) + 360.0) % 360.0

private fun buildScheme(
    hue: Double,
    primaryChroma: Double,
    secondaryChroma: Double,
    tertiaryChroma: Double,
    neutralChroma: Double,
    neutralVariantChroma: Double,
    dark: Boolean
): ColorScheme {
    val p = TonalPalette(hue, primaryChroma)
    val s = TonalPalette(hue, secondaryChroma)
    val t = TonalPalette(wrapHue(hue + 60.0), tertiaryChroma)
    val n = TonalPalette(hue, neutralChroma)
    val nv = TonalPalette(hue, neutralVariantChroma)
    val e = TonalPalette(29.0, 0.19)

    return if (dark) {
        darkColorScheme(
            primary = p.tone(80), onPrimary = p.tone(20),
            primaryContainer = p.tone(30), onPrimaryContainer = p.tone(90),
            inversePrimary = p.tone(40),
            secondary = s.tone(80), onSecondary = s.tone(20),
            secondaryContainer = s.tone(30), onSecondaryContainer = s.tone(90),
            tertiary = t.tone(80), onTertiary = t.tone(20),
            tertiaryContainer = t.tone(30), onTertiaryContainer = t.tone(90),
            background = n.tone(6), onBackground = n.tone(90),
            surface = n.tone(6), onSurface = n.tone(90),
            surfaceVariant = nv.tone(30), onSurfaceVariant = nv.tone(80),
            surfaceTint = p.tone(80),
            inverseSurface = n.tone(90), inverseOnSurface = n.tone(20),
            error = e.tone(80), onError = e.tone(20),
            errorContainer = e.tone(30), onErrorContainer = e.tone(90),
            outline = nv.tone(60), outlineVariant = nv.tone(30),
            scrim = Color.Black,
            surfaceDim = n.tone(6), surfaceBright = n.tone(24),
            surfaceContainerLowest = n.tone(4), surfaceContainerLow = n.tone(10),
            surfaceContainer = n.tone(12), surfaceContainerHigh = n.tone(17),
            surfaceContainerHighest = n.tone(22)
        )
    } else {
        lightColorScheme(
            primary = p.tone(40), onPrimary = p.tone(100),
            primaryContainer = p.tone(90), onPrimaryContainer = p.tone(10),
            inversePrimary = p.tone(80),
            secondary = s.tone(40), onSecondary = s.tone(100),
            secondaryContainer = s.tone(90), onSecondaryContainer = s.tone(10),
            tertiary = t.tone(40), onTertiary = t.tone(100),
            tertiaryContainer = t.tone(90), onTertiaryContainer = t.tone(10),
            background = n.tone(98), onBackground = n.tone(10),
            surface = n.tone(98), onSurface = n.tone(10),
            surfaceVariant = nv.tone(90), onSurfaceVariant = nv.tone(30),
            surfaceTint = p.tone(40),
            inverseSurface = n.tone(20), inverseOnSurface = n.tone(95),
            error = e.tone(40), onError = e.tone(100),
            errorContainer = e.tone(90), onErrorContainer = e.tone(10),
            outline = nv.tone(50), outlineVariant = nv.tone(80),
            scrim = Color.Black,
            surfaceDim = n.tone(87), surfaceBright = n.tone(98),
            surfaceContainerLowest = n.tone(100), surfaceContainerLow = n.tone(96),
            surfaceContainer = n.tone(94), surfaceContainerHigh = n.tone(92),
            surfaceContainerHighest = n.tone(90)
        )
    }
}

/** Full Material You scheme from the cover art, or null if the cover cannot be read. */
fun monetSchemeFromBitmap(bitmap: Bitmap, dark: Boolean): ColorScheme? {
    val seed = seedFromBitmap(bitmap) ?: return null
    return if (seed.colorful) {
        buildScheme(
            hue = seed.hue,
            primaryChroma = seed.chroma.coerceIn(0.12, 0.20),
            secondaryChroma = 0.045,
            tertiaryChroma = 0.065,
            neutralChroma = 0.016,
            neutralVariantChroma = 0.022,
            dark = dark
        )
    } else {
        buildScheme(
            hue = seed.hue,
            primaryChroma = 0.03,
            secondaryChroma = 0.02,
            tertiaryChroma = 0.03,
            neutralChroma = 0.005,
            neutralVariantChroma = 0.008,
            dark = dark
        )
    }
}
