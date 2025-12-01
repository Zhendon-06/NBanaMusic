package com.guet.stu.banamusic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.guet.stu.banamusic.R

/**
 * 简单的歌词适配器，点击任意一行可触发外部回调（用于切换回唱片视图）。
 */
class LyricsAdapter(
    private val onLineClick: (() -> Unit)? = null
) : RecyclerView.Adapter<LyricsAdapter.LyricViewHolder>() {

    private val items = mutableListOf<String>()

    fun submitList(newItems: List<String>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LyricViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lyric, parent, false)
        return LyricViewHolder(view)
    }

    override fun onBindViewHolder(holder: LyricViewHolder, position: Int) {
        holder.bind(items[position])
        holder.itemView.setOnClickListener {
            onLineClick?.invoke()
        }
    }

    override fun getItemCount(): Int = items.size

    class LyricViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.tv_lyric_line)

        fun bind(text: String) {
            textView.text = text
        }
    }
}


