package com.reikoonya.musikoo.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Shared "spring" press animation for both themes: the element slightly shrinks
 * under the finger and springs back.
 */
@Composable
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.92f): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Like Modifier.clickable, but with a spring press animation. */
fun Modifier.bounceClickable(
    pressedScale: Float = 0.96f,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    this
        .pressScale(source, pressedScale)
        .clickable(
            interactionSource = source,
            indication = LocalIndication.current,
            onClick = onClick
        )
}

/** IconButton with a spring press animation. */
@Composable
fun BounceIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        modifier = modifier.pressScale(source),
        interactionSource = source,
        content = content
    )
}

/** Surface(onClick) with a spring press animation. */
@Composable
fun BounceSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    color: Color = MaterialTheme.colorScheme.surface,
    border: BorderStroke? = null,
    pressedScale: Float = 0.94f,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        modifier = modifier.pressScale(source, pressedScale),
        shape = shape,
        color = color,
        border = border,
        interactionSource = source,
        content = content
    )
}

/** Like bounceClickable, but with long press and haptic feedback. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bounceCombinedClickable(
    pressedScale: Float = 0.96f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    this
        .pressScale(source, pressedScale)
        .combinedClickable(
            interactionSource = source,
            indication = LocalIndication.current,
            onLongClick = onLongClick?.let { action ->
                {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    action()
                }
            },
            onClick = onClick
        )
}
