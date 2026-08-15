package com.tvtime.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

class TVTimeWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_NEXT_PAGE = "com.tvtime.app.ACTION_NEXT_PAGE"
        const val ACTION_PREV_PAGE = "com.tvtime.app.ACTION_PREV_PAGE"
        private const val WIDGET_WIDTH_PX = 400
        private const val WIDGET_HEIGHT_PX = 200
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
        val ids = appWidgetManager.getAppWidgetIds(
            ComponentName(context, TVTimeWidgetProvider::class.java)
        )
        val prefs = WidgetPreferences(context)
        when (intent.action) {
            ACTION_NEXT_PAGE -> {
                prefs.currentPage = 1
                ids.forEach { updateWidget(context, appWidgetManager, it) }
            }
            ACTION_PREV_PAGE -> {
                prefs.currentPage = 0
                ids.forEach { updateWidget(context, appWidgetManager, it) }
            }
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)
        val prefs = WidgetPreferences(context)
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
            density = density,
            gradient = isLiquid
        )
        views.setImageViewBitmap(R.id.iv_widget_bg, glass)

        val accent = ColorUtils.parseArgb(prefs.accentHexColor)
            ?: ColorUtils.parseArgb("#FFFF1493")!!
        views.setTextColor(R.id.tv_widget_title_p1, accent)
        views.setTextColor(R.id.tv_widget_title_p2, accent)

        views.setDisplayedChild(R.id.view_flipper, prefs.currentPage)

        val nextPendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId,
            Intent(context, TVTimeWidgetProvider::class.java).apply { action = ACTION_NEXT_PAGE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_next_page, nextPendingIntent)

        val prevPendingIntent = PendingIntent.getBroadcast(
            context, appWidgetId,
            Intent(context, TVTimeWidgetProvider::class.java).apply { action = ACTION_PREV_PAGE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_prev_page, prevPendingIntent)

        var launchIntent = context.packageManager.getLaunchIntentForPackage("com.tubitv")
        if (launchIntent == null) {
            launchIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.tubitv")
            )
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val tubiPendingIntent = PendingIntent.getActivity(
            context, appWidgetId, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_open_tubi, tubiPendingIntent)

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
