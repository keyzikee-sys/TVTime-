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
        ShowItem("Naruto", "S4 · E40 — The Ultimate Secret", 65),
        ShowItem("Stargate SG-1", "S7 · E12 — Evolution", 80),
        ShowItem("Columbo", "S2 · E5 — The Greenhouse Jungle", 40),
        ShowItem("Dragon Ball Z", "S3 · E90 — Trunks Revealed", 55),
        ShowItem("Farscape", "S1 · E8 — That Old Black Magic", 30),
        ShowItem("Xena: Warrior Princess", "S4 · E10 — Crusader", 72),
        ShowItem("Paranormal Activity", "Film · 1h 39m", 90),
        ShowItem("One Piece", "S10 · E200 — The Light of Shandora", 25)
    )

    private fun openService(context: android.content.Context, service: StreamingServices.Service) {
        context.startActivity(StreamingServices.createLaunchIntent(context, service))
    }
}
