package com.tvtime.app

import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Loads Tubi account content and Terror on Tubi content.
 *
 * All content is normalized into ShowItem so the main app, WatchlistStore,
 * and widget use the same title/description/poster/watch URL data.
 */
object TubiRepository {

    private val QUEUE_URL =
        "https://user-queue.production-public.tubi.io/api/v2/queues"

    private val CONTENT_URL =
        "https://content.production-public.tubi.io/cms/content"

    private val DEVICE_ID =
        java.util.UUID.randomUUID().toString()

    fun fetchUserLists(
        callback: (
            watchlist: List<ShowItem>?,
            mystuff: List<ShowItem>?,
            diagnostic: String
        ) -> Unit
    ) {
        Thread {
            try {
                if (!TubiAccount.isLoggedIn()) {
                    callback(null, null, "Not signed in")
                    return@Thread
                }

                val token = TubiAccount.accessToken()
                val cookie = "connect.sid=${TubiAccount.sessionCookie()}"

                val queueResp = tryGetAuth(
                    QUEUE_URL,
                    token,
                    cookie
                )

                if (queueResp == null || queueResp.code !in 200..299) {
                    val note =
                        if (token.isEmpty()) {
                            " (no JWT; ${TubiAccount.authDebug})"
                        } else {
                            ""
                        }

                    callback(
                        null,
                        null,
                        "HTTP ${queueResp?.code ?: 0}$note fetching queue"
                    )
                    return@Thread
                }

                val items = parseQueueIds(queueResp.body)

                val cwIds =
                    items
                        .filter { it.second.contains("continue") }
                        .map { it.first }
                        .toMutableList()

                val otherIds =
                    items
                        .filterNot { it.second.contains("continue") }
                        .map { it.first }
                        .toMutableList()

                fetchViewHistoryIds(token, cookie)
                    .forEach { id ->
                        if (!cwIds.contains(id)) {
                            cwIds.add(id)
                        }
                    }

                val allIds =
                    (cwIds + otherIds)
                        .distinct()
                        .take(40)

                if (allIds.isEmpty()) {
                    callback(
                        null,
                        null,
                        "HTTP 200 but no queue items"
                    )
                    return@Thread
                }

                val details = fetchContentDetails(allIds, token, cookie)

                val wl =
                    cwIds.mapNotNull { details[it] }
                        .toMutableList()

                val ms =
                    otherIds.mapNotNull { details[it] }
                        .toMutableList()

                callback(
                    if (wl.isEmpty()) null else wl,
                    if (ms.isEmpty()) null else ms,
                    ""
                )

            } catch (e: Exception) {
                callback(
                    null,
                    null,
                    "Exception: ${e.message}"
                )
            }
        }.start()
    }

    private fun parseQueueIds(
        resp: String
    ): List<Pair<Int, String>> {

        val root =
            try {
                JSONObject(resp)
            } catch (_: Exception) {
                return emptyList()
            }

        val arr =
            root.optJSONArray("queues")
                ?: try {
                    JSONArray(resp)
                } catch (_: Exception) {
                    null
                }
                ?: return emptyList()

        val out = mutableListOf<Pair<Int, String>>()

        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue

            val id =
                o.optInt("content_id", 0)

            if (id != 0) {
                out.add(
                    id to o.optString(
                        "type",
                        ""
                    ).lowercase()
                )
            }
        }

