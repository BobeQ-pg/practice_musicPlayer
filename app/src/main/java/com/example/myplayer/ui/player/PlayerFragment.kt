package com.example.myplayer.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myplayer.R
import com.example.myplayer.SharedViewModel
import com.example.myplayer.databinding.FragmentPlayerBinding

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply insets to handle system bars
        view.setOnApplyWindowInsetsListener { v, insets ->
            val systemBarInsets = androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat(insets, v).getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            binding.controlsContainer.setPadding(systemBarInsets.left, 0, systemBarInsets.right, systemBarInsets.bottom)
            binding.queueRecyclerView.setPadding(0, systemBarInsets.top, 0, 0)
            insets
        }

        val queueAdapter = QueueAdapter { song ->
            sharedViewModel.playSong(song)
        }

        binding.queueRecyclerView.apply {
            adapter = queueAdapter
            layoutManager = LinearLayoutManager(context)
        }

        sharedViewModel.playlist.observe(viewLifecycleOwner) {
            queueAdapter.submitList(it)
        }

        sharedViewModel.nowPlaying.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.titleTextView.text = it.title
                binding.artistTextView.text = it.artist
                queueAdapter.setCurrentPlayingSong(it)
                // Scroll to the currently playing song
                val position = sharedViewModel.playlist.value?.indexOf(it) ?: -1
                if (position != -1) {
                    (binding.queueRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(position, 0)
                }
            } else {
                binding.titleTextView.text = "No song selected"
                binding.artistTextView.text = ""
                binding.seekBar.progress = 0
                queueAdapter.setCurrentPlayingSong(null)
            }
        }

        sharedViewModel.isPlaying.observe(viewLifecycleOwner) {
            val drawableRes = if (it) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            binding.playPauseButton.setImageResource(drawableRes)
        }

        sharedViewModel.duration.observe(viewLifecycleOwner) {
            binding.seekBar.max = it.toInt()
        }

        sharedViewModel.currentPosition.observe(viewLifecycleOwner) {
            binding.seekBar.progress = it.toInt()
        }

        binding.playPauseButton.setOnClickListener { sharedViewModel.togglePlayPause() }
        binding.rewindButton.setOnClickListener { sharedViewModel.skipToPrevious() }
        binding.rewindButton.setOnLongClickListener { sharedViewModel.rewind(); true }
        binding.fastForwardButton.setOnClickListener { sharedViewModel.skipToNext() }
        binding.fastForwardButton.setOnLongClickListener { sharedViewModel.fastForward(); true }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) sharedViewModel.seekTo(progress.toLong())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

