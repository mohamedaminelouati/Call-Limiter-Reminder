package com.mohamedaminelouati.calllimiterreminder.Utils

import android.app.Activity
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import com.mohamedaminelouati.calllimiterreminder.Data.PreferenceHelper
import com.mohamedaminelouati.calllimiterreminder.R

object ThemeUtils {
    @JvmStatic
    fun applyTheme(activity: Activity) {
        val theme = PreferenceHelper.getTheme()
        when (theme) {
            "OLED" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                activity.setTheme(R.style.AppTheme_Oled)
            }
            "Dark" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                activity.setTheme(R.style.AppTheme)
            }
            "Light" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                activity.setTheme(R.style.AppTheme)
            }
            else -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                val isNight = (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                if (isNight) {
                    activity.setTheme(R.style.AppTheme_Oled)
                } else {
                    activity.setTheme(R.style.AppTheme)
                }
            }
        }
    }
}
