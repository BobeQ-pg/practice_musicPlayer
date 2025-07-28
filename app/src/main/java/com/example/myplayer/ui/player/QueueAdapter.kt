package com.example.myplayer.ui.player

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myplayer.Song
import com.example.myplayer.databinding.QueueItemBinding

class QueueAdapter(private val onSongClicked: (Song) -> Unit) :
    ListAdapter<Song, QueueAdapter.QueueViewHolder>(QueueDiffCallback()) {

    private var currentPlayingSong: Song? = null

    fun setCurrentPlayingSong(song: Song?) {
        currentPlayingSong = song
        notifyDataSetChanged() // This is a simple way to update highlighting
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        val binding = QueueItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QueueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        val song = getItem(position)
        holder.bind(song, song == currentPlayingSong, onSongClicked)
    }

    class QueueViewHolder(private val binding: QueueItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song, isPlaying: Boolean, onSongClicked: (Song) -> Unit) {
            binding.songTitle.text = song.title
            binding.songArtist.text = song.artist
            binding.root.setOnClickListener { onSongClicked(song) }
            // Highlight the currently playing song
            binding.root.setBackgroundColor(if (isPlaying) Color.LTGRAY else Color.TRANSPARENT)
        }
    }
}

class QueueDiffCallback : DiffUtil.ItemCallback<Song>() {
    override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean = oldItem.uri == newItem.uri
    override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean = oldItem == newItem
}
