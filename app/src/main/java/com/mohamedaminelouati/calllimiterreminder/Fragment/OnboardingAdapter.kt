package com.mohamedaminelouati.calllimiterreminder.Fragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardingAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    override fun createFragment(position: Int): Fragment {
        return if (position == 1) {
            Permissions()
        } else {
            Intro()
        }
    }

    override fun getItemCount(): Int = 2
}
