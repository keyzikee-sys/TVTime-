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
 * which returns a `connect.sid` session cookie. Personal-library endpoints (watchlist, continue
 * watching) are wrapped in HMAC-signed requests and are not reliably callable, so this only stores
 * the session and, if the login response carries an access token / user id, those too. Everything
 * is best-effort: if a later fetch fails we simply fall back to the curated list.
 *
 * Note: we store only the session cookie + access token, never the user's password.
 */
object TubiAccount {
    private const val PREFS = "TVTimeTubiAccount"
    private const val KEY_SESSION = "connect_sid"
    private const val KEY_AT = "access_token"
    private const val KEY_UID = "user_id"
    private const val KEY_EMAIL = "email"

    private lateinit var prefs: SharedPreferences

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
                val conn = URL("https://tubitv.com/oz/auth/login/").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36")
                conn.setRequestProperty("Origin", "https://tubitv.com")
                conn.setRequestProperty("Referer", "https://tubitv.com/login")
                conn.doOutput = true

                val body = JSONObject().apply {
                    put("username", email)
                    put("password", password)
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
                    prefs.edit().apply {
                        putString(KEY_SESSION, sid)
                        putString(KEY_EMAIL, email)
                        if (json != null) {
                            if (json.has("accessToken")) putString(KEY_AT, json.getString("accessToken"))
                            val user = if (json.has("user")) json.optJSONObject("user") else null
                            if (user != null && user.has("id")) putString(KEY_UID, user.getString("id"))
                        }
                        apply()
                    }
                    callback(true, null)
                } else {
                    callback(false, "Login failed (HTTP $code)")
                }
            } catch (e: Exception) {
                callback(false, e.message ?: "Network error")
            }
        }.start()
    }
}
