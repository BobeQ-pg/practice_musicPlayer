package com.example.myplayer

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myplayer.ui.home.HomeFragment
import com.example.myplayer.ui.player.PlayerFragment
import com.example.myplayer.ui.settings.SettingsFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> PlayerFragment()
            2 -> SettingsFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}
