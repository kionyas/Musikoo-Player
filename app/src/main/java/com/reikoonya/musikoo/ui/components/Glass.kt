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

/**
 * Fill for chips and buttons: an opaque surface with no see-through.
 */
@Composable
fun glassFill(classic: Color = MaterialTheme.colorScheme.surfaceContainerHighest): Color = classic

/** Outline removed entirely (returns null). */
@Composable
fun glassStroke(): BorderStroke? = null

/** Card fill. */
@Composable
fun cardFill(classic: Color = MaterialTheme.colorScheme.surfaceContainerHigh): Color = classic

/** ListItem colors. */
@Composable
fun listItemColors(): ListItemColors = ListItemDefaults.colors()

/**
 * Top gradient fade (scrim) under the top pill for all tabs.
 */
@Composable
fun TopFadeScrim(
    modifier: Modifier = Modifier,
    height: Dp = 100.dp
) {
    val surface = MaterialTheme.colorScheme.surface
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    0f to surface,
                    0.6f to surface.copy(alpha = 0.7f),
                    1f to Color.Transparent
                )
            )
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
                    0f to Color.Transparent,
                    0.4f to surface.copy(alpha = 0.7f),
                    1f to surface
                )
            )
    )
}
