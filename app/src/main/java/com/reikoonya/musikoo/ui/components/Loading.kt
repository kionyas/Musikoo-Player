package com.reikoonya.musikoo.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.ui.theme.ShapeFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/**
 * Loading indicator that follows the selected shape family:
 * MD3E = a shape that continuously morphs (cookie, clover, pentagon, sunny...) while turning,
 * MD3  = the standard circular progress indicator.
 *
 * [containerColor] draws a round container behind the MD3E shape (like a contained indicator).
 */
@Composable
fun AppLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    containerColor: Color? = null,
) {
    if (ShapeStyle.family == ShapeFamily.MD3E) {
        MorphingLoader(modifier, size, color, containerColor)
    } else {
        CircularProgressIndicator(
            modifier = modifier.size(size),
            color = color,
            strokeWidth = (size * 0.09f).coerceAtLeast(2.dp)
        )
    }
}

/** Full-area loading screen: the indicator in the middle and an optional caption. */
@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier,
    label: String? = null,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val expressive = ShapeStyle.family == ShapeFamily.MD3E
            AppLoadingIndicator(
                size = if (expressive) 88.dp else 56.dp,
                color = if (expressive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                containerColor = if (expressive) MaterialTheme.colorScheme.primaryContainer else null
            )
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MorphingLoader(
    modifier: Modifier,
    size: Dp,
    color: Color,
    containerColor: Color?,
) {
    val shapes = ExpressiveShapes.loader
    val n = shapes.size
    val transition = rememberInfiniteTransition(label = "loader")
    // Position in the shape sequence: the integer part is the current shape, the fraction is the morph progress
    val position by transition.animateFloat(
        initialValue = 0f,
        targetValue = n.toFloat(),
        animationSpec = infiniteRepeatable(tween(n * 800, easing = LinearEasing), RepeatMode.Restart),
        label = "loaderPosition"
    )
    val turn by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4800, easing = LinearEasing), RepeatMode.Restart),
        label = "loaderTurn"
    )

    Canvas(modifier = modifier.size(size)) {
        val whole = floor(position).toInt()
        val from = shapes[whole % n].radii
        val to = shapes[(whole + 1) % n].radii
        // The shape rests for a moment, then smoothly flows into the next one
        val morph = FastOutSlowInEasing.transform(((position - whole - 0.3f) / 0.7f).coerceIn(0f, 1f))

        if (containerColor != null) {
            drawCircle(color = containerColor, radius = this.size.minDimension / 2f)
        }
        val shapeRadius = this.size.minDimension / 2f * (if (containerColor != null) 0.62f else 1f)
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val path = Path()
        for (i in from.indices) {
            val r = (from[i] + (to[i] - from[i]) * morph) * shapeRadius
            val t = (2.0 * PI * i / from.size - PI / 2).toFloat()
            val x = cx + r * cos(t)
            val y = cy + r * sin(t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        // Constant turning plus an extra quarter turn with every morph, like the expressive loader
        rotate(degrees = turn + (whole + morph) * 40f, pivot = center) {
            drawPath(path, color)
        }
    }
}
