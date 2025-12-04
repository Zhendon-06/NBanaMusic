package com.guet.stu.banamusic.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.guet.stu.banamusic.util.setLightStatusBar
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.guet.stu.banamusic.R
import com.guet.stu.banamusic.databinding.ActivityMainBinding
import com.guet.stu.banamusic.model.music.Music
import com.guet.stu.banamusic.model.music.MusicPlay
import com.guet.stu.banamusic.viewmodel.SharedMusicViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val sharedMusicViewModel: SharedMusicViewModel by viewModels()
    private val isPlayingState = mutableStateOf(MusicPlay.getPlayingStatus())
    private val coverUrlState = mutableStateOf(MusicPlay.getCurrentMusic()?.pic)
    private val playingStateListener: (Boolean) -> Unit = { isPlaying ->
        isPlayingState.value = isPlaying
        coverUrlState.value = MusicPlay.getCurrentMusic()?.pic
    }
    private var openedFromPlaylist: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 浅色背景下启用“状态栏反色”，让状态栏图标/文字变成深色，避免看不见
        setLightStatusBar(this, true)

        adjustCenterButtonPosition()
        setupNavigation()
        setupCenterComposeButton()
        observeSharedMusic()
        MusicPlay.addPlayingStateChangedListener(playingStateListener)

        // 如果是从歌单详情页等入口带着“播放队列”跳转过来的，处理一次意图
        handlePlayQueueIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePlayQueueIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicPlay.removePlayingStateChangedListener(playingStateListener)
    }

    private fun adjustCenterButtonPosition() {
        val bottomNav = binding.bottomNav
        val centerButton = binding.centerComposeButton

        bottomNav.post {
            val navHeight = bottomNav.height
            val buttonHeight = centerButton.height

            if (navHeight > 0 && buttonHeight > 0) {
                val visiblePart = buttonHeight / 3
                var margin = navHeight - buttonHeight + visiblePart

                if (margin < 0) margin = 0

                val lp = centerButton.layoutParams as CoordinatorLayout.LayoutParams
                lp.bottomMargin = margin
                centerButton.layoutParams = lp
            }
        }
    }

    private fun setupCenterComposeButton() {
        val centerButton: ComposeView = binding.centerComposeButton
        centerButton.setContent {
            MaterialTheme {
                val isPlaying by isPlayingState
                val coverUrl by coverUrlState
                RotatingMusicButton(
                    isPlaying = isPlaying,
                    coverUrl = coverUrl,
                    onClick = ::handleCenterButtonClick
                )
            }
        }
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.content_container) as NavHostFragment
        navController = navHostFragment.navController

        val bottomNav = binding.bottomNav

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.mainpage -> {
                    navigateToDestination(R.id.mainFragment)
                    true
                }
                R.id.mine -> {
                    navigateToDestination(R.id.userFragment)
                    true
                }
                else -> false
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val shouldShow = destination.id == R.id.mainFragment || destination.id == R.id.userFragment
            bottomNav.isVisible = shouldShow
            binding.centerComposeButton.isVisible = shouldShow

            when (destination.id) {
                R.id.mainFragment -> bottomNav.selectedItemId = R.id.mainpage
                R.id.userFragment -> bottomNav.selectedItemId = R.id.mine
            }
        }
    }

    private fun navigateToDestination(destinationId: Int) {
        if (!::navController.isInitialized) return
        val currentId = navController.currentDestination?.id
        if (currentId == destinationId) return

        val options = navOptions {
            launchSingleTop = true
            restoreState = true
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
                inclusive = false
            }
        }
        navController.navigate(destinationId, null, options)
    }

    private fun handleCenterButtonClick() {
        if (!::navController.isInitialized) return
        val music = sharedMusicViewModel.currentMusic ?: MusicPlay.getCurrentMusic()
        if (music == null) {
            Toast.makeText(this, getString(R.string.no_music_playing), Toast.LENGTH_SHORT).show()
            return
        }

        val bundle = Bundle().apply {
            putParcelable(MUSIC_ARG_KEY, music)
        }

        navController.navigate(
            R.id.musicPlayerFragment,
            bundle,
            navOptions {
                launchSingleTop = true
            }
        )
    }

    /**
     * 处理来自外部入口（如歌单详情页等）带来的“使用指定队列打开播放器”的意图。
     */
    private fun handlePlayQueueIntent(intent: Intent?) {
        if (intent == null) return
        openedFromPlaylist = intent.getBooleanExtra(EXTRA_FROM_PLAYLIST, false)
        val queue: ArrayList<Music>? =
            intent.getParcelableArrayListExtra(EXTRA_PLAY_QUEUE)
        val current: Music? =
            intent.getParcelableExtra(EXTRA_CURRENT_MUSIC)

        if (queue.isNullOrEmpty() || current == null) return
        if (!::navController.isInitialized) return

        // 设置当前播放队列与当前歌曲
        sharedMusicViewModel.setMusicList(queue)
        sharedMusicViewModel.setCurrentMusic(current)

        // 跳转到播放器 Fragment
        val bundle = Bundle().apply {
            putParcelable(MUSIC_ARG_KEY, current)
        }
        navController.navigate(
            R.id.musicPlayerFragment,
            bundle,
            navOptions {
                launchSingleTop = true
            }
        )
    }

    private fun observeSharedMusic() {
        sharedMusicViewModel.currentMusicLiveData.observe(this) { music ->
            coverUrlState.value = music?.pic
        }
    }

    @Composable
    fun RotatingMusicButton(
        isPlaying: Boolean,
        coverUrl: String?,
        coverRes: Int = R.drawable.fengmian,
        ringColor: Color = Color(0xFFE3DEDE),
        ringWidth: Dp = 2.dp,
        size: Dp = 80.dp,
        onClick: () -> Unit,
    ) {
        val infinite = rememberInfiniteTransition()
        val rotation by infinite.animateFloat(
            initialValue = 0f,
            targetValue = if (isPlaying) 360f else 0f,
            animationSpec = infiniteRepeatable(
                tween(6000, easing = LinearEasing),
                RepeatMode.Restart
            )
        )

        val context = LocalContext.current
        val painter = rememberAsyncImagePainter(
            model = coverUrl?.takeIf { it.isNotEmpty() }?.let { url ->
                ImageRequest.Builder(context)
                    .data(url)
                    .crossfade(true)
                    .build()
            } ?: coverRes,
            placeholder = painterResource(coverRes),
            error = painterResource(coverRes),
            fallback = painterResource(coverRes)
        )

        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .border(
                    BorderStroke(ringWidth, ringColor),
                    CircleShape
                )
                .clickable { onClick() }
                .graphicsLayer {
                    rotationZ = if (isPlaying) rotation else 0f
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .size(size - ringWidth * 2)
                    .clip(CircleShape)
            )
        }
    }

    override fun onBackPressed() {
        if (openedFromPlaylist && ::navController.isInitialized &&
            navController.currentDestination?.id == R.id.musicPlayerFragment
        ) {
            // 从歌单详情页打开播放器时，在播放器界面按返回键直接回到歌单，而不是主界面
            finish()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        private const val MUSIC_ARG_KEY = "music"
        const val EXTRA_PLAY_QUEUE = "extra_play_queue"
        const val EXTRA_CURRENT_MUSIC = "extra_current_music"
        const val EXTRA_FROM_PLAYLIST = "extra_from_playlist"
    }
}
