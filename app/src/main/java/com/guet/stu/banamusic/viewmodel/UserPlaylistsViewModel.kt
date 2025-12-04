package com.guet.stu.banamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.guet.stu.banamusic.model.music.AppDatabase
import com.guet.stu.banamusic.model.music.Playlist
import com.guet.stu.banamusic.model.music.PlaylistRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * “我的”页面专用 ViewModel：负责加载和新增歌单。
 */
class UserPlaylistsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repo: PlaylistRepository by lazy {
        PlaylistRepository(AppDatabase.getInstance(application))
    }

    /** 所有歌单列表，供 UserFragment 观察并展示在 RecyclerView 中 */
    val playlists: StateFlow<List<Playlist>> =
        repo.getAllPlaylists()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    /** 新建歌单 */
    fun createPlaylist(name: String, onFinished: (() -> Unit)? = null) {
        viewModelScope.launch {
            repo.createPlaylist(name)
            onFinished?.invoke()
        }
    }

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(UserPlaylistsViewModel::class.java)) {
                return UserPlaylistsViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}





