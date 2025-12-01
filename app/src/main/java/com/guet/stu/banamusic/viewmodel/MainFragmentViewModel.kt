package com.guet.stu.banamusic.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guet.stu.banamusic.model.music.Music
import com.guet.stu.banamusic.network.MusicApiParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 首页使用的 ViewModel：分别请求"猜你喜欢"和"每日精选"列表数据。
 * 使用缓存机制，避免频繁加载 API。
 */
class MainFragmentViewModel : ViewModel() {

    companion object {
        private const val URL_LIVE = "https://api.52vmy.cn/api/music/wy/top?t=2"
        private const val URL_DAY = "https://api.52vmy.cn/api/music/wy/top?t=3"
    }

    private val _liveMusic = MutableLiveData<List<Music>>()
    val liveMusic: LiveData<List<Music>> = _liveMusic

    private val _dayMusic = MutableLiveData<List<Music>>()
    val dayMusic: LiveData<List<Music>> = _dayMusic

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val requestMutex = Mutex()
    
    // 标记是否已经加载过数据
    private var hasLoadedLiveMusic = false
    private var hasLoadedDayMusic = false

    init {
        // 只在首次创建时加载数据，如果数据已存在则不重新加载
        if (!hasLoadedLiveMusic && _liveMusic.value.isNullOrEmpty()) {
            refreshLiveMusic()
        }
        if (!hasLoadedDayMusic && _dayMusic.value.isNullOrEmpty()) {
            refreshDayMusic()
        }
    }

    /**
     * 刷新"猜你喜欢"列表（强制重新加载）
     */
    fun refreshLiveMusic() = fetchMusic(URL_LIVE, _liveMusic, true)

    /**
     * 刷新"每日精选"列表（强制重新加载）
     */
    fun refreshDayMusic() = fetchMusic(URL_DAY, _dayMusic, true)
    
    /**
     * 加载"猜你喜欢"列表（如果已存在则不加载）
     */
    fun loadLiveMusicIfNeeded() {
        if (!hasLoadedLiveMusic && _liveMusic.value.isNullOrEmpty()) {
            fetchMusic(URL_LIVE, _liveMusic, false)
        }
    }
    
    /**
     * 加载"每日精选"列表（如果已存在则不加载）
     */
    fun loadDayMusicIfNeeded() {
        if (!hasLoadedDayMusic && _dayMusic.value.isNullOrEmpty()) {
            fetchMusic(URL_DAY, _dayMusic, false)
        }
    }

    private fun fetchMusic(url: String, target: MutableLiveData<List<Music>>, isRefresh: Boolean) {
        viewModelScope.launch {
            try {
                val list = requestMutex.withLock {
                    val result = MusicApiParser.fetchMusicList(url)
                    // 防止接口判定访问过快
                    delay(800)
                    result
                }
                target.postValue(list)
                
                // 标记已加载
                when (url) {
                    URL_LIVE -> hasLoadedLiveMusic = true
                    URL_DAY -> hasLoadedDayMusic = true
                }
            } catch (e: Exception) {
                _errorMessage.postValue(e.message ?: "网络异常，请稍后重试")
            }
        }
    }
}