package com.tvtime.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MyStuffFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mystuff, container, false)
        val recycler = view.findViewById<RecyclerView>(R.id.rv_mystuff)
        recycler?.layoutManager = LinearLayoutManager(requireContext())
        val fallback = listOf(
            ShowItem("Godzilla (1954)", "In Library · Film", 0),
            ShowItem("The 3 Stooges", "Saved · Collection", 0),
            ShowItem("Insidious", "Saved · Film", 0),
            ShowItem("Hercules: The Legendary Journeys", "In Library · S3", 0),
            ShowItem("Death Wish", "Saved · Film", 0),
            ShowItem("Sailor Moon", "Saved · S1", 0)
        )
        recycler?.adapter = ShowListAdapter(fallback)

        if (TubiAccount.isLoggedIn()) {
            TubiRepository.fetchMyStuff { items ->
                val list = items ?: fallback
                recycler?.post {
                    recycler.adapter = ShowListAdapter(list)
                }
            }
        }
        return view
    }
}
