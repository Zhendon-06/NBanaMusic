package com.guet.stu.banamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.guet.stu.banamusic.model.music.AppDatabase
import com.guet.stu.banamusic.model.music.Music
import com.guet.stu.banamusic.model.music.PlaylistRepository
import com.guet.stu.banamusic.model.music.PlaylistWithSongs
import com.guet.stu.banamusic.model.music.toMusic
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 歌单详情页 ViewModel：负责根据歌单 ID 加载歌曲列表。
 */
class PlayListActivityViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repo: PlaylistRepository by lazy {
        PlaylistRepository(AppDatabase.getInstance(application))
    }

    private val _songs = MutableLiveData<List<Music>>()
    val songs: LiveData<List<Music>> = _songs

    fun bindPlaylist(pid: Long) {
        viewModelScope.launch {
            repo.getPlaylistSongs(pid).collectLatest { playlistWithSongs ->
                val list = playlistWithSongs.songs.map { it.toMusic() }
                _songs.postValue(list)
            }
        }
    }

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PlayListActivityViewModel::class.java)) {
                return PlayListActivityViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}