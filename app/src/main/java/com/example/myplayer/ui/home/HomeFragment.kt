package com.example.myplayer.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.myplayer.R
import com.example.myplayer.SharedViewModel
import com.example.myplayer.SongAdapter
import com.example.myplayer.SortOrder
import com.example.myplayer.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()

        val songAdapter = SongAdapter { song ->
            sharedViewModel.playSong(song)
            activity?.findViewById<ViewPager2>(R.id.viewPager)?.currentItem = 1
        }

        binding.recyclerView.apply {
            adapter = songAdapter
            layoutManager = LinearLayoutManager(context)
        }

        sharedViewModel.songs.observe(viewLifecycleOwner) {
            songAdapter.submitList(it)
        }

        sharedViewModel.isScanning.observe(viewLifecycleOwner) {
            binding.progressBar.isVisible = it
            binding.recyclerView.isVisible = !it
        }
    }

    private fun setupToolbar() {
        binding.toolbar.inflateMenu(R.menu.sort_menu)
        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.sort_by_title -> {
                    sharedViewModel.setSortOrder(SortOrder.TITLE)
                    true
                }
                R.id.sort_by_artist -> {
                    sharedViewModel.setSortOrder(SortOrder.ARTIST)
                    true
                }
                R.id.sort_by_album -> {
                    sharedViewModel.setSortOrder(SortOrder.ALBUM)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}