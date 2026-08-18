package com.tvtime.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews

class TVTimeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TVTimeWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val pkg = context.packageName
            val layoutId = context.resources.getIdentifier("widget_tv_time", "layout", pkg).takeIf { it != 0 }
                ?: context.resources.getIdentifier("widget_layout", "layout", pkg).takeIf { it != 0 }
                ?: android.R.layout.simple_list_item_1

            val views = RemoteViews(pkg, layoutId)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
