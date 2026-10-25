package com.reikoonya.musikoo.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.PI
import kotlin.math.sin
import com.reikoonya.musikoo.ui.components.TopPill
import com.reikoonya.musikoo.ui.theme.FontChoice
import com.reikoonya.musikoo.ui.components.expressiveBadgeShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.rounded.ChevronRight
import com.reikoonya.musikoo.ui.theme.ShapeFamily
import com.reikoonya.musikoo.ui.components.shapeOf
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.R
import com.reikoonya.musikoo.ui.components.ArtBackdrop
import com.reikoonya.musikoo.ui.components.TopFadeScrim
import com.reikoonya.musikoo.ui.components.bounceClickable
import com.reikoonya.musikoo.ui.components.cardFill
import com.reikoonya.musikoo.ui.components.glassFill
import com.reikoonya.musikoo.ui.components.glassStroke
import com.reikoonya.musikoo.ui.components.listItemColors
import com.reikoonya.musikoo.ui.components.nowPlayingArt
import com.reikoonya.musikoo.ui.theme.ColorThemeMode
import com.reikoonya.musikoo.ui.theme.UiStyle
import com.reikoonya.musikoo.util.AppIconMode

private enum class SettingsPage(val parent: SettingsPage?) {
    ROOT(null),
    CONNECTION(ROOT),
    APPEARANCE(ROOT)
}

