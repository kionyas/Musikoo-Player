package com.reikoonya.musikoo.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp

/**
 * Переключатель в стиле приложения, замена Material3 Switch с тем же набором параметров.
 *
 * Бегунок пружинит при переключении, при нажатии становится больше и растягивается,
 * внутри включённого бегунка появляется галочка, цвета дорожки и обводки меняются плавно.
 * Без Live Animations всё переключается мгновенно.
 */
@Composable
fun MusikooSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val live = liveAnimationsEnabled()
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme

    val trackWidth = 52.dp
    val trackHeight = 32.dp
    val edge = 2.dp

    fun <T> motion(damping: Float, stiffness: Float) =
        if (live) spring<T>(dampingRatio = damping, stiffness = stiffness) else snap()

    // Размер бегунка: выключен 16, включён 24, под пальцем 28 и растягивается по ширине
    val thumbHeight by animateDpAsState(
        targetValue = when {
            pressed -> 28.dp
            checked -> 24.dp
            else -> 16.dp
        },
        animationSpec = motion(0.55f, Spring.StiffnessMedium),
        label = "switchThumbHeight"
    )
    val thumbWidth by animateDpAsState(
        targetValue = when {
            pressed -> 34.dp
            checked -> 24.dp
            else -> 16.dp
        },
        animationSpec = motion(0.55f, Spring.StiffnessMedium),
        label = "switchThumbWidth"
    )
    // Центр бегунка едет с лёгким перелётом, поэтому переключение «щёлкает»
    val centerX by animateDpAsState(
        targetValue = if (checked) trackWidth - 16.dp else 16.dp,
        animationSpec = motion(0.6f, Spring.StiffnessMediumLow),
        label = "switchThumbX"
    )
    val left: Dp = (centerX - thumbWidth / 2).coerceIn(edge, trackWidth - thumbWidth - edge)

    val trackColor by animateColorAsState(
        targetValue = if (checked) scheme.primary else scheme.surfaceContainerHighest,
        animationSpec = motion(1f, Spring.StiffnessMedium),
        label = "switchTrack"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) Color.Transparent else scheme.outline,
        animationSpec = motion(1f, Spring.StiffnessMedium),
        label = "switchBorder"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) scheme.onPrimary else scheme.outline,
        animationSpec = motion(1f, Spring.StiffnessMedium),
        label = "switchThumb"
    )
    val checkProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = motion(0.7f, Spring.StiffnessMedium),
        label = "switchCheck"
    )

    Box(
        modifier = modifier
            .size(trackWidth, trackHeight)
            .alpha(if (enabled) 1f else 0.38f)
            .clip(CircleShape)
            .background(trackColor, CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .toggleable(
                value = checked,
                enabled = enabled && onCheckedChange != null,
                role = Role.Switch,
                interactionSource = source,
                indication = null,
                onValueChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange?.invoke(it)
                }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = left)
                .size(thumbWidth, thumbHeight)
                .background(thumbColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (checkProgress > 0.01f) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = scheme.primary,
                    modifier = Modifier
                        .size(16.dp)
                        .scale(checkProgress)
                        .alpha(checkProgress)
                )
            }
        }
    }
}
