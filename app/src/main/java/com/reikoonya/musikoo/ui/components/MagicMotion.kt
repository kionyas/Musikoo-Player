package com.reikoonya.musikoo.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.util.MusicPlayerManager
import kotlinx.coroutines.delay

/**
 * Cover art with a "magic" change: the new cover appears out of a blur and slightly zoomed in,
 * while the old one dissolves into a blur and shrinks away. Put it inside a clipped container.
 */
@Composable
fun MagicArt(
    art: Bitmap?,
    modifier: Modifier = Modifier.fillMaxSize(),
    placeholder: @Composable () -> Unit,
) {
    val duration = 700
    AnimatedContent(
        targetState = art,
        modifier = modifier,
        transitionSpec = {
            // The old cover stays opaque until the new one has fully arrived: no dark dip in the middle
            fadeIn(tween(duration, easing = FastOutSlowInEasing)) togetherWith
                fadeOut(tween(120, delayMillis = duration - 100)) using
                SizeTransform(clip = false)
        },
        label = "magicArt"
    ) { image ->
        val blur by transition.animateDp(
            transitionSpec = { tween(duration, easing = FastOutSlowInEasing) },
            label = "magicBlur"
        ) { state -> if (state == EnterExitState.Visible) 0.dp else 22.dp }
        val scale by transition.animateFloat(
            transitionSpec = { tween(duration, easing = FastOutSlowInEasing) },
            label = "magicScale"
        ) { state ->
            when (state) {
                EnterExitState.Visible -> 1f
                EnterExitState.PreEnter -> 1.16f
                EnterExitState.PostExit -> 0.92f
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .blur(blur, BlurredEdgeTreatment.Unbounded),
            contentAlignment = Alignment.Center
        ) {
            if (image != null) {
                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                placeholder()
            }
        }
    }
}

/** What identifies the text of a track: it changes only when the track (or its tags) change, not when the cover loads. */
data class TrackLabel(val id: Long?, val title: String, val artist: String)

/**
 * Track change for text: the old title slides out in the direction of travel, the new one slides in.
 * Direction follows the last skip (next / previous). Use [label] inside the content, not the live song.
 */
@Composable
fun TrackTextSwap(
    label: TrackLabel,
    modifier: Modifier = Modifier,
    content: @Composable (TrackLabel) -> Unit,
) {
    AnimatedContent(
        targetState = label,
        modifier = modifier,
        transitionSpec = {
            val dir = if (MusicPlayerManager.lastTransitionForward) 1 else -1
            (slideInHorizontally(tween(460, easing = FastOutSlowInEasing)) { it * dir / 3 } +
                fadeIn(tween(340, delayMillis = 90))) togetherWith
                (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it * dir / 3 } +
                    fadeOut(tween(160))) using
                SizeTransform(clip = true)
        },
        label = "trackTextSwap"
    ) { l -> content(l) }
}

/**
 * Elements rise and fade in one after another when a screen opens. [index] sets the order.
 * A negative [offsetY] makes the element come down from above.
 */
fun Modifier.staggeredEntrance(index: Int, offsetY: Dp = 28.dp): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(70L * index + 80L)
        progress.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow))
    }
    val offsetPx = with(LocalDensity.current) { offsetY.toPx() }
    graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * offsetPx
    }
}
