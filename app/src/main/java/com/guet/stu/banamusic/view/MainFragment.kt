package com.guet.stu.banamusic.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.guet.stu.banamusic.R
import com.guet.stu.banamusic.adapter.DayMusicAdapter
import com.guet.stu.banamusic.adapter.LiveMusicAdapter
import com.guet.stu.banamusic.databinding.FragmentMainBinding
import com.guet.stu.banamusic.util.applyStatusBarSpacer
import com.guet.stu.banamusic.viewmodel.MainFragmentViewModel
import com.guet.stu.banamusic.viewmodel.SharedMusicViewModel
import com.guet.stu.banamusic.model.music.Music
import com.guet.stu.banamusic.view.MainFragmentDirections

class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    // 使用 activityViewModels 确保 ViewModel 在 Activity 级别共享，数据在 Fragment 重建时保留
    private val viewModel: MainFragmentViewModel by activityViewModels()
    private val sharedMusicViewModel: SharedMusicViewModel by activityViewModels()
    private lateinit var liveAdapter: LiveMusicAdapter
    private lateinit var dayAdapter: DayMusicAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 初始化 DataBinding
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 在视图创建完成后获取 NavController
        val navController = findNavController()
        clickAlbum(navController)
        liveAdapter = LiveMusicAdapter { navigateToPlayer(it) }
        dayAdapter = DayMusicAdapter { navigateToPlayer(it) }
        setupRecyclerViews()
        observeViewModel()
        applyStatusBarSpacer(binding.statusBarSpace.root)
        setJingBar()
        
        // 如果数据已存在，直接使用；否则加载（避免重复请求）
        viewModel.loadLiveMusicIfNeeded()
        viewModel.loadDayMusicIfNeeded()
    }
    private fun setJingBar(){
        binding.music1.actionBarTitleName.text="原创榜"
        binding.music1.cardImage.setImageResource(R.drawable.yuanchuang)
        binding.music2.actionBarTitleName.text="热歌榜"
        binding.music2.cardImage.setImageResource(R.drawable.rege)
        binding.music3.actionBarTitleName.text="新歌榜"
        binding.music3.cardImage.setImageResource(R.drawable.xinge)
        binding.music4.actionBarTitleName.text="飙升榜"
        binding.music4.cardImage.setImageResource(R.drawable.biaosheng)
    }
    private fun setupRecyclerViews() = with(binding) {
        recyclerLive.apply {
            layoutManager = GridLayoutManager(requireContext(), 3, GridLayoutManager.HORIZONTAL, false)
            adapter = liveAdapter
            isNestedScrollingEnabled = false
        }
        recyclerDay.apply {
            layoutManager = GridLayoutManager(requireContext(), 3, GridLayoutManager.HORIZONTAL, false)
            adapter = dayAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun observeViewModel() {
        viewModel.liveMusic.observe(viewLifecycleOwner) { list ->
            liveAdapter.submitList(list)
        }
        viewModel.dayMusic.observe(viewLifecycleOwner) { list ->
            dayAdapter.submitList(list)
        }
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun clickAlbum(navController: NavController) {
        binding.music1.root.setOnClickListener {
            navController.navigate(
                R.id.albumListFragment,
                Bundle().apply { putInt("albumType", 1) }
            )
        }

        binding.music2.root.setOnClickListener {
            navController.navigate(
                R.id.albumListFragment,
                Bundle().apply { putInt("albumType", 2) }
            )
        }

        binding.music3.root.setOnClickListener {
            navController.navigate(
                R.id.albumListFragment,
                Bundle().apply { putInt("albumType", 3) }
            )
        }

        binding.music4.root.setOnClickListener {
            navController.navigate(
                R.id.albumListFragment,
                Bundle().apply { putInt("albumType", 4) }
            )
        }
    }

    private fun navigateToPlayer(music: Music) {
        val liveList = viewModel.liveMusic.value
        val dayList = viewModel.dayMusic.value
        val listForPlayer = when {
            !liveList.isNullOrEmpty() && liveList.any { it.id == music.id } -> liveList
            !dayList.isNullOrEmpty() && dayList.any { it.id == music.id } -> dayList
            else -> listOf(music)
        }
        sharedMusicViewModel.setMusicList(listForPlayer)
        val action = MainFragmentDirections.actionMainFragmentToMusicPlayerFragment(music)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}