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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

private const val TWO_PI = (2 * PI).toFloat()

@Composable
private fun rememberWaveState(isPlaying: Boolean): Pair<State<Float>, State<Float>> {
    val amplitude = animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "amp"
    )
    val phase = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            var last = System.nanoTime()
            while (true) {
                delay(33)
                val now = System.nanoTime()
                val dt = (now - last) / 1_000_000_000f
                phase.floatValue = (phase.floatValue + dt * TWO_PI / 1.6f) % TWO_PI
                last = now
            }
        }
    }
    return amplitude to phase
}

/**
 * Выразительный Wavy Slider: бегущая волна при воспроизведении,
 * пружинное выравнивание при паузе и ползунок без обрезки краев.
 */
@Composable
fun PlayerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    isPlaying: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
    onDraggingChanged: (Boolean) -> Unit = {}
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentRange by rememberUpdatedState(valueRange)
    val (amplitude, phase) = rememberWaveState(isPlaying)

    var widthPx by remember { mutableFloatStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }

    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-3f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.35f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "thumbScale"
    )

    fun xToValue(touchX: Float): Float {
        val thumbRadius = 3.dp.value * 2.5f
        val trackStart = thumbRadius
        val trackEnd = (widthPx - thumbRadius).coerceAtLeast(trackStart + 1f)
        val clampedFraction = ((touchX - trackStart) / (trackEnd - trackStart)).coerceIn(0f, 1f)
        return currentRange.start + clampedFraction * (currentRange.endInclusive - currentRange.start)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isDragging = true
                        onDraggingChanged(true)
                        tryAwaitRelease()
                        isDragging = false
                        onDraggingChanged(false)
                    },
                    onTap = { offset -> currentOnValueChange(xToValue(offset.x)) }
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        onDraggingChanged(true)
                        currentOnValueChange(xToValue(offset.x))
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        currentOnValueChange(xToValue(change.position.x))
                    },
                    onDragEnd = {
                        isDragging = false
                        onDraggingChanged(false)
                    },
                    onDragCancel = {
                        isDragging = false
                        onDraggingChanged(false)
                    }
                )
            }
    ) {
        val w = size.width
        val cy = size.height / 2f

        val strokePx = 3.8.dp.toPx()
        val thumbWidthPx = 5.5.dp.toPx() * thumbScale
        val thumbHeightPx = 26.dp.toPx() * thumbScale
        val thumbRadius = thumbWidthPx / 2f

        // Ограничиваем трек внутренними полями, чтобы капсула никогда не срезалась
        val trackStart = thumbRadius
        val trackEnd = w - thumbRadius
        val x = trackStart + (trackEnd - trackStart) * fraction

        val amplitudePx = 3.6.dp.toPx() * amplitude.value
        val wavelengthPx = 28.dp.toPx()
        val k = TWO_PI / wavelengthPx
        val gapPx = 3.dp.toPx()

        // 1. Непроигранная часть (справа от ползунка)
        val inactiveStart = (x + gapPx).coerceAtMost(trackEnd)
        if (inactiveStart < trackEnd) {
            drawLine(
                color = trackColor,
                start = Offset(inactiveStart, cy),
                end = Offset(trackEnd, cy),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
        }

        // 2. Проигранная часть (волна или прямая)
        val activeEnd = (x - gapPx).coerceAtLeast(trackStart)
        if (activeEnd > trackStart) {
            if (amplitudePx > 0.5f) {
                val path = Path()
                val step = 2.dp.toPx()
                var px = trackStart
                path.moveTo(trackStart, cy + amplitudePx * sin(k * trackStart - phase.value))
                while (px < activeEnd) {
                    px = min(px + step, activeEnd)
                    path.lineTo(px, cy + amplitudePx * sin(k * px - phase.value))
                }
                drawPath(
                    path = path,
                    color = accent,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            } else {
                drawLine(
                    color = accent,
                    start = Offset(trackStart, cy),
                    end = Offset(activeEnd, cy),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }
        }

        // 3. Вертикальный капсульный ползунок (всегда строго в границах)
        drawRoundRect(
            color = accent,
            topLeft = Offset(x - thumbWidthPx / 2f, cy - thumbHeightPx / 2f),
            size = Size(thumbWidthPx, thumbHeightPx),
            cornerRadius = CornerRadius(thumbWidthPx / 2f)
        )
    }
}

/** Волнистая полоска прогресса для мини-плеера */
@Composable
fun WavyProgress(
    progress: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
) {
    val (amplitude, phase) = rememberWaveState(isPlaying)

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val w = size.width
        val cy = size.height / 2f
        val x = (w * progress.coerceIn(0f, 1f)).coerceIn(0f, w)
        val strokePx = 3.dp.toPx()
        val amplitudePx = 2.6.dp.toPx() * amplitude.value
        val wavelengthPx = 20.dp.toPx()
        val k = TWO_PI / wavelengthPx

        if (x < w) {
            drawLine(
                color = trackColor,
                start = Offset(x, cy),
                end = Offset(w, cy),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )
        }
        if (x > 0f) {
            if (amplitudePx > 0.5f) {
                val path = Path()
                val step = 2.dp.toPx()
                var px = 0f
                path.moveTo(0f, cy + amplitudePx * sin(-phase.value))
                while (px < x) {
                    px = min(px + step, x)
                    path.lineTo(px, cy + amplitudePx * sin(k * px - phase.value))
                }
                drawPath(
                    path = path,
                    color = activeColor,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            } else {
                drawLine(
                    color = activeColor,
                    start = Offset(0f, cy),
                    end = Offset(x, cy),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
