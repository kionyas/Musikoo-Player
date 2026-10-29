package com.reikoonya.musikoo.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.util.BottomBarPrefs

/**
 * Everything about "pin to the bottom bar" in one place, so every tab (genres, albums, artists,
 * playlists, ...) pins things the same way.
 *
 * [key] is a BottomBarItem key, e.g. BottomBarItem.genreKey("Rock").
 */

/** Observable: recomposes when the item is pinned or unpinned anywhere in the app. */
@Composable
fun rememberPinned(key: String): Boolean {
    val context = LocalContext.current
    remember(context) { BottomBarPrefs.ensureLoaded(context.applicationContext) }
    return key in BottomBarPrefs.items
}

fun toggleBarPin(context: Context, key: String) {
    if (key in BottomBarPrefs.items) BottomBarPrefs.remove(context, key) else BottomBarPrefs.add(context, key)
}

/** Long-press menu entry: "Pin to bottom bar" / "Remove from bottom bar". [onDone] closes the menu. */
@Composable
fun pinMenuAction(key: String, onDone: () -> Unit): MenuAction {
    val context = LocalContext.current
    val pinned = rememberPinned(key)
    return MenuAction(
        icon = Icons.Rounded.PushPin,
        label = if (pinned) "Remove from bottom bar" else "Pin to bottom bar"
    ) {
        toggleBarPin(context, key)
        onDone()
    }
}

/**
 * Pin button for a header card: the pin springs upright when pinned and leans when it is not.
 */
@Composable
fun PinToggleButton(
    pinned: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (pinned) 0f else 40f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium),
        label = "pinRotation"
    )
    BounceIconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Rounded.PushPin,
            contentDescription = if (pinned) "Remove from bottom bar" else "Pin to bottom bar",
            tint = if (pinned) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(rotation)
        )
    }
}

/** Small "pinned" mark in the top-right corner of a card; pops in with a spring. Use inside a Box. */
@Composable
fun BoxScope.PinBadge(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier
            .align(Alignment.TopEnd)
            .padding(12.dp),
        enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Icon(
            imageVector = Icons.Rounded.PushPin,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}
