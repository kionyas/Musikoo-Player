package com.reikoonya.musikoo.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.ui.theme.MiniSwipeAction
import com.reikoonya.musikoo.util.MusicPlayerManager
import com.reikoonya.musikoo.util.UiStylePrefs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * Мини-плеер.
 *
 * Жесты:
 *  • тап — открыть плеер;
 *  • свайп вверх — карточка тянется за пальцем, растёт и открывает плеер;
 *  • свайп влево / вправо — действие из настроек (Settings → Mini-player Gestures):
 *    избранное, следующий/предыдущий трек, пауза, скрыть мини-плеер или ничего.
 *
 * Как устроен жест:
 *  • ось блокируется сразу после порога касания — диагональ не даёт случайных срабатываний;
 *  • «резиновое» сопротивление: чем дальше от центра, тем тяжелее тянуть;
 *  • карточка наклоняется как маятник за пальцем, значок действия выезжает сбоку и «щёлкает» на пороге;
 *  • быстрый бросок срабатывает и без полного хода (учитывается скорость);
 *  • отпускание — пружина с начальной скоростью пальца; «скрыть» уезжает за край экрана.
 *
 * Пока двигается ползунок, соседние элементы реагируют: обложка уменьшается и наклоняется,
 * название «уезжает» в противоположную сторону, кнопки отступают.
 */
