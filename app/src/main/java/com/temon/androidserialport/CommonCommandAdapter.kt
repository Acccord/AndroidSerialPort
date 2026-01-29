package com.temon.androidserialport

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CommonCommandAdapter(
    private val items: MutableList<CommonCommand>,
    private val onClick: (CommonCommand) -> Unit,
    private val onEdit: (CommonCommand) -> Unit,
    private val onDelete: (CommonCommand) -> Unit
) : RecyclerView.Adapter<CommonCommandAdapter.CommandViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommandViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_common_command, parent, false)
        return CommandViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommandViewHolder, position: Int) {
        holder.bind(items[position], onClick, onEdit, onDelete)
    }

    override fun getItemCount(): Int = items.size

    fun setItems(newItems: List<CommonCommand>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class CommandViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.mTvTitle)
        private val tvCommand: TextView = itemView.findViewById(R.id.mTvCommand)
        private val tvEdit: TextView = itemView.findViewById(R.id.mTvEdit)
        private val tvDelete: TextView = itemView.findViewById(R.id.mTvDelete)

        fun bind(
            command: CommonCommand,
            onClick: (CommonCommand) -> Unit,
            onEdit: (CommonCommand) -> Unit,
            onDelete: (CommonCommand) -> Unit
        ) {
            val title = command.title?.trim().orEmpty()
            if (title.isBlank()) {
                tvTitle.visibility = View.GONE
            } else {
                tvTitle.text = title
                tvTitle.visibility = View.VISIBLE
            }
            tvCommand.text = command.content
            itemView.setOnClickListener { onClick(command) }
            tvEdit.setOnClickListener { onEdit(command) }
            tvDelete.setOnClickListener { onDelete(command) }
        }
    }
}
