package com.tvtime.app

import android.content.Context
import android.content.SharedPreferences

/**
 * Single source of truth for the widget's customization settings.
 * Replaces the scattered, inconsistent [android.content.SharedPreferences] reads/writes
 * previously duplicated across the provider and config UI.
 */
class WidgetPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var glassPreset: String
        get() = prefs.getString(KEY_PRESET, GlassBitmapRenderer.PRESET_LIGHT)
            ?: GlassBitmapRenderer.PRESET_LIGHT
        set(value) = prefs.edit().putString(KEY_PRESET, value).apply()

    var bgBlurOpacity: Int
        get() = prefs.getInt(KEY_BG_BLUR, 80)
        set(value) = prefs.edit().putInt(KEY_BG_BLUR, value).apply()

    var gaussianBlurRadius: Int
        get() = prefs.getInt(KEY_GAUSSIAN, 12)
        set(value) = prefs.edit().putInt(KEY_GAUSSIAN, value).apply()

    var layerBlurSoftening: Int
        get() = prefs.getInt(KEY_LAYER, 8)
        set(value) = prefs.edit().putInt(KEY_LAYER, value).apply()

    var borderThickness: Int
        get() = prefs.getInt(KEY_BORDER, 2)
        set(value) = prefs.edit().putInt(KEY_BORDER, value).apply()

    var cornerRadius: Int
        get() = prefs.getInt(KEY_CORNER, 16)
        set(value) = prefs.edit().putInt(KEY_CORNER, value).apply()

    var bgHexColor: String
        get() = prefs.getString(KEY_BG_HEX, "#CC1E1E1E") ?: "#CC1E1E1E"
        set(value) = prefs.edit().putString(KEY_BG_HEX, value).apply()

    var accentHexColor: String
        get() = prefs.getString(KEY_ACCENT_HEX, "#FFFF1493") ?: "#FFFF1493"
        set(value) = prefs.edit().putString(KEY_ACCENT_HEX, value).apply()

    var borderHexColor: String
        get() = prefs.getString(KEY_BORDER_HEX, "#3303DAC5") ?: "#3303DAC5"
        set(value) = prefs.edit().putString(KEY_BORDER_HEX, value).apply()

    /** Currently displayed widget page (0 = WatchList, 1 = My Stuff). */
    var currentPage: Int
        get() = prefs.getInt(KEY_CURRENT_PAGE, 0).coerceIn(0, 1)
        set(value) = prefs.edit().putInt(KEY_CURRENT_PAGE, value.coerceIn(0, 1)).apply()

    companion object {
        const val PREFS_NAME = "TVTimeWidgetPrefs"
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
    }
}
