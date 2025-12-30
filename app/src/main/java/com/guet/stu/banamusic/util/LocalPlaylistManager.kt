package com.guet.stu.banamusic.util

import com.guet.stu.banamusic.model.music.LocalSong

/**
 * 本地音乐歌单管理器
 * 单例模式，内存保存「本地音乐」歌单
 */
object LocalPlaylistManager {

    private var songs: List<LocalSong> = emptyList()

    /**
     * 设置歌单中的歌曲
     * @param songs 歌曲列表
     */
    fun setSongs(songs: List<LocalSong>) {
        this.songs = songs.toList()
    }

    /**
     * 获取歌单中的歌曲
     * @return 歌曲列表
     */
    fun getSongs(): List<LocalSong> {
        return songs.toList()
    }

    /**
     * 清空歌单
     */
    fun clear() {
        songs = emptyList()
    }

    /**
     * 获取歌单名称
     */
    fun getPlaylistName(): String = "本地音乐"
}

