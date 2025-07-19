package com.example.myplayer.ui.home

import android.app.Application
import android.content.ContentResolver
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myplayer.Song

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _songs = MutableLiveData<List<Song>>().apply {
        value = loadSongs()
    }
    val songs: LiveData<List<Song>> = _songs

    private fun loadSongs(): List<Song> {
        val songList = mutableListOf<Song>()
        val contentResolver: ContentResolver = getApplication<Application>().contentResolver
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0"
        val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"
        val cursor = contentResolver.query(uri, null, selection, null, sortOrder)

        if (cursor != null && cursor.moveToFirst()) {
            val titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val pathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

            do {
                val title = cursor.getString(titleColumn)
                val artist = cursor.getString(artistColumn)
                val path = cursor.getString(pathColumn)
                songList.add(Song(title, artist, path))
            } while (cursor.moveToNext())

            cursor.close()
        }
        return songList
    }
}