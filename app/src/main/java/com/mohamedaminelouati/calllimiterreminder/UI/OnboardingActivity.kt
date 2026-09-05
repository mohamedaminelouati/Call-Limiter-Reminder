package com.mohamedaminelouati.calllimiterreminder.UI

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.mohamedaminelouati.calllimiterreminder.Data.PreferenceHelper
import com.mohamedaminelouati.calllimiterreminder.Fragment.OnboardingAdapter
import com.mohamedaminelouati.calllimiterreminder.MainActivity
import com.mohamedaminelouati.calllimiterreminder.R
import com.mohamedaminelouati.calllimiterreminder.Utils.SystemBarHelper
import com.mohamedaminelouati.calllimiterreminder.Utils.ThemeUtils

class OnboardingActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var nextButton: Button
    private lateinit var skipButton: Button
    private lateinit var adapter: OnboardingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        PreferenceHelper.init(this)
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val rootView = findViewById<View>(android.R.id.content)
        SystemBarHelper.setupStatusBarAppearance(window, resources, rootView)

        viewPager = findViewById(R.id.viewPager)
        nextButton = findViewById(R.id.btnNext)
        skipButton = findViewById(R.id.btnSkip)

        adapter = OnboardingAdapter(this)
        viewPager.adapter = adapter

        nextButton.setOnClickListener {
            val pos = viewPager.currentItem
            if (pos < adapter.itemCount - 1) {
                viewPager.currentItem = pos + 1
            } else {
                setOnboardingCompleted()
                startMain()
            }
        }

        skipButton.setOnClickListener {
            setOnboardingCompleted()
            startMain()
        }
    }

    private fun setOnboardingCompleted() {
        PreferenceHelper.setOnboardingCompleted()
    }

    private fun startMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
