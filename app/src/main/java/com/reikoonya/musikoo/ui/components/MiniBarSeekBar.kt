package com.reikoonya.musikoo.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.util.MusicPlayerManager

/**
 * Прогресс и перемотка мини-плеера в нижней панели.
 * Позиция читается здесь, а не в панели: тик воспроизведения перерисовывает только эту полосу,
 * а вся нижняя панель с поиском и навигацией остаётся нетронутой.
 */
@Composable
fun MiniBarSeekBar(isPlaying: Boolean) {
    val progress = if (MusicPlayerManager.trackDuration > 0f) {
        (MusicPlayerManager.currentPosition / MusicPlayerManager.trackDuration).coerceIn(0f, 1f)
    } else 0f

    var miniBarWidthPx by remember { mutableFloatStateOf(1f) }
    fun seekMiniBar(x: Float) {
        val d = MusicPlayerManager.trackDuration
        if (d > 0f) {
            MusicPlayerManager.seekTo(((x / miniBarWidthPx).coerceIn(0f, 1f) * d).toLong())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged {
                miniBarWidthPx = it.width.toFloat().coerceAtLeast(1f)
            }
            .pointerInput(Unit) { detectTapGestures { seekMiniBar(it.x) } }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    seekMiniBar(change.position.x)
                }
            }
    ) {
        WavyProgress(
            progress = progress,
            isPlaying = isPlaying,
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
            activeColor = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
        )
    }
}
