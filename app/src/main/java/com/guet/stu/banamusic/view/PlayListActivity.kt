package com.guet.stu.banamusic.view

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.guet.stu.banamusic.R
import com.guet.stu.banamusic.adapter.MusicListAdapter
import com.guet.stu.banamusic.databinding.ActivityPlayListBinding
import com.guet.stu.banamusic.model.music.Music
import com.guet.stu.banamusic.viewmodel.PlayListActivityViewModel

/**
 * 歌单详情页：展示某个歌单下的歌曲列表，并支持后续扩展“从歌单播放”等功能。
 */
class PlayListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PLAYLIST_ID = "extra_playlist_id"
        const val EXTRA_PLAYLIST_NAME = "extra_playlist_name"
    }

    private lateinit var binding: ActivityPlayListBinding
    private val viewModel: PlayListActivityViewModel by viewModels()

    private lateinit var musicAdapter: MusicListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayListBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val pid = intent.getLongExtra(EXTRA_PLAYLIST_ID, -1L)
        val name = intent.getStringExtra(EXTRA_PLAYLIST_NAME).orEmpty()

        if (pid <= 0L) {
            finish()
            return
        }

        setupToolbar(name)
        setupRecyclerView()
        viewModel.bindPlaylist(pid)

        viewModel.songs.observe(this) { list ->
            musicAdapter.submitList(list)
        }
    }

    private fun setupToolbar(name: String) {
        binding.tvTitle.text = name
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        musicAdapter = MusicListAdapter { music: Music ->
            // TODO: 根据需要实现：点击歌单中的歌曲，跳转全屏播放器并播放该歌单
        }
        binding.rvPlaylistSongs.apply {
            layoutManager = LinearLayoutManager(this@PlayListActivity)
            adapter = musicAdapter
        }
    }
}