        return out
    }

    private fun fetchViewHistoryIds(
        token: String,
        cookie: String
    ): List<Int> {

        val candidates =
            listOf(
                "https://user-queue.production-public.tubi.io/api/v2/view_history",
                "https://tensor.production-public.tubi.io/api/v2/view_history"
            )

        for (url in candidates) {
            val r =
                tryGetAuth(
                    url,
                    token,
                    cookie
                ) ?: continue

            if (r.code !in 200..299) continue

            val root =
                try {
                    JSONObject(r.body)
                } catch (_: Exception) {
                    null
                }

            val arr =
                root?.optJSONArray("contents")
                    ?: root?.optJSONArray("items")
                    ?: root?.optJSONArray("data")
                    ?: try {
                        JSONArray(r.body)
                    } catch (_: Exception) {
                        null
                    }
                    ?: continue

            val ids =
                mutableListOf<Int>()

            for (i in 0 until arr.length()) {
                val o =
                    arr.optJSONObject(i) ?: continue

                val id =
                    o.optInt("content_id", 0)

                if (id != 0) {
                    ids.add(id)
                }
            }

            if (ids.isNotEmpty()) {
                return ids
            }
        }

        return emptyList()
    }

    /**
     * Gets the normal CMS metadata first, then fills missing descriptions
     * from the individual Tubi content endpoint.
     */
   private fun fetchContentDetails(
    ids: List<Int>,
        token: String = "",
        cookie: String = ""
): Map<Int, ShowItem> {
        val csv =
            ids.joinToString(",")

        val url =
            "$CONTENT_URL?content_ids=$csv&platform=web&device_id=$DEVICE_ID"

        val body =
            tryGetPlain(url)

        val root =
            try {
                if (body.isNullOrBlank()) {
                    null
                } else {
                    JSONObject(body)
                }
            } catch (_: Exception) {
                null
            }

        val map =
            mutableMapOf<Int, ShowItem>()

        for (id in ids) {

            val o =
                root?.optJSONObject(id.toString())

            var item =
                if (o != null) {
                    parseContentObject(
                        o,
                        id
                    )
                } else {
                    null
                }

            /*
             * If the CMS object did not provide a description,
             * ask Tubi for the individual content record.
             */
            if (
                item == null ||
                item.description.isBlank()
            ) {
                val fallback =
                    fetchIndividualContentDetails(id, token, cookie)

                if (fallback != null) {
                    item =
                        if (item == null) {
                            fallback
                        } else {
                            item.copy(
                                description =
                                    fallback.description.ifBlank {
                                        item.description
                                    },
                                imageUrl =
                                    item.imageUrl.ifBlank {
                                        fallback.imageUrl
                                    },
                                watchUrl =
                                    item.watchUrl.ifBlank {
                                        fallback.watchUrl
                                    }
                            )
                        }
                }
            }

            if (item != null) {
                map[id] = item
            }
        }

        return map
    }

    private fun parseContentObject(
        o: JSONObject,
        id: Int
    ): ShowItem? {

        val title =
            o.optString(
                "title",
                ""
            ).trim()

        if (title.isEmpty()) {
            return null
        }

        val description =
            findDescription(o)

        val year =
            o.optInt(
                "year",
                0
            )

        val dtype =
            o.optString(
                "detailed_type",
                ""
            )

        val subtitle =
            if (year > 0 && dtype.isNotBlank()) {
                "$year · $dtype"
            } else if (year > 0) {
                year.toString()
            } else {
                dtype
            }

        val imageUrl =
            findThumbnail(o)

        val watchUrl =
            resolveTubiUrl(
                o,
                id
            )

        return ShowItem(
            title = title,
            subtitle = subtitle,
            progress = 0,
            description = description,
            imageUrl = imageUrl,
            watchUrl = watchUrl
        )
    }

    /**
     * Individual Tubi content endpoint.
     * This is used specifically when the CMS record has no description.
     */
    private fun fetchIndividualContentDetails(
        id: Int,
        token: String,
        cookie: String
    ): ShowItem? {

        val url =
            "https://tubitv.com/oz/videos/$id/content"

        val body =
            tryGetAuth(url, token, cookie)?.body
                ?: return null

        val o =
            try {
                JSONObject(body)
            } catch (_: Exception) {
                return null
            }

        val title =
            o.optString(
                "title",
                ""
            ).trim()

        if (title.isEmpty()) {
            return null
        }

        val description =
            findDescription(o)

        val year =
            o.optInt(
                "year",
                0
            )

        val imageUrl =
            findThumbnail(o)

        val detailedType =
            o.optString(
                "detailed_type",
                ""
            )

        val subtitle =
            if (year > 0 && detailedType.isNotBlank()) {
                "$year · $detailedType"
            } else if (year > 0) {
                year.toString()
            } else {
                detailedType
            }

        val watchUrl =
            resolveTubiUrl(
                o,
                id
            )

        return ShowItem(
            title = title,
            subtitle = subtitle,
            progress = 0,
            description = description,
            imageUrl = imageUrl,
            watchUrl = watchUrl
        )
    }

    /**
     * Finds a real textual description anywhere inside the metadata object.
     */
    private fun findDescription(
        value: Any?
    ): String {

        val keys =
            listOf(
                "description",
                "synopsis",
                "summary",
                "short_description",
                "long_description",
                "overview",
                "plot"
            )

        fun clean(
            value: String?
        ): String? {

            val text =
                value
                    ?.trim()
                    .orEmpty()

            if (text.isEmpty()) {
                return null
            }

            if (
                text.startsWith(
                    "http://",
                    true
                ) ||
                text.startsWith(
                    "https://",
                    true
                )
            ) {
                return null
            }

            return text
        }

        fun search(
            current: Any?
        ): String? {

            when (current) {

                is JSONObject -> {

                    for (key in keys) {
                        val found =
                            clean(
                                current.optString(
                                    key,
                                    ""
                                )
                            )

                        if (found != null) {
                            return found
                        }
                    }

                    val iterator =
                        current.keys()

                    while (iterator.hasNext()) {
                        val key =
                            iterator.next()

                        val found =
                            search(
                                current.opt(key)
                            )

                        if (found != null) {
                            return found
                        }
                    }
                }

                is JSONArray -> {

                    for (i in 0 until current.length()) {

                        val found =
                            search(
                                current.opt(i)
                            )

                        if (found != null) {
                            return found
                        }
                    }
                }
            }

            return null
        }

        return search(value).orEmpty()
    }

    private fun findThumbnail(
        o: JSONObject
    ): String {

        val thumbnails = o.optJSONArray("thumbnails")

        if (thumbnails != null) {
            for (i in 0 until thumbnails.length()) {
                val value = thumbnails.optString(i, "").trim()

                if (
                    value.startsWith("http://", true) ||
                    value.startsWith("https://", true)
                ) {
                    return value
                }
            }
        }

        val candidates = listOf(
            "thumbnail",
            "thumbnail_url",
            "poster",
            "poster_url",
            "image",
            "image_url"
        )

        for (key in candidates) {
            val value = o.optString(key, "").trim()

            if (
                value.startsWith("http://", true) ||
                value.startsWith("https://", true)
            ) {
                return value
            }
        }

        return ""
    }

    private fun resolveTubiUrl(
        o: JSONObject,
        id: Int
    ): String {

        val type =
            o.optString(
                "detailed_type",
                ""
            ).lowercase()

        val slug =
            slugify(
                o.optString(
                    "title",
                    ""
                )
            )

        return if (
            type == "series" ||
            type == "episode" ||
            type == "show"
        ) {
            "https://tubitv.com/series/$id/$slug"
        } else {
            "https://tubitv.com/movies/$id/$slug"
        }
    }

    /**
     * Loads Terror on Tubi from the live Tubi category page.
     * The discovered IDs are then resolved through Tubi metadata so Terror
     * receives the same description/poster/title pipeline as My Stuff.
     */
    fun fetchTerrorOnTubi(
        callback: (
            List<ShowItem>?,
            String
        ) -> Unit
    ) {

        Thread {

            try {

                val html =
                    tryGetPlain(
                        "https://tubitv.com/hubs/terror-on-tubi-hub"
                    )

                if (html.isNullOrBlank()) {
                    callback(
                        null,
                        "Unable to load Terror on Tubi."
                    )
                    return@Thread
                }

                val ids =
                    mutableListOf<Int>()

                val fallbackUrls =
                    mutableMapOf<Int, String>()

                val pattern =
                    Regex(
                        """https://tubitv\.com/(movies|series)/(\d+)/([^"?\s<]+)"""
                    )

                for (match in pattern.findAll(html)) {

                    val type =
                        match.groupValues[1]

                    val id =
                        match.groupValues[2]
                            .toIntOrNull()
                            ?: continue

                    val slug =
                        match.groupValues[3]

                    if (!ids.contains(id)) {
                        ids.add(id)

                        fallbackUrls[id] =
                            "https://tubitv.com/$type/$id/$slug"
                    }
                }

                if (ids.isEmpty()) {
                    callback(
                        emptyList(),
                        ""
                    )
                    return@Thread
                }

                /*
                 * Resolve the Terror IDs through the same metadata pipeline
                 * used by My Stuff.
                 */
                val details =
                    fetchContentDetails(
                        ids.take(40)
                    )

                val results =
                    mutableListOf<ShowItem>()

                for (id in ids.take(40)) {

                    val item =
                        details[id]

                    if (item != null) {

                        results.add(
                            item.copy(
                                subtitle = "Terror on Tubi",
                                watchUrl =
                                    item.watchUrl.ifBlank {
                                        fallbackUrls[id].orEmpty()
                                    }
                            )
                        )

                    } else {

                        /*
                         * Final fallback: preserve the Terror title/URL
                         * even if Tubi metadata temporarily fails.
                         */
                        val fallbackUrl =
                            fallbackUrls[id]
                                ?: continue

                        val slug =
                            fallbackUrl
                                .substringAfterLast("/")
                                .substringBefore("?")

                        val title =
                            slug
                                .replace("-", " ")
                                .replaceFirstChar {
                                    it.uppercase()
                                }

                        results.add(
                            ShowItem(
                                title = title,
                                subtitle = "Terror on Tubi",
                                progress = 0,
                                description = "",
                                imageUrl = "",
                                watchUrl = fallbackUrl
                            )
                        )
                    }
                }

                callback(
                    results,
                    ""
                )

            } catch (e: Exception) {

                callback(
                    null,
                    e.message
                        ?: "Unable to load Terror on Tubi."
                )
            }

        }.start()
    }


    private fun slugify(
        title: String
    ): String {

        return title
            .lowercase()
            .replace(
                Regex("[^a-z0-9]+"),
                "-"
            )
            .trim('-')
    }

    private fun tryGetPlain(
        url: String
    ): String? {

        return try {

            val conn =
                URL(url)
                    .openConnection()
                    as HttpURLConnection

            conn.requestMethod = "GET"

            conn.setRequestProperty(
                "Accept",
                "application/json,text/html,*/*"
            )

            conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36"
            )

            conn.setRequestProperty(
                "Referer",
                "https://tubitv.com/"
            )

            val code =
                conn.responseCode

            val resp =
                try {
                    conn.inputStream
                        .bufferedReader()
                        .readText()
                } catch (_: Exception) {
                    conn.errorStream
                        ?.bufferedReader()
                        ?.readText()
                        ?: ""
                }

            conn.disconnect()

            if (code in 200..299) {
                resp
            } else {
                null
            }

        } catch (_: Exception) {
            null
        }
    }

    private data class AuthResult(
        val code: Int,
        val body: String
    )

    private fun tryGetAuth(
        url: String,
        token: String,
        cookie: String
    ): AuthResult? {

        return try {

            val conn =
                URL(url)
                    .openConnection()
                    as HttpURLConnection

            conn.requestMethod = "GET"

            conn.setRequestProperty(
                "Accept",
                "application/json"
            )

            conn.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36"
            )

            conn.setRequestProperty(
                "Origin",
                "https://tubitv.com"
            )

            conn.setRequestProperty(
                "Referer",
                "https://tubitv.com/"
            )

            if (cookie.isNotEmpty()) {
                conn.setRequestProperty(
                    "Cookie",
                    cookie
                )
            }

            if (token.isNotEmpty()) {
                conn.setRequestProperty(
                    "Authorization",
                    "Bearer $token"
                )
            }

            val code =
                conn.responseCode

            val resp =
                try {
                    conn.inputStream
                        .bufferedReader()
                        .readText()
                } catch (_: Exception) {
                    conn.errorStream
                        ?.bufferedReader()
                        ?.readText()
                        ?: ""
                }

            conn.disconnect()

            AuthResult(
                code,
                resp
            )

        } catch (_: Exception) {
            null
        }
    }
}
