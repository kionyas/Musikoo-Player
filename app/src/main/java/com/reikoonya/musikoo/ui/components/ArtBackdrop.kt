package com.reikoonya.musikoo.ui.components

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.ui.theme.LocalUiStyle
import com.reikoonya.musikoo.ui.theme.UiStyle
import com.reikoonya.musikoo.util.MusicPlayerManager

/**
 * Track-art "aura" background: a heavily blurred image with a gradient veil.
 * Smooth spring reveal when the player screen opens.
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

    val surface = MaterialTheme.colorScheme.surface
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    var visible by remember { mutableStateOf(!animateEntrance) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val entranceScale by animateFloatAsState(
        targetValue = if (visible) 1.30f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "backdropScale"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "backdropAlpha"
    )

    val saturation = 1.35f

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
                // Android 12+: a decent-resolution bitmap + a real, very strong GPU blur.
                // Older versions have no blur, so a small bitmap smoothly stretched with high-quality filtering.
                val nativeBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                val side = if (nativeBlur) 192 else 32
                val scaled = remember(bmp, side) { Bitmap.createScaledBitmap(bmp, side, side, true) }
                Image(
                    bitmap = scaled.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    colorFilter = ColorFilter.colorMatrix(
                        ColorMatrix().apply { setToSaturation(saturation) }
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = entranceScale
                            scaleY = entranceScale
                        }
                        .then(
                            if (nativeBlur) Modifier.blur(80.dp, BlurredEdgeTreatment.Unbounded) else Modifier
                        )
                )
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to surface.copy(alpha = topScrim),
                        0.55f to surface.copy(alpha = midScrim),
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
