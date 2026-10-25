package com.reikoonya.musikoo.util

import com.reikoonya.musikoo.ui.theme.FontChoice
import com.reikoonya.musikoo.ui.theme.ShapeFamily
import android.content.Context
import com.reikoonya.musikoo.ui.theme.ColorThemeMode
import com.reikoonya.musikoo.ui.theme.UiStyle

/** Stores the UI style and theme choice between app launches. */
object UiStylePrefs {
    private const val FILE = "musikoo_prefs"
    private const val KEY_STYLE = "ui_style_mode"
    private const val KEY_COLOR_MODE = "color_theme_mode"
    private const val KEY_SHAPE_FAMILY = "shape_family"
    private const val KEY_EXPRESSIVE_LEGACY = "expressive_style"

    fun loadStyle(context: Context): UiStyle {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_STYLE)) {
            val name = prefs.getString(KEY_STYLE, UiStyle.EXPENSIVE.name)
            return try {
                when (name) {
                    "EXPENSIVE_PLUS", "EXPRESSIVE" -> UiStyle.EXPENSIVE
                    else -> UiStyle.valueOf(name ?: UiStyle.EXPENSIVE.name)
                }
            } catch (_: Exception) {
                UiStyle.EXPENSIVE
            }
        }
        val isExpressive = prefs.getBoolean(KEY_EXPRESSIVE_LEGACY, true)
        return if (isExpressive) UiStyle.EXPENSIVE else UiStyle.CLASSIC
    }

    fun saveStyle(context: Context, style: UiStyle) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STYLE, style.name)
            .putBoolean(KEY_EXPRESSIVE_LEGACY, style != UiStyle.CLASSIC)
            .apply()
    }

    fun load(context: Context): UiStyle = loadStyle(context)
    fun save(context: Context, style: UiStyle) = saveStyle(context, style)

    fun loadFontChoice(context: Context): FontChoice {
        val name = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString("font_choice", FontChoice.BASE.name)
        return try { FontChoice.valueOf(name ?: FontChoice.BASE.name) } catch (_: Exception) { FontChoice.BASE }
    }

    fun saveFontChoice(context: Context, choice: FontChoice) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString("font_choice", choice.name).apply()
    }

    fun loadShapeFamily(context: Context): ShapeFamily {
        val name = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_SHAPE_FAMILY, ShapeFamily.MD3E.name)
        return try {
            ShapeFamily.valueOf(name ?: ShapeFamily.MD3E.name)
        } catch (_: Exception) {
            ShapeFamily.MD3E
        }
    }

    fun saveShapeFamily(context: Context, family: ShapeFamily) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SHAPE_FAMILY, family.name)
            .apply()
    }

    fun loadColorMode(context: Context): ColorThemeMode {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_COLOR_MODE, ColorThemeMode.DYNAMIC.name)
        return try {
            ColorThemeMode.valueOf(name ?: ColorThemeMode.DYNAMIC.name)
        } catch (_: Exception) {
            ColorThemeMode.DYNAMIC
        }
    }

    fun saveColorMode(context: Context, mode: ColorThemeMode) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COLOR_MODE, mode.name)
            .apply()
    }
}
