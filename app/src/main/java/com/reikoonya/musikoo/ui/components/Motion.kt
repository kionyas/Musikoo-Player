package com.reikoonya.musikoo.ui.components

import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay

/**
 * Shared "spring" press animation for both themes: the element shrinks under the finger and
 * springs back. With "Live Animations" on it is livelier: it sinks noticeably (~15%), leans up to 16°
 * toward the point where it was touched (like a physical button), holds the pose for a quick tap
 * and overshoots on release.
 */
@Composable
fun Modifier.pressScale(source: InteractionSource, pressedScale: Float = 0.92f): Modifier {
    val live = liveAnimationsEnabled()
    val rawPressed by source.collectIsPressedAsState()
    var touch by remember { mutableStateOf(Offset.Unspecified) }
    // A quick tap is released within ~60 ms: hold the pressed pose a little longer so it is visible
    var held by remember { mutableStateOf(false) }
    LaunchedEffect(source, live) {
        if (!live) return@LaunchedEffect
        source.interactions.collect { if (it is PressInteraction.Press) touch = it.pressPosition }
    }
    LaunchedEffect(rawPressed, live) {
        if (!live) return@LaunchedEffect
        if (rawPressed) held = true else { delay(110); held = false }
    }
    val pressed = if (live) (rawPressed || held) else rawPressed
    val target = if (pressed) (if (live) (pressedScale - 0.07f).coerceAtLeast(0.78f) else pressedScale) else 1f
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(
            dampingRatio = if (live) 0.4f else Spring.DampingRatioLowBouncy,
            stiffness = if (live) Spring.StiffnessMedium else Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    val lean by animateFloatAsState(
        targetValue = if (pressed && live) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "pressLean"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        if (live && touch != Offset.Unspecified && size.width > 0f && size.height > 0f) {
            val nx = ((touch.x / size.width) - 0.5f).coerceIn(-0.5f, 0.5f) * 2f
            val ny = ((touch.y / size.height) - 0.5f).coerceIn(-0.5f, 0.5f) * 2f
            cameraDistance = 9f * density
            rotationY = nx * 16f * lean
            rotationX = -ny * 16f * lean
        }
    }
}

/** Clips touch area and ripple to the element's own shape (no-op for the default rectangle). */
private fun Modifier.clipToShape(shape: Shape): Modifier =
    if (shape === RectangleShape) this else this.clip(shape)

/** Like Modifier.clickable, but with a spring press animation. Pass the element's shape so the touch box and highlight match it. */
fun Modifier.bounceClickable(
    pressedScale: Float = 0.96f,
    shape: Shape = RectangleShape,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    this
        .pressScale(source, pressedScale)
        .clipToShape(shape)
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
        modifier = modifier.minimumInteractiveComponentSize().pressScale(source),
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
    shape: Shape = RectangleShape,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    this
        .pressScale(source, pressedScale)
        .clipToShape(shape)
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
