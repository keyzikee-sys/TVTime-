package com.tvtime.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class WatchListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_watchlist, container, false)

        val service = StreamingServices.byPackage(
            WidgetPreferences(requireContext()).selectedServicePackage
        ) ?: StreamingServices.default()

        val button = view.findViewById<Button>(R.id.btn_open_tubi)
        button?.text = "Open ${service.name}"
        button?.setOnClickListener { openService(requireContext(), service) }

        val recycler = view.findViewById<RecyclerView>(R.id.rv_watchlist)
        recycler?.layoutManager = LinearLayoutManager(requireContext())
        recycler?.adapter = ShowListAdapter(sampleShows())

        if (TubiAccount.isLoggedIn()) {
            TubiRepository.fetchWatchlist { items ->
                val list = items ?: sampleShows()
                recycler?.post {
                    recycler.adapter = ShowListAdapter(list)
                }
            }
        }

        return view
    }

    private fun sampleShows(): List<ShowItem> = listOf(
        ShowItem("Stranger Things", "S4 · E7 — The Massacre at Hawkins Lab", 72),
        ShowItem("The Bear", "S2 · E3 — Sundae", 45),
        ShowItem("Severance", "S1 · E9 — The We We Are", 90),
        ShowItem("Wednesday", "S1 · E6 — You Reaper", 30),
        ShowItem("Dark", "S3 · E8 — The Paradise", 60),
        ShowItem("Arcane", "S1 · E9 — The Monster You Created", 100),
        ShowItem("The Last of Us", "S1 · E5 — Endure and Survive", 18),
        ShowItem("Loki", "S2 · E2 — Breaking Brad", 55)
    )

    private fun openService(context: android.content.Context, service: StreamingServices.Service) {
        context.startActivity(StreamingServices.createLaunchIntent(context, service))
    }
}
