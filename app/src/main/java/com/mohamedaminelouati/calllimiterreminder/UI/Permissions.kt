package com.mohamedaminelouati.calllimiterreminder.UI

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mohamedaminelouati.calllimiterreminder.R
import com.mohamedaminelouati.calllimiterreminder.Utils.SystemBarHelper
import com.mohamedaminelouati.calllimiterreminder.Utils.ThemeUtils

class Permissions : AppCompatActivity() {
    private lateinit var permissionContainer: LinearLayout
    private lateinit var permissionsSystemHandled: LinearLayout
    private lateinit var permissionBatteryOptimization: LinearLayout
    private lateinit var backBtn: ImageView

    private val runtimePermissions by lazy {
        mutableListOf(
            PermissionItem(getString(R.string.phone), Manifest.permission.READ_PHONE_STATE, getString(R.string.permission_1)),
            PermissionItem(getString(R.string.call_logs), Manifest.permission.READ_CALL_LOG, getString(R.string.permission_2)),
            PermissionItem(getString(R.string.contacts), Manifest.permission.READ_CONTACTS, getString(R.string.permission_3))
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                add(PermissionItem(getString(R.string.end_calls), Manifest.permission.ANSWER_PHONE_CALLS, getString(R.string.permission_4)))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(PermissionItem(getString(R.string.notifications), Manifest.permission.POST_NOTIFICATIONS, getString(R.string.permission_5)))
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        val rootView = findViewById<View>(android.R.id.content)
        SystemBarHelper.setupStatusBarAppearance(window, resources, rootView)

        backBtn = findViewById(R.id.back_btn_permissions)
        permissionContainer = findViewById(R.id.permission_container)
        permissionsSystemHandled = findViewById(R.id.permission_system_handled)
        permissionBatteryOptimization = findViewById(R.id.permission_battery_optimization)

        setupAutoPermissions()
        refreshRuntimePermissions()
        refreshBatteryOptimization()

        backBtn.setOnClickListener {
            val intent = Intent(this@Permissions, com.mohamedaminelouati.calllimiterreminder.UI.Settings::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshRuntimePermissions()
        refreshBatteryOptimization()
    }

    private fun refreshBatteryOptimization() {
        permissionBatteryOptimization.removeAllViews()
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isIgnoring = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(packageName) == true
        } else {
            true
        }

        val itemView = LayoutInflater.from(this).inflate(R.layout.permission_item, permissionBatteryOptimization, false)
        val permissionTitle: TextView = itemView.findViewById(R.id.permission_title)
        val permissionDesc: TextView = itemView.findViewById(R.id.permission_desc)
        val permissionGranted: ImageView = itemView.findViewById(R.id.permission_granted)
        val permissionNotGranted: Button = itemView.findViewById(R.id.permission_not_granted)

        permissionTitle.text = getString(R.string.battery_optimization)
        permissionDesc.text = getString(R.string.battery_optimization_desc)

        if (isIgnoring) {
            permissionGranted.visibility = View.VISIBLE
            permissionNotGranted.visibility = View.GONE
        } else {
            permissionGranted.visibility = View.GONE
            permissionNotGranted.visibility = View.VISIBLE
            permissionNotGranted.text = getString(R.string.disable_optimization)
            permissionNotGranted.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        startActivity(fallbackIntent)
                    }
                }
            }
        }
        permissionBatteryOptimization.addView(itemView)
    }

    private fun setupAutoPermissions() {
        permissionsSystemHandled.removeAllViews()
        val items = arrayOf(
            AutoPermissionItem(getString(R.string.foreground_service), getString(R.string.permission_6)),
            AutoPermissionItem(getString(R.string.auto_restart_on_boot), getString(R.string.permission_7)),
            AutoPermissionItem(getString(R.string.vibrate), getString(R.string.permission_8))
        )
        for (item in items) {
            val permissionView = LayoutInflater.from(this).inflate(R.layout.permission_item, permissionsSystemHandled, false)
            val permissionTitle: TextView = permissionView.findViewById(R.id.permission_title)
            val permissionDesc: TextView = permissionView.findViewById(R.id.permission_desc)
            val permissionGranted: ImageView = permissionView.findViewById(R.id.permission_granted)
            val permissionNotGranted: Button = permissionView.findViewById(R.id.permission_not_granted)

            permissionTitle.text = item.title
            permissionDesc.text = item.description
            permissionGranted.visibility = View.VISIBLE
            permissionNotGranted.visibility = View.GONE
            permissionsSystemHandled.addView(permissionView)
        }
    }

    private fun refreshRuntimePermissions() {
        permissionContainer.removeAllViews()
        for (item in runtimePermissions) {
            val permissionView = LayoutInflater.from(this).inflate(R.layout.permission_item, permissionContainer, false)
            val permissionTitle: TextView = permissionView.findViewById(R.id.permission_title)
            val permissionDesc: TextView = permissionView.findViewById(R.id.permission_desc)
            val permissionGranted: ImageView = permissionView.findViewById(R.id.permission_granted)
            val permissionNotGranted: Button = permissionView.findViewById(R.id.permission_not_granted)

            permissionTitle.text = item.title
            permissionDesc.text = item.description

            val granted = ContextCompat.checkSelfPermission(this, item.permission) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                permissionGranted.visibility = View.VISIBLE
                permissionNotGranted.visibility = View.GONE
            } else {
                permissionNotGranted.visibility = View.VISIBLE
                permissionGranted.visibility = View.GONE
                permissionNotGranted.setOnClickListener {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, item.permission)) {
                        ActivityCompat.requestPermissions(this@Permissions, arrayOf(item.permission), 1001)
                    } else {
                        val isFirstRequest = !getSharedPreferences("perm_req", MODE_PRIVATE).getBoolean(item.permission, false)
                        if (isFirstRequest) {
                            getSharedPreferences("perm_req", MODE_PRIVATE).edit().putBoolean(item.permission, true).apply()
                            ActivityCompat.requestPermissions(this@Permissions, arrayOf(item.permission), 1001)
                        } else {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                        }
                    }
                }
            }
            permissionContainer.addView(permissionView)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            refreshRuntimePermissions()
        }
    }

    private data class PermissionItem(
        val title: String,
        val permission: String,
        val description: String
    )

    private data class AutoPermissionItem(
        val title: String,
        val description: String
    )
}
