package com.tvtime.app

import org.junit.Assert.*
import org.junit.Test

class ColorUtilsTest {

    @Test
    fun parseArgb_returnsNullForInvalidInput() {
        assertNull(ColorUtils.parseArgb(null))
        assertNull(ColorUtils.parseArgb(""))
        assertNull(ColorUtils.parseArgb("#12"))
        assertNull(ColorUtils.parseArgb("not-a-color"))
    }

    @Test
    fun parseArgb_handlesRgb() {
        // #RRGGBB
        assertEquals(0xFFFF1493.toInt(), ColorUtils.parseArgb("#FF1493"))
        assertEquals(0xFF1E1E1E.toInt(), ColorUtils.parseArgb("#1E1E1E"))
    }

    @Test
    fun parseArgb_handlesArgb() {
        assertEquals(0xCC1E1E1E.toInt(), ColorUtils.parseArgb("#CC1E1E1E"))
        assertEquals(0x3303DAC5.toInt(), ColorUtils.parseArgb("#3303DAC5"))
    }

    @Test
    fun parseArgb_handlesShortRgb() {
        // #RGB -> #FF0000
        assertEquals(0xFFFF0000.toInt(), ColorUtils.parseArgb("#F00"))
    }

    @Test
    fun parseArgb_isCaseInsensitiveAndToleratesMissingHash() {
        assertEquals(ColorUtils.parseArgb("#ff1493"), ColorUtils.parseArgb("ff1493"))
        assertEquals(ColorUtils.parseArgb("#FF1493"), ColorUtils.parseArgb("#ff1493"))
    }

    @Test
    fun withAlpha_replacesAlphaChannel() {
        val opaque = 0xFF1E1E1E.toInt()
        val half = ColorUtils.withAlpha(opaque, 50)
        // alpha ~ 0x7F (127)
        assertEquals(0x7F1E1E1E.toInt(), half)
    }

    @Test
    fun withAlpha_clampsOutOfRange() {
        val color = 0xFF000000.toInt()
        assertEquals(0x00000000.toInt(), ColorUtils.withAlpha(color, -10))
        assertEquals(0xFF000000.toInt(), ColorUtils.withAlpha(color, 200))
    }

    @Test
    fun toArgbHex_roundTrips() {
        val hex = "#CC1E1E1E"
        val parsed = ColorUtils.parseArgb(hex)!!
        assertEquals(hex.uppercase(), ColorUtils.toArgbHex(parsed))
    }
}
