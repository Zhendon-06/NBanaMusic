package com.guet.stu.banamusic.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.guet.stu.banamusic.model.music.Music

class SharedMusicViewModel : ViewModel() {
    private var _currentMusicList: List<Music> = emptyList()
    val currentMusicList: List<Music> get() = _currentMusicList

    private val _currentMusic = MutableLiveData<Music?>()
    val currentMusicLiveData: LiveData<Music?> = _currentMusic
    val currentMusic: Music? get() = _currentMusic.value

    /**
     * 设置当前播放的音乐列表
     */
    fun setMusicList(list: List<Music>) {
        _currentMusicList = list.toList()
    }

    /**
     * 设置当前播放的音乐
     */
    fun setCurrentMusic(music: Music?) {
        _currentMusic.value = music
    }

    /**
     * 清空音乐列表
     */
    fun clearMusicList() {
        _currentMusicList = emptyList()
        _currentMusic.value = null
    }
}

