package com.reikoonya.musikoo.ui

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
import androidx.compose.runtime.Composable
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

@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRescanLibrary: () -> Unit,
    uiStyle: UiStyle,
    onUiStyleChange: (UiStyle) -> Unit,
    colorThemeMode: ColorThemeMode,
    onColorThemeModeChange: (ColorThemeMode) -> Unit,
    appIconMode: AppIconMode,
    onAppIconModeChange: (AppIconMode) -> Unit,
    lastFmUser: String? = null,
    onLastFmConnect: () -> Unit = {},
    onLastFmDisconnect: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount > 25f) {
                        onBackClick()
                    }
                }
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

            // 1. Playback
            SettingsGroupHeader(title = stringResource(R.string.playback)) {
                SettingsCardItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.player_notifications),
                    description = stringResource(R.string.player_notifications_desc),
                    onClick = onRequestNotificationPermission
                )
            }

            // 2. Library
            SettingsGroupHeader(title = stringResource(R.string.library)) {
                SettingsCardItem(
                    icon = Icons.Default.Refresh,
                    title = stringResource(R.string.rescan_library),
                    description = stringResource(R.string.rescan_library_desc),
                    onClick = onRescanLibrary
                )
            }

            // Last.fm
            SettingsGroupHeader(title = "Last.fm") {
                if (lastFmUser == null) {
                    SettingsCardItem(
                        icon = Icons.Default.Link,
                        title = "Connect Last.fm",
                        description = "Send your listening history to your Last.fm account",
                        onClick = onLastFmConnect
                    )
                } else {
                    SettingsCardItem(
                        icon = Icons.Default.Link,
                        title = "Account: $lastFmUser",
                        description = "Tap to disconnect Last.fm",
                        onClick = onLastFmDisconnect
                    )
                }
            }

            // 3. Appearance
            SettingsGroupHeader(title = stringResource(R.string.appearance)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingsCardItem(
                        icon = Icons.Default.Palette,
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
                        icon = Icons.Default.AutoAwesome,
                        title = stringResource(R.string.interface_style),
                        description = "Current style: ${
                            when (uiStyle) {
                                UiStyle.CLASSIC -> "Material 3"
                                UiStyle.EXPENSIVE -> "Expensive"
                            }
                        }"
                    )

                    StyleOptionCard(
                        title = "Material 3",
                        description = "Classic design: solid surface containers",
                        selected = uiStyle == UiStyle.CLASSIC,
                        onClick = { onUiStyleChange(UiStyle.CLASSIC) }
                    )
                    StyleOptionCard(
                        title = "Expensive",
                        description = "Sleek interface with floating pills and native blur",
                        selected = uiStyle == UiStyle.EXPENSIVE,
                        onClick = { onUiStyleChange(UiStyle.EXPENSIVE) }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    SettingsCardItem(
                        icon = Icons.Default.AppShortcut,
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

            Spacer(modifier = Modifier.height(24.dp))
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
                    .bounceClickable { onBackClick() },
                shape = CircleShape,
                color = glassFill(),
                border = glassStroke(),
                tonalElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(32.dp),
                color = glassFill(),
                border = glassStroke()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(28.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .let { if (onClick != null) it.bounceClickable { onClick() } else it },
        shape = RoundedCornerShape(26.dp),
        color = cardFill(),
        border = glassStroke(),
        tonalElevation = 1.dp
    ) {
        ListItem(
            colors = listItemColors(),
            headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(description) },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )
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
            .clip(RoundedCornerShape(26.dp))
            .bounceClickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
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
