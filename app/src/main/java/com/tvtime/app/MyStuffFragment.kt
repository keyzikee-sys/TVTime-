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
        recycler?.adapter = ShowListAdapter(
            listOf(
                ShowItem("The Boys", "Saved · S3 · E8", 0),
                ShowItem("Peaky Blinders", "In Library · S6 · E6", 0),
                ShowItem("Foundation", "Downloaded · S2 · E1", 0),
                ShowItem("Invincible", "Saved · S1 · E8", 0),
                ShowItem("Shadow and Bone", "In Library · S2 · E4", 0)
            )
        )
        return view
    }
}
