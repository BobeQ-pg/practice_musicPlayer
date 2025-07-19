package com.example.myplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class MusicService : Service() {

    private var player: ExoPlayer? = null
    private val NOTIFICATION_ID = 1
    private val NOTIFICATION_CHANNEL_ID = "MusicServiceChannel"

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "PLAY") {
            val songPath = intent.getStringExtra("SONG_PATH")
            songPath?.let {
                val mediaItem = MediaItem.fromUri(it)
                player?.setMediaItem(mediaItem)
                player?.prepare()
                player?.play()
                startForeground(NOTIFICATION_ID, createNotification())
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        player?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
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

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Now Playing")
            .setContentText("Song Title") // TODO: Get actual song title
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }
}
