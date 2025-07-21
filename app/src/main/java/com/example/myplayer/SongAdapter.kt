package com.example.myplayer

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class SongAdapter(private val onSongClicked: (Song) -> Unit) :
    ListAdapter<Song, SongAdapter.SongViewHolder>(SongDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return SongViewHolder(view as TextView)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = getItem(position)
        holder.bind(song)
        holder.itemView.setOnClickListener { onSongClicked(song) }
    }

    class SongViewHolder(private val textView: TextView) : RecyclerView.ViewHolder(textView) {
        fun bind(song: Song) {
            textView.text = song.title
        }
    }
}

class SongDiffCallback : DiffUtil.ItemCallback<Song>() {
    override fun areItemsTheSame(oldItem: Song, newItem: Song):
        Boolean = oldItem.uri == newItem.uri

    override fun areContentsTheSame(oldItem: Song, newItem: Song):
        Boolean = oldItem == newItem
}
