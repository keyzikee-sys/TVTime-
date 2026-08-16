package com.tvtime.app

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private lateinit var tabs: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val watchListTab = findViewById<Button>(R.id.tab_watchlist)
        val myStuffTab = findViewById<Button>(R.id.tab_mystuff)
        val configTab = findViewById<Button>(R.id.tab_config)
        tabs = listOf(watchListTab, myStuffTab, configTab)

        val fragments = listOf(
            WatchListFragment(),
            MyStuffFragment(),
            WidgetConfigFragment()
        )

        val onTabClick = { index: Int ->
            selectTab(index)
            loadFragment(fragments[index])
        }

        watchListTab.setOnClickListener { onTabClick(0) }
        myStuffTab.setOnClickListener { onTabClick(1) }
        configTab.setOnClickListener { onTabClick(2) }

        if (savedInstanceState == null) {
            selectTab(0)
            loadFragment(fragments[0])
        }
    }

    private fun selectTab(index: Int) {
        tabs.forEachIndexed { i, button ->
            val selected = i == index
            button.setTextColor(if (selected) 0xFFFF1493.toInt() else 0xFFFFFFFF.toInt())
            button.paint.isFakeBoldText = selected
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
