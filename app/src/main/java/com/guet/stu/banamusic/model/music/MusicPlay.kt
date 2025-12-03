package com.guet.stu.banamusic.model.music

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * 音乐播放管理单例
 * - 统一管理 MediaPlayer 生命周期
 * - 管理播放状态、进度、回调
 * - 使用协程更新进度，避免 UI 切换导致播放错乱
 */
object MusicPlay {

    /** 当前是否正在播放 */
    private var _isPlaying: Boolean = false
    val isPlaying: Boolean get() = _isPlaying

    /** 全局 MediaPlayer 实例 */
    private var mediaPlayer: MediaPlayer? = null

    /** 当前播放的音乐对象 */
    private var currentMusic: Music? = null

    /** 当前播放位置（毫秒） */
    private val _currentPosition = AtomicInteger(0)
    val currentPosition: Int get() = _currentPosition.get()

    /** 音乐总时长（毫秒） */
    private val _duration = AtomicInteger(0)
    val duration: Int get() = _duration.get()

    /** 播放协程作用域（不会随界面销毁） */
    private val playScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /** 播放任务 Job，用于取消 */
    private var progressJob: Job? = null
    private var playJob: Job? = null

    /** 播放进度更新回调 */
    private var onProgressUpdate: ((currentPosition: Int, duration: Int) -> Unit)? = null

    /** 播放完成回调 */
    private var onCompletion: (() -> Unit)? = null

    /** 播放状态变化监听器（支持多个） */
    private val playingStateListeners = mutableSetOf<(isPlaying: Boolean) -> Unit>()

    /** 旧版单一监听器（兼容以前写法） */
    private var legacyPlayingStateListener: ((isPlaying: Boolean) -> Unit)? = null

    // --------------------------
    // 回调注册部分
    // --------------------------

    /** 设置进度更新回调 */
    fun setOnProgressUpdateListener(callback: (currentPosition: Int, duration: Int) -> Unit) {
        onProgressUpdate = callback
    }

    fun removeProgressUpdateListener() {
        onProgressUpdate = null
    }

    /** 设置播放完成回调 */
    fun setOnCompletionListener(callback: () -> Unit) {
        onCompletion = callback
    }

    fun removeCompletionListener() {
        onCompletion = null
    }

    /** 设置（替换）播放状态变化监听 */
    fun setOnPlayingStateChangedListener(callback: (isPlaying: Boolean) -> Unit) {
        legacyPlayingStateListener?.let { playingStateListeners.remove(it) }
        legacyPlayingStateListener = callback
        playingStateListeners.add(callback)
    }

    /** 添加额外播放状态监听（不会覆盖旧的） */
    fun addPlayingStateChangedListener(callback: (isPlaying: Boolean) -> Unit) {
        playingStateListeners.add(callback)
    }

    /** 移除旧监听器 */
    fun removePlayingStateChangedListener() {
        legacyPlayingStateListener?.let { playingStateListeners.remove(it) }
        legacyPlayingStateListener = null
    }

    /** 移除特定监听器 */
    fun removePlayingStateChangedListener(callback: (isPlaying: Boolean) -> Unit) {
        playingStateListeners.remove(callback)
    }

    /** 通知所有监听器播放状态变化 */
    private fun notifyPlayingStateChanged(isPlaying: Boolean) {
        playingStateListeners.toList().forEach { listener ->
            listener.invoke(isPlaying)
        }
    }

    // --------------------------
    // 播放主逻辑
    // --------------------------

    /**
     * 播放音乐
     */
    fun play(music: Music, onPrepared: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) {

        // 正在播放同一首歌 → 直接 return
        if (currentMusic?.id == music.id && _isPlaying) return

        // 取消旧播放任务
        playJob?.cancel()

        playJob = playScope.launch {
            try {
                // 释放旧播放器
                releasePlayer()

                currentMusic = music

                // 创建 MediaPlayer（IO 线程）
                val player = withContext(Dispatchers.IO) {
                    MediaPlayer().apply {

                        // 设置音频属性
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )

                        // 设置音乐 URL
                        setDataSource(music.url)

                        // 准备完成回调
                        setOnPreparedListener {
                            _isPlaying = true
                            it.start()
                            _duration.set(it.duration)
                            notifyPlayingStateChanged(true)
                            onPrepared?.invoke()
                            startProgressUpdate()
                        }

                        // 播放完成回调
                        setOnCompletionListener {
                            _isPlaying = false
                            _currentPosition.set(0)
                            stopProgressUpdate()
                            notifyPlayingStateChanged(false)
                            onCompletion?.invoke()
                        }

                        // 播放错误回调
                        setOnErrorListener { _, what, extra ->
                            _isPlaying = false
                            stopProgressUpdate()
                            notifyPlayingStateChanged(false)
                            onError?.invoke("播放错误: what=$what, extra=$extra")
                            true
                        }

                        // 异步准备
                        prepareAsync()
                    }
                }

                mediaPlayer = player

            } catch (e: Exception) {
                _isPlaying = false
                onError?.invoke(e.message ?: "播放失败")
            }
        }
    }

    /** 暂停播放 */
    fun pause() {
        playScope.launch {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    _isPlaying = false
                    notifyPlayingStateChanged(false)
                }
            }
        }
    }

    /** 恢复播放 */
    fun resume() {
        playScope.launch {
            mediaPlayer?.let {
                if (!it.isPlaying && _currentPosition.get() < _duration.get()) {
                    it.start()
                    _isPlaying = true
                    notifyPlayingStateChanged(true)
                    startProgressUpdate()
                }
            }
        }
    }

    /** 切换播放/暂停 */
    fun toggle() {
        if (_isPlaying) pause() else resume()
    }

    /** 跳转到指定位置 */
    fun seekTo(position: Int) {
        playScope.launch {
            mediaPlayer?.let {
                try {
                    it.seekTo(position)
                    _currentPosition.set(position)
                } catch (_: Exception) {}
            }
        }
    }

    /** 停止播放 */
    fun stop() {
        playScope.launch {
            _isPlaying = false
            stopProgressUpdate()
            notifyPlayingStateChanged(false)
            releasePlayer()
            currentMusic = null
            _currentPosition.set(0)
            _duration.set(0)
        }
    }

    fun getCurrentMusic(): Music? = currentMusic
    fun getPlayingStatus(): Boolean = _isPlaying

    // --------------------------
    // 播放进度更新
    // --------------------------

    private fun startProgressUpdate() {
        stopProgressUpdate()

        progressJob = playScope.launch {
            while (isActive && mediaPlayer != null && _isPlaying) {

                mediaPlayer?.let {
                    try {
                        val pos = it.currentPosition
                        val dur = it.duration

                        _currentPosition.set(pos)
                        if (dur > 0) _duration.set(dur)

                        // 回调 UI 更新
                        onProgressUpdate?.invoke(pos, dur)
                    } catch (_: Exception) {}
                }

                delay(200) // 每 200ms 更新一次
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    /** 释放播放器资源 */
    private fun releasePlayer() {
        playScope.launch {
            try { mediaPlayer?.release() } catch (_: Exception) {}
            mediaPlayer = null
        }
    }

    /** 彻底清理资源（应用退出调用） */
    fun cleanup() {
        stop()
        playJob?.cancel()
        playScope.cancel()
    }

    // 兼容旧接口
    @Deprecated("使用 play() 方法")
    fun toPlaying() = resume()

    @Deprecated("使用 pause() 方法")
    fun toPause() = pause()
}
