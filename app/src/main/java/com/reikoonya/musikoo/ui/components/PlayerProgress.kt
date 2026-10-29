package com.reikoonya.musikoo.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reikoonya.musikoo.util.MusicPlayerManager
import com.reikoonya.musikoo.util.formatTime

/**
 * Слайдер прогресса и время на экране плеера.
 * Позиция читается прямо здесь, а не в родительском экране: тик воспроизведения (раз в 300 мс)
 * перерисовывает только эту строку, а весь экран плеера остаётся нетронутым.
 *
 * Пока ползунок зажат: текущее время выезжает из угла и едет под ползунком, время «до конца»
 * отодвигается; через [scrubState] остальные элементы экрана (обложка, название, кнопки)
 * получают положение ползунка и подстраиваются под него.
 */
@Composable
fun PlayerProgress(
    isPlaying: Boolean,
    accent: Color,
    onScrubbingChanged: (Boolean) -> Unit,
    scrubState: ScrubState? = null,
) {
    val duration = MusicPlayerManager.trackDuration
    val position = MusicPlayerManager.currentPosition
    val fraction = if (duration > 0f) (position / duration).coerceIn(0f, 1f) else 0f

    var dragging by remember { mutableStateOf(false) }
    val live = liveAnimationsEnabled()
    SideEffect { scrubState?.fraction = fraction }

    PlayerSlider(
        value = position,
        onValueChange = { MusicPlayerManager.seekTo(it.toLong()) },
        valueRange = 0f..(duration.takeIf { it > 0f } ?: 1f),
        isPlaying = isPlaying,
        accent = accent,
        onDraggingChanged = {
            dragging = it
            scrubState?.active = it
            onScrubbingChanged(it)
        }
    )

    val follow by animateFloatAsState(
        targetValue = if (dragging && live) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "playerTimeFollow"
    )
    var widthPx by remember { mutableFloatStateOf(1f) }
    var elapsedW by remember { mutableIntStateOf(1) }
    var remainW by remember { mutableIntStateOf(1) }

    val labelStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = 13.sp,
        fontFeatureSettings = "tnum",
        fontWeight = FontWeight.Medium
    )
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        Modifier
            .fillMaxWidth()
            .height(22.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
    ) {
        Text(
            text = formatTime(position.toLong()),
            style = labelStyle,
            color = lerp(muted, accent, follow),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .onSizeChanged { elapsedW = it.width.coerceAtLeast(1) }
                .graphicsLayer {
                    val pad = 4.dp.toPx()
                    val thumbX = pad + (widthPx - 2f * pad) * fraction
                    val target = (thumbX - elapsedW / 2f).coerceIn(0f, widthPx - elapsedW)
                    translationX = target * follow
                    val s = 1f + 0.14f * follow
                    scaleX = s
                    scaleY = s
                }
        )
        Text(
            text = "-" + formatTime((duration - position).toLong().coerceAtLeast(0L)),
            style = labelStyle,
            color = muted,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .onSizeChanged { remainW = it.width.coerceAtLeast(1) }
                .graphicsLayer {
                    val pad = 4.dp.toPx()
                    val thumbX = pad + (widthPx - 2f * pad) * fraction
                    val elapsedRight =
                        (thumbX - elapsedW / 2f).coerceIn(0f, widthPx - elapsedW) * follow + elapsedW
                    // насколько «текущее время» наезжает на «до конца»: толкаем его вправо и гасим
                    val overlap = (elapsedRight + 8.dp.toPx() - (widthPx - remainW)).coerceAtLeast(0f)
                    translationX = overlap.coerceAtMost(remainW * 0.9f)
                    alpha = (1f - overlap / remainW).coerceIn(0.1f, 1f)
                }
        )
    }
}