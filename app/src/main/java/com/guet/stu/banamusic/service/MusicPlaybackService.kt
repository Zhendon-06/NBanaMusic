package com.guet.stu.banamusic.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import coil.Coil
import coil.request.ImageRequest
import com.guet.stu.banamusic.R
import com.guet.stu.banamusic.model.music.Music
import com.guet.stu.banamusic.view.MainActivity
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * 音乐播放前台服务
 * - 使用 MediaPlayer 进行音频播放
 * - 集成 MediaSession 与系统媒体控制器
 * - 显示媒体通知卡片
 */
class MusicPlaybackService : Service() {

    companion object {
        private const val CHANNEL_ID = "music_playback_channel"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_PLAY_PAUSE = "com.guet.stu.banamusic.PLAY_PAUSE"
        private const val ACTION_STOP = "com.guet.stu.banamusic.STOP"
        private const val ACTION_NEXT = "com.guet.stu.banamusic.NEXT"
        private const val ACTION_PREVIOUS = "com.guet.stu.banamusic.PREVIOUS"
    }

    private val binder = LocalBinder()
    private var mediaPlayer: MediaPlayer? = null
    private var mediaSession: MediaSessionCompat? = null
    private var notificationManager: NotificationManager? = null

    private var currentMusic: Music? = null
    private var isPlaying: Boolean = false
    private val currentPosition = AtomicInteger(0)
    private val duration = AtomicInteger(0)

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    
    // 专辑封面缓存
    private var cachedAlbumArt: Bitmap? = null
    private var cachedAlbumArtUrl: String? = null
    
    // 上次更新通知的时间戳，用于限制更新频率
    private var lastNotificationUpdateTime: Long = 0
    private val NOTIFICATION_UPDATE_INTERVAL = 1000L // 通知最多每 1 秒更新一次
    
    // seekTo 相关标志，用于防止进度条闪回
    private var isSeeking: Boolean = false
    private var seekTargetPosition: Int = 0
    private var seekStartTime: Long = 0

