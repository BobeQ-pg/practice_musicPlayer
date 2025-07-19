package com.example.myplayer

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SongAdapter(private val songs: List<Song>) : RecyclerView.Adapter<SongAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val song = songs[position]
        holder.titleTextView.text = song.title
        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, MusicService::class.java).apply {
                action = "PLAY"
                putExtra("SONG_PATH", song.path)
            }
            context.startService(intent)
        }
    }

    override fun getItemCount() = songs.size
}
