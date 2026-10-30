package com.reikoonya.musikoo.ui.components

import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.util.UiStylePrefs

/**
 * Cheap blur for "something opened on top" moments (sheets, windows, covered screens).
 *
 * Why it is lighter than Modifier.blur:
 *  - the radius is a lambda that is read only in the draw phase (graphicsLayer), so every animation
 *    frame redraws the layer on the GPU instead of recomposing the whole screen below;
 *  - the radius is capped at [maxRadius]: a big radius costs a lot, and under a dim it is hardly visible;
 *  - nothing is created while the radius is ~0 (no offscreen layer and no RenderEffect when closed);
 *  - Android 11 and older (no RenderEffect) and the "Blur Effects" switch off simply draw nothing.
 */
fun Modifier.lightBlur(maxRadius: Dp = 10.dp, radius: () -> Dp): Modifier = graphicsLayer {
    val px = radius().coerceAtMost(maxRadius).toPx()
    renderEffect = if (Build.VERSION.SDK_INT >= 31 && px > 0.5f && UiStylePrefs.blurEnabled) {
        // Clamp: edges are stretched, so no dark/transparent fringe at the borders
        BlurEffect(px, px, TileMode.Clamp)
    } else null
    clip = false
}
