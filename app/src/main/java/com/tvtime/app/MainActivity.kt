package com.tvtime.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WatchlistStore.init(applicationContext)

        val layoutId = resources.getIdentifier("activity_main", "layout", packageName).takeIf { it != 0 }
            ?: android.R.layout.simple_list_item_1
        setContentView(layoutId)

        val containerId = resources.getIdentifier("fragment_container", "id", packageName).takeIf { it != 0 }
            ?: resources.getIdentifier("content_frame", "id", packageName).takeIf { it != 0 }
            ?: 0

        if (savedInstanceState == null && containerId != 0) {
            try {
                supportFragmentManager.beginTransaction()
                    .replace(containerId, MyStuffFragment())
                    .commit()
            } catch (_: Exception) {}
        }

        setupNav("tab_mystuff", containerId) { MyStuffFragment() }
        setupNav("tab_syncing", containerId) { SyncingFragment() }
        setupNav("tab_config", containerId) { WidgetConfigFragment() }
    }

    private fun setupNav(idName: String, containerId: Int, fragmentCreator: () -> androidx.fragment.app.Fragment) {
        if (containerId == 0) return
        val resId = resources.getIdentifier(idName, "id", packageName)
        if (resId != 0) {
            findViewById<View>(resId)?.setOnClickListener {
                try {
                    supportFragmentManager.beginTransaction()
                        .replace(containerId, fragmentCreator())
                        .commit()
                } catch (_: Exception) {}
            }
        }
    }
}
