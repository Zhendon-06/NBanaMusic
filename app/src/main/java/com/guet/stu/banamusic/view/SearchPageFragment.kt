package com.guet.stu.banamusic.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import com.guet.stu.banamusic.R
import com.guet.stu.banamusic.adapter.MusicListAdapter
import com.guet.stu.banamusic.databinding.ActivitySearchPageBinding
import com.guet.stu.banamusic.model.music.Music
import com.guet.stu.banamusic.util.applyStatusBarSpacer
import com.guet.stu.banamusic.viewmodel.SearchHistoryViewModel
import com.guet.stu.banamusic.viewmodel.SharedMusicViewModel

/**
 * 搜索页面 Fragment 版本，通过 MainActivity 的 NavHostFragment（content_container）导航进入。
 * - 进入时自动展开键盘并聚焦搜索框
 * - 在“最近播放”歌单中做模糊搜索，结果展示在列表中并支持点击播放
 */
class SearchPageFragment : Fragment() {

    private var _binding: ActivitySearchPageBinding? = null
    private val binding get() = _binding!!

    private val searchViewModel: SearchHistoryViewModel by viewModels {
        SearchHistoryViewModelFactory(requireActivity().application)
    }
    private val sharedMusicViewModel: SharedMusicViewModel by activityViewModels()

    private lateinit var adapter: MusicListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivitySearchPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 适配状态栏占位
        applyStatusBarSpacer(binding.statusBarSpace.root)

        // 返回按钮：回到上一个 Fragment
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        setupRecyclerView()
        setupSearchInput()
        observeSearchResults()

        // 进入页面时直接让搜索框获取焦点并弹出软键盘
        binding.etsearch.requestFocus()
        showKeyboard(binding.etsearch)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        adapter = MusicListAdapter { music: Music ->
            val listForPlayer = searchViewModel.searchResults.value.orEmpty()
                .takeIf { it.isNotEmpty() } ?: listOf(music)

            sharedMusicViewModel.setMusicList(listForPlayer)
            sharedMusicViewModel.setCurrentMusic(music)

            // 跳转到播放器 Fragment
            val bundle = Bundle().apply {
                putParcelable("music", music)
            }
            findNavController().navigate(R.id.musicPlayerFragment, bundle)
        }
        binding.searchList.layoutManager = LinearLayoutManager(requireContext())
        binding.searchList.adapter = adapter
    }

    private fun setupSearchInput() {
        // 文本变化实时模糊搜索
        binding.etsearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val text = s?.toString().orEmpty()
                searchViewModel.search(text)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // 软键盘“搜索”动作：如果当前没有结果，提示一下
        binding.etsearch.setOnEditorActionListener { _, _, _ ->
            val results = searchViewModel.searchResults.value.orEmpty()
            if (results.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.no_music_playing),
                    Toast.LENGTH_SHORT
                ).show()
            }
            false
        }
    }

    private fun observeSearchResults() {
        searchViewModel.searchResults.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }
    }

    private fun showKeyboard(view: View) {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        view.post {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    /**
     * 简单的 ViewModel 工厂，复用 SearchHistoryViewModel 的 AndroidViewModel 构造。
     */
    class SearchHistoryViewModelFactory(
        private val application: android.app.Application
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SearchHistoryViewModel::class.java)) {
                return SearchHistoryViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }
}

