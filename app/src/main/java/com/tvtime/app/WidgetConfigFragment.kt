package com.tvtime.app

import android.app.Activity
import android.text.Editable
import android.text.TextWatcher
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

class WidgetConfigFragment : Fragment() {

    private var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    // ---------------------------------------------------------
    // Current visual settings
    // ---------------------------------------------------------

    private var selectedFontColor =
        Color.parseColor("#FF00A6")

    private var selectedAccentColor =
        Color.parseColor("#FFFF13")

    private var selectedStrokeColor =
        Color.parseColor("#008080")

    private var selectedBgColor =
        Color.parseColor("#1B162E")

    private var strokeThicknessDp = 2
    private var cornerRadiusDp = 28
    private var bgAlphaPercent = 80
    private var blurRadiusDp = 15
    private var isBlurEnabled = true
    private var useDynamicColor = false

    private var contentSource =
        WidgetPreferences.CONTENT_MY_STUFF

    private lateinit var widgetTitleInput: EditText

    // ---------------------------------------------------------
    // Preview views
    // ---------------------------------------------------------

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

    // Optional source controls.
    // These are looked up dynamically so the fragment still compiles
    // and works even if the current XML does not contain them.
    private var contentSourceButton: Button? = null
    private var contentSourceLabel: TextView? = null

    // ---------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.fragment_widget_config,
            container,
            false
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        readWidgetId()

        bindViews(view)

        loadSettings()

        widgetTitleInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                previewTitle.text = s?.toString() ?: ""
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        setupControls(view)

        updateContentSourceLabel()

