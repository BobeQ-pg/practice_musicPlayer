package com.example.myplayer

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class MusicService : android.app.Service() {

    private var player: ExoPlayer? = null
    private val binder = MusicBinder()

    private var playlist: List<Song> = emptyList()
    private var currentTrackIndex = -1

    private val NOTIFICATION_ID = 1
    private val NOTIFICATION_CHANNEL_ID = "MusicServiceChannel"
    private val TAG = "MusicService"

    private lateinit var mediaSession: MediaSessionCompat

    var onPlaybackStateChanged: ((Boolean) -> Unit)? = null
    var onPlayerReady: (() -> Unit)? = null
    var onTrackChanged: ((Song) -> Unit)? = null
    var onPlaylistEnded: (() -> Unit)? = null

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate called")
        player = ExoPlayer.Builder(this).build()
        player?.addListener(playerListener)

        mediaSession = MediaSessionCompat(this, TAG)
        mediaSession.setCallback(mediaSessionCallback)
        mediaSession.isActive = true

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        if (intent?.action == "PLAY") {
            val newPlaylist = intent.getParcelableArrayListExtra<Song>("PLAYLIST")
            val startIndex = intent.getIntExtra("SONG_INDEX", 0)

            if (newPlaylist != null) {
                playlist = newPlaylist
                currentTrackIndex = startIndex
                playTrackAtIndex(currentTrackIndex)
            }
        }
        return START_STICKY
    }

    private fun playTrackAtIndex(index: Int) {
        if (index in playlist.indices) {
            val song = playlist[index]
            currentTrackIndex = index
            onTrackChanged?.invoke(song)

            val mediaItem = MediaItem.Builder()
                .setUri(song.uri)
                .setMediaMetadata(androidx.media3.common.MediaMetadata.Builder().setTitle(song.title).build())
                .build()
            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.play()
            startForeground(NOTIFICATION_ID, createNotification())
        } else {
            Log.d(TAG, "End of playlist reached. Firing onPlaylistEnded callback.")
            player?.stop()
            stopForeground(true)
            onPlaylistEnded?.invoke()
        }
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        stopMusicAndSelfDestruct()
        super.onDestroy()
    }

    private val mediaSessionCallback = object : MediaSessionCompat.Callback() {
        override fun onPlay() { togglePlayPause() }
        override fun onPause() { togglePlayPause() }
        override fun onSeekTo(pos: Long) { seekTo(pos) }
        override fun onSkipToNext() { skipToNext() }
        override fun onSkipToPrevious() { skipToPrevious() }
    }

    fun togglePlayPause() { if (player?.isPlaying == true) player?.pause() else player?.play() }
    fun isPlaying(): Boolean = player?.isPlaying ?: false
    fun getCurrentPosition(): Long = player?.currentPosition ?: 0
    fun getDuration(): Long = player?.duration ?: 0
    fun seekTo(position: Long) { player?.seekTo(position) }

    fun skipToNext() {
        currentTrackIndex++
        playTrackAtIndex(currentTrackIndex)
    }

    fun skipToPrevious() {
        if (player?.currentPosition ?: 0 > 3000) {
            player?.seekTo(0)
        } else {
            if (currentTrackIndex > 0) {
                currentTrackIndex--
                playTrackAtIndex(currentTrackIndex)
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            onPlaybackStateChanged?.invoke(isPlaying)
            updateMediaSessionState()
            updateNotification()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                onPlayerReady?.invoke()
                updateMediaSessionState()
            }
            if (playbackState == Player.STATE_ENDED) {
                skipToNext()
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e(TAG, "onPlayerError: ${error.message}", error)
        }
    }

    private fun updateMediaSessionState() {
        val state = if (player?.isPlaying == true) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(state, player?.currentPosition ?: 0, 1.0f)
                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SEEK_TO or PlaybackStateCompat.ACTION_SKIP_TO_NEXT or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                .build()
        )
    }

    private fun updateNotification() {
        val notification = createNotification()
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(NOTIFICATION_CHANNEL_ID, "Music Player", android.app.NotificationManager.IMPORTANCE_LOW)
            NotificationManagerCompat.from(this).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val songTitle = player?.currentMediaItem?.mediaMetadata?.title ?: "Unknown Title"

        val playPauseAction = NotificationCompat.Action(
            if (player?.isPlaying == true) R.drawable.ic_media_pause else R.drawable.ic_media_play,
            if (player?.isPlaying == true) "Pause" else "Play",
            MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)
        )

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(songTitle)
            .setContentText("Now Playing")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(playPauseAction)
            .setStyle(MediaNotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0))
            .build()
    }

    fun stopMusicAndSelfDestruct() {
        player?.stop()
        player?.release()
        player = null
        mediaSession.release()
        stopForeground(true)
        stopSelf()
    }
}
