package com.reikoonya.musikoo.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

// ---------- shared wave parts ----------

/** Wave amplitude: 1 while playing, 0 when paused (with a spring-based settle). */
@Composable
private fun rememberWaveAmplitude(isPlaying: Boolean): State<Float> =
    animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "waveAmplitude"
    )

/** The wave phase moves only while music plays; no frames are spent when paused. */
@Composable
private fun rememberWavePhase(isPlaying: Boolean): State<Float> {
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val twoPi = (2 * PI).toFloat()
            var last = System.nanoTime()
            while (true) {
                // ~30 frames per second: the wave looks the same but redraws 3–4 times less often
                delay(33)
                val now = System.nanoTime()
                val dt = (now - last) / 1_000_000_000f
                phase.floatValue = (phase.floatValue + dt * twoPi / 1.6f) % twoPi // one cycle per 1.6 s
                last = now
            }
        }
    }
    return phase
}

/** Draws the track: the played part as a wave, the rest as a straight line, optionally a thumb. */
private fun DrawScope.drawWavyTrack(
    fraction: Float,
    phase: Float,
    amplitudePx: Float,
    wavelengthPx: Float,
    strokePx: Float,
    gapPx: Float,
    activeColor: Color,
    trackColor: Color,
    thumbWidthPx: Float = 0f,
    thumbHeightPx: Float = 0f,
) {
    val w = size.width
    val cy = size.height / 2f
    val x = (w * fraction).coerceIn(0f, w)
    val k = (2 * PI / wavelengthPx).toFloat()

    // Unplayed part: straight line
    val inactiveStart = (x + gapPx).coerceAtMost(w)
    if (inactiveStart < w) {
        drawLine(
            color = trackColor,
            start = Offset(inactiveStart, cy),
            end = Offset(w, cy),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
    }

    // Played part: wave moving right
    val activeEnd = (x - gapPx).coerceAtLeast(0f)
    if (activeEnd > 0f) {
        val path = Path()
        val step = 2.dp.toPx()
        var px = 0f
        path.moveTo(0f, cy + amplitudePx * sin(-phase))
        while (px < activeEnd) {
            px = min(px + step, activeEnd)
            path.lineTo(px, cy + amplitudePx * sin(k * px - phase))
        }
        drawPath(
            path = path,
            color = activeColor,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }

    // Pill-shaped thumb
    if (thumbWidthPx > 0f && thumbHeightPx > 0f) {
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(x - thumbWidthPx / 2f, cy - thumbHeightPx / 2f),
            size = Size(thumbWidthPx, thumbHeightPx),
            cornerRadius = CornerRadius(thumbWidthPx / 2f)
        )
    }
}

// ---------- slider (player screen) ----------

/**
 * Progress slider in Material 3 Expressive style: the played part is a moving wave,
 * on pause the wave smoothly (springily) flattens. Thumb: a vertical pill.
 * Uses stable APIs, so no Material3 update is needed.
 */
@Composable
fun WavySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentRange by rememberUpdatedState(valueRange)
    val amplitude = rememberWaveAmplitude(isPlaying)
    val phase = rememberWavePhase(isPlaying)

    var widthPx by remember { mutableFloatStateOf(1f) }
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-3f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    fun xToValue(x: Float): Float {
        val r = currentRange
        return r.start + (x / widthPx).coerceIn(0f, 1f) * (r.endInclusive - r.start)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures { offset -> currentOnValueChange(xToValue(offset.x)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> currentOnValueChange(xToValue(offset.x)) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        currentOnValueChange(xToValue(change.position.x))
                    }
                )
            }
    ) {
        drawWavyTrack(
            fraction = fraction,
            phase = phase.value,
            amplitudePx = 3.5.dp.toPx() * amplitude.value,
            wavelengthPx = 26.dp.toPx(),
            strokePx = 4.dp.toPx(),
            gapPx = 6.dp.toPx(),
            activeColor = activeColor,
            trackColor = trackColor,
            thumbWidthPx = 4.dp.toPx(),
            thumbHeightPx = 24.dp.toPx()
        )
    }
}

// ---------- compact indicator (mini player) ----------

/** Thin wavy progress indicator without a thumb, for the mini player. */
@Composable
fun WavyProgress(
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
) {
    val amplitude = rememberWaveAmplitude(isPlaying)
    val phase = rememberWavePhase(isPlaying)

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        drawWavyTrack(
            fraction = progress.coerceIn(0f, 1f),
            phase = phase.value,
            amplitudePx = 2.5.dp.toPx() * amplitude.value,
            wavelengthPx = 18.dp.toPx(),
            strokePx = 3.dp.toPx(),
            gapPx = 0f,
            activeColor = activeColor,
            trackColor = trackColor
        )
    }
}

// ---------- player slider: wavy in both themes ----------

@Composable
fun PlayerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    isPlaying: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    WavySlider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        isPlaying = isPlaying,
        modifier = modifier,
        activeColor = accent
    )
}
