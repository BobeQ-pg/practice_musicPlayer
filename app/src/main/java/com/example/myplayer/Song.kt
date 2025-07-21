package com.example.myplayer

import android.net.Uri

data class Song(
    val uri: Uri,
    val title: String,
    val artist: String,
)
