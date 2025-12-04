/**
 * 用户中心Fragment
 *
 * 用途：展示“我的”页面，包含滚动联动导航与新建歌单对话框。
 */
package com.guet.stu.banamusic.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.guet.stu.banamusic.R
import com.guet.stu.banamusic.adapter.PlaylistAdapter
import com.guet.stu.banamusic.databinding.FragmentUserBinding
import com.guet.stu.banamusic.databinding.DialogNewPlaylistBinding
import com.guet.stu.banamusic.model.music.SpecialPlaylist
import com.guet.stu.banamusic.util.applyStatusBarSpacer
import com.guet.stu.banamusic.viewmodel.UserPlaylistsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class UserFragment : Fragment() {
    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserPlaylistsViewModel by viewModels {
        UserPlaylistsViewModel.Factory(requireActivity().application)
    }

    private lateinit var playlistAdapter: PlaylistAdapter

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
        cardLocal.setOnClickListener { openSpecialPlaylist(SpecialPlaylist.LOCAL) }
        cardMightLike.setOnClickListener { openSpecialPlaylist(SpecialPlaylist.MIGHT_LIKE) }
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