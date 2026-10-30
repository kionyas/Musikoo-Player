package com.reikoonya.musikoo.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.reikoonya.musikoo.util.UiStylePrefs
import androidx.compose.ui.text.style.TextAlign

/** Spring used when a top pill grows or shrinks to fit new content. */
private fun <T> pillSizeSpring() = spring<T>(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)

/**
 * Switches the content of a top pill (e.g. title <-> selection toolbar):
 * the pill smoothly expands/shrinks while the old content fades out and the new one scales in.
 * Put it directly inside the pill's Surface.
 */
@Composable
fun <T> PillTransition(targetState: T, content: @Composable (T) -> Unit) {
    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            (fadeIn(tween(240, delayMillis = 90)) +
                scaleIn(tween(320, delayMillis = 40, easing = FastOutSlowInEasing), initialScale = 0.86f)) togetherWith
                fadeOut(tween(110)) using
                SizeTransform(clip = false) { _, _ -> pillSizeSpring() }
        },
        label = "pillContent"
    ) { state -> content(state) }
}

/** Top pill with a grabber and a title; when the title changes the pill expands with the new text. */
@Composable
fun TopPill(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(max = 210.dp),
        shape = shapeOf(32.dp),
        color = glassFill(),
        border = glassStroke()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
            Spacer(modifier = Modifier.height(4.dp))
            AnimatedContent(
                targetState = text,
                transitionSpec = {
                    (slideInVertically(tween(340, easing = FastOutSlowInEasing)) { it / 2 } +
                        fadeIn(tween(260, delayMillis = 70))) togetherWith
                        (slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { -it / 2 } +
                            fadeOut(tween(140))) using
                        SizeTransform(clip = false) { _, _ -> pillSizeSpring() }
                },
                label = "pillTitle"
            ) { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.marqueeEdgeFade()
                )
            }
        }
    }
}

/**
 * Is the new top header ("Menu Top Bar" setting) on: screens then show a plain title next to the
 * round button (like the main screen) instead of the pill with a grabber.
 */
@Composable
fun rememberMenuTopBarActive(): Boolean {
    val appContext = LocalContext.current.applicationContext
    remember { UiStylePrefs.ensureLoaded(appContext) }
    return UiStylePrefs.menuTopBar
}

/** "INTERFACE & INTERACTION" -> "Interface & interaction": pill titles are upper case, plain ones are not. */
private fun String.toPlainTitle(): String =
    if (any { it.isLetter() } && this == uppercase()) lowercase().replaceFirstChar { it.uppercase() } else this

/** Plain centered title of the new top header: no pill, sits on the blurred top fade. Changes with the same slide as on the main screen. */
@Composable
fun PlainTopTitle(text: String, modifier: Modifier = Modifier, normalizeCase: Boolean = true) {
    AnimatedContent(
        targetState = if (normalizeCase) text.toPlainTitle() else text,
        modifier = modifier,
        transitionSpec = {
            (slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it } +
                fadeIn(tween(240, delayMillis = 60))) togetherWith
                (slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { -it } +
                    fadeOut(tween(120))) using
                SizeTransform(clip = false) { _, _ -> pillSizeSpring() }
        },
        label = "plainTopTitle"
    ) { title ->
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
            modifier = Modifier.marqueeEdgeFade(centerWhenFits = true)
        )
    }
}

/**
 * Title slot of a floating top bar Row (back button, title, spacer): the pill by default, and with the
 * "Menu Top Bar" setting a plain centered title without the pill.
 */
@Composable
fun RowScope.TopBarTitle(text: String) {
    if (rememberMenuTopBarActive()) {
        Box(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
        ) { PlainTopTitle(text) }
    } else {
        TopPill(text = text)
    }
}
