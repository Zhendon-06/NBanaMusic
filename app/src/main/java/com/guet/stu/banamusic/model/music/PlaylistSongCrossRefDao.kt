package com.guet.stu.banamusic.model.music

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistSongCrossRefDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :pid AND songId = :sid")
    suspend fun deleteSongFromPlaylist(pid: Long, sid: Long)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :pid AND songId IN (:songIds)")
    suspend fun deleteSongsFromPlaylist(pid: Long, songIds: List<Long>)

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_song_cross_ref WHERE playlistId = :pid AND songId = :sid)")
    fun isSongInPlaylistFlow(pid: Long, sid: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_song_cross_ref WHERE playlistId = :pid AND songId = :sid)")
    suspend fun isSongInPlaylist(pid: Long, sid: Long): Boolean

    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlistId = :pid")
    fun playlistSongCountFlow(pid: Long): Flow<Int>
}
