package com.reikoonya.musikoo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.ui.components.GlassCircleButton
import com.reikoonya.musikoo.ui.components.bounceClickable
import com.reikoonya.musikoo.ui.components.expressiveBadgeShape
import com.reikoonya.musikoo.ui.components.ShapeStyle
import com.reikoonya.musikoo.ui.components.glassFill
import com.reikoonya.musikoo.ui.components.glassStroke
import androidx.compose.animation.core.MutableTransitionState

/**
 * What the library pager tells the top bar: page titles and the live pager position
 * (currentPage + offset fraction), so the title in the top bar follows the swipe.
 */
@Stable
class LibraryTitleState {
    var titles by mutableStateOf<List<String>>(emptyList())
    var position by mutableFloatStateOf(0f)
}

/**
 * New top bar in the style of system screens: round glass button with vertical dots + plain title (`(⋮) Albums`). The button opens the menu sheet. The title follows Library swipes
 * (texts cross-slide and the pill smoothly changes width while you drag); without swipe tabs it says
 * "Library", in the statistics mode "Statistics".
 */
@Composable
fun BoxScope.MenuTopBar(
    mainMode: MainMode,
    libraryTabsMode: Boolean,
    titleState: LibraryTitleState,
    onMenuClick: () -> Unit,
    menuOpen: Boolean = false,
) {
    Row(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MenuButton(open = menuOpen, onClick = onMenuClick)

        Spacer(modifier = Modifier.width(14.dp))

        // Plain title next to the button, no pill: it sits on the blurred top fade
        AnimatedContent(
            targetState = mainMode == MainMode.Stats,
            transitionSpec = {
                (slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it } +
                    fadeIn(tween(240, delayMillis = 60))) togetherWith
                    (slideOutVertically(tween(240, easing = FastOutSlowInEasing)) { -it } +
                        fadeOut(tween(120))) using
                    SizeTransform(clip = false) { _, _ ->
                        spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                    }
            },
            label = "menuTopBarTitle"
        ) { stats ->
            when {
                stats -> TopBarTitle("Statistics")
                libraryTabsMode && titleState.titles.isNotEmpty() ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SwipeTitle(titleState.titles, titleState.position)
                        PaginationDots(titles = titleState.titles, position = titleState.position)
                    }
                else -> TopBarTitle("Library")
            }
        }
    }
}

/**
 * The "⋮" button of the new top bar. It animates with the menu sheet: the dots turn and melt into a cross
 * (and back), and in the expressive style the cookie-shaped button makes a half turn with a springy overshoot.
 * The shape is the cookie in MD3E and a plain circle in MD3.
 */
@Composable
fun MenuButton(
    open: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow),
        label = "menuButtonProgress"
    )
    val shape = remember(ShapeStyle.family) { expressiveBadgeShape(0) }
    val tint = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier
            .size(44.dp)
            // Read in the draw phase only: the turn does not recompose anything
            .graphicsLayer { rotationZ = progress * 180f }
            .bounceClickable(shape = shape, onClick = onClick),
        shape = shape,
        color = glassFill(),
        border = glassStroke(),
        tonalElevation = 2.dp
    ) {
        // The icons turn back against the button, so they only do their own small turn
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = -progress * 180f },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Menu",
                tint = tint,
                modifier = Modifier.graphicsLayer {
                    val p = progress.coerceIn(0f, 1f)
                    alpha = 1f - p
                    rotationZ = p * 90f
                    scaleX = 1f - 0.5f * p
                    scaleY = 1f - 0.5f * p
                }
            )
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.graphicsLayer {
                    val p = progress.coerceIn(0f, 1f)
                    alpha = p
                    rotationZ = (p - 1f) * 90f
                    scaleX = 0.5f + 0.5f * p
                    scaleY = 0.5f + 0.5f * p
                }
            )
        }
    }
}

@Composable
private fun TopBarTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        softWrap = false
    )
}

/**
 * Title that follows the pager: the current title slides out and fades while the next one slides in,
 * and the width of the title area is interpolated between the two, so the pill stretches smoothly.
 */
