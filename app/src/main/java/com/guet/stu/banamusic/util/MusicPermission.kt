package com.guet.stu.banamusic.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 音乐权限工具类
 * 统一处理本地音乐扫描所需的权限判断和申请
 */
object MusicPermission {

    /**
     * 检查是否有读取音频文件的权限
     * @param context 上下文
     * @return true 如果有权限，false 如果没有权限
     */
    fun hasPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) 使用 READ_MEDIA_AUDIO
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 12 及以下使用 READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 获取需要申请的权限数组
     * @return 权限数组
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            // Android 12 及以下
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}

