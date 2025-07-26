package com.example.myplayer

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val folderRepository = FolderRepository(application)
    private val sortPreferences = SortPreferences(application)

    private var currentSortOrder: SortOrder = sortPreferences.getSortOrder()

    // --- LiveData for UI ---
    private val _songs = MutableLiveData<List<Song>>()
    val songs: LiveData<List<Song>> = _songs

    private val _musicFolders = MutableLiveData<Set<String>>()
    val musicFolders: LiveData<Set<String>> = _musicFolders

    private val _isScanning = MutableLiveData<Boolean>(false)
    val isScanning: LiveData<Boolean> = _isScanning

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
                _currentPosition.postValue(musicService?.getCurrentPosition()) // Reset position for new track
                updateProgress()
            }
            musicService?.onPlaybackStateChanged = {
                _isPlaying.postValue(it)
                if (it) updateProgress() // Resume progress updates when playing
            }
            musicService?.onTrackChanged = { _nowPlaying.postValue(it) }
            musicService?.onPlaylistEnded = {
                _isPlaying.postValue(false)
                _currentPosition.postValue(0)
                _duration.postValue(0)
                _nowPlaying.postValue(null)
            }
            updateProgress()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            musicService = null
            updateSeekBarHandler.removeCallbacks(updateSeekBarRunnable)
        }
    }

    init {
        _musicFolders.value = folderRepository.getFolders()
        loadSongsFromSelectedFolders()
        Intent(application, MusicService::class.java).also { intent ->
            application.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun addMusicFolder(uri: Uri) {
        folderRepository.addFolder(uri)
        _musicFolders.value = folderRepository.getFolders()
        loadSongsFromSelectedFolders()
    }

    fun removeMusicFolder(uri: Uri) {
        folderRepository.removeFolder(uri)
        _musicFolders.value = folderRepository.getFolders()
        loadSongsFromSelectedFolders()
    }

    fun setSortOrder(sortOrder: SortOrder) {
        currentSortOrder = sortOrder
        sortPreferences.setSortOrder(sortOrder)
        songs.value?.let { sortAndPostSongs(it) }
    }

    private fun sortAndPostSongs(songs: List<Song>) {
        val sortedSongs = when (currentSortOrder) {
            SortOrder.TITLE -> songs.sortedBy { it.title }
            SortOrder.ARTIST -> songs.sortedBy { it.artist }
            SortOrder.ALBUM -> songs.sortedBy { it.album }
        }
        _songs.postValue(sortedSongs)
    }

    private fun loadSongsFromSelectedFolders() {
        _isScanning.value = true
        _songs.postValue(emptyList()) // Clear the list before scanning
        viewModelScope.launch(Dispatchers.IO) {
            val songList = mutableListOf<Song>()
            val folders = folderRepository.getFolders()
            val contentResolver = getApplication<Application>().contentResolver
            val retriever = MediaMetadataRetriever()

            folders.forEach { folderUriString ->
                val folderUri = Uri.parse(folderUriString)
                val parent = DocumentFile.fromTreeUri(getApplication(), folderUri)
                parent?.let { findAudioFiles(it, retriever, songList) }
            }

            retriever.release()
            sortAndPostSongs(songList)
            withContext(Dispatchers.Main) {
                _isScanning.value = false
            }
        }
    }

    private fun findAudioFiles(parent: DocumentFile, retriever: MediaMetadataRetriever, songList: MutableList<Song>) {
        val supportedExtensions = setOf(".mp3", ".flac", ".wav", ".ogg")
        for (file in parent.listFiles()) {
            if (file.isDirectory) {
                findAudioFiles(file, retriever, songList)
            } else {
                val fileName = file.name ?: ""
                if (supportedExtensions.any { fileName.endsWith(it, ignoreCase = true) }) {
                    android.util.Log.d("SongScanner", "-> Adding ${file.name} to the list based on extension.")
                    try {
                        val pfd = getApplication<Application>().contentResolver.openFileDescriptor(file.uri, "r")
                        pfd?.use {
                            retriever.setDataSource(it.fileDescriptor)
                            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.name ?: "Unknown Title"
                            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
                            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
                            songList.add(Song(file.uri, title, artist, album))
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SongScanner", "Failed to read metadata for ${file.uri}", e)
                    }
                }
            }
        }
    }

    fun playSong(song: Song) {
        val playlist = songs.value?.filter { it.album == song.album } ?: listOf(song)
        val songIndex = playlist.indexOf(song)

        _nowPlaying.value = song
        val intent = Intent(getApplication(), MusicService::class.java).apply {
            action = "PLAY"
            putParcelableArrayListExtra("PLAYLIST", ArrayList(playlist))
            putExtra("SONG_INDEX", songIndex)
        }
        getApplication<Application>().startService(intent)
    }

    fun togglePlayPause() { musicService?.togglePlayPause() }

    fun seekTo(position: Long) {
        musicService?.seekTo(position)
        _currentPosition.value = position
    }

    fun rewind() {
        musicService?.let {
            val newPosition = (it.getCurrentPosition() - 10000).coerceAtLeast(0)
            it.seekTo(newPosition)
            _currentPosition.value = newPosition
        }
    }

    fun fastForward() {
        musicService?.let {
            val newPosition = (it.getCurrentPosition() + 10000).coerceAtMost(it.getDuration())
            it.seekTo(newPosition)
            _currentPosition.value = newPosition
        }
    }
    fun skipToNext() { musicService?.skipToNext() }
    fun skipToPrevious() { musicService?.skipToPrevious() }

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
