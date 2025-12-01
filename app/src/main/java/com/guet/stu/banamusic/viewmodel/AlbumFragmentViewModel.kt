package com.guet.stu.banamusic.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.guet.stu.banamusic.model.music.Music
import com.guet.stu.banamusic.network.MusicApiParser
import kotlinx.coroutines.launch

class AlbumFragmentViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    companion object {
        const val ARG_ALBUM_TYPE = "albumType"//向viewmodel传值所用的key
        private const val DEFAULT_ALBUM_TYPE = 1//默认值，防止空指针
         private const val DEFAULT_API_URL = "https://api.52vmy.cn/api/music/wy/top?t=1"//默认使用的api
        const val TAG = "AlbumFragmentVM"//日志标记
    }
    private val _musicList = MutableLiveData<List<Music>>()
    val musicList: LiveData<List<Music>> = _musicList//外部暴露的music列表

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading//

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    //传入的参数决定显示那个api
    val albumType = savedStateHandle.get<Int>(ARG_ALBUM_TYPE) ?: DEFAULT_ALBUM_TYPE
    // 只计算一次 API 地址，供后续刷新共用
    val apiUrl = buildApiUrl(albumType)

    init {
        refreshMusic()
    }
//处理网络请求
    fun refreshMusic() {
        viewModelScope.launch {
            try {
                _errorMessage.postValue(null)
                _isLoading.postValue(true)
                // 直接使用封装好的解析器请求网络列表
                val list = MusicApiParser.fetchMusicList(apiUrl)
                _musicList.postValue(list)
            } catch (e: Exception) {
                Log.e(TAG, "Fetch failed: ${e.message}", e)
                _errorMessage.postValue(e.message ?: "Unknown error")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
//根据点击的不同的层面传入选择api，这个函数只负责返回
    private fun buildApiUrl(type: Int): String = when (type) {
        1 -> "https://api.52vmy.cn/api/music/wy/top?t=1"
        2 -> "https://api.52vmy.cn/api/music/wy/top"
        3 -> "https://api.52vmy.cn/api/music/wy/top?t=2"
        4 -> "https://api.52vmy.cn/api/music/wy/top?t=3"
        else -> DEFAULT_API_URL
    }
}