    // 播放状态变化回调
    private var onPlaybackStateChanged: ((isPlaying: Boolean) -> Unit)? = null
    private var onProgressUpdate: ((currentPosition: Int, duration: Int) -> Unit)? = null

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlaybackService = this@MusicPlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeMediaSession()
        // 立即启动前台服务，避免崩溃（必须在 5 秒内调用 startForeground）
        startForeground(NOTIFICATION_ID, createEmptyNotification())
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> togglePlayback()
            ACTION_STOP -> stopPlayback()
            ACTION_NEXT -> playNext()
            ACTION_PREVIOUS -> playPrevious()
        }
        // 确保前台服务已启动
        if (currentMusic == null) {
            startForeground(NOTIFICATION_ID, createEmptyNotification())
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
        mediaSession?.release()
        serviceScope.cancel()
    }

    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "音乐播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "音乐播放控制通知"
                setShowBadge(false)
            }
            notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        } else {
            notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        }
    }

    /**
     * 初始化 MediaSession
     */
    private fun initializeMediaSession() {
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSessionCompat(this, "MusicPlaybackService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    resumePlayback()
                }

                override fun onPause() {
                    pausePlayback()
                }

                override fun onStop() {
                    stopPlayback()
                }

                override fun onSkipToNext() {
                    playNext()
                }

                override fun onSkipToPrevious() {
                    playPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    seekTo(pos.toInt())
                }
            })

            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )

            isActive = true
        }
    }

    /**
     * 播放音乐
     */
    fun play(music: Music) {
        serviceScope.launch {
            try {
                // 释放旧播放器
                releaseMediaPlayer()

                currentMusic = music
                // 清除旧的专辑封面缓存
                cachedAlbumArt = null
                cachedAlbumArtUrl = null
                updateMetadata(music)

                // 创建 MediaPlayer
                val player = withContext(Dispatchers.IO) {
                    MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )

                        // 设置音乐数据源
                        // 如果是 content URI，使用 setDataSource(Context, Uri) 方法
                        if (music.url.startsWith("content://")) {
                            try {
                                val uri = android.net.Uri.parse(music.url)
                                setDataSource(this@MusicPlaybackService, uri)
                            } catch (e: Exception) {
                                // 如果解析失败，回退到字符串方式
                                setDataSource(music.url)
                            }
                        } else {
                            // 普通 URL 或文件路径
                            setDataSource(music.url)
                        }

                        setOnPreparedListener {
                            this@MusicPlaybackService.isPlaying = true
                            it.start()
                            this@MusicPlaybackService.duration.set(it.duration)
                            // 更新 MediaMetadata（包含时长信息）
                            currentMusic?.let { music ->
                                this@MusicPlaybackService.updateMetadata(music)
                            }
                            this@MusicPlaybackService.updatePlaybackState()
                            this@MusicPlaybackService.startProgressUpdate()
                            this@MusicPlaybackService.onPlaybackStateChanged?.invoke(true)
                        }

                        setOnCompletionListener {
                            this@MusicPlaybackService.isPlaying = false
                            this@MusicPlaybackService.currentPosition.set(0)
                            this@MusicPlaybackService.stopProgressUpdate()
                            this@MusicPlaybackService.updatePlaybackState()
                            this@MusicPlaybackService.onPlaybackStateChanged?.invoke(false)
                        }

                        setOnErrorListener { _, what, extra ->
                            this@MusicPlaybackService.isPlaying = false
                            this@MusicPlaybackService.stopProgressUpdate()
                            this@MusicPlaybackService.updatePlaybackState()
                            this@MusicPlaybackService.onPlaybackStateChanged?.invoke(false)
                            true
                        }

                        prepareAsync()
                    }
                }

                mediaPlayer = player
                startForeground(NOTIFICATION_ID, createNotification())

            } catch (e: Exception) {
                isPlaying = false
                updatePlaybackState()
            }
        }
    }

    /**
     * 暂停播放
     */
    fun pausePlayback() {
        serviceScope.launch {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    isPlaying = false
                    stopProgressUpdate()
                    updatePlaybackState()
                    onPlaybackStateChanged?.invoke(false)
                }
            }
        }
    }

    /**
     * 恢复播放
     */
    fun resumePlayback() {
        serviceScope.launch {
            mediaPlayer?.let {
                if (!it.isPlaying && currentPosition.get() < duration.get()) {
                    it.start()
                    isPlaying = true
                    updatePlaybackState()
                    startProgressUpdate()
                    onPlaybackStateChanged?.invoke(true)
                }
            }
        }
    }

    /**
     * 切换播放/暂停
     */
    fun togglePlayback() {
        if (isPlaying) {
            pausePlayback()
        } else {
            resumePlayback()
        }
    }

    /**
     * 停止播放
     */
    fun stopPlayback() {
        serviceScope.launch {
            isPlaying = false
            stopProgressUpdate()
            releaseMediaPlayer()
            currentMusic = null
            currentPosition.set(0)
            duration.set(0)
            updatePlaybackState()
            stopForeground(true)
            stopSelf()
            onPlaybackStateChanged?.invoke(false)
        }
    }

    /**
     * 跳转到指定位置
     */
    fun seekTo(position: Int) {
        serviceScope.launch {
            mediaPlayer?.let {
                try {
                    // 设置 seekTo 标志，防止进度更新循环读取到错误位置
                    isSeeking = true
                    seekTargetPosition = position
                    seekStartTime = System.currentTimeMillis()
                    
                    it.seekTo(position)
                    currentPosition.set(position)
                    onProgressUpdate?.invoke(position, duration.get())
                    
                    // 立即更新 MediaSession 的 PlaybackState 并强制更新通知
                    // MIUI 系统需要强制刷新通知才能正确显示进度条位置
                    withContext(Dispatchers.Main) {
                        // 先更新 MediaSession
                        updatePlaybackStateOnly()
                        // 强制更新通知，确保进度条位置立即刷新
                        notificationManager?.notify(NOTIFICATION_ID, createNotification())
                        lastNotificationUpdateTime = System.currentTimeMillis()
                    }
                    
                    // 等待一小段时间，让 MediaPlayer 完成跳转
                    delay(100)
                    
                    // 检查 MediaPlayer 的实际位置是否接近目标位置
                    var actualPos = it.currentPosition
                    var retryCount = 0
                    while (Math.abs(actualPos - position) > 500 && retryCount < 5) {
                        delay(50)
                        actualPos = it.currentPosition
                        retryCount++
                    }
                    
                    // 更新到实际位置
                    currentPosition.set(actualPos)
                    
                    // 清除 seekTo 标志
                    isSeeking = false
                    
                    // 如果正在播放，确保进度更新循环继续运行
                    if (isPlaying) {
                        // 进度更新循环应该已经在运行，但确保它继续
                        // 如果因为某种原因停止了，重新启动
                        if (progressJob?.isActive != true) {
                            startProgressUpdate()
                        }
                    }
                } catch (_: Exception) {
                    isSeeking = false
                }
            }
        }
    }

    /**
     * 播放下一首（需要外部实现播放列表逻辑）
     */
    private fun playNext() {
        // TODO: 实现播放下一首逻辑
    }

    /**
     * 播放上一首（需要外部实现播放列表逻辑）
     */
    private fun playPrevious() {
        // TODO: 实现播放上一首逻辑
    }

    /**
     * 更新 MediaMetadata
     */
    private fun updateMetadata(music: Music) {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, music.song)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, music.sing)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, music.pic)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration.get().toLong())
            .build()

        mediaSession?.setMetadata(metadata)
    }

    /**
     * 只更新 MediaSession 的 PlaybackState（不更新通知）
     * 用于进度更新，避免频繁更新通知
     */
    private fun updatePlaybackStateOnly() {
        val state = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        val currentPos = currentPosition.get().toLong()
        val totalDuration = duration.get().toLong()
        
        val playbackSpeed = if (isPlaying && totalDuration > 0) 1.0f else 0.0f

        val playbackState = PlaybackStateCompat.Builder()
            .setState(
                state,
                currentPos,
                playbackSpeed,
                System.currentTimeMillis()
            )
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setBufferedPosition(totalDuration)
            .build()

        mediaSession?.setPlaybackState(playbackState)
        // 不更新通知，让 MediaStyle 自动从 MediaSession 获取进度
    }

    /**
     * 更新播放状态并更新通知
     * 用于状态变化时（播放/暂停/切换歌曲等）
     */
    private fun updatePlaybackState() {
        val state = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        val currentPos = currentPosition.get().toLong()
        val totalDuration = duration.get().toLong()
        
        // 计算播放速度（播放时为 1.0，暂停时为 0.0）
        val playbackSpeed = if (isPlaying && totalDuration > 0) 1.0f else 0.0f

        val playbackState = PlaybackStateCompat.Builder()
            .setState(
                state,
                currentPos,
                playbackSpeed,
                System.currentTimeMillis()
            )
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setBufferedPosition(totalDuration) // 设置缓冲位置为总时长，表示已完全加载
            .setActiveQueueItemId(0) // 设置活动队列项 ID
            .build()

        mediaSession?.setPlaybackState(playbackState)
        
        // MediaStyle 通知会自动从 MediaSession 获取进度信息
        // 不需要频繁更新通知，只在状态变化或间隔足够长时更新
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNotificationUpdateTime >= NOTIFICATION_UPDATE_INTERVAL) {
            notificationManager?.notify(NOTIFICATION_ID, createNotification())
            lastNotificationUpdateTime = currentTime
        }
    }

    /**
     * 创建媒体通知
     */
    private fun createNotification(): Notification {
        val music = currentMusic ?: return createEmptyNotification()

        val sessionToken = mediaSession?.sessionToken
            ?: return createEmptyNotification()

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 使用缓存的专辑封面，如果没有则使用默认图标（异步加载后会更新）
        val largeIcon = if (cachedAlbumArtUrl == music.pic && cachedAlbumArt != null) {
            cachedAlbumArt
        } else {
            BitmapFactory.decodeResource(resources, R.drawable.music)
        }
        
        // 异步加载专辑封面（如果 URL 变化了）
        if (cachedAlbumArtUrl != music.pic) {
            loadAlbumArtAsync(music.pic)
        }

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                R.drawable.ic_pause,
                "暂停",
                createPendingIntent(ACTION_PLAY_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.play,
                "播放",
                createPendingIntent(ACTION_PLAY_PAUSE)
            )
        }

        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_menu_close_clear_cancel,
            "停止",
            createPendingIntent(ACTION_STOP)
        )

        val nextAction = NotificationCompat.Action(
            android.R.drawable.ic_media_next,
            "下一首",
            createPendingIntent(ACTION_NEXT)
        )

        val previousAction = NotificationCompat.Action(
            android.R.drawable.ic_media_previous,
            "上一首",
            createPendingIntent(ACTION_PREVIOUS)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(music.song)
            .setContentText(music.sing)
            .setSmallIcon(R.drawable.appicon)
            .setLargeIcon(largeIcon)
            .setContentIntent(mainPendingIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(sessionToken)
                    .setShowCancelButton(true)
            )
            .addAction(previousAction)
            .addAction(playPauseAction)
            .addAction(stopAction)
            .addAction(nextAction)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
    }

    /**
     * 异步加载专辑封面图片（不阻塞线程）
     */
    private fun loadAlbumArtAsync(url: String?) {
        if (url.isNullOrEmpty()) {
            cachedAlbumArt = BitmapFactory.decodeResource(resources, R.drawable.music)
            cachedAlbumArtUrl = url
            return
        }

        // 如果已经在加载相同的 URL，跳过
        if (cachedAlbumArtUrl == url && cachedAlbumArt != null) {
            return
        }

        serviceScope.launch(Dispatchers.IO) {
            try {
                val imageLoader = Coil.imageLoader(this@MusicPlaybackService)
                val request = ImageRequest.Builder(this@MusicPlaybackService)
                    .data(url)
                    .size(256, 256) // 通知大图标推荐尺寸
                    .build()
                
                val drawable = imageLoader.execute(request).drawable
                
                // 将 Drawable 转换为 Bitmap
                val bitmap = if (drawable != null) {
                    val bitmap = Bitmap.createBitmap(
                        drawable.intrinsicWidth.coerceAtLeast(1),
                        drawable.intrinsicHeight.coerceAtLeast(1),
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                    bitmap
                } else {
                    BitmapFactory.decodeResource(resources, R.drawable.music)
                }
                
                // 更新缓存并刷新通知
                cachedAlbumArt = bitmap
                cachedAlbumArtUrl = url
                
                // 在主线程更新通知
                withContext(Dispatchers.Main) {
                    notificationManager?.notify(NOTIFICATION_ID, createNotification())
                }
            } catch (e: Exception) {
                // 加载失败时使用默认图标
                cachedAlbumArt = BitmapFactory.decodeResource(resources, R.drawable.music)
                cachedAlbumArtUrl = url
            }
        }
    }

    /**
     * 创建空通知（当没有音乐时）
     */
    private fun createEmptyNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BanaMusic")
            .setContentText("暂无播放")
            .setSmallIcon(R.drawable.appicon)
            .build()
    }

    /**
     * 创建 PendingIntent
     */
    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * 开始更新进度
     */
    private fun startProgressUpdate() {
        stopProgressUpdate()

        progressJob = serviceScope.launch {
            while (isActive && mediaPlayer != null && isPlaying) {
                mediaPlayer?.let {
                    try {
                        val pos = it.currentPosition
                        val dur = it.duration
                        
                        // 如果正在 seekTo，检查位置是否合理
                        if (isSeeking) {
                            val timeSinceSeek = System.currentTimeMillis() - seekStartTime
                            // 如果 seekTo 后位置突然变小很多，可能是 seekTo 还没完成，跳过这次更新
                            if (timeSinceSeek < 300 && pos < seekTargetPosition - 1000) {
                                delay(200)
                                continue
                            }
                            // 如果位置接近目标位置，清除 seekTo 标志
                            if (Math.abs(pos - seekTargetPosition) < 500 || timeSinceSeek > 500) {
                                isSeeking = false
                            }
                        }

                        currentPosition.set(pos)
                        if (dur > 0) duration.set(dur)

                        onProgressUpdate?.invoke(pos, dur)
                        // 只更新 MediaSession 的 PlaybackState，不更新通知
                        // MediaStyle 通知会自动从 MediaSession 获取进度信息
                        withContext(Dispatchers.Main) {
                            updatePlaybackStateOnly()
                        }
                    } catch (_: Exception) {
                    }
                }
                delay(200) // 每 200ms 更新一次，确保进度条流畅
            }
        }
    }

    /**
     * 停止更新进度
     */
    private fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    /**
     * 释放 MediaPlayer
     */
    private fun releaseMediaPlayer() {
        serviceScope.launch {
            try {
                mediaPlayer?.release()
            } catch (_: Exception) {
            }
            mediaPlayer = null
        }
    }

    // 公共方法供外部调用
    fun getCurrentMusic(): Music? = currentMusic
    fun getIsPlaying(): Boolean = isPlaying
    fun getCurrentPosition(): Int = currentPosition.get()
    fun getDuration(): Int = duration.get()

    fun setOnPlaybackStateChangedListener(callback: (isPlaying: Boolean) -> Unit) {
        onPlaybackStateChanged = callback
    }

    fun setOnProgressUpdateListener(callback: (currentPosition: Int, duration: Int) -> Unit) {
        onProgressUpdate = callback
    }
}

