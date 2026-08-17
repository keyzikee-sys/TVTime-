package com.tvtime.app

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private lateinit var tabs: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        TubiAccount.init(this)
        WatchlistStore.init(this)
        Toast.makeText(this, "BUILD 1712 count-check", Toast.LENGTH_SHORT).show()

        val myStuffTab = findViewById<Button>(R.id.tab_mystuff)
        val syncingTab = findViewById<Button>(R.id.tab_syncing)
        val configTab = findViewById<Button>(R.id.tab_config)
        tabs = listOf(myStuffTab, syncingTab, configTab)

        val fragments = listOf(
            MyStuffFragment(),
            SyncingFragment(),
            WidgetConfigFragment()
        )

        val onTabClick = { index: Int ->
            selectTab(index)
            loadFragment(fragments[index])
        }

        myStuffTab.setOnClickListener { onTabClick(0) }
        syncingTab.setOnClickListener { onTabClick(1) }
        configTab.setOnClickListener { onTabClick(2) }

        if (savedInstanceState == null) {
            selectTab(0)
            loadFragment(fragments[0])
        }
    }

    private fun selectTab(index: Int) {
        tabs.forEachIndexed { i, button ->
            val selected = i == index
            button.setTextColor(if (selected) 0xFF4DD0E1.toInt() else 0xFFFFFFFF.toInt())
            button.paint.isFakeBoldText = selected
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
