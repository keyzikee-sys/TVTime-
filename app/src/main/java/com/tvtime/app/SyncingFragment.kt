package com.tvtime.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class SyncingFragment : Fragment() {

    private val FALLBACK_PARSE_BOT_KEY = "pmx_b7c9c4361f273e03c70d48956f1891e6"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_syncing, container, false)

        TubiAccount.init(requireContext())

        val etEmail = view.findViewById<EditText>(R.id.et_tubi_email)
        val etPass = view.findViewById<EditText>(R.id.et_tubi_pass)
        val btnTubiLogin = view.findViewById<Button>(R.id.btn_tubi_login)
        val tvTubiStatus = view.findViewById<TextView>(R.id.tv_tubi_status)
        val btnTubiSync = view.findViewById<Button>(R.id.btn_tubi_sync)
        val tvTubiSyncStatus = view.findViewById<TextView>(R.id.tv_tubi_sync_status)

        val catalogPrefs = requireContext().getSharedPreferences("TVTimeCatalog", Context.MODE_PRIVATE)
        val etCatalogUrl = view.findViewById<EditText>(R.id.et_catalog_url)
        val etCatalogKey = view.findViewById<EditText>(R.id.et_catalog_key)
        val btnLoadCatalog = view.findViewById<Button>(R.id.btn_load_catalog)
        val tvCatalogStatus = view.findViewById<TextView>(R.id.tv_catalog_status)
        val defaultCatalogUrl = "https://api.parse.bot/scraper/3b4482fa-50a4-475d-a612-75d5c78654eb"
        val defaultCatalogKey = BuildConfig.PARSE_BOT_KEY.ifEmpty { FALLBACK_PARSE_BOT_KEY }
        etCatalogUrl?.setText(catalogPrefs.getString("url", defaultCatalogUrl))
        etCatalogKey?.setText(catalogPrefs.getString("key", defaultCatalogKey))

        fun refreshTubiUi() {
            if (TubiAccount.isLoggedIn()) {
                tvTubiStatus?.text = "Signed in as ${TubiAccount.email()}"
                btnTubiLogin?.text = "Sign out"
                etEmail?.isEnabled = false
                etPass?.isEnabled = false
            } else {
                tvTubiStatus?.text = "Not signed in"
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
            val url = catalogPrefs.getString("url", "")?.takeIf { it.isNotEmpty() }
                ?: defaultCatalogUrl
            val key = catalogPrefs.getString("key", "")?.takeIf { it.isNotEmpty() }
                ?: BuildConfig.PARSE_BOT_KEY.ifEmpty { FALLBACK_PARSE_BOT_KEY }
            TubiRepository.fetchUserLists { wl, ms, diag ->
                val recResult = if (ms == null && url.isNotEmpty() && key.isNotEmpty()) {
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
                    if (ms == null && recommendations == null) {
                        tvTubiSyncStatus?.text = "Sync failed: $diag"
                        return@runOnUiThread
                    }
                    val finalWatchlist = wl ?: recommendations
                    val msCount = ms?.size ?: 0
                    WatchlistStore.init(requireContext())
                    // My Stuff = the user's saved bookmarks only (continue-watching/WatchList is
                    // intentionally not shown in the widget).
                    ms?.let { WatchlistStore.saveMyStuff(it) }
                    TVTimeWidgetProvider.updateAll(requireContext())
                    tvTubiSyncStatus?.text = buildString {
                        append("Synced: $msCount bookmarks into My Stuff")
                        when {
                            ms == null -> append(" (sync failed: $diag)")
                            wl != null -> append(" · ${wl.size} continue-watching ignored (WatchList removed)")
                        }
                        if (recommendations != null) append(" · recs available")
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
                            WatchlistStore.saveMyStuff(items)
                            TVTimeWidgetProvider.updateAll(requireContext())
                            tvCatalogStatus?.text = "Loaded ${items.size} titles into My Stuff"
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

        return view
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
        val rec = loadRecommendationsCatalog(rawUrl, key)
        if (rec != null && rec.isNotEmpty()) return rec
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
        if (code !in 200..299) throw java.io.IOException("HTTP $code")
        return resp
    }
}