private enum class SettingsSheet { THEME_FONT, BACKGROUND, SHAPES, APP_ICON }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRescanLibrary: () -> Unit,
    uiStyle: UiStyle,
    onUiStyleChange: (UiStyle) -> Unit,
    shapeFamily: ShapeFamily = ShapeFamily.MD3E,
    onShapeFamilyChange: (ShapeFamily) -> Unit = {},
    fontChoice: FontChoice = FontChoice.BASE,
    onFontChoiceChange: (FontChoice) -> Unit = {},
    colorThemeMode: ColorThemeMode,
    onColorThemeModeChange: (ColorThemeMode) -> Unit,
    appIconMode: AppIconMode,
    onAppIconModeChange: (AppIconMode) -> Unit,
    lastFmUser: String? = null,
    onLastFmConnect: () -> Unit = {},
    onLastFmDisconnect: () -> Unit = {}
) {
    var pageName by rememberSaveable { mutableStateOf(SettingsPage.ROOT.name) }
    val page = SettingsPage.valueOf(pageName)
    var sheetName by rememberSaveable { mutableStateOf<String?>(null) }
    val sheet = sheetName?.let { SettingsSheet.valueOf(it) }
    val goBack: () -> Unit = {
        val parent = page.parent
        if (parent == null) onBackClick() else pageName = parent.name
    }
    BackHandler(enabled = page != SettingsPage.ROOT) { goBack() }

    val backArrowRotation by animateFloatAsState(
        targetValue = if (page == SettingsPage.ROOT) 0f else 90f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "backArrowRotation"
    )

    // Blur everything behind the bottom sheet while it is open
    val backgroundBlur by animateDpAsState(
        targetValue = if (sheet != null) 8.dp else 0.dp,
        animationSpec = if (sheet != null) {
            tween(300, easing = FastOutSlowInEasing)
        } else {
            tween(120, easing = FastOutSlowInEasing)
        },
        label = "settingsBackgroundBlur"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(backgroundBlur, BlurredEdgeTreatment.Rectangle)
            .pointerInput(Unit) {
                var handled = false
                detectVerticalDragGestures(
                    onDragStart = { handled = false },
                    onVerticalDrag = { _, dragAmount ->
                        if (!handled && dragAmount > 25f) {
                            handled = true
                            goBack()
                        }
                    }
                )
            }
    ) {
        ArtBackdrop(art = nowPlayingArt(), topScrim = 0.55f, midScrim = 0.82f)

        // Settings content filling entire screen and scrolling underneath floating top pill
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(72.dp))

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val forward = targetState != SettingsPage.ROOT
                    val dir = if (forward) 1 else -1
                    (slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { it * dir / 3 } +
                        fadeIn(tween(320, delayMillis = 60))) togetherWith
                        (slideOutHorizontally(tween(420, easing = FastOutSlowInEasing)) { -it * dir / 3 } +
                            fadeOut(tween(200)))
                },
                label = "settingsPage"
            ) { current ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    when (current) {
                        SettingsPage.ROOT -> {
                            SettingsCardItem(
                                icon = Icons.Rounded.Link,
                                title = "Connection & Library",
                                description = "Rescan, Last.fm, notifications",
                                onClick = { pageName = SettingsPage.CONNECTION.name }
                            )
                            SettingsCardItem(
                                icon = Icons.Rounded.Palette,
                                title = stringResource(R.string.appearance),
                                description = "Theme & font, background, shapes, app icon",
                                onClick = { pageName = SettingsPage.APPEARANCE.name }
                            )

                            val updateContext = androidx.compose.ui.platform.LocalContext.current
                            val updateState = com.reikoonya.musikoo.util.UpdateManager.state
                            val installedVersion = com.reikoonya.musikoo.util.UpdateManager.currentVersion(updateContext)
                            SettingsCardItem(
                                icon = Icons.Rounded.SystemUpdate,
                                title = "App updates",
                                description = when (updateState) {
                                    is com.reikoonya.musikoo.util.UpdateState.Checking -> "Checking GitHub for a new version…"
                                    is com.reikoonya.musikoo.util.UpdateState.UpToDate -> "You have the latest version ($installedVersion)"
                                    is com.reikoonya.musikoo.util.UpdateState.Available -> "Version ${updateState.info.version} is available. Tap to update"
                                    is com.reikoonya.musikoo.util.UpdateState.Downloading -> "Downloading ${(updateState.progress * 100).toInt()}%"
                                    is com.reikoonya.musikoo.util.UpdateState.Ready -> "Version ${updateState.info.version} is ready to install"
                                    is com.reikoonya.musikoo.util.UpdateState.Failed -> updateState.message
                                    else -> "Version $installedVersion. Tap to check for updates"
                                },
                                onClick = { com.reikoonya.musikoo.util.UpdateManager.check(updateContext, manual = true) }
                            )
                        }

                        SettingsPage.CONNECTION -> {
                            SettingsGroupHeader(title = stringResource(R.string.library)) {
                                SettingsCardItem(
                                    icon = Icons.Rounded.Refresh,
                                    title = stringResource(R.string.rescan_library),
                                    description = stringResource(R.string.rescan_library_desc),
                                    onClick = onRescanLibrary
                                )
                            }
                            SettingsGroupHeader(title = "Last.fm") {
                                if (lastFmUser == null) {
                                    SettingsCardItem(
                                        icon = Icons.Rounded.Link,
                                        title = "Connect Last.fm",
                                        description = "Send your listening history to your Last.fm account",
                                        onClick = onLastFmConnect
                                    )
                                } else {
                                    SettingsCardItem(
                                        icon = Icons.Rounded.Link,
                                        title = "Account: $lastFmUser",
                                        description = "Tap to disconnect Last.fm",
                                        onClick = onLastFmDisconnect
                                    )
                                }
                            }
                            SettingsGroupHeader(title = stringResource(R.string.playback)) {
                                SettingsCardItem(
                                    icon = Icons.Rounded.Notifications,
                                    title = stringResource(R.string.player_notifications),
                                    description = stringResource(R.string.player_notifications_desc),
                                    onClick = onRequestNotificationPermission
                                )
                            }
                        }

                        SettingsPage.APPEARANCE -> {
                            SettingsCardItem(
                                icon = Icons.Rounded.Palette,
                                title = "Theme & Font",
                                description = "Color theme and typeface",
                                onClick = { sheetName = SettingsSheet.THEME_FONT.name }
                            )
                            SettingsCardItem(
                                icon = Icons.Rounded.AutoAwesome,
                                title = "Background",
                                description = "Solid or cover blur",
                                onClick = { sheetName = SettingsSheet.BACKGROUND.name }
                            )
                            SettingsCardItem(
                                icon = Icons.Rounded.AutoAwesome,
                                title = "Shapes",
                                description = "Material 3 or mixed shapes",
                                onClick = { sheetName = SettingsSheet.SHAPES.name }
                            )
                            SettingsCardItem(
                                icon = Icons.Rounded.AppShortcut,
                                title = stringResource(R.string.app_icon),
                                description = "Choose the launcher icon",
                                onClick = { sheetName = SettingsSheet.APP_ICON.name }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Bottom sheet for appearance options: full width, partial height, rounded top corners
        if (sheet != null) {
            val sheetColor = MaterialTheme.colorScheme.surfaceContainerLow
            ModalBottomSheet(
                onDismissRequest = { sheetName = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
                containerColor = sheetColor,
                scrimColor = Color.Black.copy(alpha = 0.25f)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 16.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (sheet) {
                        SettingsSheet.THEME_FONT -> {
                    SettingsCardItem(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.color_theme),
                        description = "Current theme: ${
                            when (colorThemeMode) {
                                ColorThemeMode.DYNAMIC -> "Dynamic Color"
                                ColorThemeMode.CLASSIC -> "Classic Color"
                                ColorThemeMode.COVER -> "Cover Color"
                            }
                        }"
                    )

                    StyleOptionCard(
                        title = "Dynamic Color",
                        description = "System dynamic colors based on wallpaper (Material You)",
                        selected = colorThemeMode == ColorThemeMode.DYNAMIC,
                        onClick = { onColorThemeModeChange(ColorThemeMode.DYNAMIC) }
                    )
                    StyleOptionCard(
                        title = "Classic Color (White/Gray)",
                        description = "Monochrome classic color palette",
                        selected = colorThemeMode == ColorThemeMode.CLASSIC,
                        onClick = { onColorThemeModeChange(ColorThemeMode.CLASSIC) }
                    )
                    StyleOptionCard(
                        title = "Cover Color",
                        description = "Dynamic color scheme extracted from current playing track art",
                        selected = colorThemeMode == ColorThemeMode.COVER,
                        onClick = { onColorThemeModeChange(ColorThemeMode.COVER) }
                    )

                        Spacer(modifier = Modifier.height(6.dp))

                    SettingsCardItem(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Font",
                        description = "Current: ${if (fontChoice == FontChoice.LORA) "Lora (adaptive)" else "Base"}"
                    )
                    StyleOptionCard(
                        title = "Base",
                        description = "System font",
                        selected = fontChoice == FontChoice.BASE,
                        onClick = { onFontChoiceChange(FontChoice.BASE) }
                    )
                    StyleOptionCard(
                        title = "Lora (adaptive)",
                        description = "Serif Lora, scales with the text size",
                        selected = fontChoice == FontChoice.LORA,
                        onClick = { onFontChoiceChange(FontChoice.LORA) }
                    )
                        }
                        SettingsSheet.BACKGROUND -> {
                    SettingsCardItem(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Background",
                        description = "Current: ${
                            when (uiStyle) {
                                UiStyle.CLASSIC -> "Solid"
                                UiStyle.EXPENSIVE -> "Cover"
                            }
                        }"
                    )

                    StyleOptionCard(
                        title = "Solid",
                        description = "Solid material background",
                        selected = uiStyle == UiStyle.CLASSIC,
                        onClick = { onUiStyleChange(UiStyle.CLASSIC) }
                    )
                    StyleOptionCard(
                        title = "Cover",
                        description = "Dynamic blur by the cover",
                        selected = uiStyle == UiStyle.EXPENSIVE,
                        onClick = { onUiStyleChange(UiStyle.EXPENSIVE) }
                    )
                        }
                        SettingsSheet.SHAPES -> {
                    SettingsCardItem(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Shapes",
                        description = "Current: ${if (shapeFamily == ShapeFamily.MD3) "Material 3" else "Expensive"}"
                    )

                    StyleOptionCard(
                        title = "Material 3",
                        description = "Standard rounded corners",
                        selected = shapeFamily == ShapeFamily.MD3,
                        onClick = { onShapeFamilyChange(ShapeFamily.MD3) }
                    )
                    StyleOptionCard(
                        title = "Expensive",
                        description = "Softer corners, stars and cookies on covers",
                        selected = shapeFamily == ShapeFamily.MD3E,
                        onClick = { onShapeFamilyChange(ShapeFamily.MD3E) }
                    )
                        }
                        SettingsSheet.APP_ICON -> {
                    SettingsCardItem(
                        icon = Icons.Rounded.AppShortcut,
                        title = stringResource(R.string.app_icon),
                        description = "Current icon: ${
                            when (appIconMode) {
                                AppIconMode.BLACK_WHITE -> "Black & White"
                                AppIconMode.WHITE_BLACK -> "White & Black"
                                AppIconMode.ONET -> "Monet"
                            }
                        }"
                    )

                    StyleOptionCard(
                        title = "Black & White",
                        description = "Minimalist dark icon (Default)",
                        selected = appIconMode == AppIconMode.BLACK_WHITE,
                        onClick = { onAppIconModeChange(AppIconMode.BLACK_WHITE) }
                    )
                    StyleOptionCard(
                        title = "White & Black",
                        description = "Light high-contrast icon",
                        selected = appIconMode == AppIconMode.WHITE_BLACK,
                        onClick = { onAppIconModeChange(AppIconMode.WHITE_BLACK) }
                    )
                    StyleOptionCard(
                        title = "Monet",
                        description = "Monet aesthetic dynamic icon",
                        selected = appIconMode == AppIconMode.ONET,
                        onClick = { onAppIconModeChange(AppIconMode.ONET) }
                    )
                        }
                    }
                }
                // Top fade: content softly disappears under the drag handle while scrolling
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(
                            Brush.verticalGradient(
                                0f to sheetColor,
                                1f to Color.Transparent
                            )
                        )
                )
                }
            }
        }

        // iOS-style top fade scrim
        TopFadeScrim(modifier = Modifier.align(Alignment.TopCenter))

        // Floating Top Bar Pill
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .bounceClickable(shape = CircleShape) { goBack() },
                shape = CircleShape,
                color = glassFill(),
                border = glassStroke(),
                tonalElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // Root: arrow down (close). Inside a section: it flips to point left (back).
                    // Springy turn with a small squash in the middle of the flip.
                    val flipProgress = (backArrowRotation / 90f).coerceIn(0f, 1f)
                    val flipSquash = 1f - 0.3f * sin(flipProgress * PI.toFloat())
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.graphicsLayer {
                            rotationZ = backArrowRotation
                            scaleX = flipSquash
                            scaleY = flipSquash
                        }
                    )
                }
            }

            TopPill(
                text = when (page) {
                            SettingsPage.ROOT -> stringResource(R.string.settings)
                            SettingsPage.CONNECTION -> "CONNECTION & LIBRARY"
                            SettingsPage.APPEARANCE -> "APPEARANCE"
                        }
            )

            Spacer(modifier = Modifier.size(44.dp))
        }
    }
}

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRescanLibrary: () -> Unit,
    uiStyle: UiStyle,
    onUiStyleChange: (UiStyle) -> Unit
) = SettingsScreen(
    onBackClick = onBackClick,
    onRequestNotificationPermission = onRequestNotificationPermission,
    onRescanLibrary = onRescanLibrary,
    uiStyle = uiStyle,
    onUiStyleChange = onUiStyleChange,
    colorThemeMode = ColorThemeMode.DYNAMIC,
    onColorThemeModeChange = {},
    appIconMode = AppIconMode.BLACK_WHITE,
    onAppIconModeChange = {}
)

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRescanLibrary: () -> Unit,
    expressiveStyle: Boolean,
    onExpressiveStyleChange: (Boolean) -> Unit
) = SettingsScreen(
    onBackClick = onBackClick,
    onRequestNotificationPermission = onRequestNotificationPermission,
    onRescanLibrary = onRescanLibrary,
    uiStyle = if (expressiveStyle) UiStyle.EXPENSIVE else UiStyle.CLASSIC,
    onUiStyleChange = { newStyle -> onExpressiveStyleChange(newStyle != UiStyle.CLASSIC) },
    colorThemeMode = ColorThemeMode.DYNAMIC,
    onColorThemeModeChange = {},
    appIconMode = AppIconMode.BLACK_WHITE,
    onAppIconModeChange = {}
)

@Composable
private fun SettingsGroupHeader(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
        )
        content()
    }
}

@Composable
private fun SettingsCardItem(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null
) {
    // Each item gets its own badge shape (cookie, star or circle), so the list does not look like identical cards
    val badgeShape = expressiveBadgeShape(title.hashCode())
    val cardShape = shapeOf(32.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.bounceClickable(shape = cardShape) { onClick() } else it },
        shape = cardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(badgeShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StyleOptionCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapeOf(26.dp))
            .bounceClickable(onClick = onClick),
        shape = shapeOf(26.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else cardFill(),
        border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else glassStroke(),
        tonalElevation = if (selected) 2.dp else 1.dp
    ) {
        ListItem(
            colors = listItemColors(),
            headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(description) },
            leadingContent = {
                RadioButton(
                    selected = selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colorScheme.primary,
                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            },
            trailingContent = {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        )
    }
}
