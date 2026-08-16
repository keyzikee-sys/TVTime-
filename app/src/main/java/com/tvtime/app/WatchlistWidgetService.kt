package com.tvtime.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Bundle
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.net.HttpURLConnection
import java.net.URL

/**
 * Supplies the widget's show list (poster + title + progress) from WatchlistStore.
 * Posters are downloaded on the factory's background thread and cached in memory.
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
    private val posterCache = LinkedHashMap<String, Bitmap?>()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        WatchlistStore.init(context)
        val type = intent.getStringExtra("list_type") ?: "watchlist"
        items = if (type == "mystuff") {
            WatchlistStore.getMyStuff()
        } else {
            WatchlistStore.getWatchlist()
        }
        posterCache.clear()
        items.take(15).forEach { item ->
            if (item.imageUrl.isNotEmpty()) {
                posterCache[item.imageUrl] = downloadBitmap(item.imageUrl)
            }
        }
    }

    override fun onDestroy() {
        posterCache.clear()
    }

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
        val bmp = if (item.imageUrl.isNotEmpty()) posterCache[item.imageUrl] else null
        if (bmp != null) {
            views.setImageViewBitmap(R.id.iv_poster, bmp)
        } else {
            views.setInt(R.id.iv_poster, "setBackgroundColor", Color.parseColor("#222222"))
        }
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

private fun roundCorners(bmp: Bitmap, radiusPx: Float): Bitmap {
    val out = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val rect = RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
    val path = Path().apply { addRoundRect(rect, radiusPx, radiusPx, Path.Direction.CW) }
    canvas.clipPath(path)
    canvas.drawBitmap(bmp, 0f, 0f, paint)
    return out
}

private fun downloadBitmap(url: String): Bitmap? {
    return try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.doInput = true
        conn.connect()
            val bmp = BitmapFactory.decodeStream(conn.inputStream) ?: return null
            val scaled = Bitmap.createScaledBitmap(bmp, 100, 150, true)
            if (scaled != bmp) bmp.recycle()
            val rounded = roundCorners(scaled, 14f)
            if (rounded != scaled) scaled.recycle()
            rounded
    } catch (_: Exception) {
        null
    }
}
