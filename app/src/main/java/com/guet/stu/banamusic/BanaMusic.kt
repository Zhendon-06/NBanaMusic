package com.guet.stu.banamusic

import android.app.Application
import android.view.HapticFeedbackConstants
import android.view.View

import com.guet.stu.banamusic.model.music.MusicPlay

class BanaMusic : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化 MusicPlay 的 Context，用于播放本地音乐的 content URI
        MusicPlay.init(this)
    }
}


