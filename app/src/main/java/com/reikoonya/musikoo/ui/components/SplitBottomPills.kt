package com.reikoonya.musikoo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.ui.theme.LocalFancyStyle
import com.reikoonya.musikoo.util.MusicPlayerManager

/** Two controls under the player: shuffle and repeat (off → all → one track). */
@Composable
fun SplitBottomPills(accent: Color = MaterialTheme.colorScheme.primary) {
    val shuffleOn = MusicPlayerManager.shuffleOn
    val repeat = MusicPlayerManager.repeatState

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassPill(
            icon = Icons.Default.Shuffle,
            label = stringResource(R.string.shuffle),
            selected = shuffleOn,
            accent = accent,
            onClick = { MusicPlayerManager.toggleShuffle() },
            modifier = Modifier.weight(1f)
        )
        GlassPill(
            icon = if (repeat == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
            label = when (repeat) {
                Player.REPEAT_MODE_ONE -> stringResource(R.string.repeat_one)
                Player.REPEAT_MODE_ALL -> stringResource(R.string.repeat_all)
                else -> stringResource(R.string.repeat)
            },
            selected = repeat != Player.REPEAT_MODE_OFF,
            accent = accent,
            onClick = { MusicPlayerManager.cycleRepeat() },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GlassPill(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill by animateColorAsState(
        targetValue = if (selected && LocalFancyStyle.current) accent.copy(alpha = 0.28f) else glassFill(),
        label = "pillFill"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "pillTint"
    )

    BounceSurface(
        modifier = modifier.height(52.dp),
        shape = CircleShape,
        color = fill,
        border = glassStroke(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
