package com.example.myplayer.ui.player

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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

        sharedViewModel.nowPlaying.observe(viewLifecycleOwner) {
            binding.titleTextView.text = it?.title
            binding.artistTextView.text = it?.artist
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

        binding.playPauseButton.setOnClickListener {
            sharedViewModel.togglePlayPause()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    sharedViewModel.seekTo(progress.toLong())
                }
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

