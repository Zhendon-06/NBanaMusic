package com.guet.stu.banamusic.network

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.guet.stu.banamusic.model.music.Music
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object MusicApiParser {

    private val client = OkHttpClient()
    private val gson = Gson()

    // suspend：让函数可以在协程里运行
    suspend fun fetchMusicList(url: String): List<Music> = withContext(Dispatchers.IO) {

        // 1. 构造请求
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        // 2. 执行网络请求
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw IOException("HTTP error: ${response.code}")
        }

        val json = response.body?.string() ?: throw IOException("Empty body")

        val trimmed = json.trim()
        val cleanedJson = run {
            val startCandidates = listOf(
                trimmed.indexOf('{').takeIf { it >= 0 },
                trimmed.indexOf('[').takeIf { it >= 0 }
            ).filterNotNull()
            val start = startCandidates.minOrNull() ?: -1
            val end = maxOf(
                trimmed.lastIndexOf('}').takeIf { it >= 0 } ?: -1,
                trimmed.lastIndexOf(']').takeIf { it >= 0 } ?: -1
            )
            if (start >= 0 && end > start) {
                trimmed.substring(start, end + 1)
            } else {
                trimmed
            }
        }

        var rootElement: JsonElement = JsonParser.parseString(cleanedJson)
        // 个别接口会返回被再次 JSON 编码的字符串，先尝试解包一次
        if (rootElement.isJsonPrimitive && rootElement.asJsonPrimitive.isString) {
            rootElement = JsonParser.parseString(rootElement.asString)
        }
        val listType = object : TypeToken<List<Music>>() {}.type

        if (rootElement.isJsonArray) {
            // 某些接口直接返回数组
            return@withContext gson.fromJson<List<Music>>(rootElement, listType)
        }

        if (!rootElement.isJsonObject) {
            throw IOException("Unexpected JSON structure: ${cleanedJson.take(200)}")
        }

        // 3. 顶层 JSON：{ code, msg, data }
        val jsonObject = rootElement.asJsonObject

        val code = jsonObject.get("code")?.asInt ?: 200
        if (code != 200) {
            throw IOException("API error: code = $code")
        }

        // 4. 解析 data 数组
        val dataElement = jsonObject.get("data")
            ?: throw IOException("Missing data field")

        return@withContext gson.fromJson<List<Music>>(dataElement, listType)
    }
}
