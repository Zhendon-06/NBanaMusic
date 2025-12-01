package com.guet.stu.banamusic.model.music

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Music(
    val id: Long,
    val song: String,
    val sing: String,
    val pic: String,
    val url: String
) : Parcelable
