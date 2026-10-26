package com.reikoonya.musikoo.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

enum class AppIconMode {
    BLACK_WHITE, // Black and white (current)
    WHITE_BLACK, // White and black
    MONET       // Monet
}

object AppIconManager {
    private const val PREFS_FILE = "musikoo_prefs"
    private const val KEY_APP_ICON = "app_icon_mode"

    fun loadIconMode(context: Context): AppIconMode {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_APP_ICON, AppIconMode.BLACK_WHITE.name)
        return try {
            AppIconMode.valueOf(name ?: AppIconMode.BLACK_WHITE.name)
        } catch (_: Exception) {
            AppIconMode.BLACK_WHITE
        }
    }

    fun setIconMode(context: Context, mode: AppIconMode) {
        val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APP_ICON, mode.name).apply()

        val pm = context.packageManager
        val pkgName = context.packageName

        val aliases = mapOf(
            AppIconMode.BLACK_WHITE to ComponentName(pkgName, "$pkgName.MainActivityBlackWhite"),
            AppIconMode.WHITE_BLACK to ComponentName(pkgName, "$pkgName.MainActivityWhiteBlack"),
            AppIconMode.MONET to ComponentName(pkgName, "$pkgName.MainActivityMonet")
        )

        aliases.forEach { (iconMode, componentName) ->
            val state = if (iconMode == mode) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                pm.setComponentEnabledSetting(
                    componentName,
                    state,
                    PackageManager.DONT_KILL_APP
                )
            } catch (_: Exception) {}
        }
    }
}
