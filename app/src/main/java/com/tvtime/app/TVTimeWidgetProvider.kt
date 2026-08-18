package com.tvtime.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class TVTimeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(
                context,
                TVTimeWidgetProvider::class.java
            )

            manager.getAppWidgetIds(component).forEach { id ->
                updateAppWidget(context, manager, id)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val pkg = context.packageName
            val views = RemoteViews(pkg, R.layout.widget_layout)

            // Load widget settings
            val prefs = WidgetPreferences(context, appWidgetId)
            val density = context.resources.displayMetrics.density

            // Glass background
            val glass = GlassBitmapRenderer.renderLauncherContainer(
                widthPx = 800,
                heightPx = 500,
                bgHex = prefs.bgHexColor,
                borderHex = prefs.borderHexColor,
                cornerRadiusDp = prefs.cornerRadius,
                borderThicknessDp = prefs.borderThickness,
                alphaPercent = prefs.bgBlurOpacity,
                gaussianBlurRadius = prefs.gaussianBlurRadius,
                density = density,
                gradient = GlassBitmapRenderer.isLiquid(prefs.glassPreset)
            )

            views.setImageViewBitmap(R.id.iv_widget_bg, glass)

            // Widget title
            views.setTextViewText(
                R.id.tv_widget_header,
                prefs.widgetTitle
            )

            // Real TVTime watchlist
            WatchlistStore.init(context)
            val watchlist = WatchlistStore.getMyStuff()
            val upNext = watchlist.firstOrNull()

            if (upNext != null) {
                views.setTextViewText(
                    R.id.tv_hero_tag,
                    "UP NEXT"
                )
                views.setTextViewText(
                    R.id.tv_hero_title,
                    upNext.title
                )
                views.setTextViewText(
                    R.id.tv_hero_desc,
                    if (upNext.subtitle.isBlank()) "Saved in My Stuff" else upNext.subtitle
                )
                views.setTextViewText(
                    R.id.tv_hero_progress_text,
                    "Progress: ${upNext.progress.coerceIn(0, 100)}%"
                )
                views.setProgressBar(
                    R.id.pb_hero_progress,
                    100,
                    upNext.progress.coerceIn(0, 100),
                    false
                )
            } else {
                views.setTextViewText(
                    R.id.tv_hero_tag,
                    "UP NEXT"
                )
                views.setTextViewText(
                    R.id.tv_hero_title,
                    "Nothing queued"
                )
                views.setTextViewText(
                    R.id.tv_hero_desc,
                    "Add a show or movie in TVTime"
                )
                views.setTextViewText(
                    R.id.tv_hero_progress_text,
                    "Progress: 0%"
                )
                views.setProgressBar(
                    R.id.pb_hero_progress,
                    100,
                    0,
                    false
                )
            }

            // Connect the real watchlist to the widget list
            val serviceIntent = Intent(
                context,
                WatchlistWidgetService::class.java
            ).apply {
                putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    appWidgetId
                )
                data = android.net.Uri.parse(
                    toUri(Intent.URI_INTENT_SCHEME)
                )
            }

            views.setRemoteAdapter(
                R.id.list_mystuff,
                serviceIntent
            )

            appWidgetManager.notifyAppWidgetViewDataChanged(
                appWidgetId,
                R.id.list_mystuff
            )

            appWidgetManager.updateAppWidget(
                appWidgetId,
                views
            )
        }
    }
}
