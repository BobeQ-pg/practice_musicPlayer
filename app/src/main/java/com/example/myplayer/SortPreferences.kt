package com.example.myplayer

import android.content.Context
import android.content.SharedPreferences

enum class SortOrder { TITLE, ARTIST, ALBUM }

class SortPreferences(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("sort_prefs", Context.MODE_PRIVATE)

    fun getSortOrder(): SortOrder {
        return SortOrder.valueOf(sharedPreferences.getString("sort_order", SortOrder.TITLE.name) ?: SortOrder.TITLE.name)
    }

    fun setSortOrder(sortOrder: SortOrder) {
        sharedPreferences.edit().putString("sort_order", sortOrder.name).apply()
    }
}
