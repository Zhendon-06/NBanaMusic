package com.guet.stu.banamusic.model.music

/**
 * 单行歌词模型：
 * @param timeMs 该行歌词开始时间（毫秒）
 * @param text   歌词内容
 */
data class LyricLine(
    val timeMs: Int,
    val text: String
)


