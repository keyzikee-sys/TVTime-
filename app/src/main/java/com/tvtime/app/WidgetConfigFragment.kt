package com.tvtime.app

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class WidgetConfigFragment : Fragment() {

    private var currentBgHex = "#CC1E1E1E"
    private var currentAccentHex = "#FFFF1493"
    private var currentStrokeHex = "#3303DAC5"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_widget_config, container, false)

        val configureId = arguments?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        val isConfigure = configureId != AppWidgetManager.INVALID_APPWIDGET_ID

        // Reads come from the default profile (in-app) or this widget's own settings
        // (configure-on-add). applyPreset / save also target this object.
        val prefs = if (isConfigure) {
            WidgetPreferences(requireContext(), configureId)
        } else {
            WidgetPreferences(requireContext())
        }

        val previewCard = view.findViewById<View>(R.id.preview_card)
        val tvPreviewTitle = view.findViewById<TextView>(R.id.tv_preview_title)

        val spinnerService = view.findViewById<Spinner>(R.id.spinner_service)
        val serviceNames = StreamingServices.ALL.map { it.name }
        spinnerService?.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            serviceNames
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        val currentServiceIndex = StreamingServices.ALL.indexOfFirst {
            it.packageName == prefs.selectedServicePackage
        }.coerceAtLeast(0)
        spinnerService?.setSelection(currentServiceIndex)

        val rgPresets = view.findViewById<RadioGroup>(R.id.rg_glass_presets)
        val seekBgBlur = view.findViewById<SeekBar>(R.id.seek_bg_blur)
        val seekGaussian = view.findViewById<SeekBar>(R.id.seek_gaussian_blur)
        val seekLayer = view.findViewById<SeekBar>(R.id.seek_layer_blur)
        val seekStrokeWidth = view.findViewById<SeekBar>(R.id.seek_stroke_width)
        val seekCornerRadius = view.findViewById<SeekBar>(R.id.seek_corner_radius)

        val etBgHex = view.findViewById<EditText>(R.id.et_bg_hex)
        val btnPickAccent = view.findViewById<Button>(R.id.btn_pick_accent)
        val viewAccentPreview = view.findViewById<View>(R.id.view_accent_preview)
        val btnPickStroke = view.findViewById<Button>(R.id.btn_pick_stroke)
        val viewStrokePreview = view.findViewById<View>(R.id.view_stroke_preview)

        val tvBgBlur = view.findViewById<TextView>(R.id.tv_bg_blur_label)
        val tvGaussian = view.findViewById<TextView>(R.id.tv_gaussian_label)
        val tvLayer = view.findViewById<TextView>(R.id.tv_layer_blur_label)
        val tvStrokeWidth = view.findViewById<TextView>(R.id.tv_stroke_width_label)
        val tvCornerRadius = view.findViewById<TextView>(R.id.tv_corner_radius_label)
        val btnSave = view.findViewById<Button>(R.id.btn_save_config)

        // --- Tubi account (best-effort personalized data) ---
        TubiAccount.init(requireContext())
        val etEmail = view.findViewById<EditText>(R.id.et_tubi_email)
        val etPass = view.findViewById<EditText>(R.id.et_tubi_pass)
        val btnTubiLogin = view.findViewById<Button>(R.id.btn_tubi_login)
        val tvTubiStatus = view.findViewById<TextView>(R.id.tv_tubi_status)

        fun refreshTubiUi() {
            if (TubiAccount.isLoggedIn()) {
                tvTubiStatus?.text = "Signed in as ${TubiAccount.email()}"
                btnTubiLogin?.text = "Sign out"
                etEmail?.isEnabled = false
                etPass?.isEnabled = false
            } else {
                tvTubiStatus?.text = "Not signed in (showing sample list)"
                btnTubiLogin?.text = "Sign in to Tubi"
                etEmail?.isEnabled = true
                etPass?.isEnabled = true
            }
        }
        refreshTubiUi()

        btnTubiLogin?.setOnClickListener {
            if (TubiAccount.isLoggedIn()) {
                TubiAccount.signOut()
                etEmail?.text?.clear()
                etPass?.text?.clear()
                refreshTubiUi()
                return@setOnClickListener
            }
            val email = etEmail?.text?.toString()?.trim() ?: ""
            val pass = etPass?.text?.toString() ?: ""
            if (email.isEmpty() || pass.isEmpty()) {
                tvTubiStatus?.text = "Enter email and password"
                return@setOnClickListener
            }
            btnTubiLogin.isEnabled = false
            tvTubiStatus?.text = "Signing in…"
            TubiAccount.login(email, pass) { ok, err ->
                requireActivity().runOnUiThread {
                    btnTubiLogin.isEnabled = true
                    if (ok) {
                        refreshTubiUi()
                    } else {
                        tvTubiStatus?.text = "Sign-in failed: ${err ?: "unknown error"}"
                    }
                }
            }
        }

        currentBgHex = prefs.bgHexColor
        currentAccentHex = prefs.accentHexColor
        currentStrokeHex = prefs.borderHexColor

        etBgHex?.setText(currentBgHex)
        updateColorView(viewAccentPreview, currentAccentHex)
        updateColorView(viewStrokePreview, currentStrokeHex)

        seekBgBlur?.progress = prefs.bgBlurOpacity
        seekGaussian?.progress = prefs.gaussianBlurRadius
        seekLayer?.progress = prefs.layerBlurSoftening
        seekStrokeWidth?.progress = prefs.borderThickness
        seekCornerRadius?.progress = prefs.cornerRadius

        when (prefs.glassPreset) {
            GlassBitmapRenderer.PRESET_DARK ->
                view.findViewById<RadioButton>(R.id.rb_preset_dark)?.isChecked = true
            GlassBitmapRenderer.PRESET_TINTED ->
                view.findViewById<RadioButton>(R.id.rb_preset_tinted)?.isChecked = true
            GlassBitmapRenderer.PRESET_LIQUID ->
                view.findViewById<RadioButton>(R.id.rb_preset_liquid)?.isChecked = true
            GlassBitmapRenderer.PRESET_LIQUID_NOBLUR ->
                view.findViewById<RadioButton>(R.id.rb_preset_liquid_noblur)?.isChecked = true
            else ->
                view.findViewById<RadioButton>(R.id.rb_preset_light)?.isChecked = true
        }

        val density = requireContext().resources.displayMetrics.density

        fun refreshLivePreview() {
            val glass = GlassBitmapRenderer.renderLauncherContainer(
                widthPx = PREVIEW_WIDTH_PX,
                heightPx = PREVIEW_HEIGHT_PX,
                bgHex = currentBgHex,
                borderHex = currentStrokeHex,
                cornerRadiusDp = seekCornerRadius?.progress ?: 16,
                borderThicknessDp = seekStrokeWidth?.progress ?: 2,
                alphaPercent = seekBgBlur?.progress ?: 80,
                gaussianBlurRadius = seekGaussian?.progress ?: 12,
                density = density,
                gradient = isLiquidSelected(rgPresets)
            )
            previewCard?.background = BitmapDrawable(requireContext().resources, glass)
            val accent = ColorUtils.parseArgb(currentAccentHex) ?: Color.parseColor("#FFFF1493")
            tvPreviewTitle?.setTextColor(accent)
        }

        refreshLivePreview()

        fun applyPreset(name: String) {
            val preset = GlassPresets[name] ?: return
            currentBgHex = preset.bgHex
            currentAccentHex = preset.accentHex
            currentStrokeHex = preset.borderHex
            prefs.apply {
                bgHexColor = preset.bgHex
                accentHexColor = preset.accentHex
                borderHexColor = preset.borderHex
                bgBlurOpacity = preset.bgBlurOpacity
                cornerRadius = preset.cornerRadius
                borderThickness = preset.borderThickness
                gaussianBlurRadius = preset.gaussianBlurRadius
                layerBlurSoftening = preset.layerBlurSoftening
            }
            etBgHex?.setText(currentBgHex)
            updateColorView(viewAccentPreview, currentAccentHex)
            updateColorView(viewStrokePreview, currentStrokeHex)
            seekBgBlur?.progress = preset.bgBlurOpacity
            seekGaussian?.progress = preset.gaussianBlurRadius
            seekLayer?.progress = preset.layerBlurSoftening
            seekStrokeWidth?.progress = preset.borderThickness
            seekCornerRadius?.progress = preset.cornerRadius
        }

        rgPresets?.setOnCheckedChangeListener { _, checkedId ->
            val isNoBlur = checkedId == R.id.rb_preset_liquid_noblur
            val presetName = when (checkedId) {
                R.id.rb_preset_dark -> GlassBitmapRenderer.PRESET_DARK
                R.id.rb_preset_tinted -> GlassBitmapRenderer.PRESET_TINTED
                R.id.rb_preset_liquid -> GlassBitmapRenderer.PRESET_LIQUID
                R.id.rb_preset_liquid_noblur -> GlassBitmapRenderer.PRESET_LIQUID
                else -> GlassBitmapRenderer.PRESET_LIGHT
            }
            applyPreset(presetName)
            if (isNoBlur) {
                seekGaussian?.progress = 0
                seekLayer?.progress = 0
                seekGaussian?.isEnabled = false
                seekLayer?.isEnabled = false
                tvGaussian?.text = "Gaussian Blur Radius: 0px (Disabled)"
                tvLayer?.text = "Layer Softening: 0px (Disabled)"
            } else {
                seekGaussian?.isEnabled = true
                seekLayer?.isEnabled = true
            }
            refreshLivePreview()
        }

        tvBgBlur?.text = "Background Opacity / Alpha: ${seekBgBlur?.progress ?: 80}%"
        tvGaussian?.text = "Gaussian Blur Radius: ${seekGaussian?.progress ?: 12}px"
        tvLayer?.text = "Layer Softening: ${seekLayer?.progress ?: 8}px"
        tvStrokeWidth?.text = "Stroke Thickness: ${seekStrokeWidth?.progress ?: 2}dp"
        tvCornerRadius?.text = "Corner Radius: ${seekCornerRadius?.progress ?: 16}dp"

        if (prefs.glassPreset == GlassBitmapRenderer.PRESET_LIQUID_NOBLUR) {
            seekGaussian?.isEnabled = false
            seekLayer?.isEnabled = false
            tvGaussian?.text = "Gaussian Blur Radius: 0px (Disabled)"
            tvLayer?.text = "Layer Softening: 0px (Disabled)"
        }

        seekBgBlur?.setOnSeekBarChangeListener(simpleChanged { progress, _ ->
            tvBgBlur?.text = "Background Opacity / Alpha: $progress%"
            refreshLivePreview()
        })
        seekStrokeWidth?.setOnSeekBarChangeListener(simpleChanged { progress, _ ->
            tvStrokeWidth?.text = "Stroke Thickness: ${progress}dp"
            refreshLivePreview()
        })
        seekCornerRadius?.setOnSeekBarChangeListener(simpleChanged { progress, _ ->
            tvCornerRadius?.text = "Corner Radius: ${progress}dp"
            refreshLivePreview()
        })
        seekGaussian?.setOnSeekBarChangeListener(simpleChanged { progress, _ ->
            tvGaussian?.text = "Gaussian Blur Radius: ${progress}px"
            refreshLivePreview()
        })
        seekLayer?.setOnSeekBarChangeListener(simpleChanged { progress, _ ->
            tvLayer?.text = "Layer Softening: ${progress}px"
            refreshLivePreview()
        })

        etBgHex?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hex = s.toString().trim()
                if (hex.startsWith("#") && (hex.length == 7 || hex.length == 9)) {
                    currentBgHex = hex
                    refreshLivePreview()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnPickAccent?.setOnClickListener {
            showColorPickerDialog(requireContext(), "Select Accent Color", currentAccentHex) { argbHex ->
                currentAccentHex = argbHex
                updateColorView(viewAccentPreview, argbHex)
                refreshLivePreview()
            }.show()
        }

        btnPickStroke?.setOnClickListener {
            showColorPickerDialog(requireContext(), "Select Stroke (Border) Color", currentStrokeHex) { argbHex ->
                currentStrokeHex = argbHex
                updateColorView(viewStrokePreview, argbHex)
                refreshLivePreview()
            }.show()
        }

        btnSave?.setOnClickListener {
            val isNoBlur = rgPresets?.checkedRadioButtonId == R.id.rb_preset_liquid_noblur
            val selectedPreset = when (rgPresets?.checkedRadioButtonId) {
                R.id.rb_preset_dark -> GlassBitmapRenderer.PRESET_DARK
                R.id.rb_preset_tinted -> GlassBitmapRenderer.PRESET_TINTED
                R.id.rb_preset_liquid -> GlassBitmapRenderer.PRESET_LIQUID
                R.id.rb_preset_liquid_noblur -> GlassBitmapRenderer.PRESET_LIQUID_NOBLUR
                else -> GlassBitmapRenderer.PRESET_LIGHT
            }
            val servicePackage = StreamingServices.ALL.getOrNull(
                spinnerService?.selectedItemPosition ?: 0
            )?.packageName ?: StreamingServices.default().packageName

            val style = WidgetStyle(
                glassPreset = selectedPreset,
                bgBlurOpacity = seekBgBlur?.progress ?: 80,
                gaussianBlurRadius = if (isNoBlur) 0 else seekGaussian?.progress ?: 12,
                layerBlurSoftening = if (isNoBlur) 0 else seekLayer?.progress ?: 8,
                borderThickness = seekStrokeWidth?.progress ?: 2,
                cornerRadius = seekCornerRadius?.progress ?: 16,
                bgHexColor = currentBgHex,
                accentHexColor = currentAccentHex,
                borderHexColor = currentStrokeHex,
                selectedServicePackage = servicePackage
            )

            val ctx = requireContext()
            if (isConfigure) {
                WidgetPreferences(ctx, configureId).applyStyle(style)
            } else {
                WidgetPreferences(ctx).applyStyle(style)
                AppWidgetManager.getInstance(ctx)
                    .getAppWidgetIds(ComponentName(ctx, TVTimeWidgetProvider::class.java))
                    .forEach { WidgetPreferences(ctx, it).applyStyle(style) }
            }

            val intent = Intent(ctx, TVTimeWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            val ids = AppWidgetManager.getInstance(ctx).getAppWidgetIds(
                ComponentName(ctx, TVTimeWidgetProvider::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            ctx.sendBroadcast(intent)

            val configureActivity = activity as? WidgetConfigureActivity
            if (configureActivity != null) {
                configureActivity.finishConfigure()
            } else {
                Toast.makeText(ctx, "Widget Customization Saved & Updated!", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun isLiquidSelected(rgPresets: RadioGroup?): Boolean {
        return when (rgPresets?.checkedRadioButtonId) {
            R.id.rb_preset_liquid, R.id.rb_preset_liquid_noblur -> true
            else -> false
        }
    }

    private fun updateColorView(v: View?, argbHex: String) {
        val color = ColorUtils.parseArgb(argbHex) ?: return
        val drawable = GradientDrawable().apply {
            setColor(color)
            setStroke(2, Color.WHITE)
            cornerRadius = 8f
        }
        v?.background = drawable
    }

    companion object {
        private const val PREVIEW_WIDTH_PX = 360
        private const val PREVIEW_HEIGHT_PX = 180
    }
}

private fun simpleChanged(onChanged: (progress: Int, fromUser: Boolean) -> Unit) =
    object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar?, progress: Int, user: Boolean) =
            onChanged(progress, user)
        override fun onStartTrackingTouch(sb: SeekBar?) {}
        override fun onStopTrackingTouch(sb: SeekBar?) {}
    }
