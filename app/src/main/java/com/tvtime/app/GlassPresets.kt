package com.tvtime.app

/**
 * Built-in glass style presets. Selecting a preset in the config UI applies these defaults
 * (background, accent, border colors and shape), which the user can then fine-tune.
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
            bgHex = "#CCFFFFFF", accentHex = "#FF3700B3", borderHex = "#33FFFFFF",
            bgBlurOpacity = 80, cornerRadius = 16, borderThickness = 2
        ),
        GlassBitmapRenderer.PRESET_DARK to Preset(
            bgHex = "#CC1E1E1E", accentHex = "#4DD0E1", borderHex = "#3303DAC5",
            bgBlurOpacity = 85, cornerRadius = 16, borderThickness = 2
        ),
        GlassBitmapRenderer.PRESET_TINTED to Preset(
            bgHex = "#CC2A1A3A", accentHex = "#FFFF1F8F", borderHex = "#3303DAC5",
            bgBlurOpacity = 80, cornerRadius = 18, borderThickness = 2
        ),
        GlassBitmapRenderer.PRESET_LIQUID to Preset(
            bgHex = "#CC0F2027", accentHex = "#FF00F2FE", borderHex = "#3303DAC5",
            bgBlurOpacity = 70, cornerRadius = 24, borderThickness = 3
        )
    )

    /** Returns the [Preset] for a given preset name, or null if unknown. */
    operator fun get(name: String): Preset? = DEFAULTS[name]
}
