package com.tvtime.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ShowItem(
    val title: String,
    val subtitle: String,
    val progress: Int
)

class ShowListAdapter(private val items: List<ShowItem>) :
    RecyclerView.Adapter<ShowListAdapter.ShowViewHolder>() {

    class ShowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_show_title)
        val subtitle: TextView = view.findViewById(R.id.tv_show_subtitle)
        val progressLabel: TextView = view.findViewById(R.id.tv_show_progress_label)
        val progressBar: ProgressBar = view.findViewById(R.id.pb_show_progress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShowViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_show, parent, false)
        return ShowViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShowViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle
        holder.progressLabel.text = "${item.progress}%"
        holder.progressBar.progress = item.progress
    }

    override fun getItemCount(): Int = items.size
}
