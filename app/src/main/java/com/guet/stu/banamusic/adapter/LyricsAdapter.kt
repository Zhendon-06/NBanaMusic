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
 * 歌词适配器，支持高亮显示、平滑滚动和点击定位
 */
class LyricsAdapter(
    private val onLineClick: ((line: LyricLine, position: Int) -> Unit)? = null
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
            onLineClick?.invoke(line, position)
        }
    }

    override fun getItemCount(): Int = items.size

    class LyricViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_lyric_line)
        private val defaultColor: Int = Color.parseColor("#888888") // 灰色
        private val highlightColor: Int = Color.parseColor("#DB7093") // 粉色
        private val defaultSize: Float = 16f
        private val highlightSize: Float = 20f

        fun bind(line: LyricLine, isActive: Boolean) {
            textView.text = line.text
            if (isActive) {
                textView.setTextColor(highlightColor)
                textView.textSize = highlightSize
                textView.setTypeface(textView.typeface, android.graphics.Typeface.BOLD)
            } else {
                textView.setTextColor(defaultColor)
                textView.textSize = defaultSize
                textView.setTypeface(textView.typeface, android.graphics.Typeface.NORMAL)
            }
        }
    }
}
