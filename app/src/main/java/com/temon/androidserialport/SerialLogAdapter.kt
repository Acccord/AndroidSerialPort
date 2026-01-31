package com.temon.androidserialport

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

data class SerialLog(
    val time: String,
    val direction: Direction,
    val content: String,
    val title: String? = null
)

enum class Direction {
    TX,
    RX
}

class SerialLogAdapter(
    private val items: MutableList<SerialLog>,
    private val onClick: (SerialLog) -> Unit
) : RecyclerView.Adapter<SerialLogAdapter.LogViewHolder>() {
    private var showTime: Boolean = true
    private var showTitle: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_serial_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, showTime, showTitle, onClick)
    }

    override fun getItemCount(): Int = items.size

    fun add(item: SerialLog) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    fun setShowTime(show: Boolean) {
        if (showTime != show) {
            showTime = show
            notifyDataSetChanged()
        }
    }

    fun setShowTitle(show: Boolean) {
        if (showTitle != show) {
            showTitle = show
            notifyDataSetChanged()
        }
    }

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLog: TextView = itemView.findViewById(R.id.mTvLog)

        fun bind(
            item: SerialLog,
            showTime: Boolean,
            showTitle: Boolean,
            onClick: (SerialLog) -> Unit
        ) {
            val arrow = if (item.direction == Direction.TX) "▶" else "◀"
            val dirText = if (item.direction == Direction.TX) "TX" else "RX"
            val title = if (showTitle) {
                item.title?.takeIf { it.isNotBlank() }
            } else {
                null
            }
            val displayContent = if (title == null) {
                item.content
            } else {
                "$title: ${item.content}"
            }
            tvLog.text = if (showTime) {
                "[${item.time}] $arrow $dirText: $displayContent"
            } else {
                "$arrow $dirText: $displayContent"
            }
            val colorRes = if (item.direction == Direction.TX) {
                R.color.sendText
            } else {
                R.color.receiveText
            }
            tvLog.setTextColor(ContextCompat.getColor(itemView.context, colorRes))
            itemView.setOnClickListener { onClick(item) }
        }
    }
}
