package com.example.myplayer

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.myplayer.ui.home.HomeFragment
import com.example.myplayer.ui.player.PlayerFragment

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> HomeFragment()
            1 -> PlayerFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}
