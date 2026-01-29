package com.temon.androidserialport

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CommonCommandAdapter(
    private val items: MutableList<String>,
    private val onClick: (String) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<CommonCommandAdapter.CommandViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommandViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_common_command, parent, false)
        return CommandViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommandViewHolder, position: Int) {
        holder.bind(items[position], onClick, onDelete)
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class CommandViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCommand: TextView = itemView.findViewById(R.id.mTvCommand)
        private val tvDelete: TextView = itemView.findViewById(R.id.mTvDelete)

        fun bind(command: String, onClick: (String) -> Unit, onDelete: (String) -> Unit) {
            tvCommand.text = command
            itemView.setOnClickListener { onClick(command) }
            tvDelete.setOnClickListener { onDelete(command) }
        }
    }
}
