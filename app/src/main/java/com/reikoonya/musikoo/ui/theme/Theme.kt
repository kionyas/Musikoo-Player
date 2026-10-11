package com.reikoonya.musikoo.ui.theme

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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.reikoonya.musikoo.util.accentFromBitmap

private val DarkColorScheme = darkColorScheme(
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

private val LightColorScheme = lightColorScheme(
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

private val ClassicDarkColorScheme = darkColorScheme(
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

private val ClassicLightColorScheme = lightColorScheme(
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

private fun createCoverColorScheme(
    art: Bitmap?,
    darkTheme: Boolean,
    fallback: ColorScheme
): ColorScheme {
    if (art == null) return fallback
    val accent = accentFromBitmap(art, darkTheme) ?: return fallback

    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(accent.toArgb(), hsv)

    val hue = hsv[0]
    val sat = hsv[1]

    fun hsvColor(h: Float, s: Float, v: Float): Color {
        return Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))
    }

    return if (darkTheme) {
        val primary = hsvColor(hue, (sat * 0.9f).coerceAtLeast(0.45f), 0.95f)
        val onPrimary = Color(0xFF101010)
        val primaryContainer = hsvColor(hue, (sat * 0.6f).coerceIn(0.25f, 0.65f), 0.35f)
        val onPrimaryContainer = hsvColor(hue, (sat * 0.3f).coerceAtMost(0.4f), 0.95f)
        val secondary = hsvColor((hue + 20f) % 360f, (sat * 0.6f).coerceIn(0.2f, 0.5f), 0.85f)

        fallback.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = secondary,
            tertiary = hsvColor((hue + 40f) % 360f, (sat * 0.5f).coerceIn(0.2f, 0.5f), 0.90f)
        )
    } else {
        val primary = hsvColor(hue, (sat * 0.95f).coerceAtLeast(0.55f), 0.50f)
        val onPrimary = Color.White
        val primaryContainer = hsvColor(hue, (sat * 0.25f).coerceIn(0.15f, 0.4f), 0.92f)
        val onPrimaryContainer = hsvColor(hue, (sat * 0.9f).coerceAtLeast(0.6f), 0.25f)

        fallback.copy(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer
        )
    }
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
    coverArt: Bitmap? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

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
            createCoverColorScheme(coverArt, darkTheme, fallback)
        }
    }

    val isFancy = uiStyle != UiStyle.CLASSIC

    CompositionLocalProvider(
        LocalUiStyle provides uiStyle,
        LocalFancyStyle provides isFancy
    ) {
        MaterialTheme(
            colorScheme = baseScheme,
            shapes = ExpressiveShapes,
            typography = if (isFancy) Typography else ClassicTypography,
            content = content
        )
    }
}

@Composable
fun MusikooTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    uiStyle: UiStyle = UiStyle.EXPENSIVE,
    content: @Composable () -> Unit
) = MusikooTheme(
    darkTheme = darkTheme,
    colorThemeMode = if (dynamicColor) ColorThemeMode.DYNAMIC else ColorThemeMode.CLASSIC,
    uiStyle = uiStyle,
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
