package com.tvtime.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import android.appwidget.AppWidgetManager

/**
 * Supplies the widget's show list (title + subtitle + progress) from WatchlistStore.
 * Text-only rows so the glass background stays visible — no poster images.
 */
class WatchlistWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WatchlistRemoteViewsFactory(applicationContext, intent)
    }
}

class WatchlistRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<ShowItem> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        try {
            WatchlistStore.init(context)
            val type = intent.getStringExtra("list_type") ?: "watchlist"
            val base = if (type == "mystuff") {
                WatchlistStore.getMyStuff()
            } else {
                WatchlistStore.getWatchlist()
            }
            val skipFirst = intent.getBooleanExtra("skip_first", false)
            items = if (skipFirst) base.drop(1) else base
        } catch (e: Exception) {
            android.util.Log.e(
                "TVTimeWidget",
                "onDataSetChanged failed\n${android.util.Log.getStackTraceString(e)}"
            )
            items = emptyList()
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = try {
        if (items.isEmpty()) 1 else items.size
    } catch (e: Exception) {
        android.util.Log.e(
            "TVTimeWidget",
            "getCount failed\n${android.util.Log.getStackTraceString(e)}"
        )
        1
    }

    override fun getViewAt(position: Int): RemoteViews {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_list_item)
            val widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                WidgetPreferences.DEFAULT_ID
            )
            val listColor = ColorUtils.parseArgb(WidgetPreferences(context, widgetId).listTextColor)
                ?: Color.WHITE
            val subColor = ColorUtils.withAlpha(listColor, 60)
            if (items.isEmpty()) {
                views.setTextViewText(R.id.tv_title, "No shows yet")
                views.setTextViewText(R.id.tv_sub, "Add titles in the app")
                views.setProgressBar(R.id.pb_show_progress, 100, 0, false)
                views.setTextColor(R.id.tv_title, listColor)
                views.setTextColor(R.id.tv_sub, subColor)
                return views
            }
            val item = items[position]
            views.setTextViewText(R.id.tv_title, item.title)
            views.setTextViewText(R.id.tv_sub, item.subtitle)
            views.setProgressBar(R.id.pb_show_progress, 100, item.progress, false)
            views.setTextColor(R.id.tv_title, listColor)
            views.setTextColor(R.id.tv_sub, subColor)
            views.setOnClickFillInIntent(
                R.id.widget_list_item_root,
                Intent().apply { putExtra("watch_url", item.watchUrl) }
            )
            return views
        } catch (e: Exception) {
            android.util.Log.e(
                "TVTimeWidget",
                "getViewAt($position) failed\n${android.util.Log.getStackTraceString(e)}"
            )
            return RemoteViews(context.packageName, R.layout.widget_list_item)
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
}
