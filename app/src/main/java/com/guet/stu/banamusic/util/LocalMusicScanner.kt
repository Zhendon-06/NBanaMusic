package com.guet.stu.banamusic.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.guet.stu.banamusic.model.music.LocalSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 本地音乐扫描工具类
 * 使用 MediaStore API 扫描设备中所有本地音频文件
 * 仅保留 IS_MUSIC = true 且 duration >= 90 秒的音频
 */
object LocalMusicScanner {

    /**
     * 扫描本地音频文件
     * @param context 上下文
     * @return 扫描到的音乐列表
     */
    suspend fun scanLocalSongs(context: Context): List<LocalSong> = withContext(Dispatchers.IO) {
        val musicList = mutableListOf<LocalSong>()
        
        try {
            val contentResolver: ContentResolver = context.contentResolver
            
            // 定义查询字段
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
            )
            
            // 查询条件：IS_MUSIC = 1 AND DURATION >= 90000 (90秒)
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} = ? AND ${MediaStore.Audio.Media.DURATION} >= ?"
            val selectionArgs = arrayOf("1", "90000")
            
            // 按标题排序
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
            
            // 执行查询
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                
                while (cursor.moveToNext()) {
                    try {
                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn) ?: ""
                        val artist = cursor.getString(artistColumn) ?: ""
                        val album = cursor.getString(albumColumn) ?: ""
                        val albumId = cursor.getLong(albumIdColumn)
                        val duration = cursor.getLong(durationColumn)
                        val path = cursor.getString(dataColumn) ?: ""
                        
                        // 生成 content Uri
                        val contentUri: Uri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        
                        // 生成专辑封面 Uri（如果 albumId 有效）
                        val albumArtUri: Uri? = if (albumId > 0) {
                            val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
                            ContentUris.withAppendedId(sArtworkUri, albumId)
                        } else {
                            null
                        }
                        
                        val localSong = LocalSong(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            path = path,
                            uri = contentUri,
                            albumArtUri = albumArtUri
                        )
                        
                        musicList.add(localSong)
                    } catch (e: Exception) {
                        // 跳过有问题的记录，继续扫描
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        musicList
    }
}
