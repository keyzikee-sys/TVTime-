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
}
