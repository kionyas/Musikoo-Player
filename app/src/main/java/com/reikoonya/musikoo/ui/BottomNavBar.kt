package com.reikoonya.musikoo.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.util.BottomBarItem
import com.reikoonya.musikoo.util.UiStylePrefs
import kotlinx.coroutines.delay

private val NavGap = 6.dp
private val NavMinItemWidth = 108.dp

/**
 * Нижняя навигация главного экрана.
 *
 * - Пока элементы помещаются, они делят ширину поровну.
 * - Когда их больше, чем влезает, панель прокручивается по горизонтали, а на краях,
 *   куда ещё можно прокрутить, появляется затухание.
 * - Ширина элементов меняется пружиной, новые элементы выезжают с лёгким отскоком.
 *
 * Используется и на главном экране, и в предпросмотре в настройках.
 */
@Composable
internal fun BottomNavBar(
    items: List<BottomBarItem>,
    onItemClick: (BottomBarItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Ключи, которые уже показывались: вернувшийся при прокрутке элемент не анимируется второй раз
    val seen = remember { HashSet<String>() }
    val leftFade by animateFloatAsState(
        targetValue = if (listState.canScrollBackward) 1f else 0f,
        animationSpec = tween(220),
        label = "navFadeLeft"
    )
    val rightFade by animateFloatAsState(
        targetValue = if (listState.canScrollForward) 1f else 0f,
        animationSpec = tween(220),
        label = "navFadeRight"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        val count = items.size.coerceAtLeast(1)
        val fitWidth: Dp = (maxWidth - NavGap * (count - 1)) / count
        val targetWidth: Dp = if (fitWidth > NavMinItemWidth) fitWidth else NavMinItemWidth
        val itemWidth by animateDpAsState(
            targetValue = targetWidth,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
            label = "navItemWidth"
        )

        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fadePx = 22.dp.toPx()
                    if (leftFade > 0.01f) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Black.copy(alpha = 1f - leftFade), Color.Black),
                                startX = 0f,
                                endX = fadePx
                            ),
                            topLeft = Offset.Zero,
                            size = Size(fadePx, size.height),
                            blendMode = BlendMode.DstIn
                        )
                    }
                    if (rightFade > 0.01f) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Black, Color.Black.copy(alpha = 1f - rightFade)),
                                startX = size.width - fadePx,
                                endX = size.width
                            ),
                            topLeft = Offset(size.width - fadePx, 0f),
                            size = Size(fadePx, size.height),
                            blendMode = BlendMode.DstIn
                        )
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(NavGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(items, key = { _, item -> item.key }) { index, item ->
                val animateIn = remember { item.key !in seen }
                LaunchedEffect(Unit) { seen.add(item.key) }
                BottomBarSegment(
                    item = item,
                    width = itemWidth,
                    index = index,
                    animateIn = animateIn,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun BottomBarSegment(
    item: BottomBarItem,
    width: Dp,
    index: Int,
    animateIn: Boolean,
    onClick: () -> Unit,
) {
    val appear = remember { Animatable(if (animateIn && UiStylePrefs.liveAnimations) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (appear.value < 1f) {
            delay(45L * index.coerceAtMost(6))
            appear.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
            )
        }
    }
    NavSegment(
        label = item.barTitle(),
        icon = item.barIcon,
        modifier = Modifier
            .width(width)
            .graphicsLayer {
                val p = appear.value
                alpha = p.coerceIn(0f, 1f)
                val s = 0.7f + 0.3f * p
                scaleX = s
                scaleY = s
                translationY = (1f - p) * 14.dp.toPx()
            },
        onClick = onClick
    )
}
