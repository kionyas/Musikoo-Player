package com.reikoonya.musikoo.ui.theme

import androidx.compose.runtime.compositionLocalOf

enum class UiStyle {
    CLASSIC,
    EXPENSIVE
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
