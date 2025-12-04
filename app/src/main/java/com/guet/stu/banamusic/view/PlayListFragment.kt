package com.guet.stu.banamusic.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.guet.stu.banamusic.R
import com.guet.stu.banamusic.adapter.PlaylistSongAdapter
import com.guet.stu.banamusic.databinding.ActivityPlayListBinding
import com.guet.stu.banamusic.model.music.Music
import com.guet.stu.banamusic.util.applyStatusBarSpacer
import com.guet.stu.banamusic.viewmodel.PlayListActivityViewModel
import com.guet.stu.banamusic.viewmodel.SharedMusicViewModel

/**
 * 歌单详情 Fragment：展示某个歌单下的歌曲列表，并支持勾选删除等操作。
 * 使用 MainActivity 中的 NavHostFragment（android:id="@+id/content_container"）进行导航。
 */
class PlayListFragment : Fragment() {

    private var _binding: ActivityPlayListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlayListActivityViewModel by viewModels {
        PlayListActivityViewModel.Factory(requireActivity().application)
    }
    private val sharedMusicViewModel: SharedMusicViewModel by activityViewModels()

    private lateinit var musicAdapter: PlaylistSongAdapter
    private var selectionMode = false
    private var playlistId: Long = -1L
    private var playlistName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityPlayListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        playlistId = arguments?.getLong(ARG_PLAYLIST_ID, -1L) ?: -1L
        playlistName = arguments?.getString(ARG_PLAYLIST_NAME).orEmpty()
        if (playlistId <= 0L) {
            findNavController().popBackStack()
            return
        }

        // 使用 StatusBarUtils.applyStatusBarSpacer 调整顶部状态栏占位高度
        applyStatusBarSpacer(binding.statusBarSpace.root)

        setupToolbar()
        setupRecyclerView()

        viewModel.bindPlaylist(playlistId)
        viewModel.songs.observe(viewLifecycleOwner) { list ->
            musicAdapter.submitList(list)
        }
    }

    private fun setupToolbar() {
        binding.tvTitle.text = playlistName.ifEmpty { getString(R.string.app_name) }
        binding.btnBack.setOnClickListener {
            if (selectionMode) {
                exitSelectionMode()
            } else {
                findNavController().popBackStack()
            }
        }
        binding.btncheck.setOnClickListener { handleCheckClick() }
        // 加号按钮：跳转到“从最近播放添加”页面
        binding.btnAddMusic.setOnClickListener {
            val context = requireContext()
            val intent = android.content.Intent(context, HistorySelectActivity::class.java).apply {
                putExtra(HistorySelectActivity.EXTRA_PLAYLIST_ID, playlistId)
            }
            context.startActivity(intent)
        }
    }

    private fun setupRecyclerView() {
        musicAdapter = PlaylistSongAdapter { music: Music ->
            val currentList = viewModel.songs.value.orEmpty()
            val listForPlayer = if (currentList.isNotEmpty()) currentList else listOf(music)
            // 设置当前播放列表和当前歌曲
            sharedMusicViewModel.setMusicList(listForPlayer)
            sharedMusicViewModel.setCurrentMusic(music)

            // 使用 NavController 直接携带参数跳转到播放器 Fragment
            val bundle = Bundle().apply {
                putParcelable("music", music)
            }
            findNavController().navigate(R.id.musicPlayerFragment, bundle)
        }
        binding.rvPlaylistSongs.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = musicAdapter
        }
    }

    private fun handleCheckClick() {
        if (!selectionMode) {
            selectionMode = true
            musicAdapter.setSelectionMode(true)
            binding.btncheck.alpha = 0.6f
            return
        }
        val selectedIds = musicAdapter.getSelectedSongIds()
        if (selectedIds.isNotEmpty()) {
            viewModel.removeSelectedSongs(selectedIds)
        }
        exitSelectionMode()
    }

    private fun exitSelectionMode() {
        selectionMode = false
        musicAdapter.setSelectionMode(false)
        binding.btncheck.alpha = 1f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_PLAYLIST_ID = "playlistId"
        const val ARG_PLAYLIST_NAME = "playlistName"
    }
}


