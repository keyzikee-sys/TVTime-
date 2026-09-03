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
    val progress: Int,
    val description: String = "",
    val imageUrl: String = "",
    val watchUrl: String = ""
)

class ShowListAdapter(
    private val items: MutableList<ShowItem>,
    private val onDelete: (ShowItem) -> Unit
) : RecyclerView.Adapter<ShowListAdapter.ShowViewHolder>() {

    class ShowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tv_show_title)
        val subtitle: TextView = view.findViewById(R.id.tv_show_subtitle)
        val description: TextView = view.findViewById(R.id.tv_show_description)
        val progressLabel: TextView = view.findViewById(R.id.tv_show_progress_label)
        val progressBar: ProgressBar = view.findViewById(R.id.pb_show_progress)
        val delete: TextView = view.findViewById(R.id.tv_delete)
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
        holder.description.text = item.description
        holder.progressLabel.text = "${item.progress}%"
        holder.progressBar.progress = item.progress
        holder.delete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size
}
