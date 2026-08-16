package com.tvtime.app

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MyStuffFragment : Fragment() {

    private var list: MutableList<ShowItem> = mutableListOf()
    private var adapter: ShowListAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mystuff, container, false)

        WatchlistStore.init(requireContext())
        list = WatchlistStore.getMyStuff()

        adapter = ShowListAdapter(list) { item ->
            list.remove(item)
            WatchlistStore.saveMyStuff(list)
            TVTimeWidgetProvider.updateAll(requireContext())
            adapter?.notifyDataSetChanged()
        }
        val recycler = view.findViewById<RecyclerView>(R.id.rv_mystuff)
        recycler?.layoutManager = LinearLayoutManager(requireContext())
        recycler?.adapter = adapter

        view.findViewById<Button>(R.id.btn_add_mystuff)
            ?.setOnClickListener { showAddDialog(WatchlistStore::saveMyStuff) }

        return view
    }

    private fun showAddDialog(onSave: (List<ShowItem>) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_show, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_title)
        val etSubtitle = dialogView.findViewById<EditText>(R.id.et_subtitle)
        val seek = dialogView.findViewById<SeekBar>(R.id.seek_progress)
        val tvProgress = dialogView.findViewById<TextView>(R.id.tv_progress_label)
        seek?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {
                tvProgress?.text = "Progress: $p%"
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = etTitle?.text?.toString()?.trim() ?: ""
                if (title.isEmpty()) return@setPositiveButton
                val subtitle = etSubtitle?.text?.toString()?.trim() ?: ""
                list.add(0, ShowItem(title, subtitle, seek?.progress ?: 0))
                onSave(list)
                TVTimeWidgetProvider.updateAll(requireContext())
                adapter?.notifyDataSetChanged()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
