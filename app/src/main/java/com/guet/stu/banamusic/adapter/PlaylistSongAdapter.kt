package com.guet.stu.banamusic.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.guet.stu.banamusic.R
import com.guet.stu.banamusic.databinding.AlbumItemBinding
import com.guet.stu.banamusic.model.music.Music

/**
 * 歌单详情页专用 Adapter，支持勾选歌曲并删除。
 */
class PlaylistSongAdapter(
    private val onItemClick: (Music) -> Unit
) : ListAdapter<Music, PlaylistSongAdapter.PlaylistSongViewHolder>(MusicDiffCallback) {

    private var selectionMode = false
    private val selectedIds = linkedSetOf<Long>()

    fun setSelectionMode(enabled: Boolean) {
        if (selectionMode == enabled) return
        selectionMode = enabled
        if (!enabled) {
            selectedIds.clear()
        }
        notifyDataSetChanged()
    }

    fun isSelectionMode(): Boolean = selectionMode

    fun toggleSelection(musicId: Long) {
        if (!selectionMode) return
        if (selectedIds.contains(musicId)) {
            selectedIds.remove(musicId)
        } else {
            selectedIds.add(musicId)
        }
        notifyDataSetChanged()
    }

    fun getSelectedSongIds(): List<Long> = selectedIds.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistSongViewHolder {
        val binding = AlbumItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaylistSongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistSongViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, selectionMode, selectedIds.contains(item.id), onItemClick) { musicId, checked ->
            if (checked) {
                selectedIds.add(musicId)
            } else {
                selectedIds.remove(musicId)
            }
        }
    }

    class PlaylistSongViewHolder(
        private val binding: AlbumItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: Music,
            selectionMode: Boolean,
            isSelected: Boolean,
            onItemClick: (Music) -> Unit,
            onCheckChanged: (Long, Boolean) -> Unit
        ) = with(binding) {
            root.tag = item
            musicItemSong.text = item.song
            musicItemSing.text = item.sing
            loadWithPlaceholder(item.pic)

            checkBox.visibility = if (selectionMode) View.VISIBLE else View.GONE
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = isSelected
            checkBox.setOnCheckedChangeListener { _, checked ->
                onCheckChanged(item.id, checked)
            }

            root.setOnClickListener {
                if (selectionMode) {
                    checkBox.isChecked = !checkBox.isChecked
                } else {
                    onItemClick(item)
                }
            }
        }

        private fun AlbumItemBinding.loadWithPlaceholder(url: String) {
            musicItemPic.load(url) {
                crossfade(true)
                placeholder(R.drawable.music)
                error(R.drawable.music)
            }
        }
    }
}

private object MusicDiffCallback : DiffUtil.ItemCallback<Music>() {
    override fun areItemsTheSame(oldItem: Music, newItem: Music): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Music, newItem: Music): Boolean =
        oldItem == newItem
}


