package com.guet.stu.banamusic.model.music

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class MusicEntity(
    @PrimaryKey val id: Long,
    val song: String,
    val sing: String,
    val pic: String,
    val url: String
)