package com.reikoonya.musikoo.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray

/**
 * Настройки нижней панели главного экрана: какие элементы на ней стоят, в каком порядке,
 * и показывается ли строка поиска. Значения наблюдаемые: панель на главном экране и
 * предпросмотр в настройках обновляются сразу, без перезапуска.
 */
object BottomBarPrefs {
    private const val FILE = "musikoo_prefs"
    private const val KEY_ITEMS = "bottom_bar_items_v1"
    private const val KEY_SEARCH = "bottom_bar_search_visible"

    /** То, что было на панели до появления настройки: Artists, Songs, Random. */
    val DEFAULT_ITEMS: List<String> = listOf("artists", "songs", "random")

    private var loaded = false

    /** Ключи элементов панели по порядку (см. [BottomBarItem]). */
    var items by mutableStateOf(DEFAULT_ITEMS)
        private set

    /** Показывать ли строку поиска над навигацией. */
    var searchVisible by mutableStateOf(true)
        private set

    /** Один раз подтягивает сохранённые значения; вызывать можно сколько угодно раз. */
    fun ensureLoaded(context: Context) {
        if (loaded) return
        loaded = true
        val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        prefs.getString(KEY_ITEMS, null)?.let { items = parse(it) }
        searchVisible = prefs.getBoolean(KEY_SEARCH, true)
    }

    private fun parse(raw: String): List<String> = try {
        val array = JSONArray(raw)
        (0 until array.length())
            .map { array.getString(it) }
            .filter { BottomBarItem.fromKey(it) != null }
            .distinct()
    } catch (_: Exception) {
        DEFAULT_ITEMS
    }

    private fun setItems(context: Context, list: List<String>) {
        ensureLoaded(context)
        items = list
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ITEMS, JSONArray(list).toString())
            .apply()
    }

    fun add(context: Context, key: String) {
        ensureLoaded(context)
        if (key in items || BottomBarItem.fromKey(key) == null) return
        setItems(context, items + key)
    }

    fun remove(context: Context, key: String) {
        ensureLoaded(context)
        if (key !in items) return
        setItems(context, items - key)
    }

    fun move(context: Context, from: Int, to: Int) {
        ensureLoaded(context)
        if (from == to || from !in items.indices || to !in items.indices) return
        val list = items.toMutableList()
        list.add(to, list.removeAt(from))
        setItems(context, list)
    }

    fun setSearchVisible(context: Context, visible: Boolean) {
        ensureLoaded(context)
        searchVisible = visible
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SEARCH, visible)
            .apply()
    }

    fun reset(context: Context) {
        setItems(context, DEFAULT_ITEMS)
        setSearchVisible(context, true)
    }
}
