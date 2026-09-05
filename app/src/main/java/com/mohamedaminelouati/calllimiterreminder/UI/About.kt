package com.mohamedaminelouati.calllimiterreminder.UI

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.mohamedaminelouati.calllimiterreminder.R
import com.mohamedaminelouati.calllimiterreminder.Utils.SystemBarHelper
import com.mohamedaminelouati.calllimiterreminder.Utils.ThemeUtils

class About : AppCompatActivity() {
    private lateinit var version: TextView
    private lateinit var backBtn: ImageView
    private lateinit var sourceCode: CardView
    private lateinit var licenseAbout: CardView
    private lateinit var termsAndConditions: CardView
    private lateinit var privacyPolicy: CardView
    private lateinit var linkedinProfile: LinearLayout
    private lateinit var githubProfile: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        val rootView = findViewById<View>(android.R.id.content)
        SystemBarHelper.setupStatusBarAppearance(window, resources, rootView)

        version = findViewById(R.id.version)
        sourceCode = findViewById(R.id.source_code)
        licenseAbout = findViewById(R.id.license_about)
        termsAndConditions = findViewById(R.id.terms_conditions_about)
        privacyPolicy = findViewById(R.id.privacy_policy_about)
        backBtn = findViewById(R.id.back_btn_about)
        linkedinProfile = findViewById(R.id.linkedin_profile)
        githubProfile = findViewById(R.id.github_profile)

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            version.text = "Version $versionName"
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("About", "Package info not found: ${e.message}")
        }

        linkedinProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.linkedin.com/in/mohamed-amine-louati-a383a367"))
            startActivity(intent)
        }

        githubProfile.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mohamedaminelouati"))
            startActivity(intent)
        }

        sourceCode.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mohamedaminelouati/Call-Limiter-Reminder"))
            startActivity(intent)
        }

        licenseAbout.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gnu.org/licenses/gpl-3.0.html"))
            startActivity(intent)
        }

        termsAndConditions.setOnClickListener {
            val termsUrl = "https://github.com/mohamedaminelouati/Call-Limiter-Reminder/blob/main/TermsAndConditions.md"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(termsUrl))
            startActivity(intent)
        }

        privacyPolicy.setOnClickListener {
            val privacyUrl = "https://github.com/mohamedaminelouati/Call-Limiter-Reminder/blob/main/PrivacyPolicy.md"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
            startActivity(intent)
        }

        backBtn.setOnClickListener {
            finish()
        }
    }
}
