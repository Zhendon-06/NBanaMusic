package com.guet.stu.banamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.guet.stu.banamusic.model.music.AppDatabase
import com.guet.stu.banamusic.model.music.Music
import com.guet.stu.banamusic.model.music.PlaylistRepository
import com.guet.stu.banamusic.model.music.SpecialPlaylist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * 提供播放过程中常用的歌单操作（收藏 / 历史等）。
 */
class PlaylistActionsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repo: PlaylistRepository by lazy {
        PlaylistRepository(AppDatabase.getInstance(application))
    }

    init {
        viewModelScope.launch {
            repo.ensureSpecialPlaylists()
        }
    }

    fun observeFavorite(songId: Long): Flow<Boolean> =
        repo.isSongInPlaylistFlow(SpecialPlaylist.COLLECT.id, songId)

    fun toggleFavorite(music: Music) {
        viewModelScope.launch {
            repo.toggleFavorite(music)
        }
    }

    fun logHistory(music: Music) {
        viewModelScope.launch {
            repo.addSongToHistory(music)
        }
    }

    fun specialPlaylistId(type: SpecialPlaylist): Long = type.id

    fun specialPlaylistName(type: SpecialPlaylist): String = type.displayName

    class Factory(
        private val application: Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PlaylistActionsViewModel::class.java)) {
                return PlaylistActionsViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}


