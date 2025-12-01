package com.guet.stu.banamusic.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * 简单歌词加载工具：
 * 从给定的 url 拉取文本，并按照 LRC 格式解析为一行一行的歌词。
 */
object LyricLoader {

    private val client = OkHttpClient()

    suspend fun loadLyricsById(songId: Long): List<String> = withContext(Dispatchers.IO) {
        val lyricUrl = "https://music.163.com/api/song/media?id=$songId"

        val request = Request.Builder()
            .url(lyricUrl)
            .get()
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("Load lyrics HTTP error: ${response.code}")
        }

        val body = response.body?.string() ?: throw IOException("Lyrics body empty")

        // 解析 JSON，取出 lyric 字段
        val lyricText = try {
            val json = com.google.gson.JsonParser.parseString(body).asJsonObject
            json.get("lyric")?.asString ?: ""
        } catch (e: Exception) {
            ""
        }

        if (lyricText.isBlank()) {
            return@withContext emptyList<String>()
        }

        // 按行拆分，并尝试解析 LRC 时间标签
        val lines = lyricText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { stripLrcTag(it) }
            .filter { it.isNotEmpty() }
            .toList()

        return@withContext lines
    }

    /**
     * 去掉类似 [00:01.23] 这样的 LRC 时间标签，返回纯歌词文本
     */
    private fun stripLrcTag(raw: String): String {
        val regex = Regex("^\\[[0-9:.]+]\\s*(.*)$")
        val match = regex.find(raw) ?: return raw
        return match.groupValues.getOrNull(1)?.trim().orEmpty()
    }
}


