package com.reikoonya.musikoo.util

import com.reikoonya.musikoo.ui.theme.FontChoice
import com.reikoonya.musikoo.ui.theme.ShapeFamily
import com.reikoonya.musikoo.ui.theme.ThemeBrightness
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.reikoonya.musikoo.ui.theme.ColorThemeMode
import com.reikoonya.musikoo.ui.theme.MiniSwipeAction
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
            val name = prefs.getString(KEY_STYLE, UiStyle.CLASSIC.name)
            return try {
                when (name) {
                    "EXPENSIVE_PLUS", "EXPRESSIVE" -> UiStyle.EXPENSIVE
                    else -> UiStyle.valueOf(name ?: UiStyle.CLASSIC.name)
                }
            } catch (_: Exception) {
                UiStyle.CLASSIC
            }
        }
        val isExpressive = prefs.getBoolean(KEY_EXPRESSIVE_LEGACY, false)
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
        val name = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_COLOR_MODE, ColorThemeMode.CLASSIC.name)
        return try {
            ColorThemeMode.valueOf(name ?: ColorThemeMode.CLASSIC.name)
        } catch (_: Exception) {
            ColorThemeMode.CLASSIC
        }
    }

    fun saveColorMode(context: Context, mode: ColorThemeMode) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_COLOR_MODE, mode.name)
            .apply()
    }

    private const val KEY_BRIGHTNESS = "theme_brightness"

    fun loadBrightness(context: Context): ThemeBrightness {
        val name = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getString(KEY_BRIGHTNESS, ThemeBrightness.SYSTEM.name)
        return try {
            ThemeBrightness.valueOf(name ?: ThemeBrightness.SYSTEM.name)
        } catch (_: Exception) {
            ThemeBrightness.SYSTEM
        }
    }

    fun saveBrightness(context: Context, mode: ThemeBrightness) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BRIGHTNESS, mode.name)
            .apply()
    }

    private const val KEY_LIVE_ANIMATIONS = "live_animations"
    private const val KEY_LIVE_LEGACY = "live_slider_animations"
    private const val KEY_CASCADE_TEXT = "cascade_text"
    private const val KEY_BLUR_ENABLED = "blur_enabled"
    private var liveLoaded = false

    /**
     * Живые анимации: ползунок двигает соседние элементы, экраны и карточки появляются каскадом,
     * обложка и название меняются «магически». Наблюдаемое значение: переключатель в настройках
     * действует сразу, без перезапуска.
     */
    var liveAnimations by mutableStateOf(true)
        private set

    /**
     * Блюр интерфейса (шторки, смена обложки, текст песни). Выключение экономит батарею и GPU.
     * Наблюдаемое значение: действует сразу.
     */
    var blurEnabled by mutableStateOf(true)
        private set

    /** Каскадный текст: заголовки и названия проявляются по буквам. Наблюдаемое значение. */
    var cascadeText by mutableStateOf(true)
        private set

    /** Один раз подтягивает сохранённые значения; вызывать можно сколько угодно раз. */
    fun ensureLoaded(context: Context) {
        if (liveLoaded) return
        liveLoaded = true
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        // Раньше настройка называлась «Live Slider Animations»: переносим её значение
        liveAnimations = if (prefs.contains(KEY_LIVE_ANIMATIONS)) {
            prefs.getBoolean(KEY_LIVE_ANIMATIONS, true)
        } else {
            prefs.getBoolean(KEY_LIVE_LEGACY, true)
        }
        cascadeText = prefs.getBoolean(KEY_CASCADE_TEXT, true)
        blurEnabled = prefs.getBoolean(KEY_BLUR_ENABLED, true)
    }

    fun loadBlurEnabled(context: Context): Boolean {
        ensureLoaded(context)
        return blurEnabled
    }

    fun saveBlurEnabled(context: Context, enabled: Boolean) {
        ensureLoaded(context)
        blurEnabled = enabled
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_BLUR_ENABLED, enabled)
            .apply()
    }

    fun loadLiveAnimations(context: Context): Boolean {
        ensureLoaded(context)
        return liveAnimations
    }

    fun saveLiveAnimations(context: Context, enabled: Boolean) {
        ensureLoaded(context)
        liveAnimations = enabled
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LIVE_ANIMATIONS, enabled)
            .apply()
    }

    fun loadCascadeText(context: Context): Boolean {
        ensureLoaded(context)
        return cascadeText
    }

    fun saveCascadeText(context: Context, enabled: Boolean) {
        ensureLoaded(context)
        cascadeText = enabled
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CASCADE_TEXT, enabled)
            .apply()
    }

    private const val KEY_LIBRARY_TABS_MODE = "library_swipe_tabs_mode"
    private const val KEY_LIBRARY_PAGES = "library_swipe_pages"
    private const val KEY_CLASSIC_SECTIONS = "library_classic_sections"

    /** Разделы библиотеки на главном экране: и страницы свайпов, и секции классического вида. */
    val LIBRARY_SECTION_KEYS: List<String> = listOf("albums", "pins", "playlists")

    private var libraryLoaded = false

    /** true: библиотека листается свайпами (Library Swipe Tabs); false: классический вид секциями. Observable. */
    var libraryTabsEnabled by mutableStateOf(false)
        private set

    /** Какие страницы есть в свайпах и в каком порядке. Не пусто. Observable. */
    var libraryPages by mutableStateOf(LIBRARY_SECTION_KEYS)
        private set

    /** Какие секции видны в классическом виде. Не пусто. Observable. */
    var classicSections by mutableStateOf(LIBRARY_SECTION_KEYS.toSet())
        private set

    private fun parseKeys(raw: String?): List<String>? =
        raw?.split(",")?.map { it.trim() }?.filter { it in LIBRARY_SECTION_KEYS }?.distinct()?.takeIf { it.isNotEmpty() }

    fun ensureLibraryLoaded(context: Context) {
        if (libraryLoaded) return
        libraryLoaded = true
        val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        libraryTabsEnabled = prefs.getBoolean(KEY_LIBRARY_TABS_MODE, false)
        parseKeys(prefs.getString(KEY_LIBRARY_PAGES, null))?.let { libraryPages = it }
        parseKeys(prefs.getString(KEY_CLASSIC_SECTIONS, null))?.let { classicSections = it.toSet() }
    }

    fun loadLibraryTabsMode(context: Context): Boolean {
        ensureLibraryLoaded(context)
        return libraryTabsEnabled
    }

    fun saveLibraryTabsMode(context: Context, enabled: Boolean) {
        ensureLibraryLoaded(context)
        libraryTabsEnabled = enabled
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_LIBRARY_TABS_MODE, enabled).apply()
    }

    private fun saveLibraryPages(context: Context, pages: List<String>) {
        libraryPages = pages
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_LIBRARY_PAGES, pages.joinToString(",")).apply()
    }

    /** Включает или выключает страницу свайпов. Последнюю оставшуюся выключить нельзя. */
    fun toggleLibraryPage(context: Context, key: String) {
        ensureLibraryLoaded(context)
        if (key !in LIBRARY_SECTION_KEYS) return
        if (key in libraryPages) {
            if (libraryPages.size > 1) saveLibraryPages(context, libraryPages - key)
        } else {
            saveLibraryPages(context, libraryPages + key)
        }
    }

    /** Двигает страницу свайпов на [delta] позиций (-1 вверх, +1 вниз) среди включённых. */
    fun moveLibraryPage(context: Context, key: String, delta: Int) {
        ensureLibraryLoaded(context)
        val from = libraryPages.indexOf(key)
        val to = from + delta
        if (from < 0 || to !in libraryPages.indices) return
        val list = libraryPages.toMutableList()
        list.add(to, list.removeAt(from))
        saveLibraryPages(context, list)
    }

    /** Показывает или прячет секцию классического вида. Последнюю видимую спрятать нельзя. */
    fun toggleClassicSection(context: Context, key: String) {
        ensureLibraryLoaded(context)
        if (key !in LIBRARY_SECTION_KEYS) return
        val next = if (key in classicSections) {
            if (classicSections.size > 1) classicSections - key else return
        } else classicSections + key
        classicSections = next
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_CLASSIC_SECTIONS, LIBRARY_SECTION_KEYS.filter { it in next }.joinToString(",")).apply()
    }

    private const val KEY_MINI_LEFT = "mini_swipe_left"
    private const val KEY_MINI_RIGHT = "mini_swipe_right"
    private var miniLoaded = false

    /** Mini player: action of the swipe to the left (default: favorite). Observable. */
    var miniSwipeLeft by mutableStateOf(MiniSwipeAction.FAVORITE)
        private set

    /** Mini player: action of the swipe to the right (default: hide). Observable. */
    var miniSwipeRight by mutableStateOf(MiniSwipeAction.HIDE)
        private set

    private fun parseMini(name: String?, fallback: MiniSwipeAction): MiniSwipeAction =
        try { MiniSwipeAction.valueOf(name ?: fallback.name) } catch (_: Exception) { fallback }

    /** Loads the saved swipe actions once; safe to call as often as needed. */
    fun ensureMiniLoaded(context: Context) {
        if (miniLoaded) return
        miniLoaded = true
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        miniSwipeLeft = parseMini(prefs.getString(KEY_MINI_LEFT, null), MiniSwipeAction.FAVORITE)
        miniSwipeRight = parseMini(prefs.getString(KEY_MINI_RIGHT, null), MiniSwipeAction.HIDE)
    }

    fun saveMiniSwipeLeft(context: Context, action: MiniSwipeAction) {
        miniLoaded = true
        miniSwipeLeft = action
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_MINI_LEFT, action.name).apply()
    }

    fun saveMiniSwipeRight(context: Context, action: MiniSwipeAction) {
        miniLoaded = true
        miniSwipeRight = action
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_MINI_RIGHT, action.name).apply()
    }
}