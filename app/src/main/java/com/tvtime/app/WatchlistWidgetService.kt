package com.tvtime.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService

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
        WatchlistStore.init(context)
        val type = intent.getStringExtra("list_type") ?: "watchlist"
        items = if (type == "mystuff") {
            WatchlistStore.getMyStuff()
        } else {
            WatchlistStore.getWatchlist()
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = if (items.isEmpty()) 1 else items.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_list_item)
        if (items.isEmpty()) {
            views.setTextViewText(R.id.tv_title, "No shows yet")
            views.setTextViewText(R.id.tv_sub, "Add titles in the app")
            views.setProgressBar(R.id.pb_show_progress, 100, 0, false)
            return views
        }
        val item = items[position]
        views.setTextViewText(R.id.tv_title, item.title)
        views.setTextViewText(R.id.tv_sub, item.subtitle)
        views.setProgressBar(R.id.pb_show_progress, 100, item.progress, false)
        views.setOnClickFillInIntent(
            R.id.widget_list_item_root,
            Intent().apply { putExtra("watch_url", item.watchUrl) }
        )
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
}
