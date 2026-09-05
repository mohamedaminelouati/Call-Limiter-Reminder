package com.mohamedaminelouati.calllimiterreminder.BroadcastReceivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.mohamedaminelouati.calllimiterreminder.Data.PreferenceHelper
import com.mohamedaminelouati.calllimiterreminder.Service.CallMonitorService
import com.mohamedaminelouati.calllimiterreminder.Worker.CallMonitorWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "Starting Foreground")
        PreferenceHelper.init(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (CallMonitorService.shouldServiceRun(context)) {
                val workRequest = OneTimeWorkRequest.Builder(CallMonitorWorker::class.java)
                    .setInitialDelay(10, TimeUnit.SECONDS)
                    .build()
                WorkManager.getInstance(context).enqueue(workRequest)
            }
        }
    }
}
