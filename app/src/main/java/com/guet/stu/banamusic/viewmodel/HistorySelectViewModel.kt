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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * “从历史记录选择歌曲”页面的 ViewModel。
 */
class HistorySelectViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repo: PlaylistRepository by lazy {
        PlaylistRepository(AppDatabase.getInstance(application))
    }

    private val _historySongs = MutableLiveData<List<Music>>()
    val historySongs: LiveData<List<Music>> = _historySongs

    init {
        viewModelScope.launch {
            repo.ensureSpecialPlaylists()
            // 加载 HISTORY 特殊歌单中的歌曲
            repo.getPlaylistSongs(SpecialPlaylist.HISTORY.id).collectLatest { playlistWithSongs ->
                val list = playlistWithSongs.songs.map { it.toMusic() }
                _historySongs.postValue(list)
            }
        }
    }

    /**
     * 将选中的历史歌曲加入指定歌单。
     */
    fun addSelectedToPlaylist(targetPid: Long, songIds: List<Long>) {
        val currentList = _historySongs.value.orEmpty()
        if (targetPid <= 0L || songIds.isEmpty() || currentList.isEmpty()) return

        val idSet = songIds.toSet()
        val toAdd = currentList.filter { it.id in idSet }

        viewModelScope.launch {
            toAdd.forEach { music ->
                repo.addSongToPlaylist(targetPid, music)
            }
        }
    }
}



