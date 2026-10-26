package com.reikoonya.musikoo.ui.theme

import androidx.compose.ui.text.font.FontFamily
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Material 3 standard shape scale (M3 shape tokens)
val Md3Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

internal val DarkColorScheme = darkColorScheme(
    primary = ExpressiveDarkPrimary,
    onPrimary = ExpressiveDarkOnPrimary,
    primaryContainer = ExpressiveDarkPrimaryContainer,
    onPrimaryContainer = ExpressiveDarkOnPrimaryContainer,
    secondary = ExpressiveDarkSecondary,
    onSecondary = ExpressiveDarkOnSecondary,
    secondaryContainer = ExpressiveDarkSecondaryContainer,
    onSecondaryContainer = ExpressiveDarkOnSecondaryContainer,
    tertiary = ExpressiveDarkTertiary,
    onTertiary = ExpressiveDarkOnTertiary,
    tertiaryContainer = ExpressiveDarkTertiaryContainer,
    onTertiaryContainer = ExpressiveDarkOnTertiaryContainer,
    background = ExpressiveDarkBackground,
    onBackground = ExpressiveDarkOnBackground,
    surface = ExpressiveDarkSurface,
    onSurface = ExpressiveDarkOnSurface,
    surfaceVariant = ExpressiveDarkSurfaceVariant,
    onSurfaceVariant = ExpressiveDarkOnSurfaceVariant,
    surfaceDim = ExpressiveDarkSurfaceDim,
    surfaceBright = ExpressiveDarkSurfaceBright,
    surfaceContainerLowest = ExpressiveDarkSurfaceContainerLowest,
    surfaceContainerLow = ExpressiveDarkSurfaceContainerLow,
    surfaceContainer = ExpressiveDarkSurfaceContainer,
    surfaceContainerHigh = ExpressiveDarkSurfaceContainerHigh,
    surfaceContainerHighest = ExpressiveDarkSurfaceContainerHighest,
)

internal val LightColorScheme = lightColorScheme(
    primary = ExpressiveLightPrimary,
    onPrimary = ExpressiveLightOnPrimary,
    primaryContainer = ExpressiveLightPrimaryContainer,
    onPrimaryContainer = ExpressiveLightOnPrimaryContainer,
    secondary = ExpressiveLightSecondary,
    onSecondary = ExpressiveLightOnSecondary,
    secondaryContainer = ExpressiveLightSecondaryContainer,
    onSecondaryContainer = ExpressiveLightOnSecondaryContainer,
    tertiary = ExpressiveLightTertiary,
    onTertiary = ExpressiveLightOnTertiary,
    tertiaryContainer = ExpressiveLightTertiaryContainer,
    onTertiaryContainer = ExpressiveLightOnTertiaryContainer,
    background = ExpressiveLightBackground,
    onBackground = ExpressiveLightOnBackground,
    surface = ExpressiveLightSurface,
    onSurface = ExpressiveLightOnSurface,
    surfaceVariant = ExpressiveLightSurfaceVariant,
    onSurfaceVariant = ExpressiveLightOnSurfaceVariant,
    surfaceDim = ExpressiveLightSurfaceDim,
    surfaceBright = ExpressiveLightSurfaceBright,
    surfaceContainerLowest = ExpressiveLightSurfaceContainerLowest,
    surfaceContainerLow = ExpressiveLightSurfaceContainerLow,
    surfaceContainer = ExpressiveLightSurfaceContainer,
    surfaceContainerHigh = ExpressiveLightSurfaceContainerHigh,
    surfaceContainerHighest = ExpressiveLightSurfaceContainerHighest,
)

internal val ClassicDarkColorScheme = darkColorScheme(
    primary = Color(0xFFE6E6E6),
    onPrimary = Color(0xFF121212),
    primaryContainer = Color(0xFF333333),
    onPrimaryContainer = Color(0xFFF5F5F5),
    secondary = Color(0xFFCCCCCC),
    onSecondary = Color(0xFF121212),
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = Color(0xFFE6E6E6),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEDEDED),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFC0C0C0),
    surfaceContainerLowest = Color(0xFF0D0D0D),
    surfaceContainerLow = Color(0xFF181818),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerHigh = Color(0xFF262626),
    surfaceContainerHighest = Color(0xFF303030)
)

internal val ClassicLightColorScheme = lightColorScheme(
    primary = Color(0xFF1A1A1A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE8E8E8),
    onPrimaryContainer = Color(0xFF121212),
    secondary = Color(0xFF3A3A3A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0F0F0),
    onSecondaryContainer = Color(0xFF1A1A1A),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1A1A1A),
    surface = Color(0xFFFAFAFA),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFF505050),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F5F5),
    surfaceContainer = Color(0xFFEEEEEE),
    surfaceContainerHigh = Color(0xFFE4E4E4),
    surfaceContainerHighest = Color(0xFFDADADA)
)

