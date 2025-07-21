package com.example.myplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class MusicService : Service() {

    private var player: ExoPlayer? = null
    private val binder = MusicBinder()

    private val NOTIFICATION_ID = 1
    private val NOTIFICATION_CHANNEL_ID = "MusicServiceChannel"
    private val TAG = "MusicService"

    // Callbacks for UI updates
    var onPlaybackStateChanged: ((Boolean) -> Unit)? = null
    var onPlayerReady: (() -> Unit)? = null

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate called")
        player = ExoPlayer.Builder(this).build()
        player?.addListener(playerListener)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called with action: ${intent?.action}")
        if (intent?.action == "PLAY") {
            val songUriString = intent.getStringExtra("SONG_URI")
            val songTitle = intent.getStringExtra("SONG_TITLE") ?: "Unknown Title"
            Log.d(TAG, "Attempting to play: $songTitle at $songUriString")

            songUriString?.let {
                try {
                    val mediaItem = MediaItem.fromUri(Uri.parse(it))
                    player?.setMediaItem(mediaItem)
                    player?.prepare()
                    player?.play()
                    Log.d(TAG, "Player prepared and play() called.")
                    startForeground(NOTIFICATION_ID, createNotification(songTitle))
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting media source", e)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "onBind called")
        return binder
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        player?.removeListener(playerListener)
        player?.release()
        super.onDestroy()
    }

    // --- Public methods for clients ---
    fun togglePlayPause() {
        if (player?.isPlaying == true) {
            player?.pause()
        } else {
            player?.play()
        }
    }

    fun isPlaying(): Boolean = player?.isPlaying ?: false

    fun getCurrentPosition(): Long = player?.currentPosition ?: 0

    fun getDuration(): Long = player?.duration ?: 0

    fun seekTo(position: Long) {
        player?.seekTo(position)
    }
    // --------------------------------

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Log.d(TAG, "onIsPlayingChanged: $isPlaying")
            onPlaybackStateChanged?.invoke(isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                Log.d(TAG, "Player is ready. Duration: ${player?.duration}")
                onPlayerReady?.invoke()
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e(TAG, "onPlayerError: ${error.message}", error)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Music Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(songTitle: String): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Now Playing")
            .setContentText(songTitle)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }
}
