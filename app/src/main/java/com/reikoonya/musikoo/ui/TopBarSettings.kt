package com.reikoonya.musikoo.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurLinear
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.ui.components.MusikooSwitch
import com.reikoonya.musikoo.ui.components.TopFadeScrim
import com.reikoonya.musikoo.ui.components.coverShapeOf
import com.reikoonya.musikoo.ui.components.expressiveBadgeShape
import com.reikoonya.musikoo.ui.components.rememberTopFadeBlurActive
import com.reikoonya.musikoo.ui.components.shapeOf
import com.reikoonya.musikoo.ui.components.topFadeBlur
import com.reikoonya.musikoo.util.UiStylePrefs
import kotlinx.coroutines.delay

/**
 * Settings → Appearance → "Top Bar": both top options in one window, with a live preview.
 * The preview is drawn with the real components (MenuButton, topFadeBlur, TopFadeScrim), so it reacts to
 * the switches immediately and looks like the app does.
 */
@Composable
internal fun TopBarSheetContent() {
    val context = LocalContext.current.applicationContext
    remember(context) { UiStylePrefs.ensureLoaded(context) }
    val menuStyle = UiStylePrefs.menuTopBar
    val fadeBlur = UiStylePrefs.topFadeBlur
    val blurReady = rememberTopFadeBlurActive()

    Text(
        text = "Top Bar",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )

    TopBarPreview(menuStyle = menuStyle)

    Text(
        text = when {
            fadeBlur && !blurReady -> "Fade blur needs Android 12+ and Blur Effects (Energy & Battery): a plain fade is shown instead"
            menuStyle && fadeBlur -> "Round ⋮ button with a plain title, content blurs under the header"
            menuStyle -> "Round ⋮ button with a plain title, content fades under the header"
            fadeBlur -> "Classic pill, content blurs under it"
            else -> "Classic pill, content fades under it"
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp)
    )

    SettingsCardItem(
        onSheet = true,
        icon = Icons.Rounded.MoreVert,
        title = "New header",
        description = "Round ⋮ button opens the menu (Statistics, Settings, About), plain centered titles on other screens",
        trailing = {
            MusikooSwitch(
                checked = menuStyle,
                onCheckedChange = { UiStylePrefs.saveMenuTopBar(context, it) }
            )
        },
        onClick = { UiStylePrefs.saveMenuTopBar(context, !menuStyle) }
    )
    SettingsCardItem(
        onSheet = true,
        icon = Icons.Rounded.BlurLinear,
        title = "Fade blur",
        description = "Content smoothly blurs and fades under the header, like iOS and ColorOS",
        trailing = {
            MusikooSwitch(
                checked = fadeBlur,
                onCheckedChange = { UiStylePrefs.saveTopFadeBlur(context, it) }
            )
        },
        onClick = { UiStylePrefs.saveTopFadeBlur(context, !fadeBlur) }
    )
}

@Composable
private fun TopBarPreview(menuStyle: Boolean) {
    val scroll = rememberScrollState()
    // The fake list keeps scrolling up and down, so the blur under the header is visible
    LaunchedEffect(scroll.maxValue) {
        if (scroll.maxValue <= 0) return@LaunchedEffect
        while (true) {
            delay(400)
            scroll.animateScrollTo(scroll.maxValue, tween(5600, easing = LinearEasing))
            delay(300)
            scroll.animateScrollTo(0, tween(5600, easing = LinearEasing))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(232.dp)
            .clip(shapeOf(28.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .topFadeBlur(height = 86.dp, maxRadius = 16.dp)
                .verticalScroll(scroll, enabled = false)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(62.dp))
            repeat(9) { PreviewRow(it) }
            Spacer(modifier = Modifier.height(14.dp))
        }

        TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter), height = 86.dp, blurred = true)

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            AnimatedContent(
                targetState = menuStyle,
                transitionSpec = { fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(140)) },
                label = "topBarPreviewHeader"
            ) { menu ->
                if (menu) PreviewMenuHeader() else PreviewClassicHeader()
            }
        }
    }
}

/** New header: the real animated ⋮ button (it opens and closes on its own here) and a plain title. */
@Composable
private fun PreviewMenuHeader() {
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2200)
            open = !open
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        MenuButton(open = open, onClick = {})
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = "Library",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

/** Classic header: one pill with settings, the mode switch and about. */
@Composable
private fun PreviewClassicHeader() {
    Box(modifier = Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PreviewBadge(Icons.Rounded.Settings, "settings".hashCode())
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Library", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("Stats", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                PreviewBadge(Icons.Rounded.Info, "about".hashCode())
            }
        }
    }
}

@Composable
private fun PreviewBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, seed: Int) {
    Surface(
        modifier = Modifier.size(32.dp),
        shape = expressiveBadgeShape(seed),
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** A fake list row: cover + two text bars. */
@Composable
private fun PreviewRow(index: Int) {
    val scheme = MaterialTheme.colorScheme
    val cover = when (index % 3) {
        0 -> scheme.primaryContainer
        1 -> scheme.tertiaryContainer
        else -> scheme.secondaryContainer
    }
    val titleWidth = listOf(0.62f, 0.48f, 0.7f, 0.55f)[index % 4]
    Surface(shape = shapeOf(18.dp), color = scheme.surfaceContainerHigh) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(38.dp).clip(coverShapeOf(10.dp)).background(cover))
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.fillMaxWidth(titleWidth).height(9.dp).clip(CircleShape).background(scheme.onSurface.copy(alpha = 0.55f)))
                Box(modifier = Modifier.fillMaxWidth(titleWidth * 0.6f).height(7.dp).clip(CircleShape).background(scheme.onSurfaceVariant.copy(alpha = 0.3f)))
            }
        }
    }
}
