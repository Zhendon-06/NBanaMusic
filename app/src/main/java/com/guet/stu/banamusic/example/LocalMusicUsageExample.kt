package com.guet.stu.banamusic.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.guet.stu.banamusic.model.music.Music
import com.guet.stu.banamusic.model.music.MusicPlay
import com.guet.stu.banamusic.util.LocalPlaylistManager
import com.guet.stu.banamusic.util.MusicPermission
import com.guet.stu.banamusic.viewmodel.LocalMusicViewModel
import kotlinx.coroutines.launch

/**
 * 本地音乐功能使用示例
 * 
 * 在 Fragment 或 Activity 中使用本地音乐扫描功能的示例代码
 * 
 * 使用步骤：
 * 1. 在 Fragment/Activity 中创建 LocalMusicViewModel 实例
 * 2. 检查权限，如果没有权限则申请
 * 3. 调用 loadLocalMusic() 开始扫描
 * 4. 观察 localSongs LiveData 获取扫描结果
 * 5. 使用 LocalSong.toMusic() 转换为 Music 对象进行播放
 */
class LocalMusicUsageExample : Fragment() {

    // 创建 ViewModel（如果使用 Fragment，使用 activityViewModels 共享实例）
    private val localMusicViewModel: LocalMusicViewModel by activityViewModels()

    // 权限申请回调
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            // 权限已授予，开始扫描
            localMusicViewModel.loadLocalMusic()
        } else {
            // 权限被拒绝，提示用户
            // TODO: 显示权限被拒绝的提示
        }
    }

    /**
     * 初始化并开始扫描本地音乐
     */
    fun initLocalMusicScan() {
        // 检查权限
        if (MusicPermission.hasPermission(requireContext())) {
            // 已有权限，直接扫描
            localMusicViewModel.loadLocalMusic()
        } else {
            // 申请权限
            requestPermissionLauncher.launch(MusicPermission.getRequiredPermissions())
        }
    }

    /**
     * 观察扫描结果
     */
    fun observeLocalMusic() {
        // 观察扫描结果
        localMusicViewModel.localSongs.observe(viewLifecycleOwner) { songs ->
            // 更新 UI，显示扫描到的歌曲列表
            // songs 是 List<LocalSong>
        }

        // 观察加载状态
        localMusicViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // 显示/隐藏加载指示器
        }

        // 观察错误信息
        localMusicViewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            // 显示错误信息
        }
    }

    /**
     * 播放本地音乐示例
     */
    fun playLocalSong(localSong: com.guet.stu.banamusic.model.music.LocalSong) {
        // 将 LocalSong 转换为 Music 对象
        val music: Music = localSong.toMusic()
        
        // 使用现有的播放逻辑播放
        MusicPlay.play(
            music,
            onPrepared = {
                // 播放准备完成的回调
            },
            onError = { error ->
                // 播放错误的回调
                // TODO: 显示错误信息
            }
        )
    }

    /**
     * 从歌单管理器获取本地音乐歌单
     */
    fun getLocalPlaylist(): List<com.guet.stu.banamusic.model.music.LocalSong> {
        return LocalPlaylistManager.getSongs()
    }

    /**
     * 播放歌单中的歌曲示例
     */
    fun playFromLocalPlaylist(index: Int) {
        val songs = LocalPlaylistManager.getSongs()
        if (index in songs.indices) {
            val localSong = songs[index]
            playLocalSong(localSong)
        }
    }
}

