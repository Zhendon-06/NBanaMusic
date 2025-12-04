package com.guet.stu.banamusic.view

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.guet.stu.banamusic.R
import com.guet.stu.banamusic.adapter.PlaylistSongAdapter
import com.guet.stu.banamusic.databinding.ActivityHistorySelectBinding
import com.guet.stu.banamusic.viewmodel.HistorySelectViewModel

/**
 * 从“最近播放”（HISTORY 特殊歌单）中批量选择歌曲，加入指定歌单。
 */
class HistorySelectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorySelectBinding
    private val viewModel: HistorySelectViewModel by viewModels()

    private lateinit var musicAdapter: PlaylistSongAdapter
    private var targetPlaylistId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistorySelectBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        targetPlaylistId = intent.getLongExtra(EXTRA_PLAYLIST_ID, -1L)
        if (targetPlaylistId <= 0L) {
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()

        viewModel.historySongs.observe(this) { list ->
            musicAdapter.submitList(list)
        }
    }

    private fun setupToolbar() {
        binding.tvTitle.text = getString(R.string.add_from_history)
        binding.btnBack.setOnClickListener { finish() }
        binding.btnConfirm.setOnClickListener {
            val selectedIds = musicAdapter.getSelectedSongIds()
            if (selectedIds.isNotEmpty()) {
                viewModel.addSelectedToPlaylist(targetPlaylistId, selectedIds)
            }
            finish()
        }
    }

    private fun setupRecyclerView() {
        musicAdapter = PlaylistSongAdapter { /* 点击单个历史歌曲，这里只负责选中，不做跳转 */ }
        musicAdapter.setSelectionMode(true)
        binding.rvHistorySongs.apply {
            layoutManager = LinearLayoutManager(this@HistorySelectActivity)
            adapter = musicAdapter
        }
    }

    companion object {
        const val EXTRA_PLAYLIST_ID = "extra_playlist_id"
    }
}


