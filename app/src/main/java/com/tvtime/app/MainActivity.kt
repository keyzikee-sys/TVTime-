package com.tvtime.app

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WatchlistStore.init(applicationContext)

        setContentView(R.layout.activity_main)

        val containerId = R.id.fragment_container

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    containerId,
                    MyStuffFragment()
                )
                .commit()
        }

        findViewById<View>(R.id.tab_mystuff)
            .setOnClickListener {
                showFragment(MyStuffFragment())
            }

        findViewById<View>(R.id.tab_syncing)
            .setOnClickListener {
                showFragment(SyncingFragment())
            }

        findViewById<View>(R.id.tab_config)
            .setOnClickListener {
                showFragment(WidgetConfigFragment())
            }

        findViewById<View>(R.id.tab_terror)
            .setOnClickListener {
                showFragment(TerrorOnTubiFragment())
            }
    }

    private fun showFragment(
        fragment: androidx.fragment.app.Fragment
    ) {
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragment_container,
                fragment
            )
            .commit()
    }
}