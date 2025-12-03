package com.guet.stu.banamusic.network

import com.guet.stu.banamusic.model.music.LyricLine
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

    /**
     * 根据歌曲 ID 拉取 LRC 歌词，并解析为带时间戳的行列表。
     */
    suspend fun loadLyricsById(songId: Long): List<LyricLine> = withContext(Dispatchers.IO) {
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
            return@withContext emptyList<LyricLine>()
        }

        // 按行拆分，并按照 LRC 时间标签解析时间与文本
        val result = mutableListOf<LyricLine>()
        lyricText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { rawLine ->
                // 一个原始行中可能包含多个时间标签，例如 [00:10.00][00:20.00]歌词
                val timeTags = TIME_TAG_REGEX.findAll(rawLine).toList()
                if (timeTags.isEmpty()) {
                    return@forEach
                }
                // 去掉所有时间标签后剩余的纯文本
                val pureText = rawLine.replace(TIME_TAG_REGEX, "").trim()
                if (pureText.isEmpty()) return@forEach

                timeTags.forEach { match ->
                    val minute = match.groupValues.getOrNull(1)?.toIntOrNull() ?: 0
                    val second = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
                    val fraction = match.groupValues.getOrNull(3)?.trimStart('.') ?: "0"
                    // fraction 可能是两位或三位，统一换算成毫秒
                    val millis = when (fraction.length) {
                        0 -> 0
                        1 -> (fraction.toIntOrNull() ?: 0) * 100
                        2 -> (fraction.toIntOrNull() ?: 0) * 10
                        else -> fraction.take(3).toIntOrNull() ?: 0
                    }
                    val timeMs = minute * 60_000 + second * 1_000 + millis
                    result.add(LyricLine(timeMs = timeMs, text = pureText))
                }
            }

        // 按时间排序，去掉完全重复的行
        val sorted = result
            .sortedBy { it.timeMs }
            .distinctBy { it.timeMs to it.text }

        return@withContext sorted
    }

    // [mm:ss.xx] 或 [mm:ss.xxx] 格式的时间标签
    private val TIME_TAG_REGEX = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{1,3}))?]""")
}


