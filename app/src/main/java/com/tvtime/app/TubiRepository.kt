package com.tvtime.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Best-effort loader for a signed-in user's Tubi lists. Tries a set of plausible personal-library
 * endpoints using the stored session cookie (+ access token if present). Parsing is lenient and any
 * failure returns null so the caller can fall back to the curated list.
 */
object TubiRepository {

    fun fetchWatchlist(callback: (List<ShowItem>?) -> Unit) = fetch("watchlist", callback)
    fun fetchMyStuff(callback: (List<ShowItem>?) -> Unit) = fetch("mystuff", callback)

    private fun fetch(kind: String, callback: (List<ShowItem>?) -> Unit) {
        Thread {
            try {
                if (!TubiAccount.isLoggedIn()) {
                    callback(null)
                    return@Thread
                }
                val uid = TubiAccount.userId()
                val base = if (uid.isNotEmpty()) "https://tubitv.com/oz/api/users/$uid" else "https://tubitv.com/oz/api/users/me"
                val candidates = if (kind == "watchlist") {
                    listOf("$base/watchlist", "$base/queue", "$base/saved")
                } else {
                    listOf("$base/library", "$base/my-stuff", "$base/continue-watching", "$base/history")
                }
                for (endpoint in candidates) {
                    val items = tryGet(endpoint)
                    if (items != null) {
                        callback(items)
                        return@Thread
                    }
                }
                callback(null)
            } catch (e: Exception) {
                callback(null)
            }
        }.start()
    }

    private fun tryGet(endpoint: String): List<ShowItem>? {
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")
        conn.setRequestProperty("Cookie", "connect.sid=${TubiAccount.sessionCookie()}")
        val at = TubiAccount.accessToken()
        if (at.isNotEmpty()) conn.setRequestProperty("Authorization", "Bearer $at")

        val code = conn.responseCode
        if (code !in 200..299) {
            conn.disconnect()
            return null
        }
        val resp = try {
            conn.inputStream.bufferedReader().readText()
        } catch (_: Exception) {
            conn.disconnect()
            return null
        }
        conn.disconnect()
        return parseItems(resp)
    }

    private fun parseItems(resp: String): List<ShowItem>? {
        val json = try { JSONObject(resp) } catch (_: Exception) { return null }
        val arr: JSONArray? = when {
            json.has("items") -> json.optJSONArray("items")
            json.has("data") -> json.optJSONArray("data")
            json.has("results") -> json.optJSONArray("results")
            json.has("contents") -> json.optJSONArray("contents")
            json.has("entities") -> json.optJSONArray("entities")
            else -> null
        }
        if (arr == null) return null
        val out = mutableListOf<ShowItem>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val node = if (o.has("content")) o.optJSONObject("content") else o
            val title = node.optString("title", node.optString("name", ""))
            if (title.isEmpty()) continue
            val subtitle = node.optString("subtitle", node.optString("description", ""))
            val progress = node.optInt("progress", 0).coerceIn(0, 100)
            out.add(ShowItem(title, subtitle, progress))
        }
        return if (out.isNotEmpty()) out else null
    }

    /**
     * Fetches the signed-in user's real Tubi lists (continue-watching + my-list) from the
     * user-queue API using the stored access token. Returns (watchlist, mystuff); either may be
     * null on failure so the caller can keep existing data.
     */
    fun fetchUserLists(callback: (watchlist: List<ShowItem>?, mystuff: List<ShowItem>?) -> Unit) {
        Thread {
            try {
                if (!TubiAccount.isLoggedIn()) { callback(null, null); return@Thread }
                val token = TubiAccount.accessToken()
                val cookie = "connect.sid=${TubiAccount.sessionCookie()}"
                val resp = tryGetAuth(
                    "https://user-queue.production-public.tubi.io/api/v2/queues",
                    token, cookie
                )
                if (resp == null) { callback(null, null); return@Thread }
                val (wl, ms) = parseQueues(resp)
                callback(wl, ms)
            } catch (e: Exception) {
                callback(null, null)
            }
        }.start()
    }

    private fun tryGetAuth(url: String, token: String, cookie: String): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Accept", "application/json")
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")
        conn.setRequestProperty("Origin", "https://tubitv.com")
        conn.setRequestProperty("Referer", "https://tubitv.com/")
        if (cookie.isNotEmpty()) conn.setRequestProperty("Cookie", cookie)
        if (token.isNotEmpty()) conn.setRequestProperty("Authorization", "Bearer $token")
        val code = conn.responseCode
        if (code !in 200..299) { conn.disconnect(); return null }
        val resp = try {
            conn.inputStream.bufferedReader().readText()
        } catch (_: Exception) { conn.disconnect(); return null }
        conn.disconnect()
        return resp
    }

    private fun parseQueues(resp: String): Pair<List<ShowItem>?, List<ShowItem>?> {
        val root = try { JSONObject(resp) } catch (_: Exception) { null }
        val arr: JSONArray? = when {
            root?.has("queues") == true -> root.optJSONArray("queues")
            root?.has("items") == true -> root.optJSONArray("items")
            root?.has("data") == true -> root.optJSONArray("data")
            root?.has("results") == true -> root.optJSONArray("results")
            root?.has("contents") == true -> root.optJSONArray("contents")
            else -> null
        }
        val queues = arr ?: run {
            try { JSONArray(resp) } catch (_: Exception) { null }
        } ?: return (null to null)

        val watch = mutableListOf<ShowItem>()
        val my = mutableListOf<ShowItem>()
        for (i in 0 until queues.length()) {
            val q = queues.optJSONObject(i) ?: continue
            val type = q.optString("type", "").lowercase()
            val target = when {
                type.contains("continue") -> watch
                type.contains("my_list") || type.contains("mylist") || type.contains("queue") || type.contains("saved") -> my
                else -> watch
            }
            val contents = q.optJSONArray("contents")
            if (contents != null) {
                for (j in 0 until contents.length()) {
                    mapContent(contents.optJSONObject(j))?.let { target.add(it) }
                }
            } else {
                mapContent(q)?.let { target.add(it) }
            }
        }
        val wl = if (watch.isNotEmpty()) watch else null
        val ms = if (my.isNotEmpty()) my else null
        return wl to ms
    }

    private fun mapContent(o: JSONObject?): ShowItem? {
        if (o == null) return null
        val node = if (o.has("content")) o.optJSONObject("content") else o
        val title = node.optString("title", node.optString("name", ""))
        if (title.isEmpty()) return null
        val subtitle = node.optString("subtitle", node.optString("description", ""))
        val wp = node.optDouble("watchedPercentage", -1.0)
        val pp = node.optDouble("progressPercentage", -1.0)
        val raw = node.optDouble("progress", -1.0)
        val progress = when {
            wp >= 0 -> wp
            pp >= 0 -> pp
            raw in 0.0..100.0 -> raw
            else -> -1.0
        }.coerceAtLeast(0.0).coerceAtMost(100.0).toInt()
        val imageUrl = node.optString(
            "poster",
            node.optString("thumbnail", node.optString("artwork", node.optString("posterUrl", "")))
        )
        var watchUrl = node.optString(
            "url",
            node.optString("shareUrl", node.optString("contentUrl", node.optString("watchUrl", "")))
        )
        if (watchUrl.startsWith("/")) watchUrl = "https://tubitv.com$watchUrl"
        return ShowItem(title, subtitle, progress, imageUrl, watchUrl)
    }
}
