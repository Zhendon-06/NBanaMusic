package com.guet.stu.banamusic.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.guet.stu.banamusic.R
import com.guet.stu.banamusic.databinding.ItemMainBinding
import com.guet.stu.banamusic.model.music.Music

/**
 * “每日精选”列表专用 Adapter，与 LiveMusicAdapter 完全独立。
 */
class DayMusicAdapter(
    private val onItemClick: ((Music) -> Unit)? = null
) : RecyclerView.Adapter<DayMusicAdapter.DayMusicViewHolder>() {

    private val items = mutableListOf<Music>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayMusicViewHolder {
        val binding = ItemMainBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DayMusicViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: DayMusicViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun submitList(list: List<Music>?) {
        items.clear()
        if (!list.isNullOrEmpty()) {
            items.addAll(list)
        }
        notifyDataSetChanged()
    }

    class DayMusicViewHolder(
        private val binding: ItemMainBinding,
        private val onItemClick: ((Music) -> Unit)?
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val item = bindingAdapterPosition
                    .takeIf { it != RecyclerView.NO_POSITION }
                    ?.let { binding.root.tag as? Music }
                if (item != null) {
                    onItemClick?.invoke(item)
                }
            }
        }

        fun bind(item: Music) = with(binding) {
            root.tag = item
            musicItemSong.text = item.song
            musicItemSing.text = item.sing
            musicItemPic.load(item.pic) {
                crossfade(true)
                placeholder(R.drawable.music)
                error(R.drawable.music)
            }
        }
    }
}

