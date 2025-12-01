package com.guet.stu.banamusic.util

import android.view.View
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




