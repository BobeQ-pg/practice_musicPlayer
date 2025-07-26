package com.example.myplayer

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri

class FolderRepository(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("music_folders", Context.MODE_PRIVATE)

    fun getFolders(): Set<String> {
        return sharedPreferences.getStringSet("folders", emptySet()) ?: emptySet()
    }

    fun addFolder(uri: Uri) {
        val currentFolders = getFolders().toMutableSet()
        currentFolders.add(uri.toString())
        sharedPreferences.edit().putStringSet("folders", currentFolders).apply()
    }

    fun removeFolder(uri: Uri) {
        val currentFolders = getFolders().toMutableSet()
        currentFolders.remove(uri.toString())
        sharedPreferences.edit().putStringSet("folders", currentFolders).apply()
    }
}
