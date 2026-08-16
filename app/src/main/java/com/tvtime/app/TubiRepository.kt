package com.tvtime.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Loads a signed-in user's real Tubi data. The user-queue API returns only content IDs + a type
 * (continue_watching / watch_later / my_list ...), so each ID is resolved to full details (title,
 * poster, year) via the content API. Continue-watching + view-history -> WatchList; saved -> My Stuff.
 */
object TubiRepository {

    private val QUEUE_URL = "https://user-queue.production-public.tubi.io/api/v2/queues"
    private val CONTENT_URL = "https://content.production-public.tubi.io/cms/content"
    private val DEVICE_ID = java.util.UUID.randomUUID().toString()

    /**
     * Fetches the signed-in user's real Tubi data. The queue API only returns content IDs + a
     * type (e.g. continue_watching / watch_later), so we resolve each to full details (title,
     * poster, year) via the content API. Continue-watching + view-history -> WatchList; saved ->
     * My Stuff. diagnostic carries failures for debugging.
     */
    fun fetchUserLists(callback: (watchlist: List<ShowItem>?, mystuff: List<ShowItem>?, diagnostic: String) -> Unit) {
        Thread {
            try {
                if (!TubiAccount.isLoggedIn()) { callback(null, null, "Not signed in"); return@Thread }
                val token = TubiAccount.accessToken()
                val cookie = "connect.sid=${TubiAccount.sessionCookie()}"
                val queueResp = tryGetAuth(QUEUE_URL, token, cookie)
                if (queueResp == null || queueResp.code !in 200..299) {
                    val note = if (token.isEmpty()) " (no JWT; ${TubiAccount.authDebug})" else ""
                    callback(null, null, "HTTP ${queueResp?.code ?: 0}$note fetching queue")
                    return@Thread
                }
                val items = parseQueueIds(queueResp.body)
                val cwIds = items.filter { it.second.contains("continue") }.map { it.first }.toMutableList()
                val otherIds = items.filterNot { it.second.contains("continue") }.map { it.first }.toMutableList()
                // Best-effort: real "watch history" also belongs on WatchList.
                fetchViewHistoryIds(token, cookie).forEach { id -> if (!cwIds.contains(id)) cwIds.add(id) }
                val allIds = (cwIds + otherIds).distinct().take(40)
                if (allIds.isEmpty()) { callback(null, null, "HTTP 200 but no queue items"); return@Thread }
                val details = fetchContentDetails(allIds)
                val wl = cwIds.mapNotNull { details[it] }.toMutableList()
                val ms = otherIds.mapNotNull { details[it] }.toMutableList()
                callback(if (wl.isEmpty()) null else wl, if (ms.isEmpty()) null else ms, "")
            } catch (e: Exception) {
                callback(null, null, "Exception: ${e.message}")
            }
        }.start()
    }

    private fun parseQueueIds(resp: String): List<Pair<Int, String>> {
        val root = try { JSONObject(resp) } catch (_: Exception) { return emptyList() }
        val arr = root.optJSONArray("queues") ?: run {
            try { JSONArray(resp) } catch (_: Exception) { null }
        } ?: return emptyList()
        val out = mutableListOf<Pair<Int, String>>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.optInt("content_id", 0)
            if (id != 0) out.add(id to o.optString("type", "").lowercase())
        }
        return out
    }

    private fun fetchViewHistoryIds(token: String, cookie: String): List<Int> {
        val candidates = listOf(
            "https://user-queue.production-public.tubi.io/api/v2/view_history",
            "https://tensor.production-public.tubi.io/api/v2/view_history"
        )
        for (url in candidates) {
            val r = tryGetAuth(url, token, cookie) ?: continue
            if (r.code !in 200..299) continue
            val root = try { JSONObject(r.body) } catch (_: Exception) { null }
            val arr = root?.optJSONArray("contents") ?: root?.optJSONArray("items")
                ?: root?.optJSONArray("data") ?: run {
                    try { JSONArray(r.body) } catch (_: Exception) { null }
                } ?: continue
            val ids = mutableListOf<Int>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optInt("content_id", 0)
                if (id != 0) ids.add(id)
            }
            if (ids.isNotEmpty()) return ids
        }
        return emptyList()
    }

    private fun fetchContentDetails(ids: List<Int>): Map<Int, ShowItem> {
        val csv = ids.joinToString(",")
        val url = "$CONTENT_URL?content_ids=$csv&platform=web&device_id=$DEVICE_ID"
        val body = tryGetPlain(url) ?: return emptyMap()
        val root = try { JSONObject(body) } catch (_: Exception) { return emptyMap() }
        val map = mutableMapOf<Int, ShowItem>()
        for (id in ids) {
            val o = root.optJSONObject(id.toString()) ?: continue
            val title = o.optString("title", "")
            if (title.isEmpty()) continue
            val year = o.optInt("year", 0)
            val dtype = o.optString("detailed_type", "")
            val subtitle = if (year > 0) "$year · $dtype" else dtype
            val thumbs = o.optJSONArray("thumbnails")
            val imageUrl = if (thumbs != null && thumbs.length() > 0) thumbs.optString(0, "") else ""
            val watchUrl = "https://tubitv.com/movies/$id"
            map[id] = ShowItem(title, subtitle, 0, imageUrl, watchUrl)
        }
        return map
    }

    private fun tryGetPlain(url: String): String? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")
            conn.setRequestProperty("Referer", "https://tubitv.com/")
            val code = conn.responseCode
            val resp = try {
                conn.inputStream.bufferedReader().readText()
            } catch (_: Exception) {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            }
            conn.disconnect()
            if (code !in 200..299) null else resp
        } catch (_: Exception) {
            null
        }
    }

    private data class AuthResult(val code: Int, val body: String)

    private fun tryGetAuth(url: String, token: String, cookie: String): AuthResult? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")
            conn.setRequestProperty("Origin", "https://tubitv.com")
            conn.setRequestProperty("Referer", "https://tubitv.com/")
            if (cookie.isNotEmpty()) conn.setRequestProperty("Cookie", cookie)
            if (token.isNotEmpty()) conn.setRequestProperty("Authorization", "Bearer $token")
            val code = conn.responseCode
            val resp = try {
                conn.inputStream.bufferedReader().readText()
            } catch (_: Exception) {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            }
            conn.disconnect()
            AuthResult(code, resp)
        } catch (e: Exception) {
            null
        }
    }
}
