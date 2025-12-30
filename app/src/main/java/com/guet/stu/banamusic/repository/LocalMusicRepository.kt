package com.guet.stu.banamusic.repository

import android.content.Context
import com.guet.stu.banamusic.model.music.LocalSong
import com.guet.stu.banamusic.util.LocalMusicScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 本地音乐 Repository
 * 提供本地音乐扫描功能
 */
class LocalMusicRepository(private val context: Context) {

    /**
     * 扫描本地音频文件
     * @return 扫描到的音乐列表
     */
    suspend fun scanLocalSongs(): List<LocalSong> = withContext(Dispatchers.IO) {
        LocalMusicScanner.scanLocalSongs(context)
    }
}

