package com.tvtime.app

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class WatchlistWidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            WidgetPreferences.DEFAULT_ID
        )

        val contentSource = intent.getStringExtra("content_source")
            ?: WidgetPreferences.CONTENT_MY_STUFF

        return WatchlistRemoteViewsFactory(
            applicationContext,
            widgetId,
            contentSource
        )
    }
}

class WatchlistRemoteViewsFactory(
    private val context: android.content.Context,
    private val widgetId: Int,
    private val contentSource: String
) : RemoteViewsService.RemoteViewsFactory {

    private val items = ArrayList<ShowItem>()

    private var titleColor = Color.parseColor("#FFFF13")
    private var subtitleColor = Color.parseColor("#AEB6C2")

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        WatchlistStore.init(context)

        items.clear()

        val widgetPrefs = WidgetPreferences(context, widgetId)

        if (contentSource == WidgetPreferences.CONTENT_TERROR_ON_TUBI) {
            items.addAll(WatchlistStore.getTerrorOnTubi())
        } else {
            items.addAll(WatchlistStore.getMyStuff())
        }

        titleColor = try {
            Color.parseColor(widgetPrefs.listTextColor)
        } catch (_: Exception) {
            Color.parseColor("#FFFF13")
        }

        subtitleColor = Color.parseColor("#AEB6C2")
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
            setTextViewText(R.id.tv_title, item.title)
            setTextColor(R.id.tv_title, titleColor)

            setTextViewText(R.id.tv_sub, item.subtitle)
            setTextColor(R.id.tv_sub, subtitleColor)

            setTextViewText(
                R.id.tv_show_description,
                item.description
            )

            setProgressBar(
                R.id.pb_show_progress,
                100,
                item.progress.coerceIn(0, 100),
                false
            )

            setOnClickFillInIntent(
                R.id.widget_list_item_root,
                Intent().apply {
                    putExtra("watch_url", item.watchUrl)
                }
            )
        }
    }

    override fun getLoadingView(): RemoteViews {
        return RemoteViews(
            context.packageName,
            R.layout.widget_list_item
        ).apply {
            setTextViewText(R.id.tv_title, "Loading...")
            setTextViewText(R.id.tv_sub, "")
            setTextViewText(R.id.tv_show_description, "")
        }
    }

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    override fun onDestroy() {
        items.clear()
    }
}
