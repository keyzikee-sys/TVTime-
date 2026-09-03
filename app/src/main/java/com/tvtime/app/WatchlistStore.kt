package com.tvtime.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores the user's TVTime lists.
 *
 * Lists:
 * - Watchlist
 * - My Stuff
 * - Terror on Tubi
 */
object WatchlistStore {

    private const val PREFS = "TVTimeWatchlists"

    private const val KEY_WATCH = "watchlist_items"
    private const val KEY_MY = "mystuff_items"
    private const val KEY_TERROR = "terror_on_tubi_items"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

        seedTerrorIfNeeded()
    }

    fun getWatchlist(): MutableList<ShowItem> {
        return load(KEY_WATCH)
    }

    fun getMyStuff(): MutableList<ShowItem> {
        return load(KEY_MY)
    }

    fun getTerrorOnTubi(): MutableList<ShowItem> {
        return load(KEY_TERROR)
    }

    fun saveWatchlist(list: List<ShowItem>) {
        save(KEY_WATCH, list)
    }

    fun saveMyStuff(list: List<ShowItem>) {
        save(KEY_MY, list)
    }

    fun saveTerrorOnTubi(list: List<ShowItem>) {
        save(KEY_TERROR, list)
    }

    private fun load(key: String): MutableList<ShowItem> {

        val arr = try {
            JSONArray(
                prefs.getString(key, "[]")
            )
        } catch (_: Exception) {
            JSONArray()
        }

        val out = mutableListOf<ShowItem>()

        for (i in 0 until arr.length()) {

            val o = arr.optJSONObject(i)
                ?: continue

            out.add(
                ShowItem(
                    title = o.optString("title", ""),
                    subtitle = o.optString("subtitle", ""),
                    progress = o.optInt("progress", 0).coerceIn(0, 100),
                    description = o.optString("description", ""),
                    imageUrl = o.optString("imageUrl", ""),
                    watchUrl = o.optString("watchUrl", "")
                )
            )
        }

        return out
    }

    private fun save(
        key: String,
        list: List<ShowItem>
    ) {

        val arr = JSONArray()

        list.forEach { item ->

            arr.put(
                JSONObject().apply {

                    put("title", item.title)
                    put("subtitle", item.subtitle)
                    put("progress", item.progress)
                                        put("description", item.description)
put("imageUrl", item.imageUrl)
                    put("watchUrl", item.watchUrl)
                }
            )
        }

        prefs.edit()
            .putString(key, arr.toString())
            .commit()
    }

    private fun seedTerrorIfNeeded() {

        if (prefs.contains(KEY_TERROR)) {
            return
        }

        saveTerrorOnTubi(
            seedTerror()
        )
    }

    private fun seedWatch() = listOf(

        ShowItem(
            "Naruto",
            "S4 · E40 — The Ultimate Secret",
            65
        ),

        ShowItem(
            "Stargate SG-1",
            "S7 · E12 — Evolution",
            80
        ),

        ShowItem(
            "Columbo",
            "S2 · E5 — The Greenhouse Jungle",
            40
        ),

        ShowItem(
            "Dragon Ball Z",
            "S3 · E90 — Trunks Revealed",
            55
        ),

        ShowItem(
            "Farscape",
            "S1 · E8 — That Old Black Magic",
            30
        ),

        ShowItem(
            "Xena: Warrior Princess",
            "S4 · E10 — Crusader",
            72
        ),

        ShowItem(
            "Paranormal Activity",
            "Film · 1h 39m",
            90
        ),

        ShowItem(
            "One Piece",
            "S10 · E200 — The Light of Shandora",
            25
        )
    )

    private fun seedMy() = listOf(

        ShowItem(
            "Godzilla (1954)",
            "In Library · Film",
            0
        ),

        ShowItem(
            "The 3 Stooges",
            "Saved · Collection",
            0
        ),

        ShowItem(
            "Insidious",
            "Saved · Film",
            0
        ),

        ShowItem(
            "Hercules: The Legendary Journeys",
            "In Library · S3",
            0
        ),

        ShowItem(
            "Death Wish",
            "Saved · Film",
            0
        ),

        ShowItem(
            "Sailor Moon",
            "Saved · S1",
            0
        )
    )

    /**
     * Initial Terror on Tubi collection.
     *
     * These are local widget entries for now.
     * They can later be replaced with real Tubi/API data.
     */
    private fun seedTerror() = listOf(

        ShowItem(
            "Insidious",
            "Horror · Film",
            0
        ),

        ShowItem(
            "Paranormal Activity",
            "Horror · Film",
            0
        ),

        ShowItem(
            "The Taking of Deborah Logan",
            "Horror · Film",
            0
        ),

        ShowItem(
            "Hell House LLC",
            "Horror · Film",
            0
        ),

        ShowItem(
            "The Houses October Built",
            "Horror · Film",
            0
        ),

        ShowItem(
            "The Collector",
            "Horror · Film",
            0
        )
    )
}