@Composable
fun MiniPlayerCard(
    song: Song,
    art: Bitmap?,
    isFavorite: Boolean,
    onOpenPlayer: () -> Unit,
    onToggleFavorite: () -> Unit,
    onHide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val live = liveAnimationsEnabled()
    val appContext = LocalContext.current.applicationContext
    remember(appContext) { UiStylePrefs.ensureMiniLoaded(appContext) }
    val leftAction = UiStylePrefs.miniSwipeLeft
    val rightAction = UiStylePrefs.miniSwipeRight
    val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }

    val sidePx = remember(density) { with(density) { 88.dp.toPx() } }
    val upPx = remember(density) { with(density) { 64.dp.toPx() } }
    val flingPx = remember(density) { with(density) { 900.dp.toPx() } }
    val flickMinPx = remember(density) { with(density) { 20.dp.toPx() } }
    val parallaxPx = remember(density) { with(density) { 12.dp.toPx() } }

    // Пока палец на карточке — положение в dragX/dragY, после отпускания — в Animatable
    var isDragging by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val releaseX = remember { Animatable(0f) }
    val releaseY = remember { Animatable(0f) }
    val pop = remember { Animatable(1f) }
    val exitAlpha = remember { Animatable(1f) }
    var scrub by remember { mutableStateOf<Float?>(null) }

    val curLeft by rememberUpdatedState(leftAction)
    val curRight by rememberUpdatedState(rightAction)
    val curOpen by rememberUpdatedState(onOpenPlayer)
    val curFavorite by rememberUpdatedState(onToggleFavorite)
    val curHide by rememberUpdatedState(onHide)

    val x = if (isDragging) dragX else releaseX.value
    val y = if (isDragging) dragY else releaseY.value

    val scrubbing = scrub != null
    val grab by animateFloatAsState(
        targetValue = if (scrubbing && live) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "miniGrab"
    )
    // -0.5..0.5: куда сдвинут ползунок относительно середины
    val lean by animateFloatAsState(
        targetValue = if (live) 0.5f - (scrub ?: 0.5f) else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "miniLean"
    )

    val upRatio = (-y / upPx).coerceIn(0f, 1.2f)
    val leftRatio = (-x / sidePx).coerceIn(0f, 1.3f)
    val rightRatio = (x / sidePx).coerceIn(0f, 1.3f)
    val passedLeft = isDragging && leftAction != MiniSwipeAction.NONE && x <= -sidePx
    val passedRight = isDragging && rightAction != MiniSwipeAction.NONE && x >= sidePx
    val passedUp = isDragging && y <= -upPx

    // 0 — ничего, 1 — слева, 2 — справа, 3 — вверх
    val armed = when {
        passedUp -> 3
        passedLeft -> 1
        passedRight -> 2
        else -> 0
    }
    var lastArmed by remember { mutableStateOf(0) }
    LaunchedEffect(armed) {
        if (armed != 0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else if (lastArmed != 0 && isDragging) {
            // Вернулся под порог — лёгкий «отщёлк»
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        lastArmed = armed
    }

    val isPlaying = MusicPlayerManager.isPlayingState
    val (leftIcon, leftLabel) = swipeVisual(leftAction, isFavorite, isPlaying)
    val (rightIcon, rightLabel) = swipeVisual(rightAction, isFavorite, isPlaying)

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Значки за карточкой, видны при перетаскивании вбок
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (x > 6f && rightAction != MiniSwipeAction.NONE) {
                DragBadge(
                    ratio = rightRatio,
                    passed = passedRight,
                    icon = rightIcon,
                    label = rightLabel,
                    fromStart = true
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
            if (x < -6f && leftAction != MiniSwipeAction.NONE) {
                DragBadge(
                    ratio = leftRatio,
                    passed = passedLeft,
                    icon = leftIcon,
                    label = leftLabel,
                    fromStart = false
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = x
                    // Карточка тянется вверх за пальцем и немного вырастает
                    translationY = y * 0.55f
                    val s = (1f + 0.035f * upRatio) * pop.value
                    scaleX = s
                    scaleY = s
                    // Наклон как у маятника: ось вращения ниже карточки
                    transformOrigin = TransformOrigin(0.5f, 2.4f)
                    rotationZ = (x / sidePx).coerceIn(-1.5f, 1.5f) * 2.2f
                    alpha = exitAlpha.value
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val tracker = VelocityTracker()
                        tracker.addPosition(down.uptimeMillis, down.position)

                        // 0 — ось не выбрана, 1 — горизонталь, 2 — вертикаль
                        var axis = 0
                        var vx = 0f
                        var vy = 0f
                        var gx = 0f
                        var gy = 0f

                        // Если карточка ещё летит назад — подхватываем её с текущего места
                        val startX = releaseX.value
                        val startY = releaseY.value

                        fun resist(v: Float, d: Float, range: Float): Float {
                            val backToZero = v != 0f && (v > 0f) != (d > 0f)
                            val k = if (backToZero) 1f else (1f - abs(v) / range).coerceIn(0.10f, 1f)
                            return v + d * k
                        }

                        fun applyDelta(dx: Float, dy: Float) {
                            if (axis == 1) {
                                val moving = if (dragX + dx >= 0f) curRight else curLeft
                                val range = if (moving == MiniSwipeAction.NONE) sidePx * 0.35f else sidePx * 2.4f
                                dragX = resist(dragX, dx, range)
                            } else if (axis == 2) {
                                // Только вверх; вниз почти не даём тянуть
                                val next = resist(dragY, dy, upPx * 2.4f)
                                dragY = next.coerceAtMost(upPx * 0.08f)
                            }
                        }

                        val first = awaitTouchSlopOrCancellation(down.id) { change, over ->
                            axis = if (abs(over.x) >= abs(over.y)) 1 else 2
                            scope.launch { releaseX.stop(); releaseY.stop() }
                            dragX = startX
                            dragY = startY
                            isDragging = true
                            applyDelta(over.x, over.y)
                            change.consume()
                        } ?: return@awaitEachGesture

                        tracker.addPosition(first.uptimeMillis, first.position)
                        val completed = drag(first.id) { change ->
                            val d = change.positionChange()
                            tracker.addPosition(change.uptimeMillis, change.position)
                            applyDelta(d.x, d.y)
                            change.consume()
                        }

                        val v = tracker.calculateVelocity()
                        vx = v.x
                        vy = v.y
                        gx = dragX
                        gy = dragY

                        // Что именно сработало
                        val horizontalDir = sign(gx)
                        val action = if (horizontalDir > 0f) curRight else curLeft
                        val flickedSideways = abs(vx) > flingPx && sign(vx) == horizontalDir && abs(gx) > flickMinPx
                        val commitSide = completed && axis == 1 && action != MiniSwipeAction.NONE &&
                            (abs(gx) >= sidePx || flickedSideways)
                        val flickedUp = vy < -flingPx && gy < -flickMinPx
                        val commitUp = completed && axis == 2 && (gy <= -upPx || flickedUp)

                        scope.launch {
                            releaseX.snapTo(gx)
                            releaseY.snapTo(gy)
                            isDragging = false

                            val back = spring<Float>(
                                dampingRatio = 0.68f,
                                stiffness = Spring.StiffnessMediumLow
                            )

                            when {
                                commitSide && action == MiniSwipeAction.HIDE -> {
                                    // Уезжает за край экрана и гаснет
                                    launch { exitAlpha.animateTo(0f, tween(200, delayMillis = 40)) }
                                    releaseX.animateTo(
                                        horizontalDir * screenWidthPx * 1.1f,
                                        tween(260, easing = FastOutLinearInEasing)
                                    )
                                    curHide()
                                    delay(120)
                                    // Если карточка всё ещё в композиции — вернуть на место
                                    releaseX.snapTo(0f)
                                    exitAlpha.snapTo(1f)
                                }

                                commitSide -> {
                                    when (action) {
                                        MiniSwipeAction.FAVORITE -> curFavorite()
                                        MiniSwipeAction.NEXT -> MusicPlayerManager.skipNext()
                                        MiniSwipeAction.PREVIOUS -> MusicPlayerManager.skipPrevious()
                                        MiniSwipeAction.PLAY_PAUSE -> MusicPlayerManager.togglePlayPause()
                                        else -> Unit
                                    }
                                    launch {
                                        pop.animateTo(1.045f, tween(90))
                                        pop.animateTo(
                                            1f,
                                            spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
                                        )
                                    }
                                    launch { releaseY.animateTo(0f, back) }
                                    releaseX.animateTo(0f, back, initialVelocity = vx)
                                }

                                commitUp -> {
                                    curOpen()
                                    launch { releaseX.animateTo(0f, back) }
                                    releaseY.animateTo(0f, back, initialVelocity = vy)
                                }

                                else -> {
                                    launch { releaseX.animateTo(0f, back, initialVelocity = vx) }
                                    releaseY.animateTo(0f, back, initialVelocity = vy)
                                }
                            }
                        }
                    }
                }
                .bounceClickable(shape = shapeOf(28.dp)) { onOpenPlayer() },
            shape = shapeOf(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            border = glassStroke(),
            tonalElevation = 2.dp,
            shadowElevation = (2f + 10f * upRatio).dp
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Ручка: намекает, что карточку можно потянуть вверх
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 5.dp)
                        .size(width = (28f + 16f * upRatio.coerceAtMost(1f)).dp, height = 4.dp)
                        .graphicsLayer { alpha = 0.22f + 0.7f * upRatio }
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Обложка: растёт при свайпе вверх, наклоняется при перемотке
                        Surface(
                            modifier = Modifier
                                .size(52.dp)
                                .graphicsLayer {
                                    val s = (1f + 0.12f * upRatio) * (1f - 0.08f * grab)
                                    scaleX = s
                                    scaleY = s
                                    rotationZ = -lean * 12f * grab
                                    // Лёгкая глубина: обложка чуть отстаёт от карточки при боковом свайпе
                                    translationX = -x * 0.05f
                                }
                                .clip(coverShapeOf(12.dp)),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            MagicArt(
                                art = art,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // 2. Название: едет против движения ползунка
                        TrackTextSwap(
                            label = TrackLabel(song.id, song.title, song.artist),
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer {
                                    translationX = lean * parallaxPx * 2f * grab + x * 0.03f
                                    alpha = 1f - 0.3f * grab
                                }
                        ) { l ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                CascadeText(
                                    text = l.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    playOnce = false,
                                    modifier = Modifier.marqueeEdgeFade()
                                )
                                CascadeText(
                                    text = l.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    delayMillis = 100,
                                    playOnce = false,
                                    modifier = Modifier.marqueeEdgeFade()
                                )
                            }
                        }

                        // 3. Кнопки: отступают, пока двигается ползунок
                        Row(
                            modifier = Modifier.graphicsLayer {
                                val s = 1f - 0.08f * grab
                                scaleX = s
                                scaleY = s
                                alpha = 1f - 0.45f * grab
                                translationX = lean * parallaxPx * grab
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            BounceIconButton(onClick = { MusicPlayerManager.skipPrevious() }) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = stringResource(R.string.previous),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            ExpressivePlayButton(
                                isPlaying = MusicPlayerManager.isPlayingState,
                                onClick = { MusicPlayerManager.togglePlayPause() },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                width = 48.dp,
                                height = 44.dp,
                                iconSize = 22.dp,
                                playingCorner = 14.dp,
                                idleCorner = 22.dp
                            )
                            BounceIconButton(onClick = { MusicPlayerManager.skipNext() }) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = stringResource(R.string.next),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 4. Ползунок + время
                    MiniBarSeekBar(
                        isPlaying = MusicPlayerManager.isPlayingState,
                        onScrubChange = { scrub = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun swipeVisual(
    action: MiniSwipeAction,
    isFavorite: Boolean,
    isPlaying: Boolean
): Pair<ImageVector, String> = when (action) {
    MiniSwipeAction.FAVORITE ->
        if (isFavorite) Icons.Rounded.Favorite to stringResource(R.string.remove_from_favorites)
        else Icons.Rounded.FavoriteBorder to stringResource(R.string.add_to_favorites)
    MiniSwipeAction.HIDE -> Icons.Rounded.Close to stringResource(R.string.hide_mini_player)
    MiniSwipeAction.NEXT -> Icons.Rounded.SkipNext to stringResource(R.string.next)
    MiniSwipeAction.PREVIOUS -> Icons.Rounded.SkipPrevious to stringResource(R.string.previous)
    MiniSwipeAction.PLAY_PAUSE ->
        if (isPlaying) Icons.Rounded.Pause to stringResource(R.string.pause)
        else Icons.Rounded.PlayArrow to stringResource(R.string.play)
    MiniSwipeAction.NONE -> Icons.Rounded.Close to ""
}

/**
 * Значок действия за карточкой: выезжает из-под неё, растёт по мере хода,
 * на пороге «щёлкает» (пружинистый всплеск), меняет цвет и показывает подпись.
 */
@Composable
private fun DragBadge(
    ratio: Float,
    passed: Boolean,
    icon: ImageVector,
    label: String,
    fromStart: Boolean,
) {
    val density = LocalDensity.current
    val slidePx = remember(density) { with(density) { 36.dp.toPx() } }
    val snap by animateFloatAsState(
        targetValue = if (passed) 1.18f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "badgeSnap"
    )
    val container by animateColorAsState(
        targetValue = if (passed) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "badgeColor"
    )
    Surface(
        shape = CircleShape,
        color = container,
        border = glassStroke(),
        modifier = Modifier.graphicsLayer {
            val p = ratio.coerceAtMost(1f)
            val s = (0.7f + 0.3f * p) * snap
            scaleX = s
            scaleY = s
            alpha = (ratio * 1.6f).coerceIn(0f, 1f)
            // Выезжает из-под карточки к краю
            translationX = (1f - p) * (if (fromStart) -slidePx else slidePx)
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (passed) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
            AnimatedVisibility(
                visible = passed && label.isNotEmpty(),
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
