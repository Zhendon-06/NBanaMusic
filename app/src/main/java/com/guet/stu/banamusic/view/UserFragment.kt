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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.guet.stu.banamusic.databinding.FragmentUserBinding
import com.guet.stu.banamusic.databinding.DialogNewPlaylistBinding
import com.guet.stu.banamusic.util.applyStatusBarSpacer

class UserFragment : Fragment() {
    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!



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
        binding.putmusic.setOnClickListener {
            showCreatePlaylistDialog()
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
                // TODO: 保存歌单逻辑
                dialog.dismiss()
            }
        }

        dialog.show()
    }

}