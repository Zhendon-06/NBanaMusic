package com.guet.stu.banamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.guet.stu.banamusic.model.music.AppDatabase
import com.guet.stu.banamusic.model.music.Music
import com.guet.stu.banamusic.model.music.PlaylistRepository
import com.guet.stu.banamusic.model.music.SpecialPlaylist
import com.guet.stu.banamusic.model.music.toMusic
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 搜索页面使用的 ViewModel：从“最近播放”歌单中加载数据并做本地模糊搜索。
 */
class SearchHistoryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repo: PlaylistRepository by lazy {
        PlaylistRepository(AppDatabase.getInstance(application))
    }

    private val _allHistory = MutableLiveData<List<Music>>(emptyList())
    private val _searchResults = MutableLiveData<List<Music>>(emptyList())
    val searchResults: LiveData<List<Music>> = _searchResults

    private var loadJob: Job? = null

    init {
        loadHistory()
    }

    private fun loadHistory() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            repo.ensureSpecialPlaylists()
            repo.getPlaylistSongs(SpecialPlaylist.HISTORY.id)
                .collectLatest { playlistWithSongs ->
                    val list = playlistWithSongs.songs.map { it.toMusic() }
                    _allHistory.postValue(list)
                    // 初始状态：不输入内容时，显示完整最近播放列表
                    _searchResults.postValue(list)
                }
        }
    }

    /**
     * 本地模糊搜索：在“最近播放”列表中按歌名或歌手包含关键字过滤。
     */
    fun search(query: String) {
        val source = _allHistory.value.orEmpty()
        if (query.isBlank()) {
            _searchResults.value = source
            return
        }
        val lower = query.trim().lowercase()
        _searchResults.value = source.filter { music ->
            music.song.lowercase().contains(lower) ||
                    music.sing.lowercase().contains(lower)
        }
    }
}


