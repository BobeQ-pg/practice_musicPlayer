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

    private var rawSongList: List<Song> = emptyList()
    private val _currentSortOrder = MutableLiveData<SortOrder>()
    val currentSortOrder: LiveData<SortOrder> get() = _currentSortOrder

    // --- LiveData for UI ---
    private val _songs = MutableLiveData<List<LibraryItem>>()
    val songs: LiveData<List<LibraryItem>> = _songs

    private val _playlist = MutableLiveData<List<Song>>()
    val playlist: LiveData<List<Song>> = _playlist

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
        _currentSortOrder.value = sortPreferences.getSortOrder()
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
        sortPreferences.setSortOrder(sortOrder)
        _currentSortOrder.value = sortOrder
        sortAndPostSongs(rawSongList)
    }

    private fun sortAndPostSongs(songs: List<Song>) {
        val libraryItems = mutableListOf<LibraryItem>()
        when (_currentSortOrder.value) {
            SortOrder.TITLE -> {
                val sortedSongs = songs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
                val groupedSongs = sortedSongs.groupBy { song -> getHeaderForTitle(song.title) }

                // Custom sort order for headers
                val headerOrder = listOf("#", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "ア", "カ", "サ", "タ", "ナ", "ハ", "マ", "ヤ", "ラ", "ワ", "漢字", "*")
                val sortedGroupedSongs = groupedSongs.toSortedMap(compareBy { header -> headerOrder.indexOf(header).let { if (it == -1) headerOrder.size else it } })

                sortedGroupedSongs.forEach { (header, songs) ->
                    libraryItems.add(LibraryItem.HeaderItem(header))
                    libraryItems.addAll(songs.map { LibraryItem.SongItem(it) })
                }
            }
            SortOrder.ARTIST -> {
                songs.sortedBy { it.artist }.groupBy { it.artist }.forEach { (artist, songs) ->
                    libraryItems.add(LibraryItem.HeaderItem(artist))
                    libraryItems.addAll(songs.map { LibraryItem.SongItem(it) })
                }
            }
            SortOrder.ALBUM -> {
                songs.sortedBy { it.album }.groupBy { it.album }.forEach { (album, songs) ->
                    libraryItems.add(LibraryItem.HeaderItem(album))
                    libraryItems.addAll(songs.map { LibraryItem.SongItem(it) })
                }
            }
            null -> { /* Do nothing, wait for LiveData to be initialized */ }
        }
        _songs.postValue(libraryItems)
    }

    private fun getHeaderForTitle(title: String): String {
        if (title.isEmpty()) return "*"
        val firstChar = title.first()

        return when {
            firstChar.isDigit() -> "#"
            firstChar.uppercaseChar() in 'A'..'Z' -> firstChar.uppercaseChar().toString()
            isJapaneseKana(firstChar) -> getKanaRowHeader(firstChar)
            isKanji(firstChar) -> "漢字"
            else -> "*"
        }
    }

    private fun isJapaneseKana(char: Char): Boolean {
        return char in '\u3040'..'\u309F' || char in '\u30A0'..'\u30FF'
    }

    private fun isKanji(char: Char): Boolean {
        return char in '\u4E00'..'\u9FAF'
    }

    private fun getKanaRowHeader(char: Char): String {
        val hiraganaChar = toHiragana(char)
        return when (hiraganaChar) {
            in 'あ'..'お' -> "ア"
            in 'か'..'ご' -> "カ"
            in 'さ'..'ぞ' -> "サ"
            in 'た'..'ど' -> "タ"
            in 'な'..'の' -> "ナ"
            in 'は'..'ぽ' -> "ハ"
            in 'ま'..'も' -> "マ"
            in 'や'..'よ' -> "ヤ"
            in 'ら'..'ろ' -> "ラ"
            in 'わ'..'ん' -> "ワ"
            else -> "*"
        }
    }

    private fun toHiragana(char: Char): Char {
        return if (char in '\u30A0'..'\u30FF') (char.code - 0x60).toChar() else char
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
            rawSongList = songList
            sortAndPostSongs(rawSongList)
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
        val songItems = rawSongList
        val currentPlaylist = songItems.filter { it.album == song.album }
        _playlist.value = currentPlaylist

        val songIndex = currentPlaylist.indexOf(song)

        _nowPlaying.value = song
        val intent = Intent(getApplication(), MusicService::class.java).apply {
            action = "PLAY"
            putParcelableArrayListExtra("PLAYLIST", ArrayList(currentPlaylist))
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
