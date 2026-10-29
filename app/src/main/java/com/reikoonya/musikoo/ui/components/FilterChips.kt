package com.reikoonya.musikoo.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** One chip in [AnimatedFilterChips]. [icon] is shown while the chip is not selected. */
data class ChipOption<T>(
    val value: T,
    val label: String,
    val icon: ImageVector? = null,
)

/**
 * The same filter row as on the Genres tab, as one reusable component: pill chips in a horizontally
 * scrolling row. Compared with a bare FilterChip it adds
 *  - spring press animation (the chip sinks under the finger, like every other button in the app),
 *  - a pop when a chip becomes selected, and the check mark springs in,
 *  - the chip smoothly grows/shrinks when the check mark appears.
 *
 * Sorting: pass [descending] (non-null) and the selected chip shows an arrow instead of the check mark.
 * [onSelect] is also called when the already selected chip is tapped again, so the caller can use that
 * to flip the direction.
 *
 * Put it into a LazyColumn/LazyGrid as a normal item (in a grid: `span = { GridItemSpan(maxLineSpan) }`).
 */
@Composable
fun <T> AnimatedFilterChips(
    options: List<ChipOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    descending: Boolean? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options, key = { it.label }) { option ->
            AnimatedChip(
                option = option,
                isSelected = option.value == selected,
                descending = descending,
                onClick = { onSelect(option.value) }
            )
        }
    }
}

@Composable
private fun <T> AnimatedChip(
    option: ChipOption<T>,
    isSelected: Boolean,
    descending: Boolean?,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }

    // Pop on selection. The first composition must not animate (the row would "bounce" on every open).
    val firstRun = remember { booleanArrayOf(true) }
    val pop = remember { Animatable(1f) }
    val iconPop = remember { Animatable(1f) }
    LaunchedEffect(isSelected) {
        if (firstRun[0]) {
            firstRun[0] = false
            return@LaunchedEffect
        }
        if (isSelected) {
            pop.snapTo(0.9f)
            iconPop.snapTo(0f)
            // Two animations at the same time: the chip itself and the icon inside it
            coroutineScope {
                launch { pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow)) }
                launch { iconPop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)) }
            }
        }
    }

    // One arrow icon turns around instead of swapping between up and down icons
    val arrowTurn by animateFloatAsState(
        targetValue = if (descending == true) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "chipArrowTurn"
    )

    val leading: ImageVector? = when {
        isSelected && descending != null -> Icons.Rounded.ArrowUpward
        isSelected -> Icons.Rounded.Check
        else -> option.icon
    }
    val isArrow = isSelected && descending != null

    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(option.label, fontWeight = FontWeight.SemiBold) },
        leadingIcon = leading?.let { vector ->
            {
                Icon(
                    imageVector = vector,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            val s = if (isSelected) iconPop.value else 1f
                            scaleX = s
                            scaleY = s
                            rotationZ = if (isArrow) arrowTurn else 0f
                        }
                )
            }
        },
        shape = CircleShape,
        interactionSource = source,
        modifier = Modifier
            .animateContentSize(
                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)
            )
            .pressScale(source, pressedScale = 0.94f)
            .graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            }
    )
}
