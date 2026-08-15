package com.tvtime.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class WatchListFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_watchlist, container, false)
        val service = StreamingServices.byPackage(WidgetPreferences(requireContext()).selectedServicePackage)
            ?: StreamingServices.default()
        val button = view.findViewById<Button>(R.id.btn_open_tubi)
        button?.text = "Open ${service.name}"
        button?.setOnClickListener { openService(requireContext(), service) }
        return view
    }

    private fun openService(context: android.content.Context, service: StreamingServices.Service) {
        context.startActivity(StreamingServices.createLaunchIntent(context, service))
    }
}
