package com.guet.stu.banamusic.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.guet.stu.banamusic.R
import com.guet.stu.banamusic.model.music.LyricLine

/**
 * 简单的歌词适配器，点击任意一行可触发外部回调（用于切换回唱片视图）。
 */
class LyricsAdapter(
    private val onLineClick: (() -> Unit)? = null
) : RecyclerView.Adapter<LyricsAdapter.LyricViewHolder>() {

    private val items = mutableListOf<LyricLine>()
    private var currentIndex: Int = -1

    fun submitList(newItems: List<LyricLine>) {
        items.clear()
        items.addAll(newItems)
        currentIndex = -1
        notifyDataSetChanged()
    }

    /**
     * 更新当前高亮行索引，只刷新前后两行，避免整列表闪烁。
     */
    fun updateCurrentIndex(index: Int) {
        if (index == currentIndex) return
        val previous = currentIndex
        currentIndex = index
        if (previous >= 0) {
            notifyItemChanged(previous)
        }
        if (index >= 0) {
            notifyItemChanged(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LyricViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lyric, parent, false)
        return LyricViewHolder(view)
    }

    override fun onBindViewHolder(holder: LyricViewHolder, position: Int) {
        val line = items[position]
        val isActive = position == currentIndex
        holder.bind(line, isActive)
        holder.itemView.setOnClickListener {
            onLineClick?.invoke()
        }
    }

    override fun getItemCount(): Int = items.size

    class LyricViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_lyric_line)
        private val defaultColor: Int = textView.currentTextColor
        private val highlightColor: Int = Color.parseColor("#FF69B4") // 粉色

        fun bind(line: LyricLine, isActive: Boolean) {
            textView.text = line.text
            textView.setTextColor(if (isActive) highlightColor else defaultColor)
        }
    }
}


