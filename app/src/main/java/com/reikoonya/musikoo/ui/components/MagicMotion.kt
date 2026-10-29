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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.util.MusicPlayerManager
import com.reikoonya.musikoo.util.UiStylePrefs
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
    val live = liveAnimationsEnabled()
    val duration = if (live) 700 else 300
    AnimatedContent(
        targetState = art,
        modifier = modifier,
        transitionSpec = {
            // The old cover stays opaque until the new one has fully arrived: no dark dip in the middle
            fadeIn(tween(duration, easing = FastOutSlowInEasing)) togetherWith
                fadeOut(tween(120, delayMillis = (duration - 100).coerceAtLeast(0))) using
                SizeTransform(clip = false)
        },
        label = "magicArt"
    ) { image ->
        val blur by transition.animateDp(
            transitionSpec = { tween(duration, easing = FastOutSlowInEasing) },
            label = "magicBlur"
        ) { state -> if (!live || !UiStylePrefs.blurEnabled || state == EnterExitState.Visible) 0.dp else 22.dp }
        val scale by transition.animateFloat(
            transitionSpec = { tween(duration, easing = FastOutSlowInEasing) },
            label = "magicScale"
        ) { state ->
            when {
                !live -> 1f
                state == EnterExitState.Visible -> 1f
                state == EnterExitState.PreEnter -> 1.16f
                else -> 0.92f
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
            if (!UiStylePrefs.liveAnimations) {
                fadeIn(tween(220)) togetherWith fadeOut(tween(160)) using SizeTransform(clip = true)
            } else {
                val dir = if (MusicPlayerManager.lastTransitionForward) 1 else -1
                (slideInHorizontally(tween(460, easing = FastOutSlowInEasing)) { it * dir / 3 } +
                    fadeIn(tween(340, delayMillis = 90))) togetherWith
                    (slideOutHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it * dir / 3 } +
                        fadeOut(tween(160))) using
                    SizeTransform(clip = true)
            }
        },
        label = "trackTextSwap"
    ) { l -> content(l) }
}

/**
 * Elements rise and fade in one after another when a screen opens. [index] sets the order.
 * A negative [offsetY] makes the element come down from above.
 *
 * Works only with "Live Animations" on; otherwise the element is simply there.
 * With [once] the entrance is not replayed when the element returns to the composition
 * (for items of a scrolling list): use it inside LazyColumn / LazyGrid.
 */
/**
 * Память каскада плиток главного экрана: сыграл один раз за запуск приложения и больше не повторяется
 * (ни при возврате на экран, ни при раскрытии секций, ни при прокрутке, ни при смене страницы).
 */
object TileCascadeMemory {
    @Volatile
    var finished = false
        private set
    private var windowStarted = false

    /** Окно каскада открывается при первом показе плиток и закрывается, когда последняя плитка доиграла. */
    fun startWindowIfNeeded(): Boolean {
        if (finished) return false
        windowStarted = true
        return true
    }

    fun finish() {
        finished = true
    }
}

/** Каскад для плиток главного экрана: проигрывается ровно один раз за запуск приложения. */
fun Modifier.tileCascade(index: Int, offsetY: Dp = 20.dp): Modifier = composed {
    val appContext = LocalContext.current.applicationContext
    remember { UiStylePrefs.ensureLoaded(appContext) }
    // Решение принимается один раз при появлении плитки: если каскад уже сыгран, плитка сразу на месте
    val skip = remember { !TileCascadeMemory.startWindowIfNeeded() }
    val progress = remember { Animatable(if (skip) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value >= 1f) return@LaunchedEffect
        if (!UiStylePrefs.liveAnimations) {
            progress.snapTo(1f)
        } else {
            delay(70L * index.coerceAtMost(10) + 80L)
            progress.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow))
        }
        // Первый запуск сыгран (или отключён): больше не повторяем
        TileCascadeMemory.finish()
    }
    val offsetPx = with(LocalDensity.current) { offsetY.toPx() }
    graphicsLayer {
        if (!UiStylePrefs.liveAnimations || progress.value >= 1f) return@graphicsLayer
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * offsetPx
    }
}

fun Modifier.staggeredEntrance(index: Int, offsetY: Dp = 28.dp, once: Boolean = false): Modifier = composed {
    val appContext = LocalContext.current.applicationContext
    remember { UiStylePrefs.ensureLoaded(appContext) }
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (once && played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (progress.value >= 1f) return@LaunchedEffect
        if (!UiStylePrefs.liveAnimations) {
            progress.snapTo(1f)
        } else {
            delay(70L * index.coerceAtMost(10) + 80L)
            progress.animateTo(1f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow))
        }
        played = true
    }
    val offsetPx = with(LocalDensity.current) { offsetY.toPx() }
    graphicsLayer {
        if (!UiStylePrefs.liveAnimations) return@graphicsLayer
        alpha = progress.value.coerceIn(0f, 1f)
        translationY = (1f - progress.value) * offsetPx
    }
}
