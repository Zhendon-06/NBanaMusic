package com.guet.stu.banamusic.view.widget

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView

/**
 * 嵌套在 Scroll/NestedScrollView 内部的 RecyclerView 扩展，强制测量出完整高度，保证多层列表都能显示。
 */
class NonScrollableRecyclerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    init {
        isNestedScrollingEnabled = false
    }

    override fun onMeasure(widthSpec: Int, heightSpec: Int) {
        val expandedHeightSpec = MeasureSpec.makeMeasureSpec(
            Int.MAX_VALUE shr 2,
            MeasureSpec.AT_MOST
        )
        super.onMeasure(widthSpec, expandedHeightSpec)
    }
}

