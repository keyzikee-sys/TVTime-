package com.tvtime.app

/**
 * Built-in TVTime liquid-glass presets.
 *
 * Final brand palette:
 * Tubi Pink  #FF00A6
 * Tubi Broom #FFFF13
 * Black Russian #0B0019
 */
object GlassPresets {

    data class Preset(
        val bgHex: String,
        val accentHex: String,
        val borderHex: String,
        val bgBlurOpacity: Int,
        val cornerRadius: Int,
        val borderThickness: Int,
        val gaussianBlurRadius: Int = 12,
        val layerBlurSoftening: Int = 8
    )

    private val DEFAULTS = mapOf(

        GlassBitmapRenderer.PRESET_LIGHT to Preset(
            bgHex = "#E60B0019",
            accentHex = "#FF00A6",
            borderHex = "#66FF00A6",
            bgBlurOpacity = 78,
            cornerRadius = 20,
            borderThickness = 2
        ),

        GlassBitmapRenderer.PRESET_DARK to Preset(
            bgHex = "#E60B0019",
            accentHex = "#FFFF13",
            borderHex = "#66FFFF13",
            bgBlurOpacity = 88,
            cornerRadius = 20,
            borderThickness = 2
        ),

        GlassBitmapRenderer.PRESET_TINTED to Preset(
            bgHex = "#E60B0019",
            accentHex = "#FF00A6",
            borderHex = "#66FFFF13",
            bgBlurOpacity = 80,
            cornerRadius = 22,
            borderThickness = 2
        ),

        GlassBitmapRenderer.PRESET_LIQUID to Preset(
            bgHex = "#D90B0019",
            accentHex = "#FF00A6",
            borderHex = "#88FFFF13",
            bgBlurOpacity = 58,
            cornerRadius = 26,
            borderThickness = 2
        )
    )

    /** Returns the preset for a given name, or null if unknown. */
    operator fun get(name: String): Preset? = DEFAULTS[name]
}
