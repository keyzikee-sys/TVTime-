package com.tvtime.app

import android.content.Context
import android.content.SharedPreferences

/**
 * Settings for a single widget instance. When [widgetId] is [DEFAULT_ID] this object edits
 * the *default profile* — the template new widgets inherit and the fallback every widget
 * reads from when it has no instance-specific override.
 *
 * All other ids are real `appWidgetId` values; their getters fall back to the default
 * profile (then to hardcoded defaults) so an un-customized widget always reflects the
 * current template.
 */
class WidgetPreferences(context: Context, private val widgetId: Int = DEFAULT_ID) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun sp(base: String): String =
        if (widgetId == DEFAULT_ID) "default_$base" else "w${widgetId}_$base"

    private fun getString(base: String, def: String): String {
        val v = prefs.getString(sp(base), null)
        if (v != null) return v
        return prefs.getString("default_$base", def) ?: def
    }

    private fun getInt(base: String, def: Int): Int {
        if (prefs.contains(sp(base))) return prefs.getInt(sp(base), def)
        return prefs.getInt("default_$base", def)
    }

    private fun getBoolean(base: String, def: Boolean): Boolean {
        if (prefs.contains(sp(base))) return prefs.getBoolean(sp(base), def)
        return prefs.getBoolean("default_$base", def)
    }

    private fun putString(base: String, value: String) =
        prefs.edit().putString(sp(base), value).apply()

    private fun putInt(base: String, value: Int) =
        prefs.edit().putInt(sp(base), value).apply()

    private fun putBoolean(base: String, value: Boolean) =
        prefs.edit().putBoolean(sp(base), value).apply()

    var glassPreset: String
        get() = getString(KEY_PRESET, GlassBitmapRenderer.PRESET_LIGHT)
        set(value) = putString(KEY_PRESET, value)

    var bgBlurOpacity: Int
        get() = getInt(KEY_BG_BLUR, 80)
        set(value) = putInt(KEY_BG_BLUR, value)

    var gaussianBlurRadius: Int
        get() = getInt(KEY_GAUSSIAN, 12)
        set(value) = putInt(KEY_GAUSSIAN, value)

    var layerBlurSoftening: Int
        get() = getInt(KEY_LAYER, 8)
        set(value) = putInt(KEY_LAYER, value)

    var borderThickness: Int
        get() = getInt(KEY_BORDER, 2)
        set(value) = putInt(KEY_BORDER, value)

    var cornerRadius: Int
        get() = getInt(KEY_CORNER, 16)
        set(value) = putInt(KEY_CORNER, value)

    var bgHexColor: String
        get() = getString(KEY_BG_HEX, "#CC1E1E1E")
        set(value) = putString(KEY_BG_HEX, value)

    var accentHexColor: String
        get() = getString(KEY_ACCENT_HEX, "#FFFF13")
        set(value) = putString(KEY_ACCENT_HEX, value)

    var borderHexColor: String
        get() = getString(KEY_BORDER_HEX, "#33343B48")
        set(value) = putString(KEY_BORDER_HEX, value)

    var selectedServicePackage: String
        get() = getString(KEY_SERVICE, "com.tubitv")
        set(value) = putString(KEY_SERVICE, value)

    var contentSource: String
        get() = getString(KEY_CONTENT_SOURCE, CONTENT_MY_STUFF)
        set(value) = putString(KEY_CONTENT_SOURCE, value)

    /** Header label shown on the widget ("My Stuff" by default). */
    var widgetTitle: String
        get() = getString(KEY_TITLE, "My Stuff")
        set(value) = putString(KEY_TITLE, value)

    /** Primary text color for the list rows on the widget. */
    var listTextColor: String
        get() = getString(KEY_LIST_TEXT, "#F2F4F7")
        set(value) = putString(KEY_LIST_TEXT, value)

    /** When true (and the device is Android 12+), the widget tints its accent + border
     * from the system wallpaper (Material You dynamic color) instead of the saved accent. */
    var useDynamicColor: Boolean
        get() = getBoolean(KEY_DYNAMIC, false)
        set(value) = putBoolean(KEY_DYNAMIC, value)

    var currentPage: Int
        get() = getInt(KEY_CURRENT_PAGE, 0).coerceIn(0, 1)
        set(value) = putInt(KEY_CURRENT_PAGE, value.coerceIn(0, 1))

    fun toStyle(): WidgetStyle = WidgetStyle(
        glassPreset = glassPreset,
        bgBlurOpacity = bgBlurOpacity,
        gaussianBlurRadius = gaussianBlurRadius,
        layerBlurSoftening = layerBlurSoftening,
        borderThickness = borderThickness,
        cornerRadius = cornerRadius,
        bgHexColor = bgHexColor,
        accentHexColor = accentHexColor,
        borderHexColor = borderHexColor,
        selectedServicePackage = selectedServicePackage
    )

    fun applyStyle(style: WidgetStyle) {
        glassPreset = style.glassPreset
        bgBlurOpacity = style.bgBlurOpacity
        gaussianBlurRadius = style.gaussianBlurRadius
        layerBlurSoftening = style.layerBlurSoftening
        borderThickness = style.borderThickness
        cornerRadius = style.cornerRadius
        bgHexColor = style.bgHexColor
        accentHexColor = style.accentHexColor
        borderHexColor = style.borderHexColor
        selectedServicePackage = style.selectedServicePackage
    }

    companion object {
        const val PREFS_NAME = "TVTimeWidgetPrefs"
        const val DEFAULT_ID = -1
        private const val KEY_PRESET = "glass_preset"
        private const val KEY_BG_BLUR = "bg_blur_opacity"
        private const val KEY_GAUSSIAN = "gaussian_blur_radius"
        private const val KEY_LAYER = "layer_blur_softening"
        private const val KEY_BORDER = "border_thickness"
        private const val KEY_CORNER = "corner_radius"
        private const val KEY_BG_HEX = "bg_hex_color"
        private const val KEY_ACCENT_HEX = "accent_hex_color"
        private const val KEY_BORDER_HEX = "border_hex_color"
        private const val KEY_CURRENT_PAGE = "current_page"
        private const val KEY_SERVICE = "selected_service_package"
        private const val KEY_CONTENT_SOURCE = "content_source"
        const val CONTENT_MY_STUFF = "my_stuff"
        const val CONTENT_TERROR_ON_TUBI = "terror_on_tubi"
        private const val KEY_DYNAMIC = "use_dynamic_color"
        private const val KEY_TITLE = "widget_title"
        private const val KEY_LIST_TEXT = "list_text_color"
    }
}

/** Complete, serializable snapshot of a widget's visual + launch configuration. */
data class WidgetStyle(
    val glassPreset: String,
    val bgBlurOpacity: Int,
    val gaussianBlurRadius: Int,
    val layerBlurSoftening: Int,
    val borderThickness: Int,
    val cornerRadius: Int,
    val bgHexColor: String,
    val accentHexColor: String,
    val borderHexColor: String,
    val selectedServicePackage: String
)
