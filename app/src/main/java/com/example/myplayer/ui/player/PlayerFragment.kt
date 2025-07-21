package com.example.myplayer.ui.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.example.myplayer.MusicService
import com.example.myplayer.databinding.FragmentPlayerBinding

class PlayerFragment : Fragment() {

    private var _binding: FragmentPlayerBinding? = null
    private val binding get() = _binding!!

    private var musicService: MusicService? = null
    private var isBound = false
    private lateinit var updateSeekBarHandler: Handler

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true

            // Set listeners for UI updates
            musicService?.onPlayerReady = { activity?.runOnUiThread { updateUi() } }
            musicService?.onPlaybackStateChanged = { activity?.runOnUiThread { updatePlayPauseButton(it) } }

            // Initial UI setup based on current service state
            updateUi()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            musicService?.onPlayerReady = null
            musicService?.onPlaybackStateChanged = null
        }
    }

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
        binding.titleTextView.text = arguments?.getString("SONG_TITLE")
        binding.artistTextView.text = arguments?.getString("SONG_ARTIST")

        binding.playPauseButton.setOnClickListener {
            musicService?.togglePlayPause()
        }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    musicService?.seekTo(progress.toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onStart() {
        super.onStart()
        Intent(requireActivity(), MusicService::class.java).also {
            requireActivity().bindService(it, connection, Context.BIND_AUTO_CREATE)
        }
        updateSeekBarHandler = Handler(Looper.getMainLooper())
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            requireActivity().unbindService(connection)
            isBound = false
        }
        updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUi() {
        musicService?.let {
            val duration = it.getDuration()
            if (duration > 0) {
                binding.seekBar.max = duration.toInt()
                updatePlayPauseButton(it.isPlaying())
                updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable) // Avoid multiple runnables
                updateSeekBarHandler.post(updateSeekBarRunnable)
            }
        }
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        val drawableRes = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        binding.playPauseButton.setImageResource(drawableRes)
    }

    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            musicService?.let {
                binding.seekBar.progress = it.getCurrentPosition().toInt()
                updateSeekBarHandler.postDelayed(this, 1000)
            }
        }
    }
}
