package com.reikoonya.musikoo.ui.components

import androidx.compose.material.icons.rounded.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.ui.theme.LocalFancyStyle
import com.reikoonya.musikoo.util.MusicPlayerManager

/** Bottom panel controls under the player: shuffle, repeat and up next (queue). */
@Composable
fun SplitBottomPills(
    accent: Color = MaterialTheme.colorScheme.primary,
    onQueueClick: (() -> Unit)? = null
) {
    val shuffleOn = MusicPlayerManager.shuffleOn
    val repeat = MusicPlayerManager.repeatState

    val outerRadius = 28.dp
    val innerRadius = 10.dp

    val leftShape = RoundedCornerShape(
        topStart = outerRadius,
        bottomStart = outerRadius,
        topEnd = innerRadius,
        bottomEnd = innerRadius
    )
    val middleShape = RoundedCornerShape(innerRadius)
    val rightShape = RoundedCornerShape(
        topStart = innerRadius,
        bottomStart = innerRadius,
        topEnd = outerRadius,
        bottomEnd = outerRadius
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassPill(
            icon = Icons.Rounded.Shuffle,
            label = stringResource(R.string.shuffle),
            selected = shuffleOn,
            accent = accent,
            shape = leftShape,
            onClick = { MusicPlayerManager.toggleShuffle() },
            modifier = Modifier.weight(1f)
        )
        GlassPill(
            icon = if (repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            label = when (repeat) {
                Player.REPEAT_MODE_ONE -> stringResource(R.string.repeat_one)
                Player.REPEAT_MODE_ALL -> stringResource(R.string.repeat_all)
                else -> stringResource(R.string.repeat)
            },
            selected = repeat != Player.REPEAT_MODE_OFF,
            accent = accent,
            shape = if (onQueueClick != null) middleShape else rightShape,
            onClick = { MusicPlayerManager.cycleRepeat() },
            modifier = Modifier.weight(1f)
        )
        if (onQueueClick != null) {
            GlassPill(
                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                label = stringResource(R.string.up_next),
                selected = false,
                accent = accent,
                shape = rightShape,
                onClick = onQueueClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun GlassPill(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    accent: Color,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill by animateColorAsState(
        targetValue = glassFill(),
        label = "pillFill"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.onSurface,
        label = "pillTint"
    )

    BounceSurface(
        modifier = modifier.height(56.dp),
        shape = shape,
        color = fill,
        border = null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
