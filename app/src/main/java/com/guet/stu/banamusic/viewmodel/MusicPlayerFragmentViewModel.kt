package com.guet.stu.banamusic.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.guet.stu.banamusic.model.music.Music

/**
 * 用于播放器页面的状态容器，维护当前选中的歌曲和整个音乐列表。
 * 借助 SavedStateHandle，在进程被杀后也能恢复音乐对象。
 */
class MusicPlayerFragmentViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_CURRENT_MUSIC = "music"
        private const val KEY_MUSIC_LIST = "musicList"
        private const val KEY_CURRENT_INDEX = "currentIndex"
    }

    // Fragment 通过 LiveData 观察当前音乐变化
    val currentMusic: LiveData<Music?> = savedStateHandle.getLiveData(KEY_CURRENT_MUSIC)
    
    // 音乐列表
    private val _musicList = MutableLiveData<List<Music>>(emptyList())
    val musicList: LiveData<List<Music>> = _musicList
    
    // 当前索引
    private val _currentIndex = MutableLiveData<Int>(-1)
    val currentIndex: LiveData<Int> = _currentIndex
    
    init {
        // 从 SavedStateHandle 恢复音乐列表和索引
        val savedList = savedStateHandle.get<ArrayList<Music>>(KEY_MUSIC_LIST)
        if (savedList != null) {
            _musicList.value = savedList
        }
        val savedIndex = savedStateHandle.get<Int>(KEY_CURRENT_INDEX)
        if (savedIndex != null && savedIndex >= 0) {
            _currentIndex.value = savedIndex
        }
    }

    /**
     * 设置音乐列表和当前播放的音乐
     * @param list 完整的音乐列表
     * @param music 当前要播放的音乐
     */
    fun setMusicListAndCurrent(list: List<Music>, music: Music) {
        _musicList.value = list
        val index = list.indexOfFirst { it.id == music.id }
        if (index >= 0) {
            _currentIndex.value = index
            savedStateHandle[KEY_CURRENT_MUSIC] = music
            // SavedStateHandle 支持 ArrayList，但不支持 Array，所以转换为 ArrayList
            savedStateHandle[KEY_MUSIC_LIST] = ArrayList(list)
            savedStateHandle[KEY_CURRENT_INDEX] = index
        }
    }

    /**
     * 播放上一首
     * @return 上一首音乐，如果没有则返回 null
     */
    fun playPrevious(): Music? {
        val list = _musicList.value ?: return null
        val currentIdx = _currentIndex.value ?: -1
        if (list.isEmpty() || currentIdx < 0) return null
        
        val previousIndex = if (currentIdx > 0) currentIdx - 1 else list.size - 1
        val previousMusic = list[previousIndex]
        _currentIndex.value = previousIndex
        savedStateHandle[KEY_CURRENT_INDEX] = previousIndex
        savedStateHandle[KEY_CURRENT_MUSIC] = previousMusic
        return previousMusic
    }

    /**
     * 播放下一首
     * @return 下一首音乐，如果没有则返回 null
     */
    fun playNext(): Music? {
        val list = _musicList.value ?: return null
        val currentIdx = _currentIndex.value ?: -1
        if (list.isEmpty() || currentIdx < 0) return null
        
        val nextIndex = if (currentIdx < list.size - 1) currentIdx + 1 else 0
        val nextMusic = list[nextIndex]
        _currentIndex.value = nextIndex
        savedStateHandle[KEY_CURRENT_INDEX] = nextIndex
        savedStateHandle[KEY_CURRENT_MUSIC] = nextMusic
        return nextMusic
    }

    /**
     * 随机播放一首（可选择是否排除当前正在播放的歌曲）。
     */
    fun playRandom(excludeCurrent: Boolean = false): Music? {
        val list = _musicList.value ?: return null
        if (list.isEmpty()) return null

        val currentIdx = _currentIndex.value ?: -1
        val indices = list.indices.toMutableList()
        if (excludeCurrent && currentIdx in indices && indices.size > 1) {
            indices.remove(currentIdx)
        }
        if (indices.isEmpty()) return null

        val nextIndex = indices.random()
        val nextMusic = list[nextIndex]
        _currentIndex.value = nextIndex
        savedStateHandle[KEY_CURRENT_INDEX] = nextIndex
        savedStateHandle[KEY_CURRENT_MUSIC] = nextMusic
        return nextMusic
    }

    /**
     * 重复当前歌曲（单曲循环模式下调用）。
     */
    fun repeatCurrent(): Music? {
        val list = _musicList.value ?: return null
        val currentIdx = _currentIndex.value ?: -1
        if (list.isEmpty() || currentIdx !in list.indices) return null

        val music = list[currentIdx]
        savedStateHandle[KEY_CURRENT_MUSIC] = music
        return music
    }
}