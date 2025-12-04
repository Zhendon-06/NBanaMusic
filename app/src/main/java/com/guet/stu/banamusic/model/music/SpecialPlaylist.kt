package com.guet.stu.banamusic.model.music

/**
 * 定义内置的特殊歌单（收藏、最近、本地、推荐）。
 * 这些歌单在 UI 顶部卡片中展示，不会出现在“我创建的歌单”列表里。
 */
enum class SpecialPlaylist(val id: Long, val displayName: String) {
    COLLECT(1L, "收藏"),
    HISTORY(2L, "最近"),
    LOCAL(3L, "本地"),
    MIGHT_LIKE(4L, "猜你喜欢");

    companion object {
        val ids: List<Long> = values().map { it.id }
    }
}


