package com.reikoonya.musikoo.ui.theme

import androidx.compose.runtime.compositionLocalOf

enum class UiStyle {
    CLASSIC,
    EXPENSIVE
}

/** Shape family: MD3 = standard Material 3 corner scale, MD3E = Material 3 Expressive leaf-like corners. */
enum class ShapeFamily {
    MD3,
    MD3E
}

enum class FontChoice {
    BASE,
    LORA
}

enum class ColorThemeMode {
    DYNAMIC,
    CLASSIC,
    COVER
}

/**
 * UI style variant:
 * CLASSIC   = plain Material 3 (solid surfaces)
 * EXPENSIVE = stylized UI with floating translucent pills and native blur
 */
val LocalUiStyle = compositionLocalOf { UiStyle.EXPENSIVE }

val LocalFancyStyle = compositionLocalOf { true }

val LocalShapeFamily = compositionLocalOf { ShapeFamily.MD3E }
