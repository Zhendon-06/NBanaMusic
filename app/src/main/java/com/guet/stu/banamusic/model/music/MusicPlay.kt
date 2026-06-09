package com.guet.stu.banamusic.model.music

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.IBinder
import com.guet.stu.banamusic.service.MusicPlaybackService
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * 音乐播放管理单例
 * - 统一管理 MediaPlayer 生命周期
 * - 管理播放状态、进度、回调
 * - 使用协程更新进度，避免 UI 切换导致播放错乱
 * - 集成 MusicPlaybackService 实现媒体通知
 */
object MusicPlay {

    /** Application Context */
    private var appContext: Context? = null

    /** MusicPlaybackService 实例 */
    private var playbackService: MusicPlaybackService? = null

    /** Service 是否已绑定 */
    private var isServiceBound: Boolean = false

    /** ServiceConnection */
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicPlaybackService.LocalBinder
            playbackService = binder.getService()
            isServiceBound = true

            // 设置控制回调
            setupControlCallbacks()

            // 同步当前状态到 Service
            syncStateToService()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playbackService = null
            isServiceBound = false
        }
    }

    /**
     * 初始化
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        bindPlaybackService()
    }

    /**
     * 绑定服务
     */
    private fun bindPlaybackService() {
        val context = appContext ?: return
        val intent = Intent(context, MusicPlaybackService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /**
     * 解绑服务
     */
    fun unbindPlaybackService() {
        if (isServiceBound) {
            appContext?.unbindService(serviceConnection)
            isServiceBound = false
            playbackService = null
        }
    }

    /**
     * 设置控制回调
     */
    private fun setupControlCallbacks() {
        playbackService?.setControlCallbacks(
            playPause = { toggle() },
            next = { playNext() },
            previous = { playPrevious() },
            stop = { stop() },
            seek = { position -> seekTo(position) }
        )
    }

    /**
     * 同步状态到 Service
     */
    private fun syncStateToService() {
        playbackService?.updatePlaybackState(
            currentMusic,
            _isPlaying,
            _currentPosition.get(),
            _duration.get()
        )
    }

    /** 播放列表 */
    private var playList: List<Music> = emptyList()
    private var currentIndex: Int = -1

    /**
     * 设置播放列表
     */
    fun setPlayList(list: List<Music>, startIndex: Int = 0) {
        playList = list
        currentIndex = startIndex.coerceIn(0, list.size - 1)
    }

    /**
     * 获取播放列表
     */
    fun getPlayList(): List<Music> = playList

    /**
     * 播放下一首
     */
    fun playNext(): Music? {
        if (playList.isEmpty()) {
            // 如果没有播放列表，触发完成回调，让外部处理
            notifyCompletion()
            return null
        }
        currentIndex = (currentIndex + 1) % playList.size
        val nextMusic = playList[currentIndex]
        play(nextMusic)
        return nextMusic
    }

    /**
     * 播放上一首
     */
    fun playPrevious(): Music? {
        if (playList.isEmpty()) {
            // 如果没有播放列表，重新开始播放当前歌曲
            currentMusic?.let {
                seekTo(0)
                if (!_isPlaying) {
                    resume()
                }
            }
            return currentMusic
        }
        currentIndex = if (currentIndex > 0) currentIndex - 1 else playList.size - 1
        val previousMusic = playList[currentIndex]
        play(previousMusic)
        return previousMusic
    }

    /** 当前播放状态 */
    private var _isPlaying: Boolean = false
    val isPlaying: Boolean get() = _isPlaying

    /** MediaPlayer 实例 */
    private var mediaPlayer: MediaPlayer? = null

    /** 当前音乐 */
    private var currentMusic: Music? = null

    /** 当前位置 */
    private val _currentPosition = AtomicInteger(0)
    val currentPosition: Int get() = _currentPosition.get()

    /** 总时长 */
    private val _duration = AtomicInteger(0)
    val duration: Int get() = _duration.get()

    /** 协程作用域 */
    private val playScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var playJob: Job? = null

    /** 回调 */
    private val progressUpdateListeners = mutableSetOf<(currentPosition: Int, duration: Int) -> Unit>()
    private var legacyProgressUpdateListener: ((currentPosition: Int, duration: Int) -> Unit)? = null
    private val completionListeners = mutableSetOf<() -> Unit>()
    private var legacyCompletionListener: (() -> Unit)? = null
    private val playingStateListeners = mutableSetOf<(isPlaying: Boolean) -> Unit>()
    private var legacyPlayingStateListener: ((isPlaying: Boolean) -> Unit)? = null

    // --------------------------
    // 回调注册
    // --------------------------

    fun setOnProgressUpdateListener(callback: (currentPosition: Int, duration: Int) -> Unit) {
        legacyProgressUpdateListener?.let { progressUpdateListeners.remove(it) }
        legacyProgressUpdateListener = callback
        addProgressUpdateListener(callback)
    }

    fun addProgressUpdateListener(callback: (currentPosition: Int, duration: Int) -> Unit) {
        progressUpdateListeners.add(callback)
        callback.invoke(_currentPosition.get(), _duration.get())
    }

    fun removeProgressUpdateListener() {
        legacyProgressUpdateListener?.let { progressUpdateListeners.remove(it) }
        legacyProgressUpdateListener = null
    }

    fun removeProgressUpdateListener(callback: (currentPosition: Int, duration: Int) -> Unit) {
        progressUpdateListeners.remove(callback)
        if (legacyProgressUpdateListener === callback) {
            legacyProgressUpdateListener = null
        }
    }

    fun setOnCompletionListener(callback: () -> Unit) {
        legacyCompletionListener?.let { completionListeners.remove(it) }
        legacyCompletionListener = callback
        addCompletionListener(callback)
    }

    fun addCompletionListener(callback: () -> Unit) {
        completionListeners.add(callback)
    }

    fun removeCompletionListener() {
        legacyCompletionListener?.let { completionListeners.remove(it) }
        legacyCompletionListener = null
    }

    fun removeCompletionListener(callback: () -> Unit) {
        completionListeners.remove(callback)
        if (legacyCompletionListener === callback) {
            legacyCompletionListener = null
        }
    }

    fun setOnPlayingStateChangedListener(callback: (isPlaying: Boolean) -> Unit) {
        legacyPlayingStateListener?.let { playingStateListeners.remove(it) }
        legacyPlayingStateListener = callback
        playingStateListeners.add(callback)
        callback.invoke(_isPlaying)
    }

    fun addPlayingStateChangedListener(callback: (isPlaying: Boolean) -> Unit) {
        playingStateListeners.add(callback)
        callback.invoke(_isPlaying)
    }

    fun removePlayingStateChangedListener() {
        legacyPlayingStateListener?.let { playingStateListeners.remove(it) }
        legacyPlayingStateListener = null
    }

    fun removePlayingStateChangedListener(callback: (isPlaying: Boolean) -> Unit) {
        playingStateListeners.remove(callback)
    }

    private fun notifyPlayingStateChanged(isPlaying: Boolean) {
        playingStateListeners.toList().forEach { listener ->
            listener.invoke(isPlaying)
        }
    }

    private fun notifyProgressUpdated(currentPosition: Int, duration: Int) {
        progressUpdateListeners.toList().forEach { listener ->
            listener.invoke(currentPosition, duration)
        }
    }

    private fun notifyCompletion() {
        completionListeners.toList().forEach { listener ->
            listener.invoke()
        }
    }

    // --------------------------
    // 播放控制
    // --------------------------

    /**
     * 播放音乐
     */
    fun play(music: Music, onPrepared: (() -> Unit)? = null, onError: ((String) -> Unit)? = null) {
        if (currentMusic?.id == music.id && _isPlaying) {
            notifyPlayingStateChanged(true)
            notifyProgressUpdated(_currentPosition.get(), _duration.get())
            onPrepared?.invoke()
            return
        }

        playJob?.cancel()

        playJob = playScope.launch {
            var player: MediaPlayer? = null
            try {
                releasePlayer()
                currentMusic = music
                _isPlaying = false
                _currentPosition.set(0)
                _duration.set(0)
                notifyPlayingStateChanged(false)
                notifyProgressUpdated(0, 0)

                // 启动前台服务
                startPlaybackService()

                val newPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                }

                player = newPlayer
                mediaPlayer = newPlayer

                newPlayer.setOnPreparedListener {
                    if (mediaPlayer !== it) return@setOnPreparedListener

                    _isPlaying = true
                    it.start()
                    _duration.set(it.duration)
                    _currentPosition.set(it.currentPosition)
                    notifyPlayingStateChanged(true)
                    notifyProgressUpdated(_currentPosition.get(), _duration.get())
                    onPrepared?.invoke()
                    startProgressUpdate()
                    updateServiceState()
                }

                newPlayer.setOnCompletionListener {
                    if (mediaPlayer !== it) return@setOnCompletionListener

                    _isPlaying = false
                    _currentPosition.set(0)
                    stopProgressUpdate()
                    notifyPlayingStateChanged(false)
                    notifyProgressUpdated(0, _duration.get())
                    updateServiceState()
                    notifyCompletion()
                }

                newPlayer.setOnErrorListener { playerWithError, what, extra ->
                    if (mediaPlayer !== playerWithError) return@setOnErrorListener true

                    _isPlaying = false
                    stopProgressUpdate()
                    notifyPlayingStateChanged(false)
                    notifyProgressUpdated(_currentPosition.get(), _duration.get())
                    updateServiceState()
                    onError?.invoke("播放错误: what=$what, extra=$extra")
                    true
                }

                withContext(Dispatchers.IO) {
                    val context = appContext
                    if (music.url.startsWith("content://") && context != null) {
                        try {
                            val uri = Uri.parse(music.url)
                            newPlayer.setDataSource(context, uri)
                        } catch (e: Exception) {
                            newPlayer.setDataSource(music.url)
                        }
                    } else {
                        newPlayer.setDataSource(music.url)
                    }
                }

                if (mediaPlayer !== newPlayer) return@launch
                newPlayer.prepareAsync()

            } catch (e: CancellationException) {
                player?.takeIf { mediaPlayer === it }?.let { releasePlayer() }
                throw e
            } catch (e: Exception) {
                player?.takeIf { mediaPlayer === it }?.let { releasePlayer() }
                _isPlaying = false
                stopProgressUpdate()
                notifyPlayingStateChanged(false)
                notifyProgressUpdated(_currentPosition.get(), _duration.get())
                updateServiceState()
                onError?.invoke(e.message ?: "播放失败")
            }
        }
    }

    /**
     * 启动前台服务
     */
    private fun startPlaybackService() {
        val context = appContext ?: return
        val intent = Intent(context, MusicPlaybackService::class.java)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /**
     * 更新 Service 状态
     */
    private fun updateServiceState() {
        playbackService?.updatePlaybackState(
            currentMusic,
            _isPlaying,
            _currentPosition.get(),
            _duration.get()
        )
    }

    /**
     * 暂停
     */
    fun pause() {
        playScope.launch {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    _currentPosition.set(it.currentPosition)
                    it.pause()
                    _isPlaying = false
                    notifyPlayingStateChanged(false)
                    notifyProgressUpdated(_currentPosition.get(), _duration.get())
                    stopProgressUpdate()
                    updateServiceState()
                }
            }
        }
    }

    /**
     * 恢复播放
     */
    fun resume() {
        playScope.launch {
            mediaPlayer?.let {
                if (!it.isPlaying && _currentPosition.get() < _duration.get()) {
                    it.start()
                    _isPlaying = true
                    notifyPlayingStateChanged(true)
                    notifyProgressUpdated(it.currentPosition, _duration.get())
                    startProgressUpdate()
                    updateServiceState()
                }
            }
        }
    }

    /**
     * 切换播放/暂停
     */
    fun toggle() {
        if (_isPlaying) pause() else resume()
    }

    /**
     * 跳转进度
     */
    fun seekTo(position: Int) {
        playScope.launch {
            mediaPlayer?.let {
                try {
                    val duration = _duration.get()
                    val target = if (duration > 0) {
                        position.coerceIn(0, duration)
                    } else {
                        position.coerceAtLeast(0)
                    }
                    it.seekTo(target)
                    _currentPosition.set(target)
                    notifyProgressUpdated(target, duration)
                    updateServiceState()
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * 停止
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
            notifyProgressUpdated(0, 0)
            updateServiceState()
        }
    }

    fun getCurrentMusic(): Music? = currentMusic
    fun getPlayingStatus(): Boolean = _isPlaying

    // --------------------------
    // 进度更新
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

                        notifyProgressUpdated(pos, dur)

                        // 每 1 秒更新一次 Service 状态（减少通知刷新频率）
                        if (pos % 1000 < 200) {
                            updateServiceState()
                        }
                    } catch (_: Exception) {}
                }
                delay(200)
            }
        }
    }

    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun releasePlayer() {
        try { mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        stop()
        playJob?.cancel()
        playScope.cancel()
        unbindPlaybackService()
    }

    @Deprecated("使用 play() 方法")
    fun toPlaying() = resume()

    @Deprecated("使用 pause() 方法")
    fun toPause() = pause()
}
