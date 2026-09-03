package com.tvtime.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews

class TVTimeWidgetProvider : AppWidgetProvider() {


    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_PAGE_1 || intent.action == ACTION_PAGE_2) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TVTimeWidgetProvider::class.java))
            val page = if (intent.action == ACTION_PAGE_2) 1 else 0

            for (id in ids) {
                WidgetPreferences(context, id).currentPage = page
                updateAppWidget(context, manager, id)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(
                context,
                appWidgetManager,
                appWidgetId
            )
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(
            context,
            appWidgetManager,
            appWidgetId,
            newOptions
        )

        updateAppWidget(
            context,
            appWidgetManager,
            appWidgetId
        )
    }

    companion object {

        const val ACTION_PAGE_1 = "com.tvtime.app.ACTION_PAGE_1"
        const val ACTION_PAGE_2 = "com.tvtime.app.ACTION_PAGE_2"


        fun updateAll(context: Context) {
            val manager =
                AppWidgetManager.getInstance(context)

            val component =
                ComponentName(
                    context,
                    TVTimeWidgetProvider::class.java
                )

            manager.getAppWidgetIds(component).forEach { id ->
                updateAppWidget(
                    context,
                    manager,
                    id
                )
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {

            val pkg = context.packageName

            val views = RemoteViews(
                pkg,
                R.layout.widget_layout
            )

            // ---------------------------------------------------------
            // WIDGET SETTINGS
            // ---------------------------------------------------------

            val prefs =
                WidgetPreferences(
                    context,
                    appWidgetId
                )

            val activeContentSource = if (prefs.currentPage == 1) {
                WidgetPreferences.CONTENT_TERROR_ON_TUBI
            } else {
                WidgetPreferences.CONTENT_MY_STUFF
            }

            val density =
                context.resources.displayMetrics.density

            val options =
                appWidgetManager.getAppWidgetOptions(
                    appWidgetId
                )

            android.util.Log.d(
                "TVTimeWidget",
                "Widget size: " +
                    "minW=${options.getInt(
                        AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
                    )}dp " +
                    "minH=${options.getInt(
                        AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
                    )}dp " +
                    "maxW=${options.getInt(
                        AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
                    )}dp " +
                    "maxH=${options.getInt(
                        AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
                    )}dp"
            )

            // ---------------------------------------------------------
            // GLASS BACKGROUND
            // ---------------------------------------------------------

            val dynamicPalette = if (prefs.useDynamicColor) DynamicWidgetColors.get(context) else null
            val dynamicBgHex = dynamicPalette?.background?.let { color -> String.format("#%08X", color) }
            val dynamicBorderHex = dynamicPalette?.border?.let { color -> String.format("#%08X", color) }

            val glass =
                GlassBitmapRenderer.renderLauncherContainer(

                    widthPx = (
                        options.getInt(
                            AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                            250
                        ) * density
                    ).toInt().coerceAtLeast(1),

                    heightPx = (
                        options.getInt(
                            AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                            110
                        ) * density
                    ).toInt().coerceAtLeast(1),

                    bgHex = dynamicBgHex ?: prefs.bgHexColor,

                    borderHex =
                        dynamicBorderHex ?: prefs.borderHexColor,

                    cornerRadiusDp =
                        prefs.cornerRadius,

                    borderThicknessDp =
                        prefs.borderThickness,

                    alphaPercent =
                        prefs.bgBlurOpacity,

                    gaussianBlurRadius =
                        prefs.gaussianBlurRadius,

                    density =
                        density,

                    gradient =
                        GlassBitmapRenderer.isLiquid(
                            prefs.glassPreset
                        )
                )

            views.setImageViewBitmap(
                R.id.iv_widget_bg,
                glass
            )

            // ---------------------------------------------------------
            // WIDGET HEADER
            // ---------------------------------------------------------

            views.setTextViewText(
                R.id.tv_widget_header,
                prefs.widgetTitle
            )

            val accentColor = dynamicPalette?.accent ?: Color.parseColor(prefs.accentHexColor)
            views.setTextColor(R.id.tv_hero_tag, accentColor)
            views.setTextColor(R.id.tv_hero_title, accentColor)
            views.setTextColor(R.id.tv_hero_desc, Color.parseColor("#737D8C"))
            views.setTextColor(R.id.tv_hero_progress_text, Color.parseColor("#737D8C"))

            val page1Intent = Intent(context, TVTimeWidgetProvider::class.java).apply { action = ACTION_PAGE_1 }
            val page2Intent = Intent(context, TVTimeWidgetProvider::class.java).apply { action = ACTION_PAGE_2 }

            val page1PendingIntent = PendingIntent.getBroadcast(context, appWidgetId * 10 + 1, page1Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val page2PendingIntent = PendingIntent.getBroadcast(context, appWidgetId * 10 + 2, page2Intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

            views.setOnClickPendingIntent(R.id.btn_page_1, page1PendingIntent)
            views.setOnClickPendingIntent(R.id.btn_page_2, page2PendingIntent)

            // ---------------------------------------------------------
            // WATCHLIST DATA
            // ---------------------------------------------------------

            WatchlistStore.init(context)

            val watchlist =
                WatchlistStore.getMyStuff()

            val upNext =
                watchlist.firstOrNull()

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
                    if (upNext.subtitle.isBlank()) {
                        "Saved in My Stuff"
                    } else {
                        upNext.subtitle
                    }
                )

                val progress =
                    upNext.progress.coerceIn(
                        0,
                        100
                    )

                views.setTextViewText(
                    R.id.tv_hero_progress_text,
                    "Progress: $progress%"
                )

                views.setProgressBar(
                    R.id.pb_hero_progress,
                    100,
                    progress,
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

            // ---------------------------------------------------------
            // WIDGET LIST SERVICE
            //
            // IMPORTANT:
            // Pass the selected content source to the service.
            //
            // This allows:
            //   my_stuff
            //   terror_on_tubi
            //
            // to use different data.
            // ---------------------------------------------------------

            val serviceIntent =
                Intent(
                    context,
                    WatchlistWidgetService::class.java
                ).apply {

                    putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        appWidgetId
                    )

                    putExtra(
                        "content_source",
                        activeContentSource
                    )

                    data =
                        android.net.Uri.parse(
                            toUri(
                                Intent.URI_INTENT_SCHEME
                            )
                        )
                }

            views.setRemoteAdapter(
                R.id.list_mystuff,
                serviceIntent
            )

            // ---------------------------------------------------------
            // LIST ITEM CLICK HANDLER
            // ---------------------------------------------------------

            val clickIntent =
                Intent(
                    context,
                    WatchDetailsActivity::class.java
                )

            val clickPendingIntent =
                PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_MUTABLE
                )

            views.setPendingIntentTemplate(
                R.id.list_mystuff,
                clickPendingIntent
            )

            // ---------------------------------------------------------
            // FORCE LIST REFRESH
            // ---------------------------------------------------------

            appWidgetManager.notifyAppWidgetViewDataChanged(
                appWidgetId,
                R.id.list_mystuff
            )

            // ---------------------------------------------------------
            // APPLY WIDGET
            // ---------------------------------------------------------

            appWidgetManager.updateAppWidget(
                appWidgetId,
                views
            )
        }
    }
}