        updatePreview()
    }

    // ---------------------------------------------------------
    // Widget ID
    // ---------------------------------------------------------

    private fun readWidgetId() {
        val id = requireActivity()
            .intent
            .getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )

        appWidgetId = id
    }

    // ---------------------------------------------------------
    // View binding
    // ---------------------------------------------------------

    private fun bindViews(view: View) {

        previewCardContainer =
            view.findViewById(R.id.preview_card_container)

        previewTitle =
            view.findViewById(R.id.preview_title)

        widgetTitleInput =
            view.findViewById(R.id.widget_title_input)

        previewFontBox =
            view.findViewById(R.id.preview_font_box)

        previewAccentBox =
            view.findViewById(R.id.preview_accent_box)

        previewStrokeBox =
            view.findViewById(R.id.preview_stroke_box)

        previewBgBox =
            view.findViewById(R.id.preview_bg_box)

        tvStrokeThickness =
            view.findViewById(R.id.tv_stroke_thickness)

        tvCornerRadius =
            view.findViewById(R.id.tv_corner_radius)

        tvBgOpacity =
            view.findViewById(R.id.tv_bg_opacity)

        tvBlurRadius =
            view.findViewById(R.id.tv_blur_radius)

        /*
         * Optional content-source controls.
         *
         * getIdentifier() prevents the configuration screen from
         * crashing if these IDs aren't present in the current XML.
         */
        contentSourceButton =
            findOptionalButton(
                view,
                "btn_content_source"
            )

        contentSourceLabel =
            findOptionalTextView(
                view,
                "tv_content_source"
            )
    }

    private fun findOptionalButton(
        root: View,
        name: String
    ): Button? {

        val id = resources.getIdentifier(
            name,
            "id",
            requireContext().packageName
        )

        if (id == 0) {
            return null
        }

        return root.findViewById(id)
    }

    private fun findOptionalTextView(
        root: View,
        name: String
    ): TextView? {

        val id = resources.getIdentifier(
            name,
            "id",
            requireContext().packageName
        )

        if (id == 0) {
            return null
        }

        return root.findViewById(id)
    }

    // ---------------------------------------------------------
    // Load saved settings
    // ---------------------------------------------------------

    private fun loadSettings() {

        val context = requireContext()

        val prefs =
            context.getSharedPreferences(
                "tvtime_prefs",
                Context.MODE_PRIVATE
            )

        selectedFontColor =
            getSavedColor(
                prefs,
                "#FF00A6",
                "font_color",
                "text_color",
                "title_color",
                "primary_text_color",
                "item_text_color"
            )

        selectedAccentColor =
            getSavedColor(
                prefs,
                "#FFFF13",
                "accent_color",
                "header_color",
                "button_color",
                "icon_color",
                "badge_color",
                "highlight_color"
            )

        selectedStrokeColor =
            getSavedColor(
                prefs,
                "#008080",
                "stroke_color",
                "border_color",
                "outline_color"
            )

        selectedBgColor =
            getSavedColor(
                prefs,
                "#1B162E",
                "bg_color",
                "background_color",
                "card_bg_color"
            )

        strokeThicknessDp =
            prefs.getInt(
                "stroke_thickness",
                2
            ).coerceIn(0, 10)

        cornerRadiusDp =
            prefs.getInt(
                "corner_radius",
                28
            ).coerceIn(0, 50)

        bgAlphaPercent =
            prefs.getInt(
                "bg_opacity",
                80
            ).coerceIn(0, 100)

        blurRadiusDp =
            prefs.getInt(
                "blur_radius",
                15
            ).coerceIn(0, 30)

        isBlurEnabled =
            prefs.getBoolean(
                "enable_blur",
                true
            )

        useDynamicColor =
            prefs.getBoolean(
                "use_dynamic_color",
                false
            )

        contentSource =
            prefs.getString(
                "content_source",
                WidgetPreferences.CONTENT_MY_STUFF
            ) ?: WidgetPreferences.CONTENT_MY_STUFF

        val widgetPrefs =
            WidgetPreferences(
                context,
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    appWidgetId
                } else {
                    WidgetPreferences.DEFAULT_ID
                }
            )

        widgetTitleInput.setText(widgetPrefs.widgetTitle)
    }

    // ---------------------------------------------------------
    // Saved color helper
    // ---------------------------------------------------------

    private fun getSavedColor(
        prefs: android.content.SharedPreferences,
        defaultHex: String,
        vararg keys: String
    ): Int {

        for (key in keys) {

            if (!prefs.contains(key)) {
                continue
            }

            try {
                return prefs.getInt(
                    key,
                    Color.parseColor(defaultHex)
                )
            } catch (_: Exception) {
            }

            try {
                val stringValue =
                    prefs.getString(
                        key,
                        null
                    )

                if (!stringValue.isNullOrBlank()) {
                    return Color.parseColor(
                        stringValue
                    )
                }
            } catch (_: Exception) {
            }
        }

        return Color.parseColor(defaultHex)
    }

    // ---------------------------------------------------------
    // Controls
    // ---------------------------------------------------------

    private fun setupControls(view: View) {

        val switchDynamicColor =
            view.findViewById<SwitchCompat>(
                R.id.switch_dynamic_color
            )

        val switchEnableBlur =
            view.findViewById<SwitchCompat>(
                R.id.switch_enable_blur
            )

        val rgGlassPresets =
            view.findViewById<RadioGroup>(
                R.id.rg_glass_presets
            )

        val sbStrokeThickness =
            view.findViewById<SeekBar>(
                R.id.sb_stroke_thickness
            )

        val sbCornerRadius =
            view.findViewById<SeekBar>(
                R.id.sb_corner_radius
            )

        val sbBgOpacity =
            view.findViewById<SeekBar>(
                R.id.sb_bg_opacity
            )

        val sbBlurRadius =
            view.findViewById<SeekBar>(
                R.id.sb_blur_radius
            )

        val btnFontColor =
            view.findViewById<Button>(
                R.id.btn_font_color
            )

        val btnAccentColor =
            view.findViewById<Button>(
                R.id.btn_accent_color
            )

        val btnStrokeColor =
            view.findViewById<Button>(
                R.id.btn_stroke_color
            )

        val btnBgColor =
            view.findViewById<Button>(
                R.id.btn_bg_color
            )

        val saveButton =
            view.findViewById<Button>(
                R.id.btn_save_config
            )

        // Initial values

        switchDynamicColor.isChecked =
            useDynamicColor

        switchEnableBlur.isChecked =
            isBlurEnabled

        sbStrokeThickness.progress =
            strokeThicknessDp

        sbCornerRadius.progress =
            cornerRadiusDp

        sbBgOpacity.progress =
            bgAlphaPercent

        sbBlurRadius.progress =
            blurRadiusDp

        sbBlurRadius.isEnabled =
            isBlurEnabled

        // -----------------------------------------------------
        // Dynamic color
        // -----------------------------------------------------

        switchDynamicColor.setOnCheckedChangeListener {
                _,
                checked ->

            useDynamicColor =
                checked

            updatePreview()
        }

        // -----------------------------------------------------
        // Blur switch
        // -----------------------------------------------------

        switchEnableBlur.setOnCheckedChangeListener {
                _,
                checked ->

            isBlurEnabled =
                checked

            sbBlurRadius.isEnabled =
                checked

            updatePreview()
        }

        // -----------------------------------------------------
        // Blur radius
        // -----------------------------------------------------

        sbBlurRadius.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {

                    blurRadiusDp =
                        progress

                    tvBlurRadius.text =
                        if (isBlurEnabled) {
                            "Blur Radius / Intensity: ${progress}dp"
                        } else {
                            "Blur Disabled"
                        }

                    updatePreview()
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }
            }
        )

        // -----------------------------------------------------
        // Stroke thickness
        // -----------------------------------------------------

        sbStrokeThickness.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {

                    strokeThicknessDp =
                        progress

                    tvStrokeThickness.text =
                        "Stroke Thickness: ${progress}dp"

                    updatePreview()
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }
            }
        )

        // -----------------------------------------------------
        // Corner radius
        // -----------------------------------------------------

        sbCornerRadius.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {

                    cornerRadiusDp =
                        progress

                    tvCornerRadius.text =
                        "Corner Radius: ${progress}dp"

                    updatePreview()
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }
            }
        )

        // -----------------------------------------------------
        // Background opacity
        // -----------------------------------------------------

        sbBgOpacity.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {

                    bgAlphaPercent =
                        progress

                    tvBgOpacity.text =
                        "Background Opacity / Alpha: $progress%"

                    updatePreview()
                }

                override fun onStartTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }

                override fun onStopTrackingTouch(
                    seekBar: SeekBar?
                ) {
                }
            }
        )

        // -----------------------------------------------------
        // Font color
        // -----------------------------------------------------

        btnFontColor.setOnClickListener {

            showHexColorPicker(
                "Font / Text Color",
                selectedFontColor
            ) { color ->

                selectedFontColor =
                    color

                updatePreview()
            }
        }

        // -----------------------------------------------------
        // Accent color
        // -----------------------------------------------------

        btnAccentColor.setOnClickListener {

            showHexColorPicker(
                "Accent Color",
                selectedAccentColor
            ) { color ->

                selectedAccentColor =
                    color

                updatePreview()
            }
        }

        // -----------------------------------------------------
        // Stroke color
        // -----------------------------------------------------

        btnStrokeColor.setOnClickListener {

            showHexColorPicker(
                "Stroke / Border Color",
                selectedStrokeColor
            ) { color ->

                selectedStrokeColor =
                    color

                updatePreview()
            }
        }

        // -----------------------------------------------------
        // Background color
        // -----------------------------------------------------

        btnBgColor.setOnClickListener {

            showHexColorPicker(
                "Background Color",
                selectedBgColor
            ) { color ->

                selectedBgColor =
                    color

                updatePreview()
            }
        }

        // -----------------------------------------------------
        // Glass presets
        // -----------------------------------------------------

        rgGlassPresets.setOnCheckedChangeListener {
                _,
                checkedId ->

            when (checkedId) {

                R.id.rb_frosted_glass -> {

                    applyPreset(
                        opacity = 80,
                        radius = 20,
                        fontColor = Color.WHITE,
                        accentColor =
                            Color.parseColor("#FF00A6"),
                        strokeColor =
                            Color.parseColor("#FFFF13"),
                        bgColor =
                            Color.parseColor("#1B162E"),
                        blurRadius = 15,
                        blurEnabled = true
                    )
                }

                R.id.rb_dark_obsidian -> {

                    applyPreset(
                        opacity = 95,
                        radius = 30,
                        fontColor = Color.WHITE,
                        accentColor = Color.WHITE,
                        strokeColor = Color.WHITE,
                        bgColor =
                            Color.parseColor("#080808"),
                        blurRadius = 5,
                        blurEnabled = true
                    )
                }

                R.id.rb_tinted_neon -> {

                    applyPreset(
                        opacity = 70,
                        radius = 28,
                        fontColor = Color.WHITE,
                        accentColor =
                            Color.parseColor("#FF00A6"),
                        strokeColor =
                            Color.parseColor("#FFFF13"),
                        bgColor =
                            Color.parseColor("#200515"),
                        blurRadius = 20,
                        blurEnabled = true
                    )
                }

                R.id.rb_liquid_blur -> {

                    applyPreset(
                        opacity = 18,
                        radius = 28,
                        fontColor = Color.WHITE,
                        accentColor =
                            Color.parseColor("#FF4FA3"),
                        strokeColor =
                            Color.parseColor("#44FFFFFF"),
                        bgColor =
                            Color.parseColor("#FF4FA3"),
                        blurRadius = 6,
                        blurEnabled = true
                    )
                }

                R.id.rb_liquid_no_blur -> {

                    applyPreset(
                        opacity = 85,
                        radius = 28,
                        fontColor = Color.WHITE,
                        accentColor =
                            Color.parseColor("#FF00A6"),
                        strokeColor =
                            Color.parseColor("#008080"),
                        bgColor =
                            Color.parseColor("#1B162E"),
                        blurRadius = 0,
                        blurEnabled = false
                    )
                }
            }

            sbStrokeThickness.progress =
                strokeThicknessDp

            sbCornerRadius.progress =
                cornerRadiusDp

            sbBgOpacity.progress =
                bgAlphaPercent

            sbBlurRadius.progress =
                blurRadiusDp

            switchEnableBlur.isChecked =
                isBlurEnabled

            updatePreview()
        }

        // -----------------------------------------------------
        // Optional content source button
        // -----------------------------------------------------

        contentSourceButton?.setOnClickListener {
            showContentSourcePicker()
        }

        // -----------------------------------------------------
        // Save
        // -----------------------------------------------------

        saveButton.setOnClickListener {
            saveConfiguration()
        }
    }

    // ---------------------------------------------------------
    // Content source
    // ---------------------------------------------------------

    private fun getContentSourceLabel(): String {

        return when (contentSource) {

            WidgetPreferences.CONTENT_TERROR_ON_TUBI ->
                "Terror on Tubi"

            else ->
                "My Stuff"
        }
    }

    private fun updateContentSourceLabel() {

        val label =
            getContentSourceLabel()

        contentSourceLabel?.text =
            "Content Source: $label"

        contentSourceButton?.text =
            "CONTENT SOURCE: $label"
    }

    private fun showContentSourcePicker() {

        val names = arrayOf(
            "My Stuff",
            "Terror on Tubi"
        )

        val current =
            if (
                contentSource ==

                WidgetPreferences.CONTENT_TERROR_ON_TUBI
            ) {
                1
            } else {
                0
            }

        AlertDialog.Builder(
            requireContext()
        )
            .setTitle("Widget Content Source")
            .setSingleChoiceItems(
                names,
                current
            ) { dialog, which ->

                contentSource =

                    if (which == 1) {
                        WidgetPreferences.CONTENT_TERROR_ON_TUBI
                    } else {
                        WidgetPreferences.CONTENT_MY_STUFF


                    }
                updateContentSourceLabel()

                dialog.dismiss()
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    // ---------------------------------------------------------
    // Apply preset
    // ---------------------------------------------------------

    private fun applyPreset(
        opacity: Int,
        radius: Int,
        fontColor: Int,
        accentColor: Int,
        strokeColor: Int,
        bgColor: Int,
        blurRadius: Int,
        blurEnabled: Boolean
    ) {

        bgAlphaPercent =
            opacity.coerceIn(0, 100)

        cornerRadiusDp =
            radius.coerceIn(0, 50)

        selectedFontColor =
            fontColor

        selectedAccentColor =
            accentColor

        selectedStrokeColor =
            strokeColor

        selectedBgColor =
            bgColor

        blurRadiusDp =
            blurRadius.coerceIn(0, 30)

        isBlurEnabled =
            blurEnabled
    }

    // ---------------------------------------------------------
    // Live preview
    // ---------------------------------------------------------

    private fun updatePreview() {

        if (!::previewTitle.isInitialized) {
            return
        }

        previewTitle.setTextColor(
            selectedFontColor
        )

        previewFontBox.setBackgroundColor(
            selectedFontColor
        )

        previewAccentBox.setBackgroundColor(
            selectedAccentColor
        )

        previewStrokeBox.setBackgroundColor(
            selectedStrokeColor
        )

        previewBgBox.setBackgroundColor(
            selectedBgColor
        )

        tvStrokeThickness.text =
            "Stroke Thickness: ${strokeThicknessDp}dp"

        tvCornerRadius.text =
            "Corner Radius: ${cornerRadiusDp}dp"

        tvBgOpacity.text =
            "Background Opacity / Alpha: $bgAlphaPercent%"

        tvBlurRadius.text =
            if (isBlurEnabled) {
                "Blur Radius / Intensity: ${blurRadiusDp}dp"
            } else {
                "Blur Disabled"
            }

        val density =
            resources.displayMetrics.density

        val strokePx =
            (strokeThicknessDp * density)
                .toInt()

        val cornerRadiusPx =
            cornerRadiusDp * density

        val alpha255 =
            (
                bgAlphaPercent / 100f * 255f
            )
                .toInt()
                .coerceIn(0, 255)

        val r =
            Color.red(selectedBgColor)

        val g =
            Color.green(selectedBgColor)

        val b =
            Color.blue(selectedBgColor)

        val cardDrawable =
            GradientDrawable().apply {

                shape =
                    GradientDrawable.RECTANGLE

                setColor(
                    Color.argb(
                        alpha255,
                        r,
                        g,
                        b
                    )
                )

                setCornerRadius(
                    cornerRadiusPx
                )

                if (strokePx > 0) {

                    setStroke(
                        strokePx,
                        selectedStrokeColor
                    )
                }
            }

        previewCardContainer.background =
            cardDrawable
    }

    // ---------------------------------------------------------
    // Hex color picker
    // ---------------------------------------------------------

    private fun showHexColorPicker(
        title: String,
        currentColor: Int,
        onColorSelected: (Int) -> Unit
    ) {

        val layout =
            LinearLayout(
                requireContext()
            ).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    40,
                    20,
                    40,
                    10
                )
            }

        val hexInput =
            EditText(
                requireContext()
            ).apply {

                hint =
                    "#FF00A6"

                setText(
                    String.format(
                        "#%06X",
                        0xFFFFFF and currentColor
                    )
                )

                setPadding(
                    20,
                    20,
                    20,
                    20
                )
            }

        layout.addView(
            hexInput
        )

        AlertDialog.Builder(
            requireContext()
        )
            .setTitle(title)
            .setView(layout)
            .setPositiveButton(
                "Set Hex"
            ) { _, _ ->

                try {

                    var hexString =
                        hexInput.text
                            .toString()
                            .trim()

                    if (
                        !hexString.startsWith("#")
                    ) {
                        hexString =
                            "#$hexString"
                    }

                    val color =
                        Color.parseColor(
                            hexString
                        )

                    onColorSelected(
                        color
                    )

                } catch (_: Exception) {

                    // Invalid hex.
                    // Leave the existing color unchanged.
                }
            }
            .setNeutralButton(
                "Presets"
            ) { _, _ ->

                showPresetPalette(
                    title,
                    onColorSelected
                )
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    // ---------------------------------------------------------
    // Preset color palette
    // ---------------------------------------------------------

    private fun showPresetPalette(
        title: String,
        onColorSelected: (Int) -> Unit
    ) {

        val colorNames =
                arrayOf(
                    "Hot Pink",
                    "Tubi Pink",
                    "Tubi Broom",
                    "Cyan",
                    "Neon Green",
                    "Gold / Yellow",
                    "Orange",
                    "Purple",
                    "Dark Purple",
                    "Black Russian",
                    "Dark Obsidian",
                    "White",
                    "Black"
                )

        val colors =
                arrayOf(
                    "#FF00A6",
                    "#FF00A6",
                    "#FFFF13",
                    "#00E5FF",
                    "#39FF14",
                    "#FFD700",
                    "#FF5722",
                    "#9C27B0",
                    "#1B162E",
                    "#0B0019",
                    "#111111",
                    "#FFFFFF",
                    "#000000"
                )

        AlertDialog.Builder(
            requireContext()
        )
            .setTitle(title)
            .setItems(
                colorNames
            ) { _, which ->

                val color =
                    Color.parseColor(
                        colors[which]
                    )

                onColorSelected(
                    color
                )
            }
            .show()
    }

    // ---------------------------------------------------------
    // Save everything
    // ---------------------------------------------------------

    private fun saveConfiguration() {

        val context =
            requireContext()

        val preferences =
            context.getSharedPreferences(
                "tvtime_prefs",
                Context.MODE_PRIVATE
            )

        /*
         * Save the broad compatibility aliases used by
         * different parts of the TVTime project.
         */
        preferences.edit().apply {

            // Font / text
            putInt(
                "font_color",
                selectedFontColor
            )

            putInt(
                "text_color",
                selectedFontColor
            )

            putInt(
                "title_color",
                selectedFontColor
            )

            putInt(
                "primary_text_color",
                selectedFontColor
            )

            putInt(
                "item_text_color",
                selectedFontColor
            )

            // Accent
            putInt(
                "accent_color",
                selectedAccentColor
            )

            putInt(
                "header_color",
                selectedAccentColor
            )

            putInt(
                "button_color",
                selectedAccentColor
            )

            putInt(
                "icon_color",
                selectedAccentColor
            )

            putInt(
                "badge_color",
                selectedAccentColor
            )

            putInt(
                "highlight_color",
                selectedAccentColor
            )

            // Stroke
            putInt(
                "stroke_color",
                selectedStrokeColor
            )

            putInt(
                "border_color",
                selectedStrokeColor
            )

            putInt(
                "outline_color",
                selectedStrokeColor
            )

            // Background
            putInt(
                "bg_color",
                selectedBgColor
            )

            putInt(
                "background_color",
                selectedBgColor
            )

            putInt(
                "card_bg_color",
                selectedBgColor
            )

            // Layout
            putInt(
                "stroke_thickness",
                strokeThicknessDp
            )

            putInt(
                "corner_radius",
                cornerRadiusDp
            )

            putInt(
                "bg_opacity",
                bgAlphaPercent
            )

            putInt(
                "blur_radius",
                blurRadiusDp
            )

            putBoolean(
                "enable_blur",
                isBlurEnabled
            )

            putBoolean(
                "use_dynamic_color",
                useDynamicColor
            )

            // Content source
            putString(
                "content_source",
                contentSource
            )

        }.commit()

        // -----------------------------------------------------
        // Save to WidgetPreferences
        // -----------------------------------------------------

        val widgetPrefs =
            WidgetPreferences(
                context,
                if (
                    appWidgetId !=
                    AppWidgetManager.INVALID_APPWIDGET_ID
                ) {
                    appWidgetId
                } else {
                    WidgetPreferences.DEFAULT_ID
                }
            )

        widgetPrefs.bgBlurOpacity =
            bgAlphaPercent

        widgetPrefs.gaussianBlurRadius =
            if (isBlurEnabled) {
                blurRadiusDp
            } else {
                0
            }

        widgetPrefs.layerBlurSoftening =
            blurRadiusDp

        widgetPrefs.borderThickness =
            strokeThicknessDp

        widgetPrefs.cornerRadius =
            cornerRadiusDp

        widgetPrefs.bgHexColor =
            colorToHex(
                selectedBgColor
            )

        widgetPrefs.accentHexColor =
            colorToHex(
                selectedAccentColor
            )

        widgetPrefs.borderHexColor =
            colorToHex(
                selectedStrokeColor
            )

        widgetPrefs.contentSource =
            contentSource
        widgetPrefs.useDynamicColor = useDynamicColor

        widgetPrefs.widgetTitle =
            widgetTitleInput.text.toString().trim().ifEmpty { "My Stuff" }

        widgetPrefs.glassPreset =
            if (isBlurEnabled) {
                GlassBitmapRenderer.PRESET_LIQUID
            } else {
                GlassBitmapRenderer.PRESET_LIQUID_NOBLUR
            }

        // -----------------------------------------------------
        // Refresh every TVTime widget
        // -----------------------------------------------------

        val manager =
            AppWidgetManager.getInstance(
                context
            )

        val component =
            ComponentName(
                context,
                TVTimeWidgetProvider::class.java
            )

        val allWidgetIds =
            manager.getAppWidgetIds(
                component
            )

        for (id in allWidgetIds) {

            TVTimeWidgetProvider.updateAppWidget(
                context,
                manager,
                id
            )
        }

        // -----------------------------------------------------
        // Tell launcher to refresh
        // -----------------------------------------------------

        if (allWidgetIds.isNotEmpty()) {

            val updateIntent =
                Intent(
                    context,
                    TVTimeWidgetProvider::class.java
                ).apply {

                    action =
                        AppWidgetManager.ACTION_APPWIDGET_UPDATE

                    putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_IDS,
                        allWidgetIds
                    )
                }

            context.sendBroadcast(
                updateIntent
            )

            /*
             * Refresh the widget's RemoteViews list.
             */
            try {

                val listId =
                    resources.getIdentifier(
                        "list_mystuff",
                        "id",
                        context.packageName
                    )

                if (listId != 0) {

                    manager.notifyAppWidgetViewDataChanged(
                        allWidgetIds,
                        listId
                    )
                }

            } catch (_: Exception) {
            }
        }

        // -----------------------------------------------------
        // Return success to widget host
        // -----------------------------------------------------

        val resultIntent =
            Intent().apply {

                putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    appWidgetId
                )
            }

        requireActivity().setResult(
            Activity.RESULT_OK,
            resultIntent
        )

        requireActivity().finish()
    }

    // ---------------------------------------------------------
    // Color conversion
    // ---------------------------------------------------------

    private fun colorToHex(
        color: Int
    ): String {

        return String.format(
            "#%08X",
            color
        )
    }
}