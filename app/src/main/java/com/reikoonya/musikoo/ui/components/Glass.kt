package com.reikoonya.musikoo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.ui.theme.LocalFancyStyle

/**
 * Fill for chips and buttons: plain surface (no accent tint in any style).
 */
@Composable
fun glassFill(classic: Color = MaterialTheme.colorScheme.surfaceContainerHighest): Color {
    return classic
}

/** Outline removed entirely (returns null). */
@Composable
fun glassStroke(): BorderStroke? = null

/** Card fill: plain surface (no accent tint in any style). */
@Composable
fun cardFill(classic: Color = MaterialTheme.colorScheme.surfaceContainerHigh): Color {
    return classic
}

/** ListItem colors. */
@Composable
fun listItemColors(): ListItemColors = ListItemDefaults.colors()

/**
 * Top gradient fade (scrim) under the top pill for all tabs.
 *
 * [blurred] = true: the content under the bar is blurred by [topFadeBlur] (set on the list of the same
 * screen), so the scrim only adds a light tint. If the effect is off or unavailable (Android < 12,
 * Blur Effects off), the classic solid fade is drawn instead.
 */
@Composable
fun TopFadeScrim(
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    blurred: Boolean = false
) {
    val surface = MaterialTheme.colorScheme.surface
    val tinted = blurred && rememberTopFadeBlurActive()
    val brush = if (tinted) {
        Brush.verticalGradient(
            0f to surface.copy(alpha = 0.50f),
            0.45f to surface.copy(alpha = 0.18f),
            1f to surface.copy(alpha = 0f)
        )
    } else {
        Brush.verticalGradient(
            0f to surface,
            0.6f to surface.copy(alpha = 0.7f),
            1f to surface.copy(alpha = 0f)
        )
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(brush)
    )
}

/**
 * Bottom gradient fade (scrim) under the bottom pill for all tabs.
 */
@Composable
fun BottomFadeScrim(
    modifier: Modifier = Modifier,
    height: Dp = 140.dp
) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    0f to surface.copy(alpha = 0f),
                    0.4f to surface.copy(alpha = 0.7f),
                    1f to surface
                )
            )
    )
}
