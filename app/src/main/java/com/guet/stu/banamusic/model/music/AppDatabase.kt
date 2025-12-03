package com.guet.stu.banamusic.model.music

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Playlist::class, MusicEntity::class, PlaylistSongCrossRef::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao
    abstract fun songDao(): SongDao
    abstract fun playlistSongCrossRefDao(): PlaylistSongCrossRefDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "banamusic.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
