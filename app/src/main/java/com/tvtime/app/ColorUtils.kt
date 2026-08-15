package com.tvtime.app

/**
 * Pure, framework-free color helpers so they can be unit-tested without Android dependencies.
 */
object ColorUtils {

    /**
     * Parses an ARGB/RGB hex string into a packed color int.
     * Supports #RGB, #RRGGBB, #ARGB and #AARRGGBB (with or without leading '#').
     * Returns null when the input is not a valid color.
     */
    fun parseArgb(hex: String?): Int? {
        if (hex == null) return null
        val s = hex.trim().removePrefix("#").lowercase()
        if (s.isEmpty()) return null
        return when (s.length) {
            3 -> { // #RGB -> duplicate each digit
                val r = s[0].digitToIntOrNull(16) ?: return null
                val g = s[1].digitToIntOrNull(16) ?: return null
                val b = s[2].digitToIntOrNull(16) ?: return null
                val rgb = ((r * 16 + r) shl 16) or ((g * 16 + g) shl 8) or (b * 16 + b)
                (0xFF shl 24) or rgb
            }
            6 -> { // #RRGGBB
                val v = s.toIntOrNull(16) ?: return null
                (0xFF shl 24) or v
            }
            8 -> { // #AARRGGBB
                s.toIntOrNull(16)
            }
            else -> null
        }
    }

    /**
     * Returns [color] with its alpha channel replaced by [alphaPercent] (0-100).
     */
    fun withAlpha(color: Int, alphaPercent: Int): Int {
        val alpha = ((alphaPercent.coerceIn(0, 100)) / 100.0f * 255).toInt().coerceIn(0, 255)
        return (color and 0x00FFFFFF) or (alpha shl 24)
    }

    /** Returns the ARGB hex string for [color], e.g. "#FFFF1493". */
    fun toArgbHex(color: Int): String = String.format("#%08X", color)

    /** Returns the RGB portion (#RRGGBB) of [color]. */
    fun toRgbHex(color: Int): String = String.format("#%06X", color and 0x00FFFFFF)
}
