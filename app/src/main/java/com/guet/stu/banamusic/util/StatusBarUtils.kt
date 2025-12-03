package com.guet.stu.banamusic.util

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun applyStatusBarSpacer(spacerView: View) {
    ViewCompat.setOnApplyWindowInsetsListener(spacerView) { v, insets ->
        val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        val layoutParams = v.layoutParams
        if (layoutParams.height != statusBarHeight) {
            layoutParams.height = statusBarHeight
            v.layoutParams = layoutParams
        }
        v.visibility = if (statusBarHeight > 0) View.VISIBLE else View.GONE
        insets
    }
    spacerView.requestApplyInsets()
}

/**
 * 在浅色背景下启用“状态栏反色”（浅色状态栏），让状态栏图标/文字变成深色，避免白底看不见。
 */
fun setLightStatusBar(activity: Activity, isLight: Boolean) {
    val window = activity.window
    val decorView = window.decorView

    // 使用 WindowInsetsControllerCompat 统一处理深浅色状态栏图标
    val controller = WindowInsetsControllerCompat(window, decorView)
    controller.isAppearanceLightStatusBars = isLight

    // 对极老设备（低于 M）的兼容，这里通常可以忽略或保持默认
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        // 无官方 API，仅保留默认样式
    }
}




