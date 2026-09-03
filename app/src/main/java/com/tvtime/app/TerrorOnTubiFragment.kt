package com.tvtime.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment

class TerrorOnTubiFragment : Fragment() {

    private lateinit var listContainer: LinearLayout
    private lateinit var loading: ProgressBar
    private lateinit var statusText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val context = requireContext()

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0B0B12"))
            setPadding(16, 20, 16, 16)
        }

        val header = TextView(context).apply {
            text = "TERROR ON TUBI"
            setTextColor(Color.parseColor("#FFFF13"))
            textSize = 24f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(4, 0, 4, 4)
        }

        root.addView(
            header,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val subtitle = TextView(context).apply {
            text = "Horror movies and shows available on Tubi"
            setTextColor(Color.parseColor("#AEB6C2"))
            textSize = 13f
            setPadding(4, 0, 4, 16)
        }

        root.addView(
            subtitle,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        statusText = TextView(context).apply {
            text = "Loading Terror on Tubi..."
            setTextColor(Color.parseColor("#FFFF13"))
            textSize = 14f
            setPadding(4, 16, 4, 16)
        }

        root.addView(
            statusText,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        loading = ProgressBar(context)

        root.addView(
            loading,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
        )

        val scrollView = android.widget.ScrollView(context)

        listContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }

        scrollView.addView(
            listContainer,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        loadTerror()

        return root
    }

    private fun loadTerror() {

        TubiRepository.fetchTerrorOnTubi { results: List<ShowItem>?, error: String ->

            activity?.runOnUiThread {

                loading.visibility = View.GONE
                listContainer.removeAllViews()

                if (results == null || results.isEmpty()) {

                    statusText.text =
                        if (error.isNotBlank()) {
                            error
                        } else {
                            "No Terror on Tubi titles found."
                        }

                    return@runOnUiThread
                }

                statusText.text =
                    "${results.size} titles available"

                results.forEach { item ->
                    addTerrorCard(item)
                }
            }
        }
    }

    private fun addTerrorCard(item: ShowItem) {

        val context = requireContext()

        val card = LayoutInflater
            .from(context)
            .inflate(
                R.layout.terror_item,
                listContainer,
                false
            )

        val title =
            card.findViewById<TextView>(
                R.id.terror_title
            )

        val subtitle =
            card.findViewById<TextView>(
                R.id.terror_subtitle
            )

        title.text = item.title
        subtitle.text =
            if (item.subtitle.isBlank()) {
                "Available on Tubi"
            } else {
                item.subtitle
            }

        card.setOnClickListener {

            val intent = Intent(
                context,
                WatchDetailsActivity::class.java
            ).apply {
                putExtra(
                    "watch_url",
                    item.watchUrl
                )
            }

            startActivity(intent)
        }

        listContainer.addView(card)
    }
}