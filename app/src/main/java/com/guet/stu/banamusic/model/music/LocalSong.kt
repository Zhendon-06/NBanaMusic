package com.guet.stu.banamusic.model.music

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 本地音乐数据类
 * 用于表示从设备扫描到的本地音频文件
 */
@Parcelize
data class LocalSong(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val path: String,
    val uri: Uri,
    val albumArtUri: Uri? = null
) : Parcelable {
    /**
     * 将 LocalSong 转换为 Music 对象，用于播放
     */
    fun toMusic(): Music {
        return Music(
            id = id,
            song = title.ifEmpty { "未知歌曲" },
            sing = artist.ifEmpty { "未知艺术家" },
            pic = albumArtUri?.toString() ?: "", // 使用专辑封面 URI，如果没有则为空字符串
            url = uri.toString() // 使用 content URI 作为播放地址
        )
    }
}

