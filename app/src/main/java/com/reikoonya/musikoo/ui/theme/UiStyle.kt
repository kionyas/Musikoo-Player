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

/** What a horizontal swipe on the mini player does (configured in Settings -> Mini-player Gestures). */
enum class MiniSwipeAction(val title: String) {
    FAVORITE("Favorite"),
    HIDE("Hide mini player"),
    NEXT("Next track"),
    PREVIOUS("Previous track"),
    PLAY_PAUSE("Play / Pause"),
    NONE("Nothing")
}

enum class FontChoice {
    BASE,
    LORA
}

/** Light / dark choice: follow the system or force one of them. */
enum class ThemeBrightness {
    SYSTEM,
    LIGHT,
    DARK
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
