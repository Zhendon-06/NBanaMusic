package com.guet.stu.banamusic.network.model

import com.google.gson.annotations.SerializedName
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

/**
 * 搜索API响应数据模型
 * 支持 data 字段为数组或对象的情况
 */
data class SearchSongResponse(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("msg")
    val msg: String?,
    @SerializedName("data")
    val data: SearchSongData?
)

/**
 * 搜索结果数据容器
 * 支持 data 是数组或包含列表字段的对象
 */
data class SearchSongData(
    val list: List<SearchSongItem>? = null,
    val songs: List<SearchSongItem>? = null,
    val results: List<SearchSongItem>? = null,
    val data: List<SearchSongItem>? = null
) {
    /**
     * 获取歌曲列表，尝试从各个可能的字段中获取
     */
    fun getSongsList(): List<SearchSongItem> {
        return list ?: songs ?: results ?: data ?: emptyList()
    }
}

/**
 * 自定义反序列化器，处理 data 可能是数组或对象的情况
 */
class SearchSongResponseDeserializer : JsonDeserializer<SearchSongResponse> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): SearchSongResponse {
        if (json == null || !json.isJsonObject) {
            throw JsonParseException("Invalid JSON structure")
        }
        
        val jsonObject = json.asJsonObject
        val code = jsonObject.get("code")?.asInt
        val msg = jsonObject.get("msg")?.asString
        
        val dataElement = jsonObject.get("data")
        val searchSongData = when {
            dataElement == null -> SearchSongData()
            dataElement.isJsonArray -> {
                // data 是数组，直接解析
                val list = context?.deserialize<List<SearchSongItem>>(
                    dataElement,
                    object : com.google.gson.reflect.TypeToken<List<SearchSongItem>>() {}.type
                )
                SearchSongData(list = list)
            }
            dataElement.isJsonObject -> {
                // data 是对象，尝试从常见字段中获取列表
                val dataObj = dataElement.asJsonObject
                
                // 打印所有字段名用于调试
                android.util.Log.d("SearchSongResponse", "Data object keys: ${dataObj.keySet()}")
                
                // 尝试从常见字段名获取数组
                val possibleArrayFields = listOf("list", "songs", "results", "data", "items", "records")
                var foundList: List<SearchSongItem>? = null
                
                for (fieldName in possibleArrayFields) {
                    val field = dataObj.get(fieldName)
                    if (field != null && field.isJsonArray) {
                        android.util.Log.d("SearchSongResponse", "Found array in field: $fieldName")
                        foundList = context?.deserialize<List<SearchSongItem>>(
                            field,
                            object : com.google.gson.reflect.TypeToken<List<SearchSongItem>>() {}.type
                        )
                        break
                    }
                }
                
                // 如果没找到，尝试查找任何数组类型的字段
                if (foundList == null) {
                    for ((key, value) in dataObj.entrySet()) {
                        if (value.isJsonArray) {
                            android.util.Log.d("SearchSongResponse", "Found array in unknown field: $key")
                            foundList = try {
                                context?.deserialize<List<SearchSongItem>>(
                                    value,
                                    object : com.google.gson.reflect.TypeToken<List<SearchSongItem>>() {}.type
                                )
                            } catch (e: Exception) {
                                android.util.Log.w("SearchSongResponse", "Failed to parse array in field $key", e)
                                null
                            }
                            if (foundList != null) break
                        }
                    }
                }
                
                SearchSongData(list = foundList)
            }
            else -> SearchSongData()
        }
        
        return SearchSongResponse(code, msg, searchSongData)
    }
}

/**
 * 搜索结果中的单首歌曲数据
 */
data class SearchSongItem(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("artist")
    val artist: String?,
    @SerializedName("source")
    val source: String?,
    @SerializedName("pic")
    val pic: String?
)

/**
 * 获取播放地址的API响应
 */
data class MusicUrlResponse(
    @SerializedName("code")
    val code: Int?,
    @SerializedName("msg")
    val msg: String?,
    @SerializedName("data")
    val data: MusicUrlData?
)

/**
 * 播放地址数据
 */
data class MusicUrlData(
    @SerializedName("url")
    val url: String?
)

