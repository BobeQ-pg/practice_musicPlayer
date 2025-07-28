package com.example.myplayer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myplayer.databinding.HeaderItemBinding
import com.example.myplayer.databinding.SongItemBinding

private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_SONG = 1

class LibraryAdapter(private val onSongClicked: (Song) -> Unit) :
    ListAdapter<LibraryItem, RecyclerView.ViewHolder>(LibraryDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is LibraryItem.HeaderItem -> ITEM_VIEW_TYPE_HEADER
            is LibraryItem.SongItem -> ITEM_VIEW_TYPE_SONG
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_VIEW_TYPE_HEADER -> HeaderViewHolder.from(parent)
            ITEM_VIEW_TYPE_SONG -> SongViewHolder.from(parent)
            else -> throw ClassCastException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SongViewHolder -> {
                val songItem = getItem(position) as LibraryItem.SongItem
                holder.bind(songItem.song, onSongClicked)
            }
            is HeaderViewHolder -> {
                val headerItem = getItem(position) as LibraryItem.HeaderItem
                holder.bind(headerItem.title)
            }
        }
    }

    class SongViewHolder private constructor(private val binding: SongItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song, onSongClicked: (Song) -> Unit) {
            binding.songTitle.text = song.title
            binding.songArtist.text = song.artist
            binding.root.setOnClickListener { onSongClicked(song) }
        }

        companion object {
            fun from(parent: ViewGroup): SongViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = SongItemBinding.inflate(layoutInflater, parent, false)
                return SongViewHolder(binding)
            }
        }
    }

    class HeaderViewHolder private constructor(private val binding: HeaderItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.headerTitle.text = title
        }

        companion object {
            fun from(parent: ViewGroup): HeaderViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = HeaderItemBinding.inflate(layoutInflater, parent, false)
                return HeaderViewHolder(binding)
            }
        }
    }
}

class LibraryDiffCallback : DiffUtil.ItemCallback<LibraryItem>() {
    override fun areItemsTheSame(oldItem: LibraryItem, newItem: LibraryItem): Boolean {
        return (oldItem is LibraryItem.HeaderItem && newItem is LibraryItem.HeaderItem && oldItem.title == newItem.title) ||
               (oldItem is LibraryItem.SongItem && newItem is LibraryItem.SongItem && oldItem.song.uri == newItem.song.uri)
    }

    override fun areContentsTheSame(oldItem: LibraryItem, newItem: LibraryItem): Boolean {
        return oldItem == newItem
    }
}