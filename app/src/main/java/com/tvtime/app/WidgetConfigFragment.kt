package com.tvtime.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.tvtime.app.R

class WidgetConfigFragment : Fragment() {

    private var appWidgetId: Int = 0

    private var selectedFontColor: Int = Color.parseColor("#FF007F")
    private var selectedAccentColor: Int = Color.parseColor("#00E5FF")
    private var selectedStrokeColor: Int = Color.parseColor("#008080")
    private var selectedBgColor: Int = Color.parseColor("#1B162E")
    
    private var strokeThicknessDp: Int = 2
    private var cornerRadiusDp: Int = 28
    private var bgAlphaPercent: Int = 80
    private var blurRadiusDp: Int = 15
    private var isBlurEnabled: Boolean = true

    private lateinit var previewCardContainer: FrameLayout
    private lateinit var previewTitle: TextView
    private lateinit var previewFontBox: View
    private lateinit var previewAccentBox: View
    private lateinit var previewStrokeBox: View
    private lateinit var previewBgBox: View

    private lateinit var tvStrokeThickness: TextView
    private lateinit var tvCornerRadius: TextView
    private lateinit var tvBgOpacity: TextView
    private lateinit var tvBlurRadius: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_widget_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activityIntent = requireActivity().intent
        val extras = activityIntent?.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                0
            )
        }

        previewCardContainer = view.findViewById(R.id.preview_card_container)
        previewTitle = view.findViewById(R.id.preview_title)
        previewFontBox = view.findViewById(R.id.preview_font_box)
        previewAccentBox = view.findViewById(R.id.preview_accent_box)
        previewStrokeBox = view.findViewById(R.id.preview_stroke_box)
        previewBgBox = view.findViewById(R.id.preview_bg_box)

        tvStrokeThickness = view.findViewById(R.id.tv_stroke_thickness)
        tvCornerRadius = view.findViewById(R.id.tv_corner_radius)
        tvBgOpacity = view.findViewById(R.id.tv_bg_opacity)
        tvBlurRadius = view.findViewById(R.id.tv_blur_radius)

        val switchDynamicColor = view.findViewById<SwitchCompat>(R.id.switch_dynamic_color)
        val switchEnableBlur = view.findViewById<SwitchCompat>(R.id.switch_enable_blur)
        val rgGlassPresets = view.findViewById<RadioGroup>(R.id.rg_glass_presets)

        val sbStrokeThickness = view.findViewById<SeekBar>(R.id.sb_stroke_thickness)
        val sbCornerRadius = view.findViewById<SeekBar>(R.id.sb_corner_radius)
        val sbBgOpacity = view.findViewById<SeekBar>(R.id.sb_bg_opacity)
        val sbBlurRadius = view.findViewById<SeekBar>(R.id.sb_blur_radius)

        val btnFontColor = view.findViewById<Button>(R.id.btn_font_color)
        val btnAccentColor = view.findViewById<Button>(R.id.btn_accent_color)
        val btnStrokeColor = view.findViewById<Button>(R.id.btn_stroke_color)
        val btnBgColor = view.findViewById<Button>(R.id.btn_bg_color)
        val saveButton = view.findViewById<Button>(R.id.btn_save_config)

        val prefs = requireContext().getSharedPreferences("tvtime_prefs", Context.MODE_PRIVATE)

        // Read all colors checking fallback aliases
        selectedFontColor = prefs.getInt("font_color", prefs.getInt("text_color", prefs.getInt("title_color", Color.parseColor("#FF007F"))))
        selectedAccentColor = prefs.getInt("accent_color", prefs.getInt("header_color", prefs.getInt("button_color", Color.parseColor("#00E5FF"))))
        selectedStrokeColor = prefs.getInt("stroke_color", prefs.getInt("border_color", Color.parseColor("#008080")))
        selectedBgColor = prefs.getInt("bg_color", prefs.getInt("background_color", Color.parseColor("#1B162E")))

        strokeThicknessDp = prefs.getInt("stroke_thickness", 2)
        cornerRadiusDp = prefs.getInt("corner_radius", 28)
        bgAlphaPercent = prefs.getInt("bg_opacity", 80)
        blurRadiusDp = prefs.getInt("blur_radius", 15)
        isBlurEnabled = prefs.getBoolean("enable_blur", true)

        sbStrokeThickness.progress = strokeThicknessDp
        sbCornerRadius.progress = cornerRadiusDp
        sbBgOpacity.progress = bgAlphaPercent
        sbBlurRadius.progress = blurRadiusDp
        switchEnableBlur.isChecked = isBlurEnabled

        updatePreview()

        btnFontColor.setOnClickListener {
            showHexColorPicker("Font / Text Color", selectedFontColor) { color ->
                selectedFontColor = color
                updatePreview()
            }
        }

        btnAccentColor.setOnClickListener {
            showHexColorPicker("Accent Color", selectedAccentColor) { color ->
                selectedAccentColor = color
                updatePreview()
            }
        }

        btnStrokeColor.setOnClickListener {
            showHexColorPicker("Stroke (Border) Color", selectedStrokeColor) { color ->
                selectedStrokeColor = color
                updatePreview()
            }
        }

        btnBgColor.setOnClickListener {
            showHexColorPicker("Background Color", selectedBgColor) { color ->
                selectedBgColor = color
                updatePreview()
            }
        }

        switchEnableBlur.setOnCheckedChangeListener { _, isChecked ->
            isBlurEnabled = isChecked
            sbBlurRadius.isEnabled = isChecked
            updatePreview()
        }

        sbBlurRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                blurRadiusDp = progress
                tvBlurRadius.text = "Blur Radius / Intensity: ${progress}dp"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sbStrokeThickness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                strokeThicknessDp = progress
                tvStrokeThickness.text = "Stroke Thickness: ${progress}dp"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sbCornerRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                cornerRadiusDp = progress
                tvCornerRadius.text = "Corner Radius: ${progress}dp"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        sbBgOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                bgAlphaPercent = progress
                tvBgOpacity.text = "Background Opacity / Alpha: $progress%"
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        rgGlassPresets.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_frosted_glass -> applyPreset(80, 20, Color.parseColor("#FF007F"), Color.parseColor("#00E5FF"), Color.parseColor("#1B162E"), 15, true)
                R.id.rb_dark_obsidian -> applyPreset(95, 30, Color.parseColor("#FFFFFF"), Color.parseColor("#111111"), Color.parseColor("#080808"), 5, true)
                R.id.rb_tinted_neon -> applyPreset(70, 28, Color.parseColor("#FF007F"), Color.parseColor("#00E5FF"), Color.parseColor("#200515"), 20, true)
                R.id.rb_liquid_blur -> applyPreset(65, 35, Color.parseColor("#00E5FF"), Color.parseColor("#FF007F"), Color.parseColor("#0A192F"), 25, true)
                R.id.rb_liquid_no_blur -> applyPreset(85, 28, Color.parseColor("#FF007F"), Color.parseColor("#008080"), Color.parseColor("#1B162E"), 0, false)
            }
            sbCornerRadius.progress = cornerRadiusDp
            sbBgOpacity.progress = bgAlphaPercent
            sbBlurRadius.progress = blurRadiusDp
            switchEnableBlur.isChecked = isBlurEnabled
            updatePreview()
        }

        saveButton.setOnClickListener {
            val context = requireContext()
            val preferences = context.getSharedPreferences("tvtime_prefs", Context.MODE_PRIVATE)

            // Save key aliases synchronously so all layout references find their exact color key
            preferences.edit().apply {
                // Font / Text Key Aliases
                putInt("font_color", selectedFontColor)
                putInt("text_color", selectedFontColor)
                putInt("title_color", selectedFontColor)
                putInt("primary_text_color", selectedFontColor)
                putInt("item_text_color", selectedFontColor)

                // Accent Key Aliases
                putInt("accent_color", selectedAccentColor)
                putInt("header_color", selectedAccentColor)
                putInt("button_color", selectedAccentColor)
                putInt("icon_color", selectedAccentColor)
                putInt("badge_color", selectedAccentColor)
                putInt("highlight_color", selectedAccentColor)

                // Stroke / Border Key Aliases
                putInt("stroke_color", selectedStrokeColor)
                putInt("border_color", selectedStrokeColor)
                putInt("outline_color", selectedStrokeColor)

                // Background Key Aliases
                putInt("bg_color", selectedBgColor)
                putInt("background_color", selectedBgColor)
                putInt("card_bg_color", selectedBgColor)

                // Layout parameters
                putInt("stroke_thickness", strokeThicknessDp)
                putInt("corner_radius", cornerRadiusDp)
                putInt("bg_opacity", bgAlphaPercent)
                putInt("blur_radius", blurRadiusDp)
                putBoolean("enable_blur", isBlurEnabled)
                putBoolean("use_dynamic_color", switchDynamicColor.isChecked)
            }.commit()

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TVTimeWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            // Force widget layout refresh
            for (id in allWidgetIds) {
                TVTimeWidgetProvider.updateAppWidget(context, appWidgetManager, id)
            }

            // Send broadast update to the launcher host
            val updateIntent = Intent(context, TVTimeWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds)
            }
            context.sendBroadcast(updateIntent)

            // Reload list views
            try {
                val listResId = resources.getIdentifier("widget_list_view", "id", context.packageName)
                if (listResId != 0) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(allWidgetIds, listResId)
                }
            } catch (_: Exception) {}

            val resultValue = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }

            val hostActivity = activity
            hostActivity?.setResult(Activity.RESULT_OK, resultValue)
            hostActivity?.finish()
        }
    }

    private fun applyPreset(
        opacity: Int, 
        radius: Int, 
        fontColor: Int,
        strokeColor: Int, 
        bgColor: Int, 
        blurRadius: Int, 
        blurEnabled: Boolean
    ) {
        bgAlphaPercent = opacity
        cornerRadiusDp = radius
        selectedFontColor = fontColor
        selectedStrokeColor = strokeColor
        selectedBgColor = bgColor
        blurRadiusDp = blurRadius
        isBlurEnabled = blurEnabled
    }

    private fun updatePreview() {
        previewTitle.setTextColor(selectedFontColor)
        previewFontBox.setBackgroundColor(selectedFontColor)
        previewAccentBox.setBackgroundColor(selectedAccentColor)
        previewStrokeBox.setBackgroundColor(selectedStrokeColor)
        previewBgBox.setBackgroundColor(selectedBgColor)

        tvStrokeThickness.text = "Stroke Thickness: ${strokeThicknessDp}dp"
        tvCornerRadius.text = "Corner Radius: ${cornerRadiusDp}dp"
        tvBgOpacity.text = "Background Opacity / Alpha: $bgAlphaPercent%"
        tvBlurRadius.text = if (isBlurEnabled) "Blur Radius / Intensity: ${blurRadiusDp}dp" else "Blur Disabled"

        val density = resources.displayMetrics.density
        val strokePx = (strokeThicknessDp * density).toInt()
        val cornerRadiusPx = cornerRadiusDp * density
        val alpha255 = ((bgAlphaPercent / 100f) * 255).toInt()

        val r = Color.red(selectedBgColor)
        val g = Color.green(selectedBgColor)
        val b = Color.blue(selectedBgColor)

        val cardDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.argb(alpha255, r, g, b))
            setCornerRadius(cornerRadiusPx)
            if (strokePx > 0) {
                setStroke(strokePx, selectedStrokeColor)
            }
        }

        previewCardContainer.background = cardDrawable
    }

    private fun showHexColorPicker(title: String, currentColor: Int, onColorSelected: (Int) -> Unit) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 10)
        }

        val hexInput = EditText(requireContext()).apply {
            hint = "#FF007F"
            setText(String.format("#%06X", (0xFFFFFF and currentColor)))
            setPadding(20, 20, 20, 20)
        }

        layout.addView(hexInput)

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(layout)
            .setPositiveButton("Set Hex") { _, _ ->
                try {
                    var hexString = hexInput.text.toString().trim()
                    if (!hexString.startsWith("#")) {
                        hexString = "#$hexString"
                    }
                    val color = Color.parseColor(hexString)
                    onColorSelected(color)
                } catch (e: Exception) {
                    // Invalid Hex Code, fallback silently
                }
            }
            .setNeutralButton("Presets") { _, _ ->
                showPresetPalette(title, onColorSelected)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPresetPalette(title: String, onColorSelected: (Int) -> Unit) {
        val colorNames = arrayOf(
            "Hot Pink", "Cyan", "Neon Green", "Gold / Yellow", 
            "Orange", "Purple", "Dark Purple", "Dark Obsidian", "White", "Black"
        )
        val colors = arrayOf(
            "#FF007F", "#00E5FF", "#39FF14", "#FFD700", 
            "#FF5722", "#9C27B0", "#1B162E", "#111111", "#FFFFFF", "#000000"
        )

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setItems(colorNames) { _, which ->
                val color = Color.parseColor(colors[which])
                onColorSelected(color)
            }
            .show()
    }
}
