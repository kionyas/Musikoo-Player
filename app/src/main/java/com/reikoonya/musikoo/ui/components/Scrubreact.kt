package com.reikoonya.musikoo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.util.UiStylePrefs
import kotlinx.coroutines.flow.collectLatest

/**
 * Состояние перемотки: ползунок пишет сюда, остальные элементы экрана реагируют
 * через [scrubReact]. Всё читается на этапе отрисовки, поэтому экран при движении ползунка
 * не пересобирается.
 */
@Stable
class ScrubState {
    /** Палец на ползунке. */
    var active by mutableStateOf(false)

    /** Положение ползунка, 0..1. */
    var fraction by mutableFloatStateOf(0.5f)
}

/** Включены ли «живые» анимации (настройка Interface and interaction → Live Animations). */
@Composable
fun liveAnimationsEnabled(): Boolean {
    val appContext = LocalContext.current.applicationContext
    remember { UiStylePrefs.ensureLoaded(appContext) }
    return UiStylePrefs.liveAnimations
}

/**
 * «Живой сосед» ползунка. Пока ползунок двигается, элемент:
 *  • [shift] — отъезжает в сторону, противоположную ползунку (в начале трека вправо, в конце влево);
 *  • [tiltDegrees] — наклоняется;
 *  • [shrink] — уменьшается (0.1 = на 10%);
 *  • [fade] — становится прозрачнее (0.3 = до 70% непрозрачности).
 * После отпускания всё пружинисто возвращается.
 */
fun Modifier.scrubReact(
    state: ScrubState,
    shift: Dp = 0.dp,
    tiltDegrees: Float = 0f,
    shrink: Float = 0f,
    fade: Float = 0f,
): Modifier = composed {
    val appContext = LocalContext.current.applicationContext
    remember { UiStylePrefs.ensureLoaded(appContext) }
    val lean = remember { Animatable(0f) }
    val grab = remember { Animatable(0f) }

    LaunchedEffect(state) {
        snapshotFlow { state.active }.collectLatest { active ->
            grab.animateTo(
                if (active) 1f else 0f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { if (state.active) 0.5f - state.fraction else 0f }.collectLatest { target ->
            lean.animateTo(
                target,
                spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    graphicsLayer {
        if (!UiStylePrefs.liveAnimations) return@graphicsLayer
        translationX = lean.value * 2f * shift.toPx()
        rotationZ = -lean.value * 2f * tiltDegrees
        val s = 1f - shrink * grab.value
        scaleX = s
        scaleY = s
        alpha = 1f - fade * grab.value
    }
}