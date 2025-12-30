package com.guet.stu.banamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.guet.stu.banamusic.model.music.LocalSong
import com.guet.stu.banamusic.repository.LocalMusicRepository
import com.guet.stu.banamusic.util.LocalPlaylistManager
import kotlinx.coroutines.launch

/**
 * 本地音乐 ViewModel
 * 管理本地音乐扫描和加载
 */
class LocalMusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocalMusicRepository(application)

    private val _localSongs = MutableLiveData<List<LocalSong>>(emptyList())
    val localSongs: LiveData<List<LocalSong>> = _localSongs

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * 加载本地音乐
     */
    fun loadLocalMusic() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val songs = repository.scanLocalSongs()
                _localSongs.value = songs
                
                // 更新歌单管理器
                LocalPlaylistManager.setSongs(songs)
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "扫描本地音乐失败"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}

