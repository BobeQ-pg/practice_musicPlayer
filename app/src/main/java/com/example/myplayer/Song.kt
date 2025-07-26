package com.example.myplayer

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Song(
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String
) : Parcelable
