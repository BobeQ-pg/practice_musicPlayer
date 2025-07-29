package com.example.myplayer

sealed class LibraryItem {
    data class HeaderItem(val title: String) : LibraryItem()
    data class SongItem(val song: Song) : LibraryItem()
}
