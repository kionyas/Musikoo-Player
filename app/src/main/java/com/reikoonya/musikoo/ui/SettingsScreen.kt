package com.reikoonya.musikoo.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Intent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import android.os.Build
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.reikoonya.musikoo.ui.components.WavyProgress
import com.reikoonya.musikoo.ui.components.plainNotes
import com.reikoonya.musikoo.util.UpdateConfig
import com.reikoonya.musikoo.util.UpdateManager
import com.reikoonya.musikoo.util.UpdateState
import com.reikoonya.musikoo.util.UiStylePrefs
import com.reikoonya.musikoo.ui.components.ExpressiveShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    DEVICE(ROOT),
    INTERFACE(ROOT),
    APPEARANCE(ROOT)
}

private fun SettingsPage.depth(): Int = generateSequence(this) { it.parent }.count() - 1

private enum class SettingsSheet { THEME_FONT, BACKGROUND, SHAPES, APP_ICON, LASTFM, DOWNLOADS }

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
    lastFmAvatarUrl: String? = null,
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

    // Soft blur + light dim behind the bottom sheet.
    // The radius is only read inside graphicsLayer {}, so each animation frame redraws the layer
    // on the GPU and does not recompose the whole settings screen.
    val sheetOpen = sheet != null
    val blurRadiusDp by animateDpAsState(
        targetValue = if (sheetOpen) 10.dp else 0.dp,
        animationSpec = if (sheetOpen) {
            tween(300, easing = FastOutSlowInEasing)
        } else {
            tween(160, easing = FastOutSlowInEasing)
        },
        label = "settingsBackgroundBlur"
    )
    val backgroundScrim by animateFloatAsState(
        targetValue = if (sheetOpen) 0.16f else 0f,
        animationSpec = if (sheetOpen) {
            tween(300, easing = FastOutSlowInEasing)
        } else {
            tween(160, easing = FastOutSlowInEasing)
        },
        label = "settingsBackgroundScrim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                // RenderEffect blur needs Android 12+; older versions keep just the dim
                val radius = blurRadiusDp.toPx()
                renderEffect = if (Build.VERSION.SDK_INT >= 31 && radius > 0.5f) {
                    // Clamp: the edges are stretched, so no dark/transparent fringe appears at the borders
                    BlurEffect(radius, radius, TileMode.Clamp)
                } else null
                clip = false
            }
            .drawWithContent {
                drawContent()
                drawRect(Color.Black.copy(alpha = backgroundScrim))
            }
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
                    val forward = targetState.depth() > initialState.depth()
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (current) {
                        SettingsPage.ROOT -> {
                            SettingsCardItem(
                                icon = if (lastFmUser != null) Icons.Rounded.Cookie else Icons.Rounded.Person,
                                title = "Connection",
                                description = lastFmUser?.let { "Connected as $it" } ?: "Connect Last.fm account & history",
                                hero = true,
                                avatarUrl = lastFmAvatarUrl,
                                onClick = { sheetName = SettingsSheet.LASTFM.name }
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            SettingsGroupCard {
                                SettingsCardItem(
                                    icon = Icons.Rounded.Phonelink,
                                    title = "Device",
                                    description = "Notifications, storage, cache and battery",
                                    grouped = true,
                                    onClick = { pageName = SettingsPage.DEVICE.name }
                                )
                                SettingsCardItem(
                                    icon = Icons.Rounded.Tune,
                                    title = "Interface and Interaction",
                                    description = "Interface settings and customization",
                                    grouped = true,
                                    onClick = { pageName = SettingsPage.INTERFACE.name }
                                )
                                SettingsCardItem(
                                    icon = Icons.Rounded.Palette,
                                    title = "Appearance & Typeface",
                                    description = "Themes, accent colors, fonts and shapes",
                                    grouped = true,
                                    onClick = { pageName = SettingsPage.APPEARANCE.name }
                                )
                            }
                            SettingsGroupCard {
                                SettingsCardItem(
                                    icon = Icons.Rounded.Download,
                                    title = "Download Manager",
                                    description = "App updates and source code",
                                    grouped = true,
                                    onClick = { sheetName = SettingsSheet.DOWNLOADS.name }
                                )
                            }

                            val rootContext = androidx.compose.ui.platform.LocalContext.current
                            val avatarBitmap = rememberAssetBitmap("avatar.jpg")
                            val sunShape = remember { ExpressiveShapes.sunny.toShape() }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .bounceClickable(shape = sunShape) {
                                            runCatching {
                                                rootContext.startActivity(
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse("https://github.com/kionyas")
                                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                )
                                            }
                                        },
                                    shape = sunShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = glassStroke(),
                                    tonalElevation = 2.dp
                                ) {
                                    if (avatarBitmap != null) {
                                        Image(
                                            bitmap = avatarBitmap,
                                            contentDescription = "kionyas GitHub",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Rounded.WbSunny,
                                                contentDescription = "kionyas GitHub",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        SettingsPage.DEVICE -> {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            var cacheSize by remember { mutableStateOf(getCacheSizeFormatted(context)) }
                            SettingsGroupCard {
                                SettingsCardItem(
                                    icon = Icons.Rounded.NotificationsActive,
                                    title = "Playback Notifications",
                                    description = "Manage system notification permissions for media playback controls",
                                    grouped = true,
                                    onClick = onRequestNotificationPermission
                                )
                                SettingsCardItem(
                                    icon = Icons.Rounded.DeleteSweep,
                                    title = "Cache and Storage",
                                    description = "Current cache size: $cacheSize. Tap to clear lyric and artwork caches.",
                                    grouped = true,
                                    onClick = {
                                        context.cacheDir.deleteRecursively()
                                        cacheSize = getCacheSizeFormatted(context)
                                    }
                                )
                                SettingsCardItem(
                                    icon = Icons.Rounded.EnergySavingsLeaf,
                                    title = "Energy & Battery",
                                    description = "Optimize animations and background task efficiency",
                                    grouped = true,
                                    onClick = {}
                                )
                            }
                        }

                        SettingsPage.INTERFACE -> {
                            val context = androidx.compose.ui.platform.LocalContext.current
                            var libraryTabsMode by remember { mutableStateOf(UiStylePrefs.loadLibraryTabsMode(context)) }
                            SettingsGroupCard {
                                SettingsCardItem(
                                    icon = Icons.Rounded.ViewCarousel,
                                    title = "Library Swipe Tabs",
                                    description = "Swipe between Albums, Pins, and Playlists with title and page dots below the top bar",
                                    grouped = true,
                                    trailing = {
                                        Switch(
                                            checked = libraryTabsMode,
                                            onCheckedChange = {
                                                libraryTabsMode = it
                                                UiStylePrefs.saveLibraryTabsMode(context, it)
                                            }
                                        )
                                    },
                                    onClick = {
                                        val next = !libraryTabsMode
                                        libraryTabsMode = next
                                        UiStylePrefs.saveLibraryTabsMode(context, next)
                                    }
                                )
                                SettingsCardItem(
                                    icon = Icons.Rounded.Gesture,
                                    title = "Mini-player Gestures",
                                    description = "Swipe controls, favorites toggle, and quick navigation",
                                    grouped = true,
                                    onClick = {}
                                )
                            }
                        }

                        SettingsPage.APPEARANCE -> {
                            SettingsGroupCard {
                                SettingsCardItem(
                                    icon = Icons.Rounded.Palette,
                                    title = "Theme & Font",
                                    description = "Color theme and typeface",
                                    grouped = true,
                                    onClick = { sheetName = SettingsSheet.THEME_FONT.name }
                                )
                                SettingsCardItem(
                                    icon = Icons.Rounded.AutoAwesome,
                                    title = "Background",
                                    description = "Solid or cover blur",
                                    grouped = true,
                                    onClick = { sheetName = SettingsSheet.BACKGROUND.name }
                                )
                                SettingsCardItem(
                                    icon = Icons.Rounded.AutoAwesome,
                                    title = "Shapes",
                                    description = "Material 3 or mixed shapes",
                                    grouped = true,
                                    onClick = { sheetName = SettingsSheet.SHAPES.name }
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
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
                scrimColor = Color.Black.copy(alpha = 0.12f)
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
                            SettingsSheet.LASTFM -> {
                                Text(
                                    text = "Connect LastFM & Rescan",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                                )
                                if (lastFmUser == null) {
                                    SettingsCardItem(
                                        onSheet = true,
                                        icon = Icons.Rounded.Link,
                                        title = "Connect Last.fm",
                                        description = "Send your listening history to your Last.fm account",
                                        onClick = {
                                            sheetName = null
                                            onLastFmConnect()
                                        }
                                    )
                                } else {
                                    SettingsCardItem(
                                        onSheet = true,
                                        icon = Icons.Rounded.Link,
                                        title = "Account: $lastFmUser",
                                        description = "Tap to disconnect Last.fm",
                                        onClick = {
                                            sheetName = null
                                            onLastFmDisconnect()
                                        }
                                    )
                                }
                                SettingsCardItem(
                                    onSheet = true,
                                    icon = Icons.Rounded.Refresh,
                                    title = stringResource(R.string.rescan_library),
                                    description = stringResource(R.string.rescan_library_desc),
                                    onClick = {
                                        sheetName = null
                                        onRescanLibrary()
                                    }
                                )
                            }
                            SettingsSheet.DOWNLOADS -> {
                                val updateContext = androidx.compose.ui.platform.LocalContext.current
                                val updateState = com.reikoonya.musikoo.util.UpdateManager.state
                                val installedVersion = com.reikoonya.musikoo.util.UpdateManager.currentVersion(updateContext)

                                Text(
                                    text = "Download Manager",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                                )

                                SettingsCardItem(
                                    onSheet = true,
                                    icon = Icons.Rounded.SystemUpdate,
                                    title = "Version $installedVersion",
                                    description = when (updateState) {
                                        is com.reikoonya.musikoo.util.UpdateState.Checking -> "Checking GitHub for a new version…"
                                        is com.reikoonya.musikoo.util.UpdateState.UpToDate -> "You have the latest version"
                                        is com.reikoonya.musikoo.util.UpdateState.Available -> "Version ${updateState.info.version} is available"
                                        is com.reikoonya.musikoo.util.UpdateState.Downloading -> "Downloading ${(updateState.progress * 100).toInt()}%"
                                        is com.reikoonya.musikoo.util.UpdateState.Ready -> "Version ${updateState.info.version} is ready to install"
                                        is com.reikoonya.musikoo.util.UpdateState.Failed -> updateState.message
                                        else -> "Tap Check Updates to check for new releases"
                                    },
                                    onClick = {
                                        com.reikoonya.musikoo.util.UpdateManager.check(updateContext, manual = true, showDialog = false)
                                    }
                                )

                                if (updateState is com.reikoonya.musikoo.util.UpdateState.Downloading) {
                                    WavyProgress(
                                        progress = updateState.progress,
                                        isPlaying = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp)
                                    )
                                }

                                when (updateState) {
                                    is com.reikoonya.musikoo.util.UpdateState.Available -> SettingsCardItem(
                                        onSheet = true,
                                        icon = Icons.Rounded.Download,
                                        title = "Download Update",
                                        description = "Version ${updateState.info.version}",
                                        onClick = { com.reikoonya.musikoo.util.UpdateManager.startDownload(updateContext) }
                                    )
                                    is com.reikoonya.musikoo.util.UpdateState.Ready -> SettingsCardItem(
                                        onSheet = true,
                                        icon = Icons.Rounded.SystemUpdate,
                                        title = "Install Update",
                                        description = if (com.reikoonya.musikoo.util.UpdateManager.canInstall(updateContext)) {
                                            "Open system installer"
                                        } else {
                                            "Android will ask to allow installs from Musikoo"
                                        },
                                        onClick = { com.reikoonya.musikoo.util.UpdateManager.install(updateContext) }
                                    )
                                    is com.reikoonya.musikoo.util.UpdateState.Failed -> if (updateState.info != null) {
                                        SettingsCardItem(
                                            onSheet = true,
                                            icon = Icons.Rounded.Refresh,
                                            title = "Retry Download",
                                            description = updateState.message,
                                            onClick = { com.reikoonya.musikoo.util.UpdateManager.startDownload(updateContext) }
                                        )
                                    }
                                    else -> Unit
                                }

                                SettingsCardItem(
                                    onSheet = true,
                                    icon = Icons.Rounded.Code,
                                    title = "Source Code",
                                    description = "github.com/${UpdateConfig.OWNER}/${UpdateConfig.REPO}",
                                    onClick = {
                                        runCatching {
                                            updateContext.startActivity(
                                                android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse(UpdateConfig.REPO_URL)
                                                ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            )
                                        }
                                    }
                                )
                            }
                            SettingsSheet.THEME_FONT -> {
                                SettingsCardItem(
                                    onSheet = true,
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
                                    onSheet = true,
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
                                    onSheet = true,
                                    icon = Icons.Rounded.AutoAwesome,
                                    title = "Background",
                                    description = "Current: ${
                                        when (uiStyle) {
                                            UiStyle.CLASSIC -> "Solid"
                                            UiStyle.EXPENSIVE -> "Solid"
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
                                    onSheet = true,
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
                                    onSheet = true,
                                    icon = Icons.Rounded.AppShortcut,
                                    title = stringResource(R.string.app_icon),
                                    description = "Current icon: ${
                                        when (appIconMode) {
                                            AppIconMode.BLACK_WHITE -> "Black & White"
                                            AppIconMode.WHITE_BLACK -> "White & Black"
                                            AppIconMode.MONET -> "Monet"
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
                                    selected = appIconMode == AppIconMode.MONET,
                                    onClick = { onAppIconModeChange(AppIconMode.MONET) }
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
            val backShape = expressiveBadgeShape("back".hashCode())
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .bounceClickable(shape = backShape) { goBack() },
                shape = backShape,
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
                    SettingsPage.DEVICE -> "DEVICE"
                    SettingsPage.INTERFACE -> "INTERFACE & INTERACTION"
                    SettingsPage.APPEARANCE -> "APPEARANCE & TYPEFACE"
                }
            )

            Spacer(modifier = Modifier.size(44.dp))
        }
    }
}

@Composable
private fun AppUpdatesPage() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val state = UpdateManager.state
    val installed = UpdateManager.currentVersion(context)

    LaunchedEffect(Unit) {
        UpdateManager.dismissDialog()
        UpdateManager.check(context, manual = true, showDialog = false)
    }
    // This page shows the state itself, so the popup dialog must not appear on top of it
    LaunchedEffect(UpdateManager.dialogVisible) {
        if (UpdateManager.dialogVisible) UpdateManager.dismissDialog()
    }

    val info = when (state) {
        is UpdateState.Available -> state.info
        is UpdateState.Downloading -> state.info
        is UpdateState.Ready -> state.info
        is UpdateState.Failed -> state.info
        else -> null
    }

    SettingsCardItem(
        icon = Icons.Rounded.SystemUpdate,
        title = "Version $installed",
        description = when (state) {
            is UpdateState.Checking -> "Checking GitHub for a new version…"
            is UpdateState.UpToDate -> "You have the latest version"
            is UpdateState.Available -> "Version ${state.info.version} is available"
            is UpdateState.Downloading -> "Downloading ${(state.progress * 100).toInt()}%"
            is UpdateState.Ready -> "Version ${state.info.version} is ready to install"
            is UpdateState.Failed -> state.message
            else -> "Tap below to check for updates"
        }
    )

    if (state is UpdateState.Downloading) {
        WavyProgress(
            progress = state.progress,
            isPlaying = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )
    }

    when (state) {
        is UpdateState.Available -> SettingsCardItem(
            icon = Icons.Rounded.Download,
            title = "Download",
            description = "Version ${state.info.version}",
            onClick = { UpdateManager.startDownload(context) }
        )
        is UpdateState.Ready -> SettingsCardItem(
            icon = Icons.Rounded.SystemUpdate,
            title = "Install",
            description = if (UpdateManager.canInstall(context)) {
                "Open the system installer"
            } else {
                "Android will first ask to allow installs from Musikoo. Allow it, come back and tap Install again"
            },
            onClick = { UpdateManager.install(context) }
        )
        is UpdateState.Failed -> if (state.info != null) {
            SettingsCardItem(
                icon = Icons.Rounded.Refresh,
                title = "Retry download",
                description = "Version ${state.info.version}",
                onClick = { UpdateManager.startDownload(context) }
            )
        } else {
            SettingsCardItem(
                icon = Icons.Rounded.Refresh,
                title = "Check again",
                description = "Look for a new version on GitHub",
                onClick = { UpdateManager.check(context, manual = true, showDialog = false) }
            )
        }
        is UpdateState.Checking, is UpdateState.Downloading -> Unit
        else -> SettingsCardItem(
            icon = Icons.Rounded.Refresh,
            title = "Check again",
            description = "Look for a new version on GitHub",
            onClick = { UpdateManager.check(context, manual = true, showDialog = false) }
        )
    }

    val notes = info?.let { plainNotes(it.notes) }.orEmpty()
    if (notes.isNotEmpty() && state !is UpdateState.Downloading) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shapeOf(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(18.dp)
            )
        }
    }

    SettingsCardItem(
        icon = Icons.Rounded.Link,
        title = "Release page",
        description = "Open the latest release on GitHub",
        onClick = {
            val url = info?.pageUrl?.takeIf { it.isNotBlank() } ?: UpdateConfig.LATEST_RELEASE_URL
            runCatching {
                context.startActivity(
                    android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    )
}

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
private fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapeOf(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsCardItem(
    icon: ImageVector,
    title: String,
    description: String,
    hero: Boolean = false,
    grouped: Boolean = false,
    onSheet: Boolean = false,
    avatarUrl: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val badgeShape = if (hero || title == "Connection") CircleShape else expressiveBadgeShape(title.hashCode())
    val avatarBitmap = rememberAsyncImageBitmap(avatarUrl)
    val content = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (onClick != null && !grouped) it.bounceClickable(shape = shapeOf(if (hero) 36.dp else 24.dp)) { onClick() } else if (onClick != null && grouped) it.bounceClickable { onClick() } else it }
                .padding(horizontal = if (hero) 20.dp else 16.dp, vertical = if (hero) 22.dp else if (grouped) 10.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(if (hero) 64.dp else 40.dp)
                    .clip(badgeShape)
                    .background(if (hero) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (avatarBitmap != null) {
                    Image(
                        bitmap = avatarBitmap,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (hero) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(if (hero) 32.dp else 20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(if (hero) 18.dp else 14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = if (hero) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (hero) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = if (hero) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodySmall,
                    color = if (hero) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            } else if (onClick != null) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = if (hero) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (grouped) {
        content()
    } else if (onSheet && onClick == null) {
        // Section header inside a bottom sheet: plain row, no pill
        content()
    } else {
        val cardShape = shapeOf(if (hero) 36.dp else 24.dp)
        // The sheet itself is surfaceContainerLow, so a pill of the same color would be invisible there
        val fill = when {
            hero -> MaterialTheme.colorScheme.primaryContainer
            onSheet -> cardFill()
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .let { if (onClick != null) it.bounceClickable(shape = cardShape) { onClick() } else it },
            shape = cardShape,
            color = fill,
            tonalElevation = if (hero) 2.dp else 0.dp
        ) {
            content()
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
    val shape = shapeOf(26.dp)
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .bounceClickable(shape = shape, onClick = onClick),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else cardFill(),
        border = if (selected) BorderStroke(1.5.dp, accent.copy(alpha = 0.8f)) else glassStroke(),
        tonalElevation = if (selected) 2.dp else 1.dp
    ) {
        // Own Row instead of ListItem: ListItem top-aligns the radio and the dot when the text has 3+ lines
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = accent,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
        }
    }
}

private fun getCacheSizeFormatted(context: android.content.Context): String {
    val size = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    return android.text.format.Formatter.formatFileSize(context, size)
}

@Composable
private fun rememberAssetBitmap(assetName: String): androidx.compose.ui.graphics.ImageBitmap? {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(assetName) {
        runCatching {
            context.assets.open(assetName).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }
}

@Composable
private fun rememberAsyncImageBitmap(url: String?): androidx.compose.ui.graphics.ImageBitmap? {
    if (url.isNullOrBlank()) return null
    var bitmap by remember(url) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 8_000
                conn.readTimeout = 8_000
                conn.inputStream.use { stream ->
                    val b = android.graphics.BitmapFactory.decodeStream(stream)
                    if (b != null) {
                        bitmap = b.asImageBitmap()
                    }
                }
            } catch (_: Exception) {
                bitmap = null
            }
        }
    }
    return bitmap
}