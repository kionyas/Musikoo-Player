package com.reikoonya.musikoo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Running text (marquee) with fading edges that follow the movement.
 *
 * - Text that fits is static and has no fade at all.
 * - Long text waits, runs to the end, pauses, and returns to the start.
 * - The left edge fades only while the text has already moved away from the start,
 *   the right edge fades only while there is still hidden text on that side.
 *
 * Replaces basicMarquee() + a constant edge fade: use it alone, without basicMarquee().
 */
fun Modifier.marqueeEdgeFade(
    width: Dp = 16.dp,
    velocity: Dp = 38.dp,
    initialDelayMillis: Int = 1400,
    endPauseMillis: Int = 900,
): Modifier = composed {
    var contentWidth by remember { mutableIntStateOf(0) }
    var viewWidth by remember { mutableIntStateOf(0) }
    val offset = remember { Animatable(0f) }
    val velocityPx = with(LocalDensity.current) { velocity.toPx() }

    LaunchedEffect(contentWidth, viewWidth, velocityPx) {
        offset.snapTo(0f)
        val distance = (contentWidth - viewWidth).toFloat()
        if (distance <= 0f) return@LaunchedEffect
        while (true) {
            delay(initialDelayMillis.toLong())
            offset.animateTo(
                distance,
                tween((distance / velocityPx * 1000f).toInt().coerceAtLeast(500), easing = LinearEasing)
            )
            delay(endPauseMillis.toLong())
            offset.animateTo(0f, tween(550, easing = FastOutSlowInEasing))
        }
    }

    this
        .clipToBounds()
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val distance = contentWidth - size.width
            if (distance > 0f) {
                val fade = width.toPx().coerceAtMost(size.width / 2f)
                // 0..1: how much of the edge is currently "eaten" by hidden text
                val left = (offset.value / fade).coerceIn(0f, 1f)
                val right = ((distance - offset.value) / fade).coerceIn(0f, 1f)
                if (left > 0f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = 1f - left), Color.Black),
                            startX = 0f,
                            endX = fade
                        ),
                        size = Size(fade, size.height),
                        blendMode = BlendMode.DstIn
                    )
                }
                if (right > 0f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(Color.Black, Color.Black.copy(alpha = 1f - right)),
                            startX = size.width - fade,
                            endX = size.width
                        ),
                        topLeft = Offset(size.width - fade, 0f),
                        size = Size(fade, size.height),
                        blendMode = BlendMode.DstIn
                    )
                }
            }
        }
        .layout { measurable, constraints ->
            // Measure the text at its full width, show only the visible window of it
            val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
            val w = if (constraints.hasBoundedWidth) {
                placeable.width.coerceIn(constraints.minWidth, constraints.maxWidth)
            } else {
                placeable.width
            }
            if (contentWidth != placeable.width) contentWidth = placeable.width
            if (viewWidth != w) viewWidth = w
            layout(w, placeable.height) {
                placeable.placeRelative(-offset.value.roundToInt(), 0)
            }
        }
}
