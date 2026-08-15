package com.tvtime.app

import android.content.Intent
import android.net.Uri
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
        view.findViewById<Button>(R.id.btn_open_tubi)?.setOnClickListener {
            openTubi(requireContext())
        }
        return view
    }

    private fun openTubi(context: android.content.Context) {
        var launchIntent = context.packageManager.getLaunchIntentForPackage("com.tubitv")
        if (launchIntent == null) {
            launchIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.tubitv")
            )
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }
}