/** Smoothly cross-fades every role when the scheme changes (new cover, theme switch). */
@Composable
private fun ColorScheme.animated(): ColorScheme {
    val spec = tween<Color>(durationMillis = 600)
    @Composable
    fun a(c: Color): Color = animateColorAsState(c, spec, label = "scheme").value
    return copy(
        primary = a(primary), onPrimary = a(onPrimary),
        primaryContainer = a(primaryContainer), onPrimaryContainer = a(onPrimaryContainer),
        inversePrimary = a(inversePrimary),
        secondary = a(secondary), onSecondary = a(onSecondary),
        secondaryContainer = a(secondaryContainer), onSecondaryContainer = a(onSecondaryContainer),
        tertiary = a(tertiary), onTertiary = a(onTertiary),
        tertiaryContainer = a(tertiaryContainer), onTertiaryContainer = a(onTertiaryContainer),
        background = a(background), onBackground = a(onBackground),
        surface = a(surface), onSurface = a(onSurface),
        surfaceVariant = a(surfaceVariant), onSurfaceVariant = a(onSurfaceVariant),
        surfaceTint = a(surfaceTint),
        inverseSurface = a(inverseSurface), inverseOnSurface = a(inverseOnSurface),
        error = a(error), onError = a(onError),
        errorContainer = a(errorContainer), onErrorContainer = a(onErrorContainer),
        outline = a(outline), outlineVariant = a(outlineVariant),
        surfaceDim = a(surfaceDim), surfaceBright = a(surfaceBright),
        surfaceContainerLowest = a(surfaceContainerLowest), surfaceContainerLow = a(surfaceContainerLow),
        surfaceContainer = a(surfaceContainer), surfaceContainerHigh = a(surfaceContainerHigh),
        surfaceContainerHighest = a(surfaceContainerHighest)
    )
}

// Material 3 Expressive (Android 17 style) hyper-rounded experimental shapes
val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(18.dp),
    small = RoundedCornerShape(26.dp),
    medium = RoundedCornerShape(36.dp),
    large = RoundedCornerShape(48.dp),
    extraLarge = RoundedCornerShape(64.dp)
)

@Composable
fun MusikooTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorThemeMode: ColorThemeMode = ColorThemeMode.DYNAMIC,
    uiStyle: UiStyle = UiStyle.EXPENSIVE,
    shapeFamily: ShapeFamily = ShapeFamily.MD3E,
    fontChoice: FontChoice = FontChoice.BASE,
    coverArt: Bitmap? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    // Shape helpers read the family from here, so the setting applies to every shapeOf() call
    com.reikoonya.musikoo.ui.components.ShapeStyle.family = shapeFamily

    val baseScheme = when (colorThemeMode) {
        ColorThemeMode.DYNAMIC -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        ColorThemeMode.CLASSIC -> {
            if (darkTheme) ClassicDarkColorScheme else ClassicLightColorScheme
        }
        ColorThemeMode.COVER -> {
            val fallback = if (darkTheme) DarkColorScheme else LightColorScheme
            // Real Material You palette (tonal, with matching surfaces) built from the cover
            val coverScheme = remember(coverArt, darkTheme) {
                coverArt?.let { monetSchemeFromBitmap(it, darkTheme) }
            }
            coverScheme ?: fallback
        }
    }

    val isFancy = uiStyle != UiStyle.CLASSIC

    // Status / navigation bar icons must contrast with the chosen (maybe forced) light or dark theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var ctx = view.context
            while (ctx is android.content.ContextWrapper && ctx !is android.app.Activity) ctx = ctx.baseContext
            val window = (ctx as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalUiStyle provides uiStyle,
        LocalFancyStyle provides isFancy
    ) {
        MaterialTheme(
            colorScheme = baseScheme.animated(),
            shapes = if (shapeFamily == ShapeFamily.MD3) Md3Shapes else ExpressiveShapes,
            typography = (if (isFancy) Typography else ClassicTypography)
                .let { if (fontChoice == FontChoice.LORA) it.withFamily(LoraFamily) else it },
            content = content
        )
    }
}

@Composable
fun MusikooTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    uiStyle: UiStyle = UiStyle.EXPENSIVE,
    shapeFamily: ShapeFamily = ShapeFamily.MD3E,
    content: @Composable () -> Unit
) = MusikooTheme(
    darkTheme = darkTheme,
    colorThemeMode = if (dynamicColor) ColorThemeMode.DYNAMIC else ColorThemeMode.CLASSIC,
    uiStyle = uiStyle,
    shapeFamily = shapeFamily,
    content = content
)

@Composable
fun MusikooTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    expressive: Boolean,
    content: @Composable () -> Unit
) = MusikooTheme(
    darkTheme = darkTheme,
    colorThemeMode = if (dynamicColor) ColorThemeMode.DYNAMIC else ColorThemeMode.CLASSIC,
    uiStyle = if (expressive) UiStyle.EXPENSIVE else UiStyle.CLASSIC,
    content = content
)

@Composable
fun PlayeerokTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) = MusikooTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)

@Composable
fun MyMD3PlayerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) = MusikooTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
