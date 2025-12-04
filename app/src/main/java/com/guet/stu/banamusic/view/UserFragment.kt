/**
 * 用户中心Fragment
 *
 * 用途：展示“我的”页面，包含滚动联动导航与新建歌单对话框。
 */
package com.guet.stu.banamusic.view

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.guet.stu.banamusic.adapter.PlaylistAdapter
import com.guet.stu.banamusic.databinding.FragmentUserBinding
import com.guet.stu.banamusic.databinding.DialogNewPlaylistBinding
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

        setupPlaylistList()
        observePlaylists()

        binding.putmusic.setOnClickListener {
            showCreatePlaylistDialog()
        }
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
            // 点击歌单，跳转到 PlayListActivity 显示该歌单内的歌曲
            val context = requireContext()
            val intent = Intent(context, PlayListActivity::class.java).apply {
                putExtra(PlayListActivity.EXTRA_PLAYLIST_ID, playlist.playlistId)
                putExtra(PlayListActivity.EXTRA_PLAYLIST_NAME, playlist.name)
            }
            startActivity(intent)
        }
        binding.myMusicList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = playlistAdapter
        }
    }

    /**
     * 监听数据库中歌单变化并刷新 UI
     */
    private fun observePlaylists() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.playlists.collectLatest { list ->
                playlistAdapter.submitList(list)
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