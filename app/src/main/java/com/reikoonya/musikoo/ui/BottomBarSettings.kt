package com.reikoonya.musikoo.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.reikoonya.musikoo.ui.components.MusikooSwitch
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
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.reikoonya.musikoo.model.Song
import com.reikoonya.musikoo.ui.components.cardFill
import com.reikoonya.musikoo.ui.components.expressiveBadgeShape
import com.reikoonya.musikoo.ui.components.shapeOf
import com.reikoonya.musikoo.util.BottomBarItem
import com.reikoonya.musikoo.util.BottomBarPrefs
import com.reikoonya.musikoo.util.groupByGenre
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Страница «Bottom Bar» в настройках: предпросмотр, поиск, порядок и состав элементов. */
@Composable
internal fun BottomBarSettingsPage(onAddClick: () -> Unit) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    remember(appContext) { BottomBarPrefs.ensureLoaded(appContext) }

    BottomBarPreview()

    SettingsGroupCard {
        SettingsCardItem(
            icon = Icons.Rounded.Search,
            title = "Search Bar",
            description = "The search field above the navigation. Turn it off and add a Search button to the bar instead",
            grouped = true,
            trailing = {
                MusikooSwitch(
                    checked = BottomBarPrefs.searchVisible,
                    onCheckedChange = { BottomBarPrefs.setSearchVisible(appContext, it) }
                )
            },
            onClick = { BottomBarPrefs.setSearchVisible(appContext, !BottomBarPrefs.searchVisible) }
        )
    }

    Text(
        text = "ITEMS",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp)
    )
    Text(
        text = "Hold the handle and drag to reorder. The bar scrolls sideways when it has more items than fit.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp)
    )

    ReorderableBarItems(
        keys = BottomBarPrefs.items,
        onMove = { from, to -> BottomBarPrefs.move(appContext, from, to) },
        onRemove = { BottomBarPrefs.remove(appContext, it) }
    )

    SettingsCardItem(
        icon = Icons.Rounded.Add,
        title = "Add Item",
        description = "Pages, Search, Random or a shortcut to a genre",
        onClick = onAddClick
    )
    SettingsCardItem(
        icon = Icons.Rounded.Refresh,
        title = "Reset to Default",
        description = "Artists, Songs, Random and the search bar",
        onClick = { BottomBarPrefs.reset(appContext) }
    )
}

/** Настоящая панель, как на главном экране: меняется сразу при каждом изменении ниже. */
@Composable
private fun BottomBarPreview() {
    val keys = BottomBarPrefs.items
    val items = remember(keys) { keys.mapNotNull { BottomBarItem.fromKey(it) } }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            AnimatedVisibility(
                visible = BottomBarPrefs.searchVisible,
                enter = fadeIn(tween(200)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(tween(150)) + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Column {
                    CollapsedSearchPill(onClick = {}, navHidden = false, onToggleNav = {})
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
            if (items.isEmpty()) {
                Text(
                    text = if (BottomBarPrefs.searchVisible) "No navigation items" else "The bottom bar is hidden",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 12.dp)
                )
            } else {
                BottomNavBar(items = items, onItemClick = {})
            }
        }
    }
}

private val RowHeight = 64.dp
private val RowGap = 8.dp

/**
 * Список элементов панели с перетаскиванием за ручку. Соседние строки расступаются пружиной,
 * отпущенная строка плавно садится на место, новые появляются с отскоком, удалённые схлопываются.
 */
@Composable
private fun ReorderableBarItems(
    keys: List<String>,
    onMove: (Int, Int) -> Unit,
    onRemove: (String) -> Unit,
) {
    val density = LocalDensity.current
    val stepPx = with(density) { (RowHeight + RowGap).toPx() }
    var draggingKey by remember { mutableStateOf<String?>(null) }
    val totalHeight: Dp by animateDpAsState(
        targetValue = if (keys.isEmpty()) 0.dp else (RowHeight + RowGap) * keys.size - RowGap,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "barListHeight"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight)
    ) {
        keys.forEachIndexed { index, itemKey ->
            key(itemKey) {
                BarItemRow(
                    itemKey = itemKey,
                    index = index,
                    lastIndex = keys.lastIndex,
                    stepPx = stepPx,
                    draggingKey = draggingKey,
                    onDragStart = { draggingKey = it },
                    onDragFinished = { draggingKey = null },
                    onMove = onMove,
                    onRemove = onRemove
                )
            }
        }
    }
}

@Composable
private fun BarItemRow(
    itemKey: String,
    index: Int,
    lastIndex: Int,
    stepPx: Float,
    draggingKey: String?,
    onDragStart: (String) -> Unit,
    onDragFinished: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (String) -> Unit,
) {
    val item = remember(itemKey) { BottomBarItem.fromKey(itemKey) } ?: return
    val scope = rememberCoroutineScope()
    val currentIndex by rememberUpdatedState(index)
    val currentLastIndex by rememberUpdatedState(lastIndex)
    val currentDraggingKey by rememberUpdatedState(draggingKey)
    val isDragging = draggingKey == itemKey
    var dragOffset by remember { mutableFloatStateOf(0f) }

    val animatedY by animateFloatAsState(
        targetValue = index * stepPx,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium),
        label = "barRowY"
    )
    val lift by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "barRowLift"
    )

    // Появление при добавлении и схлопывание при удалении
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    LaunchedEffect(visibleState.currentState, visibleState.targetState) {
        // Выход доигран: текущее состояние уже «скрыто» и цель тоже «скрыто»
        if (!visibleState.currentState && !visibleState.targetState) onRemove(itemKey)
    }

    AnimatedVisibility(
        visibleState = visibleState,
        modifier = Modifier
            .fillMaxWidth()
            .height(RowHeight)
            .zIndex(if (isDragging) 2f else 0f)
            .offset {
                val base = if (isDragging) index * stepPx else animatedY
                IntOffset(0, (base + dragOffset).roundToInt())
            }
            .graphicsLayer {
                val s = 1f + 0.03f * lift
                scaleX = s
                scaleY = s
            },
        enter = fadeIn(tween(220)) + scaleIn(
            animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
            initialScale = 0.85f
        ),
        exit = fadeOut(tween(160)) + scaleOut(tween(180), targetScale = 0.9f)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(RowHeight),
            shape = shapeOf(24.dp),
            color = if (isDragging) MaterialTheme.colorScheme.primaryContainer else cardFill(),
            shadowElevation = (10f * lift).dp
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = if (isDragging) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp)
                        .pointerInput(itemKey) {
                            var active = false
                            var localIndex = 0
                            detectDragGestures(
                                onDragStart = {
                                    if (currentDraggingKey == null) {
                                        active = true
                                        localIndex = currentIndex
                                        dragOffset = 0f
                                        onDragStart(itemKey)
                                    }
                                },
                                onDrag = { change, amount ->
                                    if (!active) return@detectDragGestures
                                    change.consume()
                                    dragOffset += amount.y
                                    val target = ((localIndex * stepPx + dragOffset) / stepPx)
                                        .roundToInt()
                                        .coerceIn(0, currentLastIndex)
                                    if (target != localIndex) {
                                        onMove(localIndex, target)
                                        dragOffset -= (target - localIndex) * stepPx
                                        localIndex = target
                                    }
                                },
                                onDragEnd = {
                                    if (active) {
                                        active = false
                                        scope.launch {
                                            animate(
                                                initialValue = dragOffset,
                                                targetValue = 0f,
                                                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)
                                            ) { value, _ -> dragOffset = value }
                                            onDragFinished()
                                        }
                                    }
                                },
                                onDragCancel = {
                                    if (active) {
                                        active = false
                                        dragOffset = 0f
                                        onDragFinished()
                                    }
                                }
                            )
                        }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = expressiveBadgeShape(itemKey.hashCode()),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.barIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.barTitle(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.barDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { visibleState.targetState = false }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove from bottom bar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Содержимое шита «Add to Bottom Bar»: встроенные элементы и жанры из библиотеки. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BottomBarAddSheetContent(songs: List<Song>) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    remember(appContext) { BottomBarPrefs.ensureLoaded(appContext) }
    val keys = BottomBarPrefs.items
    val available = BottomBarItem.builtIn.filter { it.key !in keys }
    val genres = remember(songs) {
        groupByGenre(songs).sortedByDescending { it.songs.size }.take(80)
    }
    val freeGenres = genres.filter { BottomBarItem.genreKey(it.name) !in keys }

    Text(
        text = "Add to Bottom Bar",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
    Text(
        text = "Tap to add. Items you add land at the end of the bar and you can drag them anywhere.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )

    Column(
        modifier = Modifier.animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        available.forEach { item ->
            key(item.key) {
                SettingsCardItem(
                    onSheet = true,
                    icon = item.barIcon,
                    title = item.barTitle(),
                    description = item.barDescription,
                    trailing = {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = { BottomBarPrefs.add(appContext, item.key) }
                )
            }
        }
        if (available.isEmpty()) {
            Text(
                text = "Every built-in item is already on the bar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }

    Text(
        text = "GENRE SHORTCUTS",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp)
    )
    Text(
        text = "Each one opens the list of tracks with that genre tag.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
    if (genres.isEmpty()) {
        Text(
            text = "No genre tags found in your library yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    } else {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            freeGenres.forEach { group ->
                key(group.name) {
                    FilterChip(
                        selected = false,
                        onClick = { BottomBarPrefs.add(appContext, BottomBarItem.genreKey(group.name)) },
                        label = { Text("${group.name} · ${group.songs.size}") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        shape = CircleShape
                    )
                }
            }
        }
        if (freeGenres.isEmpty()) {
            Text(
                text = "All genres are already on the bar.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
