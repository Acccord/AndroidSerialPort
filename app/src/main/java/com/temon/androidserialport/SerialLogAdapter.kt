package com.temon.androidserialport

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.style.ForegroundColorSpan
import android.text.style.MetricAffectingSpan
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

data class SerialLog(
    val time: String,
    val direction: Direction,
    val content: String,
    val title: String? = null,
    val isHex: Boolean = false
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
    private var showHexGroupBold: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_serial_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, showTime, showTitle, showHexGroupBold, onClick)
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

    fun setShowHexGroupBold(show: Boolean) {
        if (showHexGroupBold != show) {
            showHexGroupBold = show
            notifyDataSetChanged()
        }
    }

    class LogViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvLog: TextView = itemView.findViewById(R.id.mTvLog)

        fun bind(
            item: SerialLog,
            showTime: Boolean,
            showTitle: Boolean,
            showHexGroupBold: Boolean,
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
            val prefix = if (showTime) {
                "[${item.time}] $arrow $dirText: "
            } else {
                "$arrow $dirText: "
            }
            val fullText = prefix + displayContent
            if (showHexGroupBold && item.isHex) {
                val contentStartInDisplay = if (title == null) 0 else title.length + 2
                val contentStart = prefix.length + contentStartInDisplay
                val spannable = SpannableString(fullText)
                val markerColorRes = if (item.direction == Direction.TX) {
                    R.color.sendTextEmphasis
                } else {
                    R.color.receiveTextEmphasis
                }
                val markerColor = ContextCompat.getColor(itemView.context, markerColorRes)
                applyHexByteMarker(spannable, contentStart, item.content, markerColor)
                tvLog.text = spannable
            } else {
                tvLog.text = fullText
            }
            val colorRes = if (item.direction == Direction.TX) {
                R.color.sendText
            } else {
                R.color.receiveText
            }
            tvLog.setTextColor(ContextCompat.getColor(itemView.context, colorRes))
            itemView.setOnClickListener { onClick(item) }
        }

        private fun applyHexByteMarker(
            spannable: SpannableString,
            startOffset: Int,
            content: String,
            markerColor: Int
        ) {
            var digitCount = 0
            var byteIndex = 0
            var byteStart = -1
            for (i in content.indices) {
                val c = content[i]
                val isHexDigit = (c in '0'..'9') || (c in 'a'..'f') || (c in 'A'..'F')
                if (isHexDigit) {
                    digitCount++
                    if (digitCount % 2 == 1) {
                        byteStart = i
                    }
                    if (digitCount % 2 == 0) {
                        byteIndex++
                        if (byteIndex % 5 == 0 && byteStart >= 0) {
                            val spanStart = startOffset + byteStart
                            val spanEnd = startOffset + i + 1
                            if (spanStart >= 0 && spanEnd > spanStart) {
                                spannable.setSpan(
                                    HexMarkerBoldSpan(),
                                    spanStart,
                                    spanEnd,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                                spannable.setSpan(
                                    ForegroundColorSpan(markerColor),
                                    spanStart,
                                    spanEnd,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }
                        }
                    }
                }
            }
        }

        private class HexMarkerBoldSpan : MetricAffectingSpan() {
            override fun updateDrawState(tp: TextPaint) {
                tp.isFakeBoldText = true
            }

            override fun updateMeasureState(tp: TextPaint) {
                tp.isFakeBoldText = true
            }
        }
    }
}
