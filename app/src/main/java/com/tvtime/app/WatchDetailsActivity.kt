package com.tvtime.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/** Launched from a widget list-row tap; opens the show's Tubi page in a browser. */
class WatchDetailsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent?.getStringExtra("watch_url")?.takeIf { it.isNotEmpty() }
            ?: "https://tubitv.com"
        try {
            startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
        }
        finish()
    }
}
