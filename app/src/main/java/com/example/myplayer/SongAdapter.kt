package com.example.myplayer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.myplayer.ui.player.PlayerFragment

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

            // Start the service to play music
            val serviceIntent = Intent(context, MusicService::class.java).apply {
                action = "PLAY"
                putExtra("SONG_URI", song.uri.toString())
                putExtra("SONG_TITLE", song.title)
            }
            context.startService(serviceIntent)

            // Navigate to PlayerFragment
            val activity = context as AppCompatActivity
            val fragment = PlayerFragment().apply {
                arguments = Bundle().apply {
                    putString("SONG_URI", song.uri.toString())
                    putString("SONG_TITLE", song.title)
                    putString("SONG_ARTIST", song.artist)
                }
            }
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    override fun getItemCount() = songs.size
}
