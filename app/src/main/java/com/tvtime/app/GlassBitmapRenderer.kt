package com.tvtime.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader

/**
 * Renders a glassmorphism-style container (rounded, semi-transparent fill with an optional
 * border and a subtle top highlight for the "liquid" preset) into a [Bitmap] that can be
 * applied to both the home-screen widget and the in-app live preview.
 *
 * Note: a widget background is rendered into an offscreen [Bitmap] (a software canvas), so
 * framework blur APIs like [android.graphics.RenderEffect]/[android.graphics.RenderNode] are
 * unavailable here. We therefore apply a manual box blur, which softens the gradient sheen
 * to give a frosted-glass look on every Android version.
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
        gaussianBlurRadius: Int = 0,
        density: Float,
        gradient: Boolean = false,
        backdrop: Bitmap? = null
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

        if (backdrop != null) {
            // Real frosted glass: sample the wallpaper tiny (heavy downscale = natural blur),
            // center-cover it into the widget, then tint it with the chosen shade. Content
            // stays clipped to the rounded card so the glass has crisp corners.
            val path = Path().apply { addRoundRect(rect, rx, rx, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(path)
            val sample = sampleForGlass(backdrop, 110)
            if (sample != null) {
                drawCenterCover(canvas, sample, rect)
                canvas.drawRect(
                    rect,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.FILL
                        color = finalBg
                    }
                )
                // Subtle top sheen to sell the glass surface.
                if (gradient) {
                    canvas.drawRect(
                        RectF(rect.left, rect.top, rect.right, rect.top + rect.height() * 0.45f),
                        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x22FFFFFF }
                    )
                }
            }
            canvas.restore()
        } else {
            val useBlur = gaussianBlurRadius > 0

            // When blurring, use a soft vertical gradient so the blur is actually visible
            // (a flat fill would blur into itself). Otherwise a solid / gradient fill.
            val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                if (useBlur) {
                    shader = LinearGradient(
                        0f, 0f, 0f, heightPx.toFloat(),
                        blendWithWhite(finalBg, 0.25f), finalBg, Shader.TileMode.CLAMP
                    )
                } else {
                    color = if (gradient) blendWithWhite(finalBg, 0.14f) else finalBg
                }
            }
            canvas.drawRoundRect(rect, rx, rx, fillPaint)

            if (gradient) {
                val sheen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    shader = LinearGradient(
                        0f, 0f,
                        widthPx.toFloat(), heightPx.toFloat() * 0.65f,
                        0x22FFFFFF,
                        0x00FFFFFF,
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(rect, rx, rx, sheen)
            }

            if (useBlur) {
                applyBoxBlur(bitmap, gaussianBlurRadius)
            }
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

    /** Downscales [wallpaper] to at most [maxDim] on its longest side (~ intrinsic frost). */
    private fun sampleForGlass(wallpaper: Bitmap, maxDim: Int): Bitmap? {
        val bw = wallpaper.width
        val bh = wallpaper.height
        if (bw <= 0 || bh <= 0) return null
        val scale = maxDim.toFloat() / maxOf(bw, bh)
        if (scale >= 1f) return wallpaper
        val w = (bw * scale).toInt().coerceAtLeast(1)
        val h = (bh * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(wallpaper, w, h, true)
    }

    /** Draws [src] centered over [dst], scaling to cover (cropping as needed). */
    private fun drawCenterCover(canvas: Canvas, src: Bitmap, dst: RectF) {
        val sw = src.width.toFloat()
        val sh = src.height.toFloat()
        val scale = maxOf(dst.width() / sw, dst.height() / sh)
        val w = sw * scale
        val h = sh * scale
        val left = dst.centerX() - w / 2f
        val top = dst.centerY() - h / 2f
        canvas.drawBitmap(
            src,
            Rect(0, 0, src.width, src.height),
            RectF(left, top, left + w, top + h),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        )
    }

    /** In-place separable box blur — safe on software canvases (unlike RenderNode). */
    private fun applyBoxBlur(bitmap: Bitmap, radius: Int) {
        val r = radius.coerceIn(1, 25)
        val w = bitmap.width
        val h = bitmap.height
        if (w == 0 || h == 0) return

        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val temp = IntArray(w * h)
        val window = r * 2 + 1

        // Horizontal pass.
        for (y in 0 until h) {
            var a = 0
            var red = 0
            var green = 0
            var blue = 0
            for (k in -r..r) {
                val c = pixels[y * w + k.coerceIn(0, w - 1)]
                a += c shr 24 and 0xFF
                red += c shr 16 and 0xFF
                green += c shr 8 and 0xFF
                blue += c and 0xFF
            }
            for (x in 0 until w) {
                temp[y * w + x] = pack(a / window, red / window, green / window, blue / window)
                val cOut = pixels[y * w + (x - r).coerceIn(0, w - 1)]
                val cIn = pixels[y * w + (x + r + 1).coerceIn(0, w - 1)]
                a += (cIn shr 24 and 0xFF) - (cOut shr 24 and 0xFF)
                red += (cIn shr 16 and 0xFF) - (cOut shr 16 and 0xFF)
                green += (cIn shr 8 and 0xFF) - (cOut shr 8 and 0xFF)
                blue += (cIn and 0xFF) - (cOut and 0xFF)
            }
        }

        // Vertical pass.
        for (x in 0 until w) {
            var a = 0
            var red = 0
            var green = 0
            var blue = 0
            for (k in -r..r) {
                val c = temp[(k.coerceIn(0, h - 1)) * w + x]
                a += c shr 24 and 0xFF
                red += c shr 16 and 0xFF
                green += c shr 8 and 0xFF
                blue += c and 0xFF
            }
            for (y in 0 until h) {
                pixels[y * w + x] = pack(a / window, red / window, green / window, blue / window)
                val cOut = temp[(y - r).coerceIn(0, h - 1) * w + x]
                val cIn = temp[(y + r + 1).coerceIn(0, h - 1) * w + x]
                a += (cIn shr 24 and 0xFF) - (cOut shr 24 and 0xFF)
                red += (cIn shr 16 and 0xFF) - (cOut shr 16 and 0xFF)
                green += (cIn shr 8 and 0xFF) - (cOut shr 8 and 0xFF)
                blue += (cIn and 0xFF) - (cOut and 0xFF)
            }
        }

        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    }

    private fun pack(a: Int, r: Int, g: Int, b: Int): Int =
        (a.coerceIn(0, 255) shl 24) or
            (r.coerceIn(0, 255) shl 16) or
            (g.coerceIn(0, 255) shl 8) or
            b.coerceIn(0, 255)

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
