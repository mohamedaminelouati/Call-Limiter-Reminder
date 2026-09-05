package com.mohamedaminelouati.calllimiterreminder.Utils

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController

object SystemBarHelper {

    @JvmStatic
    fun setupStatusBarAppearance(window: Window, resources: Resources, rootView: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.setDecorFitsSystemWindows(false)

            rootView.setOnApplyWindowInsetsListener { v, insets ->
                val systemBarsInsets = insets.getInsets(WindowInsets.Type.systemBars())
                v.setPadding(0, systemBarsInsets.top, 0, 0)
                insets
            }

            val controller = window.insetsController
            if (controller != null) {
                val isDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                controller.setSystemBarsAppearance(
                    if (isDarkTheme) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            }
        }
    }
}
