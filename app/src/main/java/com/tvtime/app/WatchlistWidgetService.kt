package com.tvtime.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class WatchlistWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        WatchlistStore.init(applicationContext)
        return WatchlistRemoteViewsFactory(applicationContext)
    }
}

class WatchlistRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private val items = ArrayList<ShowItem>()

    private var titleColor = Color.WHITE
    private var subtitleColor = Color.parseColor("#B0BEC5")

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        WatchlistStore.init(context)

        items.clear()
        items.addAll(WatchlistStore.getMyStuff())

        val prefs = context.getSharedPreferences("tvtime_prefs", Context.MODE_PRIVATE)

        titleColor = getSavedColor(
            prefs,
            "title_color",
            "text_color",
            "accent_color",
            "#00E5FF"
        )

        subtitleColor = getSavedColor(
            prefs,
            "subtitle_color",
            "subtext_color",
            "#B0BEC5"
        )
    }

    private fun getSavedColor(
        prefs: android.content.SharedPreferences,
        vararg keys: String
    ): Int {
        val defaultColor = keys.last()

        for (i in 0 until keys.size - 1) {
            val key = keys[i]

            if (prefs.contains(key)) {
                try {
                    return prefs.getInt(key, Color.parseColor(defaultColor))
                } catch (_: Exception) {
                    try {
                        return Color.parseColor(
                            prefs.getString(key, defaultColor)
                                ?: defaultColor
                        )
                    } catch (_: Exception) {
                    }
                }
            }
        }

        return Color.parseColor(defaultColor)
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position < 0 || position >= items.size) {
            return getLoadingView()
        }

        val item = items[position]

        return RemoteViews(
            context.packageName,
            R.layout.widget_list_item
        ).apply {
            setTextViewText(R.id.item_title, item.title)
            setTextColor(R.id.item_title, titleColor)

            setTextViewText(R.id.item_subtitle, item.subtitle)
            setTextColor(R.id.item_subtitle, subtitleColor)

            setOnClickFillInIntent(
                R.id.item_container,
                Intent()
            )
        }
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(
            context.packageName,
            R.layout.widget_list_item
        ).apply {
            setTextViewText(R.id.item_title, "Loading...")
            setTextViewText(R.id.item_subtitle, "")
        }
    }

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    override fun onDestroy() {
        items.clear()
    }
}
