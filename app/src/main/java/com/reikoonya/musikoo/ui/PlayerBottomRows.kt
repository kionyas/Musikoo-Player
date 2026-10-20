package com.reikoonya.musikoo.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.reikoonya.musikoo.ui.components.BounceIconButton
import com.reikoonya.musikoo.ui.components.BounceSurface
import com.reikoonya.musikoo.ui.components.ExpressivePlayButton
import com.reikoonya.musikoo.ui.components.glassStroke
import com.reikoonya.musikoo.util.MusicPlayerManager

@Composable
fun PlayerTransportRow(isPlaying: Boolean, accent: Color, onAccent: Color, onPlayPause: () -> Unit) {
    val shuffleOn = MusicPlayerManager.shuffleOn
    val repeat = MusicPlayerManager.repeatState
    val circleContainer = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
    val circleContent = MaterialTheme.colorScheme.onSecondaryContainer

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f), Alignment.Center) {
            BounceIconButton(onClick = { MusicPlayerManager.toggleShuffle() }) {
                Icon(Icons.Rounded.Shuffle, "Shuffle", Modifier.size(23.dp), tint = if (shuffleOn) accent else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box(Modifier.weight(1.3f), Alignment.Center) {
            BounceSurface(onClick = { MusicPlayerManager.skipPrevious() }, modifier = Modifier.size(58.dp), shape = CircleShape, color = circleContainer) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { Icon(Icons.Rounded.SkipPrevious, "Previous", Modifier.size(28.dp), tint = circleContent) }
            }
        }
        Box(Modifier.weight(1.8f), Alignment.Center) {
            ExpressivePlayButton(isPlaying = isPlaying, onClick = onPlayPause, containerColor = accent, contentColor = onAccent, width = 88.dp, height = 76.dp, iconSize = 36.dp, playingCorner = 28.dp)
        }
        Box(Modifier.weight(1.3f), Alignment.Center) {
            BounceSurface(onClick = { MusicPlayerManager.skipNext() }, modifier = Modifier.size(58.dp), shape = CircleShape, color = circleContainer) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { Icon(Icons.Rounded.SkipNext, "Next", Modifier.size(28.dp), tint = circleContent) }
            }
        }
        Box(Modifier.weight(1f), Alignment.Center) {
            BounceIconButton(onClick = { MusicPlayerManager.cycleRepeat() }) {
                Icon(if (repeat == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat, "Repeat", Modifier.size(23.dp), tint = if (repeat != Player.REPEAT_MODE_OFF) accent else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * Слитная нижняя панель навигации (ровный MD3E-док как на главном экране):
 * Единый аккуратный контейнер без скачущих отступов с равными сегментами.
 */
@Composable
fun PlayerPills(accent: Color, onAccent: Color, lyricsOn: Boolean, onToggleLyrics: () -> Unit, onQueueClick: (() -> Unit)?) {
    val speed = MusicPlayerManager.playbackSpeed
    val speedLabel = if ((speed % 1f) == 0f) "${speed.toInt()}x" else "${speed}x"

    Surface(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = glassStroke(),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerNavSegment(speedLabel, Icons.Rounded.Speed, false, accent, onAccent) { MusicPlayerManager.cycleSpeed() }
            PlayerNavSegment("Lyrics", Icons.AutoMirrored.Rounded.Notes, lyricsOn, accent, onAccent, onToggleLyrics)
            PlayerNavSegment("Up next", Icons.AutoMirrored.Rounded.PlaylistPlay, false, accent, onAccent) { onQueueClick?.invoke() }
        }
    }
}

@Composable
private fun RowScope.PlayerNavSegment(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    accent: Color,
    onAccent: Color,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "navBg"
    )
    val fg by animateColorAsState(targetValue = if (selected) onAccent else MaterialTheme.colorScheme.onSurface, label = "navFg")

    BounceSurface(
        modifier = Modifier.weight(1f).fillMaxHeight(),
        shape = CircleShape,
        color = bg,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 6.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = fg)
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = fg,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
