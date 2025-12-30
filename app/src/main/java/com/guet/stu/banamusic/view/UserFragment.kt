/**
 * 用户中心Fragment
 *
 * 用途：展示“我的”页面，包含滚动联动导航与新建歌单对话框。
 */
package com.guet.stu.banamusic.view

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.guet.stu.banamusic.R
import com.guet.stu.banamusic.adapter.PlaylistAdapter
import com.guet.stu.banamusic.databinding.FragmentUserBinding
import com.guet.stu.banamusic.databinding.DialogNewPlaylistBinding
import com.guet.stu.banamusic.model.music.AppDatabase
import com.guet.stu.banamusic.model.music.PlaylistRepository
import com.guet.stu.banamusic.model.music.SpecialPlaylist
import com.guet.stu.banamusic.util.MusicPermission
import com.guet.stu.banamusic.util.applyStatusBarSpacer
import com.guet.stu.banamusic.viewmodel.LocalMusicViewModel
import com.guet.stu.banamusic.viewmodel.UserPlaylistsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UserFragment : Fragment() {
    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserPlaylistsViewModel by viewModels {
        UserPlaylistsViewModel.Factory(requireActivity().application)
    }

    private val localMusicViewModel: LocalMusicViewModel by activityViewModels()

    private lateinit var playlistAdapter: PlaylistAdapter

    // 权限申请回调
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            // 权限已授予，开始扫描本地音乐
            scanAndShowLocalMusic()
        } else {
            // 权限被拒绝，提示用户
            Snackbar.make(
                binding.root,
                "需要存储权限才能扫描本地音乐",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 初始化 DataBinding
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 设置actionbar中的TextView显示为"我的"
        binding.mianbar.tvHome.text = "我的"
        applyStatusBarSpacer(binding.statusBarSpace.root)

        // 顶部搜索框点击：跳转到 SearchPageFragment
        binding.mianbar.etSearch.setOnClickListener {
            findNavController().navigate(R.id.searchPageFragment)
        }

        setupPlaylistList()
        setupTopCards()
        observeViewModel()

        binding.putmusic.setOnClickListener { showCreatePlaylistDialog() }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * 初始化“我创建的歌单”列表
     */
    private fun setupPlaylistList() {
        playlistAdapter = PlaylistAdapter { playlist ->
            // 点击歌单，使用 NavController 跳转到 PlayListFragment 显示该歌单内的歌曲
            val args = Bundle().apply {
                putLong(PlayListFragment.ARG_PLAYLIST_ID, playlist.playlistId)
                putString(PlayListFragment.ARG_PLAYLIST_NAME, playlist.name)
            }
            findNavController().navigate(R.id.playListFragment, args)
        }
        binding.myMusicList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = playlistAdapter
        }
    }

    private fun setupTopCards() = with(binding) {
        cardCollect.setOnClickListener { openSpecialPlaylist(SpecialPlaylist.COLLECT) }
        cardHistory.setOnClickListener { openSpecialPlaylist(SpecialPlaylist.HISTORY) }
        cardLocal.setOnClickListener { handleLocalMusicClick() }
        cardMightLike.setOnClickListener { openSpecialPlaylist(SpecialPlaylist.MIGHT_LIKE) }
    }

    /**
     * 处理本地音乐卡片点击
     * 先检查数据库中是否已有本地音乐，如果有则直接显示，否则才扫描
     */
    private fun handleLocalMusicClick() {
        viewLifecycleOwner.lifecycleScope.launch {
            // 检查数据库中是否已有本地音乐
            val localCount = viewModel.localCount.value
            if (localCount > 0) {
                // 数据库中已有本地音乐，直接跳转显示
                openSpecialPlaylist(SpecialPlaylist.LOCAL)
            } else {
                // 数据库中没有本地音乐，需要扫描
                if (MusicPermission.hasPermission(requireContext())) {
                    // 已有权限，直接扫描
                    scanAndShowLocalMusic()
                } else {
                    // 申请权限
                    requestPermissionLauncher.launch(MusicPermission.getRequiredPermissions())
                }
            }
        }
    }

    /**
     * 扫描本地音乐并显示
     */
    private fun scanAndShowLocalMusic() {
        // 显示加载提示
        val loadingDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("扫描本地音乐")
            .setMessage("正在扫描设备中的本地音乐，请稍候...")
            .setCancelable(false)
            .create()
        loadingDialog.show()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 扫描本地音乐
                localMusicViewModel.loadLocalMusic()

                // 等待扫描完成
                var isCompleted = false
                localMusicViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
                    if (!isLoading && !isCompleted) {
                        isCompleted = true
                        loadingDialog.dismiss()
                        
                        val songs = localMusicViewModel.localSongs.value ?: emptyList()
                        if (songs.isNotEmpty()) {
                            // 将扫描到的音乐添加到本地音乐歌单（需要在协程中调用）
                            launch {
                                val repo = PlaylistRepository(AppDatabase.getInstance(requireContext()))
                                val musicList = songs.map { it.toMusic() }
                                repo.setLocalMusicPlaylist(musicList)
                                
                                // 跳转到歌单页面显示
                                openSpecialPlaylist(SpecialPlaylist.LOCAL)
                            }
                        } else {
                            Snackbar.make(
                                binding.root,
                                "未找到本地音乐（需要时长≥90秒的音乐文件）",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                // 观察错误信息
                localMusicViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
                    if (error != null && !isCompleted) {
                        isCompleted = true
                        loadingDialog.dismiss()
                        Snackbar.make(
                            binding.root,
                            "扫描失败: $error",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                loadingDialog.dismiss()
                Snackbar.make(
                    binding.root,
                    "扫描失败: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openSpecialPlaylist(type: SpecialPlaylist) {
        val args = Bundle().apply {
            putLong(PlayListFragment.ARG_PLAYLIST_ID, viewModel.specialPlaylistId(type))
            putString(PlayListFragment.ARG_PLAYLIST_NAME, viewModel.specialPlaylistName(type))
        }
        findNavController().navigate(R.id.playListFragment, args)
    }

    /**
     * 监听数据库中歌单及特殊歌单统计，并刷新 UI
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.playlists.collectLatest { list ->
                        playlistAdapter.submitList(list)
                    }
                }
                launch {
                    viewModel.collectCount.collectLatest {
                        binding.loveSize.text = it.toString()
                    }
                }
                launch {
                    viewModel.historyCount.collectLatest {
                        binding.historySize.text = it.toString()
                    }
                }
                launch {
                    viewModel.localCount.collectLatest {
                        binding.localSize.text = it.toString()
                    }
                }
                launch {
                    viewModel.mightLikeCount.collectLatest {
                        binding.mightLikeSize.text = it.toString()
                    }
                }
            }
        }
    }

    /**
     * 显示新建歌单对话框
     */
    private fun showCreatePlaylistDialog() {
        val dialogBinding = DialogNewPlaylistBinding.inflate(layoutInflater)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("新建歌单")
            .setView(dialogBinding.root)
            .create()

        // 设置按钮点击事件
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnConfirm.setOnClickListener {
            val name = dialogBinding.etPlaylistName.text.toString().trim()
            if (name.isEmpty()) {
                dialogBinding.etPlaylistName.error = "名称不能为空"
            } else {
                viewModel.createPlaylist(name) {
                    // 歌单插入完成后再关闭对话框，列表会自动刷新
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

}