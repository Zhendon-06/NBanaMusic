package com.guet.stu.banamusic.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.guet.stu.banamusic.databinding.AlbumItemBinding
import com.guet.stu.banamusic.model.music.Music
import com.guet.stu.banamusic.R

class MusicListAdapter(
    private val onItemClick: (Music) -> Unit
) : ListAdapter<Music, MusicListAdapter.MusicViewHolder>(DiffCallback) {
//M=页面能显示的item数量+两三条缓存
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val binding = AlbumItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MusicViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MusicViewHolder(
        private val binding: AlbumItemBinding,
        private val onItemClick: (Music) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        // 点击事件只创建一次
        init {
            binding.root.setOnClickListener {
                val item = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?.let { pos -> (binding.root.tag as? Music) }
                if(item!=null){
                    onItemClick(item)
                }
            }
        }

        fun bind(item: Music) = with(binding) {
            // 用 tag 记录当前 item，方便点击事件拿到
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

    private object DiffCallback : DiffUtil.ItemCallback<Music>() {
        override fun areItemsTheSame(oldItem: Music, newItem: Music): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Music, newItem: Music): Boolean =
            oldItem == newItem
    }
}
