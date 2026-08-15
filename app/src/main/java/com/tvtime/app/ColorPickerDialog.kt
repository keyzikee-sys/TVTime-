package com.tvtime.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

/**
 * A functional color picker: Hue / Saturation / Value / Alpha sliders with a live preview.
 * Returns the chosen color as an `#AARRGGBB` hex string via [onColorSelected].
 */
fun showColorPickerDialog(
    context: Context,
    title: String,
    initialHex: String,
    onColorSelected: (String) -> Unit
): AlertDialog {
    val initial = ColorUtils.parseArgb(initialHex) ?: Color.BLACK
    val hsv = FloatArray(3)
    Color.colorToHSV(initial, hsv)
    val alpha = (initial shr 24) and 0xFF
    var current = initial

    val dp = context.resources.displayMetrics.density.toInt()
    val layout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(24 * dp, 16 * dp, 24 * dp, 8 * dp)
    }

    val preview = TextView(context).apply {
        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            56 * dp
        ).apply { bottomMargin = 12 * dp }
        text = "Preview"
        gravity = android.view.Gravity.CENTER
        setTextColor(Color.WHITE)
        background = ColorDrawable(current)
    }
    layout.addView(preview)

    fun updatePreview() {
        preview.background = ColorDrawable(current)
    }

    val hueSb = SeekBar(context)
    val satSb = SeekBar(context)
    val valSb = SeekBar(context)
    val alphaSb = SeekBar(context)
    hueSb.max = 360
    satSb.max = 100
    valSb.max = 100
    alphaSb.max = 100
    hueSb.progress = hsv[0].toInt()
    satSb.progress = (hsv[1] * 100).toInt()
    valSb.progress = (hsv[2] * 100).toInt()
    alphaSb.progress = alpha

    fun label(text: String): TextView = TextView(context).apply {
        this.text = text
        setTextColor(Color.WHITE)
        textSize = 13f
        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 8 * dp }
    }

    layout.addView(label("Hue"))
    layout.addView(hueSb)
    layout.addView(label("Saturation"))
    layout.addView(satSb)
    layout.addView(label("Value"))
    layout.addView(valSb)
    layout.addView(label("Alpha"))
    layout.addView(alphaSb)

    fun recompute() {
        val h = hueSb.progress.toFloat()
        val s = satSb.progress / 100f
        val v = valSb.progress / 100f
        val a = alphaSb.progress
        current = Color.HSVToColor(a, floatArrayOf(h, s, v))
        updatePreview()
    }
    hueSb.setOnSeekBarChangeListener(simpleChanged { _, _ -> recompute() })
    satSb.setOnSeekBarChangeListener(simpleChanged { _, _ -> recompute() })
    valSb.setOnSeekBarChangeListener(simpleChanged { _, _ -> recompute() })
    alphaSb.setOnSeekBarChangeListener(simpleChanged { _, _ -> recompute() })

    return AlertDialog.Builder(context)
        .setTitle(title)
        .setView(layout)
        .setPositiveButton("OK") { _, _ -> onColorSelected(ColorUtils.toArgbHex(current)) }
        .setNegativeButton("Cancel", null)
        .create()
}

private fun simpleChanged(onChanged: (progress: Int, fromUser: Boolean) -> Unit) =
    object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar?, progress: Int, user: Boolean) =
            onChanged(progress, user)
        override fun onStartTrackingTouch(sb: SeekBar?) {}
        override fun onStopTrackingTouch(sb: SeekBar?) {}
    }
