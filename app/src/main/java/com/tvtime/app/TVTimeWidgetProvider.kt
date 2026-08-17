package com.tvtime.app

import android.app.PendingIntent
import android.app.WallpaperManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews

class TVTimeWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val WIDGET_WIDTH_PX = 400
        private const val WIDGET_HEIGHT_PX = 200
        private val ACCENT = ColorUtils.parseArgb("#4DD0E1")!!

        /** Tell all live widgets to reload their show lists from WatchlistStore. */
        fun notifyDataChanged(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(
                ComponentName(context, TVTimeWidgetProvider::class.java)
            )
            ids.forEach { mgr.notifyAppWidgetViewDataChanged(it, R.id.list_mystuff) }
        }

        /** Fully rebind every live widget and re-query the RemoteViewsService so it
         * picks up replaced data. */
        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(
                ComponentName(context, TVTimeWidgetProvider::class.java)
            )
            ids.forEach {
                TVTimeWidgetProvider().updateWidget(context, mgr, it)
                mgr.notifyAppWidgetViewDataChanged(it, R.id.list_mystuff)
            }
            // Belt-and-suspenders: also push a full APPWIDGET_UPDATE so launchers that
            // cache the collection re-bind it from scratch.
            val intent = Intent(context, TVTimeWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { updateWidget(context, appWidgetManager, it) }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            val prefs = WidgetPreferences(context, appWidgetId)
            val density = context.resources.displayMetrics.density

            val isLiquid = GlassBitmapRenderer.isLiquid(prefs.glassPreset)

            val dynamicColor = resolveDynamicAccent(context, prefs)
            val accent = dynamicColor ?: (ColorUtils.parseArgb(prefs.accentHexColor) ?: ACCENT)
            val borderHex = dynamicColor?.let { ColorUtils.toArgbHex(it) } ?: prefs.borderHexColor

            val glass = GlassBitmapRenderer.renderLauncherContainer(
                widthPx = WIDGET_WIDTH_PX,
                heightPx = WIDGET_HEIGHT_PX,
                bgHex = prefs.bgHexColor,
                borderHex = borderHex,
                cornerRadiusDp = prefs.cornerRadius,
                borderThicknessDp = prefs.borderThickness,
                alphaPercent = prefs.bgBlurOpacity,
                gaussianBlurRadius = prefs.gaussianBlurRadius,
                density = density,
                gradient = isLiquid
            )
            views.setImageViewBitmap(R.id.iv_widget_bg, glass)

            views.setTextColor(R.id.tv_widget_header, accent)

            try {
                WatchlistStore.init(context)
                val n = WatchlistStore.getMyStuff().size
                views.setTextViewText(R.id.tv_count, if (n == 0) "" else "($n)")
            } catch (_: Exception) {
                views.setTextViewText(R.id.tv_count, "")
            }

            val watchIntent = Intent(context, WatchDetailsActivity::class.java)
            val watchPendingIntent = PendingIntent.getActivity(
                context, appWidgetId, watchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setPendingIntentTemplate(R.id.list_mystuff, watchPendingIntent)

            val myStuffIntent = Intent(context, WatchlistWidgetService::class.java).apply {
                action = "com.tvtime.app.LIST_MYSTUFF"
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra("list_type", "mystuff")
            }
            views.setRemoteAdapter(appWidgetId, R.id.list_mystuff, myStuffIntent)
            views.setEmptyView(R.id.list_mystuff, R.id.tv_empty_p2)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.e("TVTimeWidget", "updateWidget OK for #$appWidgetId")
        } catch (e: Exception) {
            Log.e(
                "TVTimeWidget",
                "updateWidget failed for #$appWidgetId\n${Log.getStackTraceString(e)}"
            )
            val fallback = RemoteViews(context.packageName, R.layout.widget_fallback)
            fallback.setTextViewText(R.id.tv_fallback, "TVTime err: ${e.message}")
            appWidgetManager.updateAppWidget(appWidgetId, fallback)
        }
    }

    /** Material You: on Android 12+ with dynamic color enabled, derive the accent from the
     * system wallpaper (primary color). Returns null when dynamic isn't available. */
    private fun resolveDynamicAccent(context: Context, prefs: WidgetPreferences): Int? {
        if (!prefs.useDynamicColor) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return try {
            val wm = context.getSystemService(Context.WALLPAPER_SERVICE) as WallpaperManager
            val colors = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            colors?.primaryColor?.toArgb()
        } catch (_: Exception) {
            null
        }
    }
}
