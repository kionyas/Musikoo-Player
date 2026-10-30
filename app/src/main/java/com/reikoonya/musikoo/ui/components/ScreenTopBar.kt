package com.reikoonya.musikoo.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.R

// ---------------------------------------------------------------------------------------------
// Search state + filtering
// ---------------------------------------------------------------------------------------------

/**
 * State of the search built into a screen's top bar. Survives rotation and navigation
 * (rememberSaveable), so coming back from a detail screen keeps the query.
 */
@Stable
class ScreenSearchState(initialOpen: Boolean = false, initialQuery: String = "") {
    var open by mutableStateOf(initialOpen)
    var query by mutableStateOf(initialQuery)

    /** True while a non-empty query is filtering the list. */
    val filtering: Boolean get() = open && query.isNotBlank()

    fun toggle() {
        if (open) close() else open = true
    }

    fun close() {
        open = false
        query = ""
    }

    companion object {
        val Saver = listSaver<ScreenSearchState, Any>(
            save = { listOf(it.open, it.query) },
            restore = { ScreenSearchState(it[0] as Boolean, it[1] as String) }
        )
    }
}

@Composable
fun rememberScreenSearchState(): ScreenSearchState =
    rememberSaveable(saver = ScreenSearchState.Saver) { ScreenSearchState() }

/**
 * Every word of the query must occur in at least one of [fields] (case-insensitive), so
 * "daft punk disc" finds "Discovery" by "Daft Punk". An empty query matches everything.
 */
fun matchesQuery(query: String, vararg fields: String?): Boolean {
    val tokens = query.trim().lowercase().split(' ').filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return true
    val haystack = fields.map { it?.lowercase().orEmpty() }
    return tokens.all { token -> haystack.any { it.contains(token) } }
}

/** `albums.filterByQuery(search.query) { listOf(it.name, it.artist) }` */
fun <T> List<T>.filterByQuery(query: String, fields: (T) -> List<String?>): List<T> =
    if (query.isBlank()) this else filter { matchesQuery(query, *fields(it).toTypedArray()) }

@Composable
fun SearchNoResults(query: String, modifier: Modifier = Modifier) {
    Text(
        text = "Nothing found for “${query.trim()}”",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(16.dp)
    )
}

// ---------------------------------------------------------------------------------------------
// Gestures
// ---------------------------------------------------------------------------------------------

/** Swipe down anywhere on the screen = go back (fires once per gesture). */
fun Modifier.swipeDownToBack(onBack: () -> Unit): Modifier = composed {
    val current by rememberUpdatedState(onBack)
    pointerInput(Unit) {
        var fired = false
        detectVerticalDragGestures(
            onDragStart = { fired = false },
            onDragEnd = { fired = false },
            onDragCancel = { fired = false }
        ) { _, dragAmount ->
            if (!fired && dragAmount > 25f) {
                fired = true
                current()
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Buttons and the top bar itself
// ---------------------------------------------------------------------------------------------

/** The 44 dp round button of the floating top bar (back, search, close, ...). */
@Composable
fun GlassCircleButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = modifier
            .size(44.dp)
            .bounceClickable(shape = CircleShape, onClick = onClick),
        shape = CircleShape,
        color = glassFill(),
        border = glassStroke(),
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
        }
    }
}

/**
 * Floating top bar shared by every list/detail screen: back button, title pill, and on the right
 * either the built-in search (pass [search]) or any custom [trailing] content.
 *
 * Search: the right button turns into a close button and the title pill morphs into a search field.
 * The system back button closes the search first. Filter your list with [filterByQuery].
 *
 * Place it as the last child of the screen's Box, after TopFadeScrim:
 * `ScreenTopBar(title, onBack, search = search, modifier = Modifier.align(Alignment.TopCenter))`
 */
@Composable
fun ScreenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    search: ScreenSearchState? = null,
    searchHint: String = "Search",
    trailing: (@Composable () -> Unit)? = null,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val searching = search?.open == true
    BackHandler(enabled = searching) { search?.close() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassCircleButton(
            icon = Icons.Rounded.KeyboardArrowDown,
            contentDescription = stringResource(R.string.back),
            onClick = {
                keyboard?.hide()
                onBack()
            }
        )

        val menuStyle = rememberMenuTopBarActive()
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = searching,
                transitionSpec = {
                    (fadeIn(tween(220, delayMillis = 60)) +
                        scaleIn(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow), initialScale = 0.88f)) togetherWith
                        fadeOut(tween(110)) using
                        SizeTransform(clip = false) { _, _ ->
                            spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                        }
                },
                label = "topBarCenter"
            ) { isSearching ->
                if (isSearching && search != null) {
                    InlineSearchField(
                        query = search.query,
                        onQueryChange = { search.query = it },
                        placeholder = searchHint,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (menuStyle) {
                    PlainTopTitle(text = title)
                } else {
                    TopPill(text = title)
                }
            }
        }

        when {
            search != null -> AnimatedContent(
                targetState = searching,
                transitionSpec = {
                    (scaleIn(spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)) + fadeIn(tween(120))) togetherWith
                        (scaleOut(tween(120)) + fadeOut(tween(90)))
                },
                label = "topBarSearchToggle"
            ) { isSearching ->
                GlassCircleButton(
                    icon = if (isSearching) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = if (isSearching) "Close search" else "Search",
                    onClick = {
                        keyboard?.hide()
                        search.toggle()
                    }
                )
            }
            trailing != null -> trailing()
            else -> Spacer(modifier = Modifier.size(44.dp))
        }
    }
}

/** Search field in the same glass pill look as the top bar buttons. Takes focus when it appears. */
@Composable
fun InlineSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focus.requestFocus() }

    Surface(
        modifier = modifier.height(44.dp),
        shape = CircleShape,
        color = glassFill(),
        border = glassStroke(),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus)
                )
            }
        }
    }
}
