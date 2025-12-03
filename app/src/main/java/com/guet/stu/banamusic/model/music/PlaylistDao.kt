package com.guet.stu.banamusic.model.music

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {

    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Query("SELECT * FROM playlists")
    fun getAllPlaylists(): Flow<List<Playlist>>

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Transaction
    @Query("SELECT * FROM playlists WHERE playlistId = :pid")
    fun getPlaylistWithSongs(pid: Long): Flow<PlaylistWithSongs>
}
