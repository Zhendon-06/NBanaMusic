package com.guet.stu.banamusic.model.music

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * 音乐播放单例类：统一管理 MediaPlayer、播放状态和播放进度
 * 使用协程处理播放逻辑，防止界面频繁切换导致播放错乱
 */
object MusicPlay {
    private var _isPlaying: Boolean = false
    val isPlaying: Boolean get() = _isPlaying
    
    private var mediaPlayer: MediaPlayer? = null
    private var currentMusic: Music? = null
    
    // 播放进度相关
    private val _currentPosition = AtomicInteger(0)
    val currentPosition: Int get() = _currentPosition.get()
    
    private val _duration = AtomicInteger(0)
    val duration: Int get() = _duration.get()
    
    // 协程管理
    private val playScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var playJob: Job? = null
    
    // 播放进度回调
    private var onProgressUpdate: ((currentPosition: Int, duration: Int) -> Unit)? = null
    
    // 播放完成回调
    private var onCompletion: (() -> Unit)? = null
    
    // 播放状态变化回调
    private val playingStateListeners = mutableSetOf<(isPlaying: Boolean) -> Unit>()
    private var legacyPlayingStateListener: ((isPlaying: Boolean) -> Unit)? = null
    
    /**
     * 设置播放进度更新回调
     */
    fun setOnProgressUpdateListener(callback: (currentPosition: Int, duration: Int) -> Unit) {
        onProgressUpdate = callback
    }
    
    /**
     * 移除播放进度更新回调
     */
    fun removeProgressUpdateListener() {
        onProgressUpdate = null
    }
    
    /**
     * 设置播放完成回调
     */
    fun setOnCompletionListener(callback: () -> Unit) {
        onCompletion = callback
    }
    
    /**
     * 移除播放完成回调
     */
    fun removeCompletionListener() {
        onCompletion = null
    }
    
    /**
     * 设置播放状态变化回调
     */
    fun setOnPlayingStateChangedListener(callback: (isPlaying: Boolean) -> Unit) {
        legacyPlayingStateListener?.let { playingStateListeners.remove(it) }
        legacyPlayingStateListener = callback
        playingStateListeners.add(callback)
    }

    /**
     * 添加播放状态变化监听（支持多个监听器）
     */
    fun addPlayingStateChangedListener(callback: (isPlaying: Boolean) -> Unit) {
        playingStateListeners.add(callback)
    }
    
    /**
     * 移除播放状态变化回调
     */
    fun removePlayingStateChangedListener() {
        legacyPlayingStateListener?.let { playingStateListeners.remove(it) }
        legacyPlayingStateListener = null
    }

    /**
     * 移除指定的播放状态监听
     */
    fun removePlayingStateChangedListener(callback: (isPlaying: Boolean) -> Unit) {
        playingStateListeners.remove(callback)
    }
    
    /**
     * 通知播放状态变化
     */
    private fun notifyPlayingStateChanged(isPlaying: Boolean) {
        playingStateListeners.toList().forEach { listener ->
            listener.invoke(isPlaying)
        }
    }
    
    /**
     * 播放音乐
     */
    fun play(music: Music, onPrepared: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        // 如果正在播放同一首歌，不重新播放
        if (currentMusic?.id == music.id && _isPlaying) {
            return
        }
        
        // 取消之前的播放任务
        playJob?.cancel()
        
        playJob = playScope.launch {
            try {
                // 释放旧的播放器
                releasePlayer()
                
                currentMusic = music
                
                // 创建新的 MediaPlayer
                withContext(Dispatchers.IO) {
                    MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        setDataSource(music.url)
                        
                        setOnPreparedListener {
                            _isPlaying = true
                            it.start()
                            _duration.set(it.duration)
                            notifyPlayingStateChanged(true)
                            onPrepared?.invoke()
                            startProgressUpdate()
                        }
                        
                        setOnCompletionListener {
                            _isPlaying = false
                            _currentPosition.set(0)
                            stopProgressUpdate()
                            notifyPlayingStateChanged(false)
                            onCompletion?.invoke()
                        }
                        
                        setOnErrorListener { _, what, extra ->
                            _isPlaying = false
                            stopProgressUpdate()
                            notifyPlayingStateChanged(false)
                            onError?.invoke("播放错误: what=$what, extra=$extra")
                            true
                        }
                        
                        prepareAsync()
                    }
                }.let { player ->
                    mediaPlayer = player
                }
            } catch (e: Exception) {
                _isPlaying = false
                onError?.invoke(e.message ?: "播放失败")
            }
        }
    }
    
    /**
     * 暂停播放
     */
    fun pause() {
        playScope.launch {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    _isPlaying = false
                    notifyPlayingStateChanged(false)
                }
            }
        }
    }
    
    /**
     * 继续播放
     */
    fun resume() {
        playScope.launch {
            mediaPlayer?.let { player ->
                if (!player.isPlaying && _currentPosition.get() < _duration.get()) {
                    player.start()
                    _isPlaying = true
                    notifyPlayingStateChanged(true)
                    startProgressUpdate()
                }
            }
        }
    }
    
    /**
     * 切换播放/暂停状态
     */
    fun toggle() {
        if (_isPlaying) {
            pause()
        } else {
            resume()
        }
    }
    
    /**
     * 跳转到指定位置
     */
    fun seekTo(position: Int) {
        playScope.launch {
            mediaPlayer?.let { player ->
                try {
                    player.seekTo(position)
                    _currentPosition.set(position)
                } catch (e: Exception) {
                    // 忽略 seekTo 错误
                }
            }
        }
    }
    
    /**
     * 停止播放并释放资源
     */
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
    
    /**
     * 获取当前播放的音乐
     */
    fun getCurrentMusic(): Music? = currentMusic
    
    /**
     * 获取播放状态
     */
    fun getPlayingStatus(): Boolean = _isPlaying
    
    /**
     * 开始更新播放进度
     */
    private fun startProgressUpdate() {
        stopProgressUpdate()
        
        progressJob = playScope.launch {
            while (isActive && mediaPlayer != null && _isPlaying) {
                mediaPlayer?.let { player ->
                    try {
                        val pos = player.currentPosition
                        val dur = player.duration
                        
                        _currentPosition.set(pos)
                        if (dur > 0) {
                            _duration.set(dur)
                        }
                        
                        // 通知进度更新
                        onProgressUpdate?.invoke(pos, dur)
                    } catch (e: Exception) {
                        // 忽略更新错误
                    }
                }
                delay(200)
            }
        }
    }
    
    /**
     * 停止更新播放进度
     */
    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }
    
    /**
     * 释放 MediaPlayer
     */
    private fun releasePlayer() {
        playScope.launch {
            try {
                mediaPlayer?.release()
            } catch (e: Exception) {
                // 忽略释放错误
            } finally {
                mediaPlayer = null
            }
        }
    }
    
    /**
     * 清理所有资源（应用退出时调用）
     */
    fun cleanup() {
        stop()
        playJob?.cancel()
        playScope.cancel()
    }
    
    // 兼容旧接口
    @Deprecated("使用 play() 方法", ReplaceWith("play(music)"))
    fun toPlaying() {
        resume()
    }
    
    @Deprecated("使用 pause() 方法", ReplaceWith("pause()"))
    fun toPause() {
        pause()
    }
}
