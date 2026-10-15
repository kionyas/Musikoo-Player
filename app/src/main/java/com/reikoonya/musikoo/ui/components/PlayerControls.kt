package com.reikoonya.musikoo.ui.components

import androidx.compose.material.icons.rounded.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.ui.theme.LocalFancyStyle

/**
 * Play/Pause button with a spring animation in both themes: the shape morphs between a circle and a pill
 * (paused) and a rounded square (playing), and it springs in when pressed.
 * Sizes are configurable: large in the player, small in the mini player.
 */
@Composable
fun ExpressivePlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    width: Dp = 72.dp,
    height: Dp = 72.dp,
    size: Dp? = null,
    iconSize: Dp = 32.dp,
    playingCorner: Dp = 22.dp,
) {
    val actualWidth = size ?: width
    val actualHeight = size ?: height
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val targetCorner = if (isPlaying) playingCorner else actualHeight / 2
    val targetWidth = if (isPlaying) actualWidth else actualHeight

    val corner by animateDpAsState(
        targetValue = targetCorner,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "playCorner"
    )
    val currentWidth by animateDpAsState(
        targetValue = if (pressed) targetWidth * 0.9f else targetWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "playWidth"
    )
    val currentHeight by animateDpAsState(
        targetValue = if (pressed) actualHeight * 0.9f else actualHeight,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "playHeight"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.size(width = currentWidth, height = currentHeight),
        shape = RoundedCornerShape(corner),
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interaction
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = isPlaying,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium), initialScale = 0.5f)) togetherWith
                            (fadeOut(animationSpec = tween(180)) + scaleOut(animationSpec = tween(180), targetScale = 0.5f))
                },
                label = "playIcon"
            ) { playing ->
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

/** Secondary button (back, forward, ±10 seconds, menu) with a spring press animation. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 56.dp,
    height: Dp = 52.dp,
    iconSize: Dp = 26.dp,
    shape: Shape = RoundedCornerShape(22.dp),
    containerColor: Color = glassFill(),
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    /** In plain M3, draw just the icon without a container (as before). */
    bareWhenClassic: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }

    if (bareWhenClassic && !LocalFancyStyle.current) {
        IconButton(
            onClick = onClick,
            modifier = modifier
                .size(width = width, height = height)
                .pressScale(interaction),
            interactionSource = interaction
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = contentColor
            )
        }
        return
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .size(width = width, height = height)
            .pressScale(interaction),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = null,
        interactionSource = interaction
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
