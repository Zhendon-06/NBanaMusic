package com.guet.stu.banamusic.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.guet.stu.banamusic.R
import com.guet.stu.banamusic.adapter.LyricsAdapter
import com.guet.stu.banamusic.databinding.FragmentMusicPlayerBinding
import com.guet.stu.banamusic.model.music.Music
import com.guet.stu.banamusic.model.music.MusicPlay
import com.guet.stu.banamusic.network.LyricLoader
import com.guet.stu.banamusic.util.applyStatusBarSpacer
import com.guet.stu.banamusic.viewmodel.MusicPlayerFragmentViewModel
import com.guet.stu.banamusic.viewmodel.SharedMusicViewModel

/**
 * 全屏播放页：负责展示歌曲信息并控制 MediaPlayer。
 */
class MusicPlayerFragment : Fragment() {
    private var _binding: FragmentMusicPlayerBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MusicPlayerFragmentViewModel by viewModels()
    private val sharedMusicViewModel: SharedMusicViewModel by activityViewModels()
    private val args: MusicPlayerFragmentArgs by navArgs()
    private var lyricsAdapter: LyricsAdapter? = null
    private var isLyricsVisible = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMusicPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 进入页面前读取 SharedMusicViewModel 中的列表
        setupMusicList(args.music)
        applyStatusBarSpacer(binding.statusBarSpace.root) // 处理沉浸式状态栏
        setupLyricsView()
        observeViewModel()                                // 绑定 LiveData
        binding.containerDiscLyrics.setOnClickListener { toggleDiscLyricsView() }
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnPlayPause.setOnClickListener { togglePlayback() }
        binding.btnPrev.setOnClickListener { playPrevious() }
        binding.btnNext.setOnClickListener { playNext() }
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    MusicPlay.seekTo(progress)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 设置播放进度更新监听
        MusicPlay.setOnProgressUpdateListener { currentPosition, duration ->
            updateProgress(currentPosition, duration)
        }
        
        // 设置播放状态变化监听，根据 MusicPlay.isPlaying 更新图标
        MusicPlay.setOnPlayingStateChangedListener { isPlaying ->
            updatePlayPauseIcon(isPlaying)
        }
        
        // 设置播放完成监听（自动播放下一首）
        MusicPlay.setOnCompletionListener {
            val nextMusic = viewModel.playNext()
            // 播放状态变化会通过 onPlayingStateChangedListener 自动更新图标
        }
        
