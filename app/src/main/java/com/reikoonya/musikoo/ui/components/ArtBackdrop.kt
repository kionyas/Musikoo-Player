package com.reikoonya.musikoo.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import com.reikoonya.musikoo.ui.theme.LocalUiStyle
import com.reikoonya.musikoo.ui.theme.UiStyle
import com.reikoonya.musikoo.util.MusicPlayerManager
import com.reikoonya.musikoo.util.accentFromBitmap
import kotlin.math.cos
import kotlin.math.sin

private const val TAU = 6.2831855f

/**
 * Track-art "aura" background.
 *
 * Alive, but cheap:
 *  - the cover is softened ONCE on the CPU (tiny bitmap + box blur) and stretched with high-quality
 *    filtering, so there is no per-frame GPU blur and it looks the same on every Android version;
 *  - two copies of it (the second one mirrored) slowly drift, turn and "breathe" in opposite phases;
 *  - two soft glows in the cover's accent color wander across the screen;
 *  - while music is playing everything moves a bit more, on pause it calms down;
 *  - on list screens (strong scrim) the motion is quieter, in light theme the veil is a bit denser,
 *    so the text on top always stays readable.
 */
@Composable
fun ArtBackdrop(
    art: Bitmap?,
    modifier: Modifier = Modifier,
    topScrim: Float = 0.30f,
    midScrim: Float = 0.72f,
    animateEntrance: Boolean = true
) {
    val style = LocalUiStyle.current
    if (style == UiStyle.CLASSIC) return

    val scheme = MaterialTheme.colorScheme
    val surface = scheme.surface
    val primary = scheme.primary
    val tertiary = scheme.tertiary
    val primaryContainer = scheme.primaryContainer
    val isLight = surface.luminance() > 0.5f
    val motion = if (topScrim >= 0.5f) 0.55f else 1f

    var visible by remember { mutableStateOf(!animateEntrance) }
    LaunchedEffect(Unit) { visible = true }

    val entranceScale by animateFloatAsState(
        targetValue = if (visible) 1.25f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "backdropScale"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "backdropAlpha"
    )

    // 1 while playing, a calm 0.35 on pause
    val energy by animateFloatAsState(
        targetValue = if (MusicPlayerManager.isPlayingState) 1f else 0.35f,
        animationSpec = tween(durationMillis = 1400),
        label = "backdropEnergy"
    )

    // Three loops with different periods: the picture never visibly repeats
    val life = rememberInfiniteTransition(label = "backdropLife")
    val phaseA by life.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(41000, easing = LinearEasing)),
        label = "phaseA"
    )
    val phaseB by life.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(57000, easing = LinearEasing)),
        label = "phaseB"
    )
    val phaseC by life.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(23000, easing = LinearEasing)),
        label = "phaseC"
    )

    // Accent of the cover for the glows; falls back to the theme color for gray covers
    val accent = remember(art, isLight) { art?.let { accentFromBitmap(it, dark = !isLight) } }
    val glow by animateColorAsState(
        targetValue = accent ?: primary,
        animationSpec = tween(durationMillis = 800),
        label = "backdropGlow"
    )
    val glow2 = lerp(glow, tertiary, 0.35f)

    val saturation = remember { ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.4f) }) }
    val lightBoost = if (isLight) 0.10f else 0f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surface)
            .graphicsLayer { alpha = entranceAlpha }
    ) {
        Crossfade(
            targetState = art,
            animationSpec = tween(durationMillis = 700),
            label = "ArtBackdrop"
        ) { bmp ->
            if (bmp != null) {
                val soft = remember(bmp) { softBlurred(bmp) }
                val image = remember(soft) { soft.asImageBitmap() }
                Box(modifier = Modifier.fillMaxSize()) {
                    // Layer A: drifts one way
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.High,
                        colorFilter = saturation,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val amp = motion * (0.35f + 0.65f * energy)
                                val a = phaseA * TAU
                                val b = phaseB * TAU
                                val c = phaseC * TAU
                                val breath = 0.5f + 0.5f * sin(c)
                                val s = entranceScale * (1f + 0.07f * amp * breath)
                                scaleX = s
                                scaleY = s
                                translationX = size.width * 0.035f * amp * sin(a)
                                translationY = size.height * 0.030f * amp * cos(b)
                                rotationZ = 3.2f * amp * sin(b + 1.3f)
                            }
                    )
                    // Layer B: mirrored, goes the other way, shows through at half strength
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.High,
                        colorFilter = saturation,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val amp = motion * (0.35f + 0.65f * energy)
                                val a = phaseA * TAU
                                val b = phaseB * TAU
                                val c = phaseC * TAU
                                val breath = 0.5f + 0.5f * cos(c)
                                val s = entranceScale * 1.06f * (1f + 0.07f * amp * breath)
                                scaleX = -s
                                scaleY = s
                                translationX = -size.width * 0.035f * amp * cos(b + 0.6f)
                                translationY = -size.height * 0.030f * amp * sin(a)
                                rotationZ = -3.2f * amp * sin(a + 0.4f)
                                alpha = 0.50f + 0.10f * sin(c)
                            }
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(primaryContainer.copy(alpha = 0.55f), surface)
                            )
                        )
                )
            }
        }

        // Two soft color glows that wander over the picture
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val amp = motion * (0.35f + 0.65f * energy)
                    val a = phaseA * TAU
                    val b = phaseB * TAU
                    val c = phaseC * TAU
                    val w = size.width
                    val h = size.height
                    val strength = (0.20f + 0.10f * sin(c)) * (0.4f + 0.6f * energy) *
                        (if (isLight) 0.8f else 1f)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(glow.copy(alpha = strength), Color.Transparent),
                            center = Offset(w * (0.5f + 0.35f * amp * sin(a)), h * (0.28f + 0.12f * amp * cos(b))),
                            radius = w * 0.95f
                        )
                    )
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(glow2.copy(alpha = strength * 0.8f), Color.Transparent),
                            center = Offset(w * (0.5f - 0.35f * amp * cos(b + 0.7f)), h * (0.70f + 0.10f * amp * sin(a))),
                            radius = w * 0.85f
                        )
                    )
                }
        )

        // Veil: keeps text readable
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to surface.copy(alpha = (topScrim + lightBoost).coerceAtMost(0.95f)),
                        0.55f to surface.copy(alpha = (midScrim + lightBoost).coerceAtMost(0.95f)),
                        1f to surface
                    )
                )
        )
    }
}

