package com.guet.stu.banamusic.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.guet.stu.banamusic.R
import com.guet.stu.banamusic.databinding.ItemMainBinding
import com.guet.stu.banamusic.model.music.Music

/**
 * “猜你喜欢”列表的专用 Adapter，只关心首页 live 区域数据。
 */
class LiveMusicAdapter(
    private val onItemClick: ((Music) -> Unit)? = null
) : RecyclerView.Adapter<LiveMusicAdapter.LiveMusicViewHolder>() {

    private val items = mutableListOf<Music>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LiveMusicViewHolder {
        val binding = ItemMainBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LiveMusicViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: LiveMusicViewHolder, position: Int) {
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

    class LiveMusicViewHolder(
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