        // 初始化时根据当前播放状态显示图标
        updatePlayPauseIcon(MusicPlay.isPlaying)

    }
    
    /**
     * 设置音乐列表和当前音乐
     * 优先从 SharedMusicViewModel 获取列表，如果没有则创建单元素列表
     */
    private fun setupMusicList(music: Music) {
        val musicList = sharedMusicViewModel.currentMusicList
        if (musicList.isNotEmpty()) {
            // 如果从 AlbumListFragment 导航过来，使用完整的音乐列表
            viewModel.setMusicListAndCurrent(musicList, music)
        } else {
            // 如果从其他地方导航过来，创建单元素列表
            viewModel.setMusicListAndCurrent(listOf(music), music)
        }
        // 同步当前播放的音乐到 SharedMusicViewModel，以便底部 musicbar 点击时能获取
        sharedMusicViewModel.setCurrentMusic(music)
    }
    
    /**
     * 播放上一首
     */
    private fun playPrevious() {
        val previousMusic = viewModel.playPrevious()
        previousMusic?.let { music ->
            // 音乐切换会自动触发 observeViewModel 中的监听，从而调用 prepareAndPlay
        }
    }
    
    /**
     * 播放下一首
     */
    private fun playNext() {
        val nextMusic = viewModel.playNext()
        nextMusic?.let { music ->
            // 音乐切换会自动触发 observeViewModel 中的监听，从而调用 prepareAndPlay
        }
    }
    
    private fun observeViewModel() {
        viewModel.currentMusic.observe(viewLifecycleOwner) { music ->
            music?.let { 
                renderMusic(it)
                // 如果当前是暂停状态且是同一首歌，不自动播放
                val currentMusic = MusicPlay.getCurrentMusic()
                val isSameMusic = currentMusic?.id == music.id
                val isPaused = !MusicPlay.isPlaying
                
                if (isSameMusic && isPaused) {
                    // 同一首歌且暂停状态，不自动播放，只更新 UI
                    // 更新进度显示
                    updateProgress(MusicPlay.currentPosition, MusicPlay.duration)
                } else {
                    // 不同歌曲或正在播放，正常播放
                    playMusic(it)
                }
                // 同步当前播放的音乐到 SharedMusicViewModel，以便MainActivity更新musicbar
                sharedMusicViewModel.setCurrentMusic(it)
            }
        }
    }

    private fun renderMusic(music: Music) = with(binding) {
        // 基本信息直接绑定到 UI
        tvSongName.text = music.song
        tvArtist.text = music.sing
        ivAlbum.load(music.pic) {
            crossfade(true)
            allowHardware(false)
        }
        updateLyricsForMusic(music)
        // 根据 MusicPlay.isPlaying 显示对应的图标
        updatePlayPauseIcon(MusicPlay.isPlaying)
    }

    /**
     * 播放音乐（使用 MusicPlay 单例管理）
     */
    private fun playMusic(music: Music) {
        MusicPlay.play(
            music = music,
            onPrepared = {
                // 播放状态变化会通过 onPlayingStateChangedListener 自动更新图标
            },
            onError = { errorMessage ->
                // 播放状态变化会通过 onPlayingStateChangedListener 自动更新图标
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        )
        // 初始状态：准备播放时根据当前状态显示图标
        updatePlayPauseIcon(MusicPlay.isPlaying)
    }
    
    /**
     * 更新播放进度显示
     */
    private fun updateProgress(currentPosition: Int, duration: Int) {
        binding.seekBar.max = duration
        binding.seekBar.progress = currentPosition
        binding.tvCurrentTime.text = formatTime(currentPosition)
        binding.tvTotalTime.text = formatTime(duration)
    }
    private fun formatTime(ms: Int): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%d:%02d", min, sec)
    }


    private fun togglePlayback() {
        MusicPlay.toggle()
        // 播放状态变化会通过 onPlayingStateChangedListener 自动更新图标
        // 这里立即更新一次，确保 UI 响应及时
        updatePlayPauseIcon(MusicPlay.isPlaying)
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        val icon = if (isPlaying) R.drawable.ic_pause else R.drawable.play
        binding.btnPlayPause.setImageResource(icon)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 移除所有监听，但不停止播放
        MusicPlay.removeProgressUpdateListener()
        MusicPlay.removeCompletionListener()
        MusicPlay.removePlayingStateChangedListener()
        lyricsAdapter = null
        _binding = null
    }
    
    override fun onResume() {
        super.onResume()
        // 恢复时根据当前播放状态更新图标
        updatePlayPauseIcon(MusicPlay.isPlaying)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Fragment 销毁时不停止播放，保持后台播放
        // 只有在应用退出时才调用 MusicPlay.cleanup()
    }

    private fun setupLyricsView() {
        val adapter = LyricsAdapter(onLineClick = { toggleDiscLyricsView() })
        lyricsAdapter = adapter
        binding.layoutLyrics.rvLyrics.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }
        // 歌词界面点击空白区域时也能切回唱片
        binding.layoutLyrics.root.setOnClickListener { toggleDiscLyricsView() }

        // 如果此时已经有当前歌曲，补一次歌词数据
        viewModel.currentMusic.value?.let { current ->
            updateLyricsForMusic(current)
        }
    }

    private fun toggleDiscLyricsView() {
        isLyricsVisible = !isLyricsVisible
        binding.discContent.isVisible = !isLyricsVisible
        binding.layoutLyrics.root.isVisible = isLyricsVisible
    }

    private fun updateLyricsForMusic(music: Music) {
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            try {
                val lines = LyricLoader.loadLyricsById(music.id)
                if (lines.isNotEmpty()) {
                    lyricsAdapter?.submitList(lines)
                } else {
                    lyricsAdapter?.submitList(
                        listOf(
                            "${music.song} - ${music.sing}",
                            "",
                            getString(R.string.lyrics_placeholder_hint)
                        )
                    )
                }
            } catch (e: Exception) {
                // 请求失败时用占位歌词提示
                lyricsAdapter?.submitList(
                    listOf(
                        "${music.song} - ${music.sing}",
                        "",
                        getString(R.string.lyrics_placeholder_hint)
                    )
                )
            }
        }
    }
}