/** Current track cover (or null if nothing is playing), used as background on screens without their own cover. */
@Composable
fun nowPlayingArt(): Bitmap? {
    val index = MusicPlayerManager.currentSongIndex
    return rememberSongArt(MusicPlayerManager.songs.getOrNull(index))
}

/**
 * Softens the cover once: tiny square bitmap + three box-blur passes (~gaussian).
 * Drawn later with high-quality filtering it looks like a strong blur, but costs nothing per frame.
 */
private fun softBlurred(src: Bitmap, side: Int = 64, radius: Int = 5, passes: Int = 3): Bitmap {
    val small = Bitmap.createScaledBitmap(src, side, side, true)
    val a = IntArray(side * side)
    small.getPixels(a, 0, side, 0, 0, side, side)
    val b = IntArray(side * side)
    repeat(passes) {
        boxBlurLine(a, b, side, radius, horizontal = true)
        boxBlurLine(b, a, side, radius, horizontal = false)
    }
    val out = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    out.setPixels(a, 0, side, 0, 0, side, side)
    return out
}

private fun boxBlurLine(src: IntArray, dst: IntArray, n: Int, r: Int, horizontal: Boolean) {
    val div = 2 * r + 1
    for (line in 0 until n) {
        fun at(i: Int): Int = if (horizontal) line * n + i else i * n + line
        var sa = 0
        var sr = 0
        var sg = 0
        var sb = 0
        for (k in -r..r) {
            val c = src[at(k.coerceIn(0, n - 1))]
            sa += c ushr 24
            sr += (c shr 16) and 255
            sg += (c shr 8) and 255
            sb += c and 255
        }
        for (i in 0 until n) {
            dst[at(i)] = ((sa / div) shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
            val add = src[at((i + r + 1).coerceAtMost(n - 1))]
            val rem = src[at((i - r).coerceAtLeast(0))]
            sa += (add ushr 24) - (rem ushr 24)
            sr += ((add shr 16) and 255) - ((rem shr 16) and 255)
            sg += ((add shr 8) and 255) - ((rem shr 8) and 255)
            sb += (add and 255) - (rem and 255)
        }
    }
}
