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
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import coil.Coil
import coil.request.ImageRequest
import com.guet.stu.banamusic.R
import com.guet.stu.banamusic.model.music.Music
import com.guet.stu.banamusic.view.MainActivity
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * 音乐播放前台服务
 * - 仅负责显示媒体通知和控制回调
 * - 实际播放逻辑由 MusicPlay 管理
 * - 集成 MediaSession 与系统媒体控制器
 */
class MusicPlaybackService : Service() {

    companion object {
        private const val CHANNEL_ID = "music_playback_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_PLAY_PAUSE = "com.guet.stu.banamusic.PLAY_PAUSE"
        const val ACTION_STOP = "com.guet.stu.banamusic.STOP"
        const val ACTION_NEXT = "com.guet.stu.banamusic.NEXT"
        const val ACTION_PREVIOUS = "com.guet.stu.banamusic.PREVIOUS"
    }

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

    // 上次更新通知的时间戳
    private var lastNotificationUpdateTime: Long = 0
    private val NOTIFICATION_UPDATE_INTERVAL = 1000L

    // 播放控制回调
    private var onPlayPause: (() -> Unit)? = null
    private var onPlayNext: (() -> Unit)? = null
    private var onPlayPrevious: (() -> Unit)? = null
    private var onStop: (() -> Unit)? = null
    private var onSeek: ((position: Int) -> Unit)? = null

    inner class LocalBinder : android.os.Binder() {
        fun getService(): MusicPlaybackService = this@MusicPlaybackService
    }

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        initializeMediaSession()
        // 立即启动前台服务
        startForeground(NOTIFICATION_ID, createEmptyNotification())
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> onPlayPause?.invoke()
            ACTION_STOP -> onStop?.invoke()
            ACTION_NEXT -> onPlayNext?.invoke()
            ACTION_PREVIOUS -> onPlayPrevious?.invoke()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
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
                    onPlayPause?.invoke()
                }

                override fun onPause() {
                    onPlayPause?.invoke()
                }

                override fun onStop() {
                    onStop?.invoke()
                }

                override fun onSkipToNext() {
                    onPlayNext?.invoke()
                }

                override fun onSkipToPrevious() {
                    onPlayPrevious?.invoke()
                }

                override fun onSeekTo(pos: Long) {
                    onSeek?.invoke(pos.toInt())
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
     * 更新播放状态（由 MusicPlay 调用）
     */
    fun updatePlaybackState(music: Music?, playing: Boolean, position: Int, totalDuration: Int) {
        currentMusic = music
        isPlaying = playing
        currentPosition.set(position)
        duration.set(totalDuration)

        music?.let {
            updateMetadata(it)
            // 异步加载封面
            if (cachedAlbumArtUrl != it.pic) {
                loadAlbumArtAsync(it.pic)
            }
        }

        updateMediaSessionState()
        updateNotification()
    }

    /**
     * 设置控制回调
     */
    fun setControlCallbacks(
        playPause: () -> Unit,
        next: () -> Unit,
        previous: () -> Unit,
        stop: () -> Unit,
        seek: (position: Int) -> Unit
    ) {
        onPlayPause = playPause
        onPlayNext = next
        onPlayPrevious = previous
        onStop = stop
        onSeek = seek
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
     * 更新 MediaSession 播放状态
     */
    private fun updateMediaSessionState() {
        val state = if (isPlaying) {
            PlaybackStateCompat.STATE_PLAYING
        } else {
            PlaybackStateCompat.STATE_PAUSED
        }

        val playbackSpeed = if (isPlaying) 1.0f else 0.0f

        val playbackState = PlaybackStateCompat.Builder()
            .setState(state, currentPosition.get().toLong(), playbackSpeed)
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .setBufferedPosition(duration.get().toLong())
            .build()

        mediaSession?.setPlaybackState(playbackState)
    }

    /**
     * 更新通知
     */
    private fun updateNotification() {
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
        val sessionToken = mediaSession?.sessionToken ?: return createEmptyNotification()

        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIcon = cachedAlbumArt ?: BitmapFactory.decodeResource(resources, R.drawable.music)

        val playPauseIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.play
        val playPauseTitle = if (isPlaying) "暂停" else "播放"

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
                    .setCancelButtonIntent(createPendingIntent(ACTION_STOP, 4))
            )
            .addAction(createAction(android.R.drawable.ic_media_previous, "上一首", ACTION_PREVIOUS, 0))
            .addAction(createAction(playPauseIcon, playPauseTitle, ACTION_PLAY_PAUSE, 1))
            .addAction(createAction(android.R.drawable.ic_media_next, "下一首", ACTION_NEXT, 2))
            .addAction(createAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", ACTION_STOP, 3))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .build()
    }

    /**
     * 创建通知动作
     */
    private fun createAction(icon: Int, title: String, action: String, requestCode: Int): NotificationCompat.Action {
        return NotificationCompat.Action(icon, title, createPendingIntent(action, requestCode))
    }

    /**
     * 创建 PendingIntent
     */
    private fun createPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, MusicPlaybackService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * 异步加载专辑封面
     */
    private fun loadAlbumArtAsync(url: String?) {
        if (url.isNullOrEmpty()) {
            cachedAlbumArt = BitmapFactory.decodeResource(resources, R.drawable.music)
            cachedAlbumArtUrl = url
            updateNotification()
            return
        }

        // 如果已经在加载相同的 URL，跳过
        if (cachedAlbumArtUrl == url && cachedAlbumArt != null) {
            return
        }

        // 立即标记为正在加载，防止重复加载
        cachedAlbumArtUrl = url

        serviceScope.launch(Dispatchers.IO) {
            try {
                // 使用 Coil 加载图片
                val imageLoader = Coil.imageLoader(this@MusicPlaybackService)
                val request = ImageRequest.Builder(this@MusicPlaybackService)
                    .data(url)
                    .size(256, 256)
                    .allowHardware(false) // 通知栏不支持硬件加速的 Bitmap
                    .build()

                val result = imageLoader.execute(request)

                if (result is coil.request.SuccessResult) {
                    val drawable = result.drawable
                    val bitmap = if (drawable != null) {
                        // 创建可变的 Bitmap
                        val width = drawable.intrinsicWidth.coerceAtLeast(1)
                        val height = drawable.intrinsicHeight.coerceAtLeast(1)
                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp)
                        drawable.setBounds(0, 0, width, height)
                        drawable.draw(canvas)
                        bmp
                    } else {
                        BitmapFactory.decodeResource(resources, R.drawable.music)
                    }
                    cachedAlbumArt = bitmap
                } else {
                    // 加载失败使用默认图标
                    cachedAlbumArt = BitmapFactory.decodeResource(resources, R.drawable.music)
                }

                // 在主线程更新通知
                withContext(Dispatchers.Main) {
                    notificationManager?.notify(NOTIFICATION_ID, createNotification())
                }
            } catch (e: Exception) {
                e.printStackTrace()
                cachedAlbumArt = BitmapFactory.decodeResource(resources, R.drawable.music)
                withContext(Dispatchers.Main) {
                    notificationManager?.notify(NOTIFICATION_ID, createNotification())
                }
            }
        }
    }

    /**
     * 创建空通知
     */
    private fun createEmptyNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BanaMusic")
            .setContentText("暂无播放")
            .setSmallIcon(R.drawable.appicon)
            .build()
    }
}
