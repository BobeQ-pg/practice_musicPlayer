package com.example.myplayer

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.ContentUris

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    // --- LiveData for UI ---
    private val _songs = MutableLiveData<List<Song>>()
    val songs: LiveData<List<Song>> = _songs

    private val _nowPlaying = MutableLiveData<Song?>()
    val nowPlaying: LiveData<Song?> = _nowPlaying

    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val _currentPosition = MutableLiveData<Long>()
    val currentPosition: LiveData<Long> = _currentPosition

    private val _duration = MutableLiveData<Long>()
    val duration: LiveData<Long> = _duration

    // --- MusicService Connection ---
    private var musicService: MusicService? = null
    private var isBound = false
    private val updateSeekBarHandler = Handler(Looper.getMainLooper())

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isBound = true

            musicService?.onPlayerReady = {
                _duration.postValue(musicService?.getDuration())
                updateProgress()
            }
            musicService?.onPlaybackStateChanged = {
                _isPlaying.postValue(it)
                if(it) updateProgress() // Resume progress updates when playing
            }
            // Reflect initial state
            _isPlaying.postValue(musicService?.isPlaying())
            _duration.postValue(musicService?.getDuration())
            updateProgress()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            musicService = null
            updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
        }
    }

    init {
        loadSongs()
        Intent(application, MusicService::class.java).also { intent ->
            application.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun loadSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            val songList = mutableListOf<Song>()
            val contentResolver = getApplication<Application>().contentResolver
            val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST)
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.MIME_TYPE} IN (?, ?, ?, ?)"
            val selectionArgs = arrayOf("audio/mpeg", "audio/flac", "audio/x-wav", "audio/aac")
            val sortOrder = MediaStore.Audio.Media.TITLE + " ASC"
            val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)

            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn)
                    val artist = it.getString(artistColumn)
                    val contentUri = ContentUris.withAppendedId(uri, id)
                    songList.add(Song(contentUri, title, artist))
                }
            }
            _songs.postValue(songList)
        }
    }

    fun playSong(song: Song) {
        _nowPlaying.value = song
        val intent = Intent(getApplication(), MusicService::class.java).apply {
            action = "PLAY"
            putExtra("SONG_URI", song.uri.toString())
            putExtra("SONG_TITLE", song.title)
        }
        getApplication<Application>().startService(intent)
    }

    fun togglePlayPause() {
        musicService?.togglePlayPause()
    }

    fun seekTo(position: Long) {
        musicService?.seekTo(position)
    }

    private val updateSeekBarRunnable = object : Runnable {
        override fun run() {
            val currentPos = musicService?.getCurrentPosition()
            _currentPosition.postValue(currentPos)

            if (musicService?.isPlaying() == true) {
                updateSeekBarHandler.postDelayed(this, 1000)
            }
        }
    }

    private fun updateProgress() {
        updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
        updateSeekBarHandler.post(updateSeekBarRunnable)
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            musicService?.stopMusicAndSelfDestruct()
            getApplication<Application>().unbindService(connection)
            isBound = false
        }
    }
}
