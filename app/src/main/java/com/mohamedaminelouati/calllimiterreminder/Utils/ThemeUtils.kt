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
        val accent = PreferenceHelper.getAccentColor()
        activity.theme.applyStyle(getAccentOverlay(accent), true)
    }

    @JvmStatic
    fun getAccentOverlay(accent: String): Int {
        return when (accent.lowercase()) {
            "blue" -> R.style.ThemeOverlay_CallLimiter_Accent_Blue
            "teal" -> R.style.ThemeOverlay_CallLimiter_Accent_Teal
            "orange" -> R.style.ThemeOverlay_CallLimiter_Accent_Orange
            "rose" -> R.style.ThemeOverlay_CallLimiter_Accent_Rose
            "purple" -> R.style.ThemeOverlay_CallLimiter_Accent_Purple
            else -> R.style.ThemeOverlay_CallLimiter_Accent_Green
        }
    }
}
