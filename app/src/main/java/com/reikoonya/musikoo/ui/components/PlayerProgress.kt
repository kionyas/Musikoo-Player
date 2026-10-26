package com.reikoonya.musikoo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reikoonya.musikoo.util.MusicPlayerManager
import com.reikoonya.musikoo.util.formatTime

/**
 * Слайдер прогресса и время на экране плеера.
 * Позиция читается прямо здесь, а не в родительском экране: тик воспроизведения (раз в 300 мс)
 * перерисовывает только эту строку, а весь экран плеера остаётся нетронутым.
 */
@Composable
fun PlayerProgress(
    isPlaying: Boolean,
    accent: Color,
    onScrubbingChanged: (Boolean) -> Unit,
) {
    PlayerSlider(
        value = MusicPlayerManager.currentPosition,
        onValueChange = { MusicPlayerManager.seekTo(it.toLong()) },
        valueRange = 0f..(MusicPlayerManager.trackDuration.takeIf { it > 0f } ?: 1f),
        isPlaying = isPlaying,
        accent = accent,
        onDraggingChanged = onScrubbingChanged
    )

    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            formatTime(MusicPlayerManager.currentPosition.toLong()),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "-" + formatTime((MusicPlayerManager.trackDuration - MusicPlayerManager.currentPosition).toLong().coerceAtLeast(0L)),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
