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
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class WidgetConfigFragment : Fragment() {

    private val FALLBACK_PARSE_BOT_KEY = "pmx_b7c9c4361f273e03c70d48956f1891e6"

    private var currentBgHex = "#CC1E1E1E"
    private var currentAccentHex = "#4DD0E1"
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

        // The widget is Tubi-only (recommendations + your bookmarks, taps open Tubi), so the
        // streaming-service spinner was removed — the service is always forced to Tubi.
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
        val btnTubiSync = view.findViewById<Button>(R.id.btn_tubi_sync)
        val tvTubiSyncStatus = view.findViewById<TextView>(R.id.tv_tubi_sync_status)

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

        fun syncTubi() {
            if (!TubiAccount.isLoggedIn()) {
                tvTubiSyncStatus?.text = "Sign in to Tubi first"
                return
            }
            btnTubiSync?.isEnabled = false
            tvTubiSyncStatus?.text = "Syncing your Tubi lists…"
            val catalogPrefs = requireContext().getSharedPreferences("TVTimeCatalog", Context.MODE_PRIVATE)
            val url = catalogPrefs.getString("url", "")?.takeIf { it.isNotEmpty() }
                ?: "https://api.parse.bot/scraper/3b4482fa-50a4-475d-a612-75d5c78654eb"
            val key = catalogPrefs.getString("key", "")?.takeIf { it.isNotEmpty() }
                ?: BuildConfig.PARSE_BOT_KEY.ifEmpty { FALLBACK_PARSE_BOT_KEY }
            TubiRepository.fetchUserLists { wl, ms, diag ->
                // This callback runs on a background thread, so network calls are safe here.
                val recResult = if (wl == null && url.isNotEmpty() && key.isNotEmpty()) {
                    try {
                        loadRecommendationsCatalog(url, key) to null
                    } catch (e: Exception) {
                        null to (e.message ?: e.javaClass.simpleName)
                    }
                } else null to null
                val recommendations = recResult.first
                val recErr = recResult.second
                requireActivity().runOnUiThread {
                    btnTubiSync?.isEnabled = true
                    if (wl == null && ms == null && recommendations == null) {
                        tvTubiSyncStatus?.text = "Sync failed: $diag"
                        return@runOnUiThread
                    }
                    val finalWatchlist = wl ?: recommendations
                    val wlCount = finalWatchlist?.size ?: 0
                    val msCount = ms?.size ?: 0
                    WatchlistStore.init(requireContext())
                    finalWatchlist?.let { WatchlistStore.saveWatchlist(it) }
                    ms?.let { WatchlistStore.saveMyStuff(it) }
                    TVTimeWidgetProvider.notifyDataChanged(requireContext())
                    tvTubiSyncStatus?.text = buildString {
                        append("Synced: $wlCount in WatchList")
                        when {
                            wl != null -> append(" (continue-watching)")
                            recommendations != null -> append(" (recommended)")
                            recErr != null -> append(" (recs err: $recErr)")
                            else -> append(" (no recs: key/url empty?)")
                        }
                        append(", $msCount saved.")
                    }
                }
            }
        }

        btnTubiSync?.setOnClickListener { syncTubi() }

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
                        syncTubi()
                    } else {
                        tvTubiStatus?.text = "Sign-in failed: ${err ?: "unknown error"}"
                    }
                }
            }
        }

        // --- Catalog data (optional third-party Tubi catalog API: URL + key) ---
        val catalogPrefs = requireContext().getSharedPreferences("TVTimeCatalog", Context.MODE_PRIVATE)
        val etCatalogUrl = view.findViewById<EditText>(R.id.et_catalog_url)
        val etCatalogKey = view.findViewById<EditText>(R.id.et_catalog_key)
        val btnLoadCatalog = view.findViewById<Button>(R.id.btn_load_catalog)
        val tvCatalogStatus = view.findViewById<TextView>(R.id.tv_catalog_status)
        val defaultCatalogUrl = "https://api.parse.bot/scraper/3b4482fa-50a4-475d-a612-75d5c78654eb"
        val defaultCatalogKey = BuildConfig.PARSE_BOT_KEY.ifEmpty { FALLBACK_PARSE_BOT_KEY }
        etCatalogUrl?.setText(catalogPrefs.getString("url", defaultCatalogUrl))
        etCatalogKey?.setText(catalogPrefs.getString("key", defaultCatalogKey))

        btnLoadCatalog?.setOnClickListener {
            val url = etCatalogUrl?.text?.toString()?.trim() ?: ""
            val key = etCatalogKey?.text?.toString()?.trim() ?: ""
            if (url.isEmpty() || key.isEmpty()) {
                tvCatalogStatus?.text = "Enter both API URL and key"
                return@setOnClickListener
            }
            catalogPrefs.edit().putString("url", url).putString("key", key).apply()
            btnLoadCatalog.isEnabled = false
            tvCatalogStatus?.text = "Loading categories…"
            Thread {
                try {
                    val items = loadTubiCatalog(url, key)
                    requireActivity().runOnUiThread {
                        btnLoadCatalog.isEnabled = true
                        if (items.isNullOrEmpty()) {
                            tvCatalogStatus?.text = "No items found in response"
                        } else {
                            WatchlistStore.init(requireContext())
                            WatchlistStore.saveWatchlist(items)
                            TVTimeWidgetProvider.notifyDataChanged(requireContext())
                            tvCatalogStatus?.text = "Loaded ${items.size} titles into WatchList"
                        }
                    }
                } catch (e: Exception) {
                    requireActivity().runOnUiThread {
                        btnLoadCatalog.isEnabled = true
                        tvCatalogStatus?.text = "Error: ${e.message ?: "network"}"
                    }
                }
            }.start()
        }

        currentBgHex = prefs.bgHexColor
        currentAccentHex = prefs.accentHexColor
        currentStrokeHex = prefs.borderHexColor
        // Ensure the service is always Tubi (spinner removed).
        prefs.selectedServicePackage = "com.tubitv"

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
            val servicePackage = "com.tubitv"

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

            // Auto-populate WatchList with Tubi recommendations so the widget isn't empty.
            val acUrl = etCatalogUrl?.text?.toString()?.trim() ?: ""
            val acKey = etCatalogKey?.text?.toString()?.trim() ?: ""
            if (acUrl.isNotEmpty() && acKey.isNotEmpty()) {
                Thread {
                    try {
                        val recs = loadRecommendationsCatalog(acUrl, acKey)
                        if (!recs.isNullOrEmpty()) {
                            WatchlistStore.init(ctx)
                            WatchlistStore.saveWatchlist(recs)
                            TVTimeWidgetProvider.notifyDataChanged(ctx)
                        }
                    } catch (_: Exception) {
                    }
                }.start()
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

    private fun parseCatalog(resp: String): List<ShowItem>? {
        val json = try { JSONObject(resp) } catch (_: Exception) { null }
        var node: Any? = json
        if (json != null && json.has("result") && json.optJSONObject("result") != null) {
            node = json.optJSONObject("result")
        }
        if (node is JSONObject && node.has("data")) node = node.opt("data")
        val arr: JSONArray? = when {
            node is JSONArray -> node
            node is JSONObject && node.has("items") -> node.optJSONArray("items")
            node is JSONObject && node.has("results") -> node.optJSONArray("results")
            node is JSONObject && node.has("contents") -> node.optJSONArray("contents")
            node is JSONObject && node.has("entities") -> node.optJSONArray("entities")
            json != null && json.has("items") -> json.optJSONArray("items")
            else -> null
        }
        val source = arr ?: run {
            try { JSONArray(resp) } catch (_: Exception) { null }
        }
        if (source == null) return null
        val out = mutableListOf<ShowItem>()
        for (i in 0 until source.length()) {
            val o = source.optJSONObject(i) ?: continue
            val title = o.optString("title", o.optString("name", ""))
            if (title.isEmpty()) continue
            val subtitle = o.optString("description", o.optString("subtitle", ""))
            val imageUrl = o.optString("poster_url", o.optString("thumbnail_url", ""))
            val watchUrl = o.optString("watch_url", o.optString("url", ""))
            out.add(ShowItem(title, subtitle, 0, imageUrl, watchUrl))
        }
        return if (out.isNotEmpty()) out else null
    }

    private fun loadTubiCatalog(rawUrl: String, key: String): List<ShowItem>? {
        // WatchList is Tubi's "Recommended for you" section by default.
        val rec = loadRecommendationsCatalog(rawUrl, key)
        if (rec != null && rec.isNotEmpty()) return rec
        // Fallback: merge a handful of general categories (excluding recommendation-style rows).
        var base = rawUrl.removeSuffix("/")
        val li = base.indexOf("/list_content")
        if (li >= 0) base = base.substring(0, li)
        val all = mutableListOf<ShowItem>()
        val seen = mutableSetOf<String>()
        val slugs = parseCategories(fetchCatalog("$base/list_categories?limit=100", key))
        if (slugs.isNotEmpty()) {
            val usable = slugs.filter { s ->
                !s.contains("recommended") && !s.contains("for_you") &&
                        !s.contains("watch_it_again") && !s.contains("on_now")
            }.take(8)
            for (slug in usable) {
                try {
                    val page = parseCatalog(fetchCatalog("$base/list_content?category=$slug&limit=10", key)) ?: continue
                    for (it in page) if (seen.add(it.title.lowercase())) all.add(it)
                } catch (_: Exception) {}
            }
        }
        if (all.isEmpty()) {
            val page = parseCatalog(fetchCatalog("$base/list_content?limit=50", key))
            if (page != null) for (it in page) if (seen.add(it.title.lowercase())) all.add(it)
        }
        return if (all.isNotEmpty()) all else null
    }

    private fun loadRecommendationsCatalog(rawUrl: String, key: String): List<ShowItem>? {
        var base = rawUrl.removeSuffix("/")
        val li = base.indexOf("/list_content")
        if (li >= 0) base = base.substring(0, li)
        return parseCatalog(fetchCatalog("$base/list_content?category=recommended_for_you&limit=20", key))
    }

    private fun parseCategories(resp: String): List<String> {
        val json = try { JSONObject(resp) } catch (_: Exception) { null }
        val cats = json?.optJSONObject("data")?.optJSONArray("categories")
        val out = mutableListOf<String>()
        if (cats != null) {
            for (i in 0 until cats.length()) {
                val o = cats.optJSONObject(i) ?: continue
                val slug = o.optString("slug", "")
                if (slug.isNotEmpty()) out.add(slug)
            }
        }
        return out
    }

    private fun fetchCatalog(url: String, key: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "TVTimeApp")
        conn.setRequestProperty("X-API-Key", key)
        conn.connectTimeout = 20000
        conn.readTimeout = 20000
        val code = conn.responseCode
        val resp = try {
            conn.inputStream.bufferedReader().readText()
        } catch (_: Exception) {
            conn.errorStream?.bufferedReader()?.readText() ?: ""
        }
        conn.disconnect()
        if (code !in 200..299) throw IOException("HTTP $code")
        return resp
    }
}

private fun simpleChanged(onChanged: (progress: Int, fromUser: Boolean) -> Unit) =
    object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(sb: SeekBar?, progress: Int, user: Boolean) =
            onChanged(progress, user)
        override fun onStartTrackingTouch(sb: SeekBar?) {}
        override fun onStopTrackingTouch(sb: SeekBar?) {}
    }
