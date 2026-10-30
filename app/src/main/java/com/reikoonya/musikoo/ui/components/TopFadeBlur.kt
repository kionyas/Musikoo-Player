package com.reikoonya.musikoo.ui.components

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.util.UiStylePrefs

/**
 * Is the top fade blur really drawn right now: needs Android 12+ (RenderEffect), the setting
 * "Top Fade Blur" and the global "Blur Effects" switch (energy saving).
 */
@Composable
fun rememberTopFadeBlurActive(): Boolean {
    val appContext = LocalContext.current.applicationContext
    remember { UiStylePrefs.ensureLoaded(appContext) }
    return Build.VERSION.SDK_INT >= 31 && UiStylePrefs.topFadeBlur && UiStylePrefs.blurEnabled
}

/**
 * iOS / ColorOS style progressive blur: whatever scrolls into the top [height] of this node gets
 * blurred more and more strongly towards the top edge and melts smoothly into the clean image below, instead of just being covered
 * by a colored gradient.
 *
 * Put it on the scrolling container itself (LazyColumn, LazyVerticalGrid, pager, verticalScroll Column),
 * after fillMaxSize() and BEFORE verticalScroll(). Pair it with `TopFadeScrim(blurred = true)`.
 *
 * How it works: the content is recorded once into a GraphicsLayer. The sharp copy is erased in the top
 * band, then [steps] blurred copies with growing radius are drawn over each other. Each copy is
 * masked by its own slice of the gradient, so the blur radius grows smoothly (progressive blur) and
 * there is no visible border between the steps. Only the top band is blurred, so the GPU cost is small.
 */
fun Modifier.topFadeBlur(
    height: Dp = 120.dp,
    maxRadius: Dp = 26.dp,
    steps: Int = 8,
): Modifier = composed {
    if (Build.VERSION.SDK_INT < 31) return@composed this

    val appContext = LocalContext.current.applicationContext
    remember { UiStylePrefs.ensureLoaded(appContext) }

    val base = rememberGraphicsLayer()
    val passes = List(steps) { rememberGraphicsLayer() }

    this
        // Offscreen: the erasing (DstOut) below must only touch this node's own pixels, not the backdrop behind it
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            // State reads inside the draw block: switching the setting redraws immediately
            val active = UiStylePrefs.topFadeBlur && UiStylePrefs.blurEnabled
            if (!active || size.width < 1f || size.height < 1f) {
                drawContent()
                return@drawWithContent
            }

            val w = size.width.toInt()
            val h = size.height.toInt()
            val band = height.toPx().coerceAtMost(size.height)
            val maxR = maxRadius.toPx()
            // A bit of extra content under the band, so the blur at its lower edge sees real pixels
            val passHeight = (band + maxR * 1.5f).toInt().coerceAtMost(h)

            base.record(size = IntSize(w, h)) { this@drawWithContent.drawContent() }

            // 1) sharp content, erased in the top band: fully above the last step, fading out over the first one
            drawLayer(base)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startY = band * (1f - 1f / steps),
                    endY = band
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, band),
                blendMode = BlendMode.DstOut
            )

            // 2) blurred copies. Level 0 is the sharp image, level j = 1..steps is the copy number j with a
            // growing radius. Each level has a "hat" weight (1 at its own height, 0 at the neighbours' heights), so
            // the weights of two neighbours always add up to exactly 1. The copies are ADDED (BlendMode.Plus,
            // premultiplied) to the faded sharp image: sharp * (1 - w) + blurred * w. Drawing them with a normal
            // "over" blend left a translucent seam where the backdrop (dark) showed through: the black stripe.
            fun yOf(level: Float) = band * (1f - level / steps)
            for (i in 0 until steps) {
                val level = i + 1
                val layer = passes[i]
                // eased radii: many weak steps near the clean image -> smooth, invisible transition
                val radius = maxR * Math.pow((level.toFloat() / steps).toDouble(), 1.6).toFloat()
                layer.compositingStrategy = androidx.compose.ui.graphics.layer.CompositingStrategy.Offscreen
                layer.renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                layer.blendMode = BlendMode.Plus
                layer.record(size = IntSize(w, passHeight)) {
                    drawLayer(base)
                    val stops = ArrayList<Pair<Float, Color>>(3)
                    // the top-most copy stays fully visible up to the screen edge
                    if (level < steps) stops += (yOf(level + 1f) / passHeight).coerceIn(0f, 1f) to Color.Transparent
                    stops += (yOf(level.toFloat()) / passHeight).coerceIn(0f, 1f) to Color.Black
                    stops += (yOf(level - 1f) / passHeight).coerceIn(0f, 1f) to Color.Transparent
                    drawRect(
                        brush = Brush.verticalGradient(
                            colorStops = stops.toTypedArray(),
                            startY = 0f,
                            endY = passHeight.toFloat()
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
                drawLayer(layer)
            }
        }
}
