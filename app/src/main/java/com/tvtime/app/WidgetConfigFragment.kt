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

class WidgetConfigFragment : Fragment() {

    private var appWidgetId: Int = 0

    private var selectedFontColor = Color.parseColor("#FFFFFFFF")
    private var selectedAccentColor = Color.parseColor("#00E5FF")
    private var selectedStrokeColor = Color.parseColor("#008080")
    private var selectedBgColor = Color.parseColor("#1B162E")

    private var strokeThicknessDp = 2
    private var cornerRadiusDp = 28
    private var bgAlphaPercent = 80
    private var blurRadiusDp = 15
    private var isBlurEnabled = true

    private var contentSource = WidgetPreferences.CONTENT_MY_STUFF

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

    private var contentSourceButton: Button? = null
    private var contentSourceLabel: TextView? = null

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

        appWidgetId = requireActivity()
            .intent
            .getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                WidgetPreferences.DEFAULT_ID
            )

        previewCardContainer =
            view.findViewById(R.id.preview_card_container)

        previewTitle =
            view.findViewById(R.id.preview_title)

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
         * These two are optional.
         *
         * That means the configuration screen will still work
         * even if the current XML does not contain the content-source
         * controls yet.
         */
        contentSourceButton =
            findOptionalButton(view, "btn_content_source")

        contentSourceLabel =
            findOptionalTextView(view, "tv_content_source")

        loadSettings(view)
        setupControls(view)
        updateContentSourceLabel()
        updatePreview()
    }

    private fun findOptionalButton(
        root: View,
        idName: String
    ): Button? {

        val id = resources.getIdentifier(
            idName,
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
        idName: String
    ): TextView? {

        val id = resources.getIdentifier(
            idName,
            "id",
            requireContext().packageName
        )

        if (id == 0) {
            return null
        }

        return root.findViewById(id)
    }

    private fun loadSettings(view: View) {

        val context = requireContext()

        val widgetPrefs =
            WidgetPreferences(
                context,
                appWidgetId
            )

        val legacyPrefs =
            context.getSharedPreferences(
                "tvtime_prefs",
                Context.MODE_PRIVATE
            )

        selectedFontColor =
            getSavedColor(
                legacyPrefs,
                "font_color",
                "text_color",
                "title_color",
                "#FFFFFFFF"
            )

        selectedAccentColor =
            getSavedColor(
                legacyPrefs,
                "accent_color",
                "header_color",
                "button_color",
                "#00E5FF"
            )

        selectedStrokeColor =
            getSavedColor(
                legacyPrefs,
                "stroke_color",
                "border_color",
                "#008080"
            )

        selectedBgColor =
            getSavedColor(
                legacyPrefs,
                "bg_color",
                "background_color",
                "#1B162E"
            )

        strokeThicknessDp =
            legacyPrefs.getInt(
                "stroke_thickness",
                widgetPrefs.borderThickness
            )

        cornerRadiusDp =
            legacyPrefs.getInt(
                "corner_radius",
                widgetPrefs.cornerRadius
            )

        bgAlphaPercent =
            legacyPrefs.getInt(
                "bg_opacity",
                widgetPrefs.bgBlurOpacity
            )

        blurRadiusDp =
            legacyPrefs.getInt(
                "blur_radius",
                widgetPrefs.gaussianBlurRadius
            )

        isBlurEnabled =
            legacyPrefs.getBoolean(
                "enable_blur",
                true
            )

        contentSource =
            widgetPrefs.contentSource
    }

    private fun setupControls(view: View) {

        val switchDynamicColor =
            view.findViewById<SwitchCompat>(
                R.id.switch_dynamic_color
            )

        val switchEnableBlur =
            view.findViewById<SwitchCompat>(
                R.id.switch_enable_blur
            )

        val radioGroup =
            view.findViewById<RadioGroup>(
                R.id.rg_glass_presets
            )

        val sbStroke =
            view.findViewById<SeekBar>(
                R.id.sb_stroke_thickness
            )

        val sbCorner =
            view.findViewById<SeekBar>(
                R.id.sb_corner_radius
            )

        val sbOpacity =
            view.findViewById<SeekBar>(
                R.id.sb_bg_opacity
            )

        val sbBlur =
            view.findViewById<SeekBar>(
                R.id.sb_blur_radius
            )

        val btnFont =
            view.findViewById<Button>(
                R.id.btn_font_color
            )

        val btnAccent =
            view.findViewById<Button>(
                R.id.btn_accent_color
            )

        val btnStroke =
            view.findViewById<Button>(
                R.id.btn_stroke_color
            )

        val btnBackground =
            view.findViewById<Button>(
                R.id.btn_bg_color
            )

        val saveButton =
            view.findViewById<Button>(
                R.id.btn_save_config
            )

        /*
         * Initial slider values.
         */
        sbStroke.progress =
            strokeThicknessDp.coerceIn(
                0,
                sbStroke.max
            )

        sbCorner.progress =
            cornerRadiusDp.coerceIn(
                0,
                sbCorner.max
            )

        sbOpacity.progress =
            bgAlphaPercent.coerceIn(
                0,
                sbOpacity.max
            )

        sbBlur.progress =
            blurRadiusDp.coerceIn(
                0,
                sbBlur.max
            )

        switchEnableBlur.isChecked =
            isBlurEnabled

        /*
         * Content source.
         */
        contentSourceButton?.setOnClickListener {
            showContentSourcePicker()
        }

        /*
         * Color buttons.
         */
        btnFont.setOnClickListener {
            showHexColorPicker(
                "Font / Text Color",
                selectedFontColor
            ) { color ->
                selectedFontColor = color
                updatePreview()
            }
        }

        btnAccent.setOnClickListener {
            showHexColorPicker(
                "Accent Color",
                selectedAccentColor
            ) { color ->
                selectedAccentColor = color
                updatePreview()
            }
        }

        btnStroke.setOnClickListener {
            showHexColorPicker(
                "Stroke / Border Color",
                selectedStrokeColor
            ) { color ->
                selectedStrokeColor = color
                updatePreview()
            }
        }

        btnBackground.setOnClickListener {
            showHexColorPicker(
                "Background Color",
                selectedBgColor
            ) { color ->
                selectedBgColor = color
                updatePreview()
            }
        }

        /*
         * Blur switch.
         */
        switchEnableBlur.setOnCheckedChangeListener {
                _,
                checked ->

            isBlurEnabled = checked

            sbBlur.isEnabled = checked

            updatePreview()
        }

        sbBlur.isEnabled =
            isBlurEnabled

        /*
         * Stroke thickness.
         */
        sbStroke.setOnSeekBarChangeListener(
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

        /*
         * Corner radius.
         */
        sbCorner.setOnSeekBarChangeListener(
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

        /*
         * Background opacity.
         */
        sbOpacity.setOnSeekBarChangeListener(
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

        /*
         * Blur radius.
         */
        sbBlur.setOnSeekBarChangeListener(
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