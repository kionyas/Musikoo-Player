package com.reikoonya.musikoo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.ui.components.bounceClickable
import com.reikoonya.musikoo.ui.components.MusikooSwitch
import com.reikoonya.musikoo.ui.components.cardFill
import com.reikoonya.musikoo.ui.components.shapeOf
import com.reikoonya.musikoo.util.UiStylePrefs

/** Краткое описание текущего вида библиотеки для строки в настройках. */
fun libraryLayoutSummary(): String =
    if (UiStylePrefs.libraryTabsEnabled) {
        "Swipe tabs: " + UiStylePrefs.libraryPages.joinToString(" · ") { libraryTitle(it) }
    } else {
        "Classic sections: " + UiStylePrefs.LIBRARY_SECTION_KEYS
            .filter { it in UiStylePrefs.classicSections }.joinToString(" · ") { libraryTitle(it) }
    }

/** Содержимое шторки Settings → Interface → Library Layout. */
@Composable
fun LibraryLayoutSheetContent() {
    val context = LocalContext.current.applicationContext
    remember(context) { UiStylePrefs.ensureLibraryLoaded(context) }
    val swipes = UiStylePrefs.libraryTabsEnabled
    val pages = UiStylePrefs.libraryPages
    val hiddenPages = UiStylePrefs.LIBRARY_SECTION_KEYS.filter { it !in pages }
    val classic = UiStylePrefs.classicSections

    Text(
        text = "Library Layout",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )

    // Режим: классический вид секциями или свайпы между страницами
    LayoutModeCard(
        title = "Classic sections",
        description = "Pins, Playlists and Albums one under another on a single screen",
        selected = !swipes,
        onClick = { UiStylePrefs.saveLibraryTabsMode(context, false) }
    )
    LayoutModeCard(
        title = "Swipe tabs",
        description = "Swipe between pages, title and page dots below the top bar",
        selected = swipes,
        onClick = { UiStylePrefs.saveLibraryTabsMode(context, true) }
    )

    // Переход между режимами: содержимое плавно меняется, высота шторки подстраивается пружиной
    AnimatedContent(
        targetState = swipes,
        transitionSpec = {
            (fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(120)))
                .using(SizeTransform(clip = false) { _, _ ->
                    spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                })
        },
        label = "libraryLayoutContent"
    ) { isSwipes ->
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (isSwipes) {
                SectionLabel("Pages · top to bottom is left to right")
                // Включённые страницы по порядку, выключенные внизу. Каждая строка сама едет на своё место,
                // поэтому и перестановка, и включение/выключение анимируются
                val ordered = pages + hiddenPages
                val step = ROW_HEIGHT + ROW_GAP
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(step * ordered.size - ROW_GAP)
                ) {
                    UiStylePrefs.LIBRARY_SECTION_KEYS.forEach { key ->
                        val y by animateDpAsState(
                            targetValue = step * ordered.indexOf(key),
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                            label = "pageRowY"
                        )
                        val position = pages.indexOf(key)
                        val enabled = position >= 0
                        LayoutRow(
                            modifier = Modifier.offset(y = y).height(ROW_HEIGHT),
                            title = libraryTitle(key),
                            checked = enabled,
                            // Последнюю страницу выключить нельзя
                            switchEnabled = !enabled || pages.size > 1,
                            onToggle = { UiStylePrefs.toggleLibraryPage(context, key) }
                        ) {
                            if (enabled) {
                                IconButton(
                                    onClick = { UiStylePrefs.moveLibraryPage(context, key, -1) },
                                    enabled = position > 0
                                ) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = { UiStylePrefs.moveLibraryPage(context, key, 1) },
                                    enabled = position < pages.lastIndex
                                ) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                SectionLabel("Sections")
                UiStylePrefs.LIBRARY_SECTION_KEYS.forEach { key ->
                    LayoutRow(
                        modifier = Modifier.height(ROW_HEIGHT),
                        title = libraryTitle(key),
                        checked = key in classic,
                        switchEnabled = key !in classic || classic.size > 1,
                        onToggle = { UiStylePrefs.toggleClassicSection(context, key) }
                    )
                }
            }
        }
    }
}

private val ROW_HEIGHT: Dp = 56.dp
private val ROW_GAP: Dp = 10.dp

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    )
}

@Composable
private fun LayoutModeCard(title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else cardFill(),
        label = "modeCard"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClickable(shape = shapeOf(24.dp), onClick = onClick),
        shape = shapeOf(24.dp),
        color = container
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RadioButton(selected = selected, onClick = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LayoutRow(
    modifier: Modifier = Modifier,
    title: String,
    checked: Boolean,
    switchEnabled: Boolean,
    onToggle: () -> Unit,
    extra: @Composable () -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shapeOf(20.dp),
        color = cardFill()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = if (checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            extra()
            MusikooSwitch(checked = checked, onCheckedChange = { onToggle() }, enabled = switchEnabled)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(end = 4.dp))
        }
    }
}
