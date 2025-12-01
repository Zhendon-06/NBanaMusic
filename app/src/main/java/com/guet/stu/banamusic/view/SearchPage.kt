/**
 * 搜索页面Activity - 音乐搜索功能
 * 
 * 功能描述：
 * - 提供音乐搜索功能的主界面
 * - 显示搜索分类、热门搜索、推荐内容
 * - 支持多种布局展示不同类型的内容
 * - 提供返回功能和沉浸式状态栏
 * 
 * 页面结构：
 * 1. 搜索分类：2列网格布局展示音乐分类
 * 2. 热门搜索：垂直列表展示热门搜索词
 * 3. 推荐内容：水平滚动展示推荐歌单/专辑
 * 
 * 技术特点：
 * - 使用多种RecyclerView布局管理器
 * - 自定义适配器处理不同类型的数据
 * - 响应式设计，适配不同屏幕尺寸
 * 
 * @author BanaMusic Team
 * @version 1.0
 * @since 2024
 */
package com.guet.stu.banamusic.view

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.guet.stu.banamusic.R
import com.guet.stu.banamusic.databinding.ActivitySearchPageBinding

class SearchPage : AppCompatActivity() {

    private lateinit var binding: ActivitySearchPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 启用边缘到边缘显示，实现沉浸式状态栏效果
        enableEdgeToEdge()
        
        // 初始化 DataBinding
        binding = ActivitySearchPageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 设置窗口边距监听器，处理系统栏（状态栏、导航栏）的适配
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            // 获取系统栏的边距信息
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 为根视图设置内边距，避免内容被系统栏遮挡
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 设置返回按钮的点击事件
        binding.btnBack.setOnClickListener {
            // 结束当前Activity，返回上一个页面
            finish()
        }
    }
}
