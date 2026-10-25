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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
fun TopPill(text: String) {
    Surface(
        shape = shapeOf(32.dp),
        color = glassFill(),
        border = glassStroke()
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
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
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
