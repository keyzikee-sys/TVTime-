package com.tvtime.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter

/**
 * Best-effort Tubi account/session handling.
 *
 * Tubi has no official API. Login is `POST https://tubitv.com/oz/auth/login/` (email + password)
 * which returns a `connect.sid` session cookie. Modern Tubi protects that POST with CSRF, so we
 * first GET the login page to obtain the CSRF cookie/token, then replay it on the login POST.
 * Personal-library endpoints remain HMAC-signed and are not reliably callable; on any failure we
 * fall back to the curated list.
 *
 * We store only the session cookie + access token, never the user's password.
 */
object TubiAccount {
    private const val PREFS = "TVTimeTubiAccount"
    private const val KEY_SESSION = "connect_sid"
    private const val KEY_AT = "access_token"
    private const val KEY_UID = "user_id"
    private const val KEY_EMAIL = "email"

    private const val UA = "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile"

    private lateinit var prefs: SharedPreferences

    /** In-memory diagnostic describing how the last login captured (or failed to capture) the JWT. */
    var authDebug: String = ""

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    fun isLoggedIn(): Boolean = prefs.getString(KEY_SESSION, null)?.isNotEmpty() == true

    fun sessionCookie(): String = prefs.getString(KEY_SESSION, "") ?: ""
    fun accessToken(): String = prefs.getString(KEY_AT, "") ?: ""
    fun userId(): String = prefs.getString(KEY_UID, "") ?: ""
    fun email(): String = prefs.getString(KEY_EMAIL, "") ?: ""

    fun signOut() {
        prefs.edit().clear().apply()
    }

    fun login(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        Thread {
            try {
                val (cookies, csrf) = fetchCsrf()

                val conn = URL("https://tubitv.com/oz/auth/login/").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("User-Agent", UA)
                conn.setRequestProperty("Origin", "https://tubitv.com")
                conn.setRequestProperty("Referer", "https://tubitv.com/login")
                if (cookies.isNotEmpty()) conn.setRequestProperty("Cookie", cookies)
                if (csrf.isNotEmpty()) conn.setRequestProperty("X-CSRF-Token", csrf)
                conn.doOutput = true

                val body = JSONObject().apply {
                    put("username", email)
                    put("password", password)
                    if (csrf.isNotEmpty()) put("_csrf", csrf)
                }.toString()
                OutputStreamWriter(conn.outputStream).use { it.write(body) }

                val code = conn.responseCode
                val resp = try {
                    conn.inputStream.bufferedReader().readText()
                } catch (_: Exception) {
                    conn.errorStream?.bufferedReader()?.readText() ?: ""
                }
                val setCookie = conn.getHeaderField("Set-Cookie")
                conn.disconnect()

                if (code in 200..299 && setCookie != null) {
                    val sid = setCookie.split(";")
                        .firstOrNull { it.trim().startsWith("connect.sid") }
                        ?.substringAfter("=")?.trim() ?: ""

                    val json = try { JSONObject(resp) } catch (_: Exception) { null }
                    // The user-queue API needs the JWT access token, which the legacy login
                    // response often omits. Fetch it from /oz/auth/loadAuth using the new session.
                    val fullCookie = "$cookies; connect.sid=$sid"
                    val authJson = fetchLoadAuth(fullCookie)
                    val at = pickToken(authJson) ?: pickToken(json) ?: ""
                    val uid = authJson?.let { it.optJSONObject("user")?.optString("id", "") ?: it.optString("tubiId", "") }
                        ?.ifEmpty { json?.optJSONObject("user")?.optString("id", "") ?: "" }
                        ?: ""
                    authDebug = buildString {
                        append("loginRespToken=")
                        append(if (json != null && (json.has("access_token") || json.has("accessToken") || json.has("token"))) "yes" else "no")
                        append("; loadAuth=")
                        if (authJson != null) {
                            val keys = authJson.keys().asSequence().take(15).joinToString(",")
                            append("keys[$keys] tokenLen=${at.length}; body=")
                            append(authJson.toString().take(220))
                        } else {
                            append("null")
                        }
                    }
                    prefs.edit().apply {
                        putString(KEY_SESSION, sid)
                        putString(KEY_EMAIL, email)
                        if (at.isNotEmpty()) putString(KEY_AT, at)
                        if (uid.isNotEmpty()) putString(KEY_UID, uid)
                        apply()
                    }
                    callback(true, null)
                } else {
                    val msg = try { JSONObject(resp).optString("message", "Login failed (HTTP $code)") } catch (_: Exception) { "Login failed (HTTP $code)" }
                    callback(false, msg)
                }
            } catch (e: Exception) {
                callback(false, e.message ?: "Network error")
            }
        }.start()
    }

    private fun fetchCsrf(): Pair<String, String> {
        return try {
            val conn = URL("https://tubitv.com/login").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("User-Agent", UA)
            conn.setRequestProperty("Accept", "text/html")
            val code = conn.responseCode
            val cookies = conn.headerFields["Set-Cookie"]
                ?.joinToString("; ") { it.split(";").first() } ?: ""
            val html = try {
                conn.inputStream.bufferedReader().readText()
            } catch (_: Exception) {
                ""
            }
            conn.disconnect()
            if (code !in 200..399) return "" to ""
            cookies to extractCsrf(html)
        } catch (e: Exception) {
            "" to ""
        }
    }

    private fun extractCsrf(html: String): String {
        val patterns = listOf(
            """<meta[^>]+name=["']csrf-token["'][^>]+content=["']([^"']+)["']""",
            """<meta[^>]+content=["']([^"']+)["'][^>]+name=["']csrf-token["']""",
            """name=["']_csrf["'][^>]+value=["']([^"']+)["']""",
            """value=["']([^"']+)["'][^>]+name=["']_csrf["']""",
            """__CSRF__\s*=\s*["']([^"']+)["']"""
        )
        for (p in patterns) {
            Regex(p, RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)?.let { return it }
        }
        return ""
    }

    /** Reads the JWT access token from /oz/auth/loadAuth using the session cookie. */
    private fun fetchLoadAuth(cookie: String): JSONObject? {
        return try {
            val conn = URL("https://tubitv.com/oz/auth/loadAuth").openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", UA)
            conn.setRequestProperty("Origin", "https://tubitv.com")
            conn.setRequestProperty("Referer", "https://tubitv.com/")
            conn.setRequestProperty("Cookie", cookie)
            val code = conn.responseCode
            val body = try {
                conn.inputStream.bufferedReader().readText()
            } catch (_: Exception) {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            }
            conn.disconnect()
            if (code !in 200..299 || body.isEmpty()) null else JSONObject(body)
        } catch (_: Exception) {
            null
        }
    }

    private fun pickToken(json: JSONObject?): String? {
        if (json == null) return null
        for (key in listOf("access_token", "accessToken", "token")) {
            if (json.has(key)) {
                val v = json.optString(key, "")
                if (v.isNotEmpty()) return v
            }
        }
        return null
    }
}
