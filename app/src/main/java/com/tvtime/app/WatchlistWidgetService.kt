package com.tvtime.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.tvtime.app.R

class WatchlistWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WatchlistRemoteViewsFactory(this.applicationContext)
    }
}

class WatchlistRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private val items = ArrayList<Pair<String, String>>()
    private var titleColor: Int = Color.parseColor("#00E5FF")
    private var subtitleColor: Int = Color.parseColor("#B0BEC5")

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        items.clear()

        val prefs = context.getSharedPreferences("tvtime_prefs", Context.MODE_PRIVATE)

        titleColor = getSavedColor(prefs, "title_color", "text_color", "accent_color", defaultColor = "#00E5FF")
        subtitleColor = getSavedColor(prefs, "subtitle_color", "subtext_color", defaultColor = "#B0BEC5")

        val savedData = prefs.getStringSet("watchlist_items", null)

        if (!savedData.isNullOrEmpty()) {
            for (entry in savedData) {
                val parts = entry.split("|")
                if (parts.size >= 2) {
                    items.add(Pair(parts[0], parts[1]))
                } else if (parts.isNotEmpty()) {
                    items.add(Pair(parts[0], "movie"))
                }
            }
        }

        if (items.isEmpty()) {
            items.add(Pair("The Black-Eyed Children", "2025 · movie"))
            items.add(Pair("Crybaby Bridge", "2026 · movie"))
            items.add(Pair("Mr. Tomorrow", "2026 · movie"))
            items.add(Pair("Never Blink", "2025 · movie"))
        }
    }

    private fun getSavedColor(
        prefs: android.content.SharedPreferences,
        vararg keys: String,
        defaultColor: String
    ): Int {
        for (key in keys) {
            if (prefs.contains(key)) {
                try {
                    return prefs.getInt(key, Color.parseColor(defaultColor))
                } catch (e: ClassCastException) {
                    val hex = prefs.getString(key, null)
                    if (!hex.isNullOrEmpty()) {
                        return try { Color.parseColor(hex) } catch (e: Exception) { Color.parseColor(defaultColor) }
                    }
                }
            }
        }
        return Color.parseColor(defaultColor)
    }

    override fun onDestroy() {
        items.clear()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= items.size) {
            return getLoadingView()
        }

        val views = RemoteViews(context.packageName, R.layout.widget_list_item)
        val item = items[position]

        views.setTextViewText(R.id.item_title, item.first)
        views.setTextColor(R.id.item_title, titleColor)

        views.setTextViewText(R.id.item_subtitle, item.second)
        views.setTextColor(R.id.item_subtitle, subtitleColor)

        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_list_item).apply {
            setTextViewText(R.id.item_title, "Loading...")
            setTextColor(R.id.item_title, titleColor)
            setTextViewText(R.id.item_subtitle, "")
        }
    }

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
