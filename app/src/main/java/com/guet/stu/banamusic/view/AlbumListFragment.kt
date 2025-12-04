package com.guet.stu.banamusic.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.guet.stu.banamusic.R
import com.guet.stu.banamusic.adapter.MusicListAdapter
import com.guet.stu.banamusic.databinding.FragmentAlbumListBinding
import com.guet.stu.banamusic.model.music.Music
import com.guet.stu.banamusic.util.applyStatusBarSpacer
import com.guet.stu.banamusic.viewmodel.AlbumFragmentViewModel
import com.guet.stu.banamusic.viewmodel.SharedMusicViewModel

/**
 * 专辑榜单列表页面：负责展示排行榜封面与歌曲列表，并处理跳转到播放器。
 */
class AlbumListFragment : Fragment() {

    private val viewModel: AlbumFragmentViewModel by viewModels()
    private val sharedMusicViewModel: SharedMusicViewModel by activityViewModels()
    private var _binding: FragmentAlbumListBinding? = null
    private val binding get() = _binding!!
    // 点击列表项就跳转播放器，因此在适配器构造时注入点击回调
    private val musicAdapter = MusicListAdapter { music -> navigateToMusicPlayer(music) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlbumListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()                       // 初始化 RecyclerView
        observeViewModel()                        // 监听数据、加载状态和错误
        applyStatusBarSpacer(binding.statusBarSpace.root) // 调整沉浸式状态栏占位

        // 根据榜单类型设置标题
        setupBangTitle()

        // 设置返回按钮点击事件
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // 顺序播放：从第一首开始
        binding.ivPlay.setOnClickListener {
            val list = viewModel.musicList.value
            val first = list?.firstOrNull()
            if (first == null) {
                Toast.makeText(requireContext(), getString(R.string.no_music_playing), Toast.LENGTH_SHORT).show()
            } else {
                // 将完整列表传给 SharedMusicViewModel，播放器页就能顺序/下一首播放
                val listForPlayer = if (!list.isNullOrEmpty()) list else listOf(first)
                sharedMusicViewModel.setMusicList(listForPlayer)
                navigateToMusicPlayer(first)
            }
        }

        // 随机播放：从当前列表中随机选一首
        binding.ivShuffle.setOnClickListener {
            val list = viewModel.musicList.value
            val randomMusic = list?.takeIf { it.isNotEmpty() }?.random()
            if (randomMusic == null) {
                Toast.makeText(requireContext(), getString(R.string.no_music_playing), Toast.LENGTH_SHORT).show()
            } else {
                sharedMusicViewModel.setMusicList(list)
                navigateToMusicPlayer(randomMusic)
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        // 垂直列表排布歌曲
        binding.rvMusicList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = musicAdapter
        }
    }

    /**
     * 根据榜单类型设置标题
     */
    private fun setupBangTitle() {
        val title = when (viewModel.albumType) {
            1 -> "原创榜"
            2 -> "热歌榜"
            3 -> "新歌榜"
            4 -> "飙升榜"
            else -> "榜"
        }
        binding.bang.text = title
    }

    private fun observeViewModel() {
        // 歌曲数据变化时刷新列表并取第一首歌作为封面
        viewModel.musicList.observe(viewLifecycleOwner) { list ->
            musicAdapter.submitList(list)
            val coverUrl = list?.firstOrNull()?.pic
            val firstMusic = list?.firstOrNull()

            // 更新专辑封面
            if (coverUrl != null) {
                binding.ivAlbumCover.load(coverUrl) {
                    crossfade(true)
                    placeholder(R.drawable.music)
                    error(R.drawable.music)
                }
            }

            // 更新顶部标题与歌手名
            if (firstMusic != null) {
                binding.tvAlbumTitle.text = firstMusic.song
                binding.tvArtist.text = firstMusic.sing
            }
        }

        // 加载时显示进度条，完成后还原列表
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressLoading.isVisible = loading
            binding.rvMusicList.isVisible = !loading
        }

        // 拉取失败弹出 toast 告知用户
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToMusicPlayer(music: Music) {
        val currentList = viewModel.musicList.value
        val listForPlayer = if (!currentList.isNullOrEmpty()) currentList else listOf(music)
        sharedMusicViewModel.setMusicList(listForPlayer)
        val action = AlbumListFragmentDirections
            .actionAlbumListFragmentToMusicPlayerFragment(music)
        findNavController().navigate(action)
    }

    private companion object {
        const val TAG = "AlbumListFragment"
    }
}