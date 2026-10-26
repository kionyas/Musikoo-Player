package com.reikoonya.musikoo.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.ui.theme.ShapeFamily
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Set by the theme. MD3 = standard rounded corners, MD3E = expressive: smooth "continuous" corners and playful shapes. */
object ShapeStyle {
    @Volatile
    var family: ShapeFamily = ShapeFamily.MD3E
}

/**
 * Shape for containers (cards, pills, sheets). Never cuts content: text stays inside.
 * MD3  = plain rounded rectangle.
 * MD3E = bigger radius with continuous (squircle-like) corners.
 */
fun shapeOf(radius: Dp): Shape = when {
    ShapeStyle.family == ShapeFamily.MD3 -> RoundedCornerShape(radius)
    radius >= 40.dp -> CircleShape
    else -> SmoothRoundedShape(radius * 1.2f, maxFraction = 0.5f, smoothing = 1f)
}

/**
 * Shape for cover art: the same plain rounded square as the player cover, in both families.
 * Never a circle or a blob: pick the radius by size (about 12 dp for 52 dp covers, 16 dp for big tiles).
 */
fun coverShapeOf(radius: Dp): Shape = RoundedCornerShape(radius)

/**
 * Decorative shape for icon badges and placeholders. MD3E gets a different playful shape per [index]
 * (cookie, sunny, clover, flower, puffy, pentagon), MD3 keeps a calm rounded square.
 * Do not use for containers with text: the outline clips content.
 */
fun expressiveBadgeShape(index: Int): Shape = when (ShapeStyle.family) {
    ShapeFamily.MD3 -> CircleShape
    ShapeFamily.MD3E -> ExpressiveShapes.badges[abs(index) % ExpressiveShapes.badges.size].toShape()
}

/** Rounded rectangle whose corners can be made "continuous" (smoother, more squared than a circular arc). */
private class SmoothRoundedShape(
    private val radius: Dp,
    private val maxFraction: Float,
    private val smoothing: Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val side = min(w, h)
        val r = min(with(density) { radius.toPx() }, side * maxFraction)
        if (r <= 0.5f) return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
        // Continuous corner: the curve starts earlier on the edge and is a little "squarer" than a circle
        val e = min(r * (1f + 0.28f * smoothing), side / 2f)
        val k = 0.5523f + 0.1f * smoothing
        val c = e * k
        val path = Path().apply {
            moveTo(e, 0f)
            lineTo(w - e, 0f)
            cubicTo(w - e + c, 0f, w, e - c, w, e)
            lineTo(w, h - e)
            cubicTo(w, h - e + c, w - e + c, h, w - e, h)
            lineTo(e, h)
            cubicTo(e - c, h, 0f, h - e + c, 0f, h - e)
            lineTo(0f, e)
            cubicTo(0f, e - c, e - c, 0f, e, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

/** Closed shape described by a radius for every angle (0..1 of the half side), sampled at [SAMPLES] points. */
class PolarShapeSpec(val radii: FloatArray) {
    fun toShape(): Shape = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val base = min(size.width, size.height) / 2f
            val path = Path()
            for (i in 0 until radii.size) {
                val t = (2.0 * PI * i / radii.size - PI / 2).toFloat()
                val x = cx + base * radii[i] * cos(t)
                val y = cy + base * radii[i] * sin(t)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            return Outline.Generic(path)
        }
    }
}

/** The set of expressive shapes (Material 3 Expressive style): also used by the morphing loading indicator. */
object ExpressiveShapes {
    const val SAMPLES = 180

    private fun lobes(n: Int, depth: Float) = PolarShapeSpec(FloatArray(SAMPLES) { i ->
        val t = (2.0 * PI * i / SAMPLES).toFloat()
        1f - depth + depth * cos(n * t)
    })

    private fun polygon(n: Int, roundness: Float) = PolarShapeSpec(FloatArray(SAMPLES) { i ->
        val t = (2.0 * PI * i / SAMPLES).toFloat()
        val sector = (2.0 * PI / n).toFloat()
        val phi = ((t + sector / 2f) % sector) - sector / 2f
        val poly = cos(sector / 2f) / cos(phi)
        poly + (1f - poly) * roundness
    })

    val circle = PolarShapeSpec(FloatArray(SAMPLES) { 1f })
    val cookie = lobes(9, 0.07f)
    val sunny = lobes(8, 0.11f)
    val clover = lobes(4, 0.2f)
    val flower = lobes(6, 0.15f)
    val puffy = lobes(5, 0.12f)
    val pentagon = polygon(5, 0.5f)
    val gem = polygon(6, 0.35f)

    /** Shapes for badges and placeholders. */
    val badges = listOf(cookie, clover, sunny, flower, puffy, pentagon)

    /** Sequence the loading indicator morphs through. */
    val loader = listOf(cookie, clover, pentagon, sunny, flower, gem, puffy, circle)
}
