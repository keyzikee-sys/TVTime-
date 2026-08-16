package com.tvtime.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class TVTimeWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_NEXT_PAGE = "com.tvtime.app.ACTION_NEXT_PAGE"
        const val ACTION_PREV_PAGE = "com.tvtime.app.ACTION_PREV_PAGE"
        private const val WIDGET_WIDTH_PX = 400
        private const val WIDGET_HEIGHT_PX = 200

        /** Tell all live widgets to reload their show lists from WatchlistStore. */
        fun notifyDataChanged(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(
                ComponentName(context, TVTimeWidgetProvider::class.java)
            )
            ids.forEach { id ->
                mgr.notifyAppWidgetViewDataChanged(id, R.id.list_watchlist)
                mgr.notifyAppWidgetViewDataChanged(id, R.id.list_mystuff)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        when (intent.action) {
            ACTION_NEXT_PAGE ->
                flipPage(context, appWidgetManager, intent, 1)
            ACTION_PREV_PAGE ->
                flipPage(context, appWidgetManager, intent, 0)
        }
    }

    /** Flips the page of the targeted widget only (if its id is carried in the intent). */
    private fun flipPage(
        context: Context,
        appWidgetManager: AppWidgetManager,
        intent: Intent,
        page: Int
    ) {
        val targetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (targetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            WidgetPreferences(context, targetId).currentPage = page
            updateWidget(context, appWidgetManager, targetId)
        } else {
            // Fallback for any broadcast sent without an explicit id.
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, TVTimeWidgetProvider::class.java)
            )
            ids.forEach { WidgetPreferences(context, it).currentPage = page }
            ids.forEach { updateWidget(context, appWidgetManager, it) }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        val prefs = WidgetPreferences(context, appWidgetId)
        val density = context.resources.displayMetrics.density

        val isLiquid = GlassBitmapRenderer.isLiquid(prefs.glassPreset)
        val glass = GlassBitmapRenderer.renderLauncherContainer(
            widthPx = WIDGET_WIDTH_PX,
            heightPx = WIDGET_HEIGHT_PX,
            bgHex = prefs.bgHexColor,
            borderHex = prefs.borderHexColor,
            cornerRadiusDp = prefs.cornerRadius,
            borderThicknessDp = prefs.borderThickness,
            alphaPercent = prefs.bgBlurOpacity,
            gaussianBlurRadius = prefs.gaussianBlurRadius,
            density = density,
            gradient = isLiquid
        )
        views.setImageViewBitmap(R.id.iv_widget_bg, glass)

        val accent = ColorUtils.parseArgb(prefs.accentHexColor)
            ?: ColorUtils.parseArgb("#FFFF1493")!!
        views.setTextColor(R.id.tv_widget_title_p1, accent)
        views.setTextColor(R.id.tv_widget_title_p2, accent)

        views.setDisplayedChild(R.id.view_flipper, prefs.currentPage)

        val service = StreamingServices.byPackage(prefs.selectedServicePackage)
            ?: StreamingServices.default()
        views.setTextViewText(R.id.btn_open_tubi, "Open ${service.name}")

        val nextPendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId,
            Intent(context, TVTimeWidgetProvider::class.java).apply {
                action = ACTION_NEXT_PAGE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_next_page, nextPendingIntent)

        val prevPendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId,
            Intent(context, TVTimeWidgetProvider::class.java).apply {
                action = ACTION_PREV_PAGE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_prev_page, prevPendingIntent)

        val launchIntent = StreamingServices.createLaunchIntent(context, service)
        val tubiPendingIntent = PendingIntent.getActivity(
            context, appWidgetId, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_open_tubi, tubiPendingIntent)

        val watchIntent = Intent(context, WatchDetailsActivity::class.java)
        val watchPendingIntent = PendingIntent.getActivity(
            context, appWidgetId, watchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setPendingIntentTemplate(R.id.list_watchlist, watchPendingIntent)
        views.setPendingIntentTemplate(R.id.list_mystuff, watchPendingIntent)

        val watchListIntent = Intent(context, WatchlistWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra("list_type", "watchlist")
        }
        views.setRemoteAdapter(appWidgetId, R.id.list_watchlist, watchListIntent)
        views.setEmptyView(R.id.list_watchlist, R.id.tv_empty_p1)

        val myStuffIntent = Intent(context, WatchlistWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra("list_type", "mystuff")
        }
        views.setRemoteAdapter(appWidgetId, R.id.list_mystuff, myStuffIntent)
        views.setEmptyView(R.id.list_mystuff, R.id.tv_empty_p2)

        val configIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val configPendingIntent = PendingIntent.getActivity(
            context, appWidgetId, configIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_open_config, configPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
