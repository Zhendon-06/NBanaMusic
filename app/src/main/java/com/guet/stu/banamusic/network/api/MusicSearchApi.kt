package com.guet.stu.banamusic.network.api

import com.guet.stu.banamusic.network.model.MusicUrlResponse
import com.guet.stu.banamusic.network.model.SearchSongResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 音乐搜索API接口
 */
interface MusicSearchApi {
    
    /**
     * 搜索歌曲
     * GET https://music-dl.sayqz.com/api/?type=aggregateSearch&keyword=xxx&limit=20&page=1
     */
    @GET("/api/")
    suspend fun searchSongs(
        @Query("type") type: String = "aggregateSearch",
        @Query("keyword") keyword: String,
        @Query("limit") limit: Int = 20,
        @Query("page") page: Int = 1
    ): SearchSongResponse
    
    /**
     * 获取歌曲播放地址
     * GET https://music-dl.sayqz.com/api/?type=url&id=xxx&source=xxx
     */
    @GET("/api/")
    suspend fun getMusicUrl(
        @Query("type") type: String = "url",
        @Query("id") id: String,
        @Query("source") source: String
    ): MusicUrlResponse
}



