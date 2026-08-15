package com.tvtime.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/**
 * Renders a glassmorphism-style container (rounded, semi-transparent fill with an optional
 * border and a subtle top highlight for the "liquid" preset) into a [Bitmap] that can be
 * applied to both the home-screen widget and the in-app live preview.
 */
object GlassBitmapRenderer {

    const val PRESET_LIGHT = "LIGHT"
    const val PRESET_DARK = "DARK"
    const val PRESET_TINTED = "TINTED"
    const val PRESET_LIQUID = "LIQUID"
    const val PRESET_LIQUID_NOBLUR = "PRESET_LIQUID_NOBLUR"

    /** Whether a stored preset name should be rendered with the liquid gradient look. */
    fun isLiquid(preset: String): Boolean =
        preset == PRESET_LIQUID || preset == PRESET_LIQUID_NOBLUR

    private val DEFAULT_BG = ColorUtils.parseArgb("#CC1E1E1E")!!
    private val DEFAULT_BORDER = ColorUtils.parseArgb("#3303DAC5")!!

    @JvmStatic
    fun renderLauncherContainer(
        widthPx: Int,
        heightPx: Int,
        bgHex: String,
        borderHex: String,
        cornerRadiusDp: Int,
        borderThicknessDp: Int,
        alphaPercent: Int,
        density: Float,
        gradient: Boolean = false
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(
            widthPx.coerceAtLeast(1),
            heightPx.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)

        val bgColor = ColorUtils.parseArgb(bgHex) ?: DEFAULT_BG
        val borderColor = ColorUtils.parseArgb(borderHex) ?: DEFAULT_BORDER
        val finalBg = ColorUtils.withAlpha(bgColor, alphaPercent)

        val rx = cornerRadiusDp * density
        val stroke = borderThicknessDp * density
        val rect = RectF(
            stroke / 2f,
            stroke / 2f,
            widthPx - (stroke / 2f),
            heightPx - (stroke / 2f)
        )

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (gradient) blendWithWhite(finalBg, 0.14f) else finalBg
        }
        canvas.drawRoundRect(rect, rx, rx, fillPaint)

        if (gradient) {
            val highlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = 0x33FFFFFF
            }
            canvas.drawRoundRect(
                RectF(rect.left, rect.top, rect.right, rect.top + rect.height() * 0.45f),
                rx, rx, highlight
            )
        }

        if (stroke > 0) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                color = borderColor
            }
            canvas.drawRoundRect(rect, rx, rx, borderPaint)
        }

        return bitmap
    }

    private fun blendWithWhite(color: Int, amount: Float): Int {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val nr = (r + (255 - r) * amount).toInt().coerceIn(0, 255)
        val ng = (g + (255 - g) * amount).toInt().coerceIn(0, 255)
        val nb = (b + (255 - b) * amount).toInt().coerceIn(0, 255)
        return (color and 0xFF000000.toInt()) or (nr shl 16) or (ng shl 8) or nb
    }
}
