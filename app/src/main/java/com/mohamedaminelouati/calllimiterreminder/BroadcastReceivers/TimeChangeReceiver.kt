package com.mohamedaminelouati.calllimiterreminder.BroadcastReceivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mohamedaminelouati.calllimiterreminder.Data.PreferenceHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeChangeReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        PreferenceHelper.init(context)
        resetTime(context)
        Log.d("TimeChangeReceiver", "Time Reset")
    }

    fun resetTime(context: Context) {
        updateDate(context)
        PreferenceHelper.resetAllContactsRemainingTime()
    }

    private fun updateDate(context: Context) {
        val currentDate = getTodayDate()
        val lastUpdated = PreferenceHelper.getLastUpdatedDate()

        if (lastUpdated.isNotEmpty() && lastUpdated != currentDate) {
            PreferenceHelper.saveLastUpdatedDate(currentDate)
        }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }
}