@Composable
private fun SwipeTitle(titles: List<String>, position: Float) {
    val style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val widths = remember(titles, style) { titles.map { measurer.measure(it, style).size.width } }

    val pos = position.coerceIn(0f, (titles.size - 1).toFloat())
    val i = pos.toInt().coerceIn(0, titles.lastIndex)
    val j = (i + 1).coerceAtMost(titles.lastIndex)
    val f = if (i == j) 0f else pos - i
    val widthPx = widths[i] + (widths[j] - widths[i]) * f
    val shiftPx = with(density) { 18.dp.toPx() }

    Box(
        modifier = Modifier.width(with(density) { widthPx.toDp() }),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = titles[i],
            style = style,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .graphicsLayer {
                    alpha = 1f - f
                    translationX = -f * shiftPx
                }
        )
        if (j != i) {
            Text(
                text = titles[j],
                style = style,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .wrapContentWidth(unbounded = true)
                    .graphicsLayer {
                        alpha = f
                        translationX = (1f - f) * shiftPx
                    }
            )
        }
    }
}

/**
 * Menu sheet that slides up from the bottom: Statistics, Settings, About. While the statistics mode
 * is open an extra "Library" item lets you go back. Closes by tapping the dim, swiping down or "back".
 */
@Composable
fun MainMenuSheet(
    visible: Boolean,
    mainMode: MainMode,
    onDismiss: () -> Unit,
    onLibraryClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
) {
    BackHandler(enabled = visible, onBack = onDismiss)

    // The sheet is only composed while it is open, so a plain AnimatedVisibility(visible = true) would start
    // already shown and never animate in. This state always starts hidden and then goes to the target.
    val sheetState = remember { MutableTransitionState(false) }.apply { targetState = visible }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (visible) 0.45f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "menuSheetScrim"
    )
    if (!visible && scrimAlpha < 0.001f && !sheetState.currentState) return

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
        )
        AnimatedVisibility(
            visibleState = sheetState,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
            ) { it } + fadeIn(tween(160)),
            exit = slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it } +
                fadeOut(tween(200))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        var total = 0f
                        detectVerticalDragGestures(
                            onDragStart = { total = 0f },
                            onVerticalDrag = { _, delta -> total += delta },
                            onDragEnd = { if (total > 140f) onDismiss() }
                        )
                    },
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    if (mainMode == MainMode.Stats) {
                        SideMenuItem(Icons.Rounded.LibraryMusic, "Library", selected = false, onClick = onLibraryClick)
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                    SideMenuItem(Icons.Rounded.BarChart, "Statistics", selected = mainMode == MainMode.Stats, onClick = onStatisticsClick)
                    Spacer(modifier = Modifier.height(6.dp))
                    SideMenuItem(Icons.Rounded.Settings, "Settings", selected = false, onClick = onSettingsClick)
                    Spacer(modifier = Modifier.height(6.dp))
                    SideMenuItem(Icons.Rounded.Info, "About", selected = false, onClick = onAboutClick)
                }
            }
        }

        // The top bar button stays sharp above the dim and turns into a cross; a tap closes the menu.
        // It starts closed for one frame, so it opens with the same turn as the button under it.
        var armed by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { armed = true }
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp)
        ) {
            MenuButton(open = visible && armed, onClick = onDismiss)
        }
    }
}

@Composable
private fun SideMenuItem(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh
            )
            .bounceClickable(shape = shape) { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = expressiveBadgeShape(title.hashCode()),
            // Accent badge: solid accent when selected, accent container otherwise
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PaginationDots(titles: List<String>, position: Float) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        titles.forEachIndexed { index, _ ->
            val distance = (position - index).let { if (it < 0f) -it else it }
            val selected = distance < 0.5f
            val dotWidth by animateDpAsState(
                targetValue = if (selected) 12.dp else 4.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "dotWidth"
            )
            val dotColor by animateColorAsState(
                targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                label = "dotColor"
            )
            Box(
                modifier = Modifier
                    .height(4.dp)
                    .width(dotWidth)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}
