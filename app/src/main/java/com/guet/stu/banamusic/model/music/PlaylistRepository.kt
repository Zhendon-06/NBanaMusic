package com.guet.stu.banamusic.model.music

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaylistRepository(private val db: AppDatabase) {

    /**
     * 确保四个内置歌单存在（收藏 / 最近 / 本地 / 猜你喜欢）
     */
    suspend fun ensureSpecialPlaylists() {
        val dao = db.playlistDao()
        SpecialPlaylist.values().forEach { playlist ->
            val existed = dao.getPlaylistById(playlist.id)
            if (existed == null) {
                dao.insertPlaylist(
                    Playlist(
                        playlistId = playlist.id,
                        name = playlist.displayName
                    )
                )
            }
        }
    }

    fun getAllPlaylists(): Flow<List<Playlist>> = db.playlistDao().getAllPlaylists()

    fun getUserPlaylists(): Flow<List<Playlist>> =
        db.playlistDao().getPlaylistsExcluding(SpecialPlaylist.ids)

    suspend fun createPlaylist(name: String): Long {
        return db.playlistDao().insertPlaylist(Playlist(name = name))
    }

    fun getPlaylistSongs(pid: Long) =
        db.playlistDao().getPlaylistWithSongs(pid)

    fun getSpecialPlaylistCountFlow(type: SpecialPlaylist): Flow<Int> =
        db.playlistSongCrossRefDao().playlistSongCountFlow(type.id)

    fun isSongInPlaylistFlow(pid: Long, songId: Long): Flow<Boolean> =
        db.playlistSongCrossRefDao().isSongInPlaylistFlow(pid, songId)

    suspend fun addSongToPlaylist(pid: Long, music: Music) {
        db.songDao().insertSong(music.toEntity())
        db.playlistSongCrossRefDao().insertCrossRef(
            PlaylistSongCrossRef(pid, music.id)
        )
    }

    suspend fun addSongToSpecialPlaylist(type: SpecialPlaylist, music: Music) {
        addSongToPlaylist(type.id, music)
    }

    suspend fun addSongToHistory(music: Music) {
        addSongToSpecialPlaylist(SpecialPlaylist.HISTORY, music)
    }

    suspend fun removeSongs(pid: Long, songIds: List<Long>) {
        if (songIds.isEmpty()) return
        db.playlistSongCrossRefDao().deleteSongsFromPlaylist(pid, songIds)
    }

    suspend fun deleteSong(pid: Long, sid: Long) {
        db.playlistSongCrossRefDao().deleteSongFromPlaylist(pid, sid)
    }

    suspend fun toggleFavorite(music: Music): Boolean {
        val pid = SpecialPlaylist.COLLECT.id
        val exists = db.playlistSongCrossRefDao().isSongInPlaylist(pid, music.id)
        return if (exists) {
            deleteSong(pid, music.id)
            false
        } else {
            addSongToPlaylist(pid, music)
            true
        }
    }

    /**
     * 清空并重新设置本地音乐歌单
     * @param musicList 要添加的音乐列表
     */
    suspend fun setLocalMusicPlaylist(musicList: List<Music>) {
        val pid = SpecialPlaylist.LOCAL.id
        // 先清空现有歌曲关联
        db.playlistSongCrossRefDao().deleteAllSongsFromPlaylist(pid)
        // 添加新歌曲
        musicList.forEach { music ->
            addSongToPlaylist(pid, music)
        }
    }

    /**
     * 批量添加音乐到歌单（如果不存在则添加）
     * @param pid 歌单ID
     * @param musicList 要添加的音乐列表
     */
    suspend fun addSongsToPlaylistIfNotExists(pid: Long, musicList: List<Music>) {
        musicList.forEach { music ->
            val exists = db.playlistSongCrossRefDao().isSongInPlaylist(pid, music.id)
            if (!exists) {
                addSongToPlaylist(pid, music)
            }
        }
    }
}
