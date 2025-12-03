package com.guet.stu.banamusic.model.music

class PlaylistRepository(private val db: AppDatabase) {

    fun getAllPlaylists() = db.playlistDao().getAllPlaylists()

    suspend fun createPlaylist(name: String): Long {
        return db.playlistDao().insertPlaylist(Playlist(name = name))
    }

    fun getPlaylistSongs(pid: Long) =
        db.playlistDao().getPlaylistWithSongs(pid)

    suspend fun addSongToPlaylist(pid: Long, music: Music) {
        db.songDao().insertSong(music.toEntity())
        db.playlistSongCrossRefDao().insertCrossRef(
            PlaylistSongCrossRef(pid, music.id)
        )
    }

    suspend fun deleteSong(pid: Long, sid: Long) {
        db.playlistSongCrossRefDao().deleteSongFromPlaylist(pid, sid)
    }
}
