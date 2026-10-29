package com.reikoonya.musikoo.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.util.MusicPlayerManager
import com.reikoonya.musikoo.util.formatTime
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Прогресс и перемотка мини-плеера: волнистый ползунок + время.
 *
 * Отзывчивость: палец сразу же ставит ползунок (без ожидания «touch slop»), пока он зажат —
 * позиция рисуется локально, а реальная перемотка выполняется один раз при отпускании.
 *
 * «Живые соседи»: метка текущего времени выезжает из угла и едет под ползунком,
 * метка длительности отодвигается, когда её догоняют, а родитель получает [onScrubChange],
 * чтобы подвинуть обложку, название и кнопки.
 *
 * Позиция читается здесь, поэтому тик воспроизведения перерисовывает только эту полосу.
 */
@Composable
fun MiniBarSeekBar(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    /** Доля 0..1 пока палец на ползунке, null — когда отпустили. */
    onScrubChange: (Float?) -> Unit = {},
) {
    val duration = MusicPlayerManager.trackDuration
    val playerFraction =
        if (duration > 0f) (MusicPlayerManager.currentPosition / duration).coerceIn(0f, 1f) else 0f

    var scrub by remember { mutableStateOf<Float?>(null) }
    // Держим позицию пальца ещё чуть-чуть после отпускания, пока плеер не «догонит» перемотку
    var settle by remember { mutableStateOf<Float?>(null) }
    var widthPx by remember { mutableFloatStateOf(1f) }
    val currentOnScrub by rememberUpdatedState(onScrubChange)

    LaunchedEffect(settle) {
        if (settle != null) {
            delay(350)
            settle = null
        }
    }

    val dragging = scrub != null
    val live = liveAnimationsEnabled()
    val shown = scrub ?: settle ?: playerFraction

    val grab by animateFloatAsState(
        targetValue = if (dragging) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "miniGrab"
    )
    val (amplitudeState, phaseState) = rememberWaveState(isPlaying)

    var elapsedW by remember { mutableIntStateOf(1) }
    var totalW by remember { mutableIntStateOf(1) }

    val activeColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val pad = 8.dp.toPx()
                        fun toFraction(x: Float): Float =
                            ((x - pad) / (widthPx - 2f * pad).coerceAtLeast(1f)).coerceIn(0f, 1f)

                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        var f = toFraction(down.position.x)
                        scrub = f
                        currentOnScrub(f)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            f = toFraction(change.position.x)
                            scrub = f
                            currentOnScrub(f)
                            change.consume()
                        }
                        scrub = null
                        currentOnScrub(null)
                        settle = f
                        val d = MusicPlayerManager.trackDuration
                        if (d > 0f) MusicPlayerManager.seekTo((f * d).toLong())
                    }
                }
        ) {
            val w = size.width
            val cy = size.height / 2f
            val pad = 8.dp.toPx()
            val trackStart = pad
            val trackEnd = w - pad
            val x = trackStart + (trackEnd - trackStart) * shown

            val strokePx = (3f + 1.8f * grab).dp.toPx()
            val thumbW = (4f + 2.5f * grab).dp.toPx()
            val thumbH = (14f + 10f * grab).dp.toPx()
            val gap = (3f + 2f * grab).dp.toPx()
            val amp = 2.6.dp.toPx() * amplitudeState.value * (1f + 0.55f * grab)
            val k = (2 * PI).toFloat() / 20.dp.toPx()
            val phase = phaseState.value

            val inactiveStart = (x + gap).coerceAtMost(trackEnd)
            if (inactiveStart < trackEnd) {
                drawLine(
                    color = trackColor,
                    start = Offset(inactiveStart, cy),
                    end = Offset(trackEnd, cy),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }
            val activeEnd = (x - gap).coerceAtLeast(trackStart)
            if (activeEnd > trackStart) {
                if (amp > 0.4f) {
                    val path = Path()
                    val step = 2.dp.toPx()
                    var px = trackStart
                    path.moveTo(px, cy + amp * sin(k * px - phase))
                    while (px < activeEnd) {
                        px = min(px + step, activeEnd)
                        path.lineTo(px, cy + amp * sin(k * px - phase))
                    }
                    drawPath(
                        path = path,
                        color = activeColor,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                } else {
                    drawLine(
                        color = activeColor,
                        start = Offset(trackStart, cy),
                        end = Offset(activeEnd, cy),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                }
            }
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(x - thumbW / 2f, cy - thumbH / 2f),
                size = Size(thumbW, thumbH),
                cornerRadius = CornerRadius(thumbW / 2f)
            )
        }

        // Время. Текущее едет под ползунком, длительность отодвигается
        val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
        val primary = MaterialTheme.colorScheme.primary
        val labelStyle = MaterialTheme.typography.labelSmall.copy(
            fontFeatureSettings = "tnum",
            fontWeight = FontWeight.Medium
        )
        val follow by animateFloatAsState(
            targetValue = if (dragging && live) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "miniTimeFollow"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
        ) {
            val elapsedMs = (shown * duration).toLong()
            Text(
                text = formatTime(elapsedMs),
                style = labelStyle,
                color = androidx.compose.ui.graphics.lerp(onSurfaceVariant, primary, follow),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .onSizeChanged { elapsedW = it.width.coerceAtLeast(1) }
                    .graphicsLayer {
                        val pad = 8.dp.toPx()
                        val thumbX = pad + (widthPx - 2f * pad) * shown
                        val target = (thumbX - elapsedW / 2f).coerceIn(0f, widthPx - elapsedW)
                        translationX = target * follow
                        val s = 1f + 0.12f * follow
                        scaleX = s
                        scaleY = s
                    }
            )
            Text(
                text = formatTime(duration.toLong()),
                style = labelStyle,
                color = onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .onSizeChanged { totalW = it.width.coerceAtLeast(1) }
                    .graphicsLayer {
                        val pad = 8.dp.toPx()
                        val thumbX = pad + (widthPx - 2f * pad) * shown
                        val elapsedRight =
                            (thumbX - elapsedW / 2f).coerceIn(0f, widthPx - elapsedW) * follow + elapsedW
                        // Насколько метка текущего времени «наезжает» на длительность
                        val overlap = (elapsedRight + 6.dp.toPx() - (widthPx - totalW)).coerceAtLeast(0f)
                        translationX = overlap.coerceAtMost(totalW * 0.9f)
                        alpha = (1f - overlap / totalW).coerceIn(0.1f, 1f)
                    }
            )
        }
    }
}