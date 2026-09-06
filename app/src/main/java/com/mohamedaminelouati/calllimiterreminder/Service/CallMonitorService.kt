package com.mohamedaminelouati.calllimiterreminder.Service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.mohamedaminelouati.calllimiterreminder.BroadcastReceivers.CancelTimerReceiver
import com.mohamedaminelouati.calllimiterreminder.Data.PreferenceHelper
import com.mohamedaminelouati.calllimiterreminder.MainActivity
import com.mohamedaminelouati.calllimiterreminder.R
import com.mohamedaminelouati.calllimiterreminder.Utils.ContactHelper
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class CallMonitorService : Service() {
    private val channelIdIdle = "CallMonitorIdleChannel"
    private val channelIdActive = "CallMonitorActiveChannel"
    private var telephonyManager: TelephonyManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private var endCallRunnable: Runnable? = null
    private var callTimeLimit = 10 * 1000
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyCallback: Any? = null
    private var isTimerRunning = false
    private var isReminderOnlyMode = false
    private val triggeredWarnings = mutableSetOf<Int>()
    private val alertExecutor = Executors.newSingleThreadExecutor()
    private var elapsedTime = 0
    private var pendingIntent: PendingIntent? = null
    private var wasInCall = false
    private var currentCallNumber: String? = null
    private var lastIncomingNumber: String? = null

    companion object {
        private var instance: CallMonitorService? = null

        @JvmStatic
        fun getInstance(): CallMonitorService? = instance

        @JvmStatic
        fun shouldServiceRun(context: Context): Boolean {
            PreferenceHelper.init(context)
            val hasContacts = PreferenceHelper.getAllContactSize() > 0
            val hasGlobalLimit = PreferenceHelper.getLimitForAllNumbersEnabled()
            val hasReminder = PreferenceHelper.getWarningReminderEnabled()
            return hasContacts || hasGlobalLimit || hasReminder
        }

        @JvmStatic
        fun syncServiceState(context: Context) {
            val shouldRun = shouldServiceRun(context)
            val isRunning = instance != null
            val intent = Intent(context, CallMonitorService::class.java)

            if (shouldRun) {
                if (!isRunning) {
                    ContextCompat.startForegroundService(context, intent)
                }
            } else {
                if (isRunning) {
                    context.stopService(intent)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        PreferenceHelper.init(this)
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

        val clickIntent = Intent(this, CancelTimerReceiver::class.java)
        pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        if (isTimerRunning || wasInCall) {
            updateTimerNotification()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, getIdleNotification(), FOREGROUND_SERVICE_TYPE_PHONE_CALL)
            } else {
                startForeground(1, getIdleNotification())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(1)
        }

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        registerCallListener()
        return START_STICKY
    }

    private fun registerCallListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleCallState(state, null)
                }
            }
            telephonyCallback = callback
            telephonyManager?.registerTelephonyCallback(mainExecutor, callback)
        } else {
            phoneStateListener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    super.onCallStateChanged(state, phoneNumber)
                    handleCallState(state, phoneNumber)
                }
            }
            @Suppress("DEPRECATION")
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    private fun unregisterCallListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (telephonyCallback as? TelephonyCallback)?.let {
                telephonyManager?.unregisterTelephonyCallback(it)
            }
            telephonyCallback = null
        } else {
            phoneStateListener?.let {
                @Suppress("DEPRECATION")
                telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE)
            }
            phoneStateListener = null
        }
    }

    private fun handleCallState(state: Int, phoneNumber: String?) {
        if (state == TelephonyManager.CALL_STATE_RINGING) {
            if (!phoneNumber.isNullOrEmpty()) {
                lastIncomingNumber = cleanPhoneNumber(phoneNumber)
            }
            return
        }

        if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
            if (wasInCall) {
                return
            }
            wasInCall = true
            elapsedTime = 0

            val resolvedNumber = if (!phoneNumber.isNullOrEmpty()) {
                cleanPhoneNumber(phoneNumber)
            } else {
                lastIncomingNumber ?: ""
            }
            currentCallNumber = resolvedNumber

            if (resolvedNumber.isNotEmpty() && PreferenceHelper.isWhitelisted(resolvedNumber)) {
                return
            }

            var phoneNumberData: String? = null
            if (resolvedNumber.isNotEmpty()) {
                phoneNumberData = PreferenceHelper.getContact(resolvedNumber)
            }

            val isGlobalLimitEnabled = PreferenceHelper.getLimitForAllNumbersEnabled()
            val isWarningReminderEnabled = PreferenceHelper.getWarningReminderEnabled()

            if (phoneNumberData != null) {
                isReminderOnlyMode = false
                val jsonObject = JSONObject(phoneNumberData)
                var remainingTime = jsonObject.optInt("remaining_time", 0)
                val bufferTime = PreferenceHelper.getBufferTime()
                if (remainingTime < 10) {
                    remainingTime = bufferTime
                }
                callTimeLimit = remainingTime * 1000
                if (PreferenceHelper.getCallStartBufferValue()) {
                    callTimeLimit += 10000
                }
                val existingName = jsonObject.optString("name", "")
                if (existingName.isEmpty() && resolvedNumber.isNotEmpty()) {
                    val foundName = ContactHelper.getContactName(this, resolvedNumber)
                    if (foundName.isNotEmpty()) {
                        jsonObject.put("name", foundName)
                        PreferenceHelper.saveContact(resolvedNumber, jsonObject.toString())
                    }
                }
                startCallTimer(hasHardLimit = true)
            } else if (isGlobalLimitEnabled) {
                isReminderOnlyMode = false
                var remainingTime = PreferenceHelper.getTimeLimitForAllNumbers()
                if (remainingTime <= 0) {
                    remainingTime = 15
                }
                callTimeLimit = remainingTime * 1000
                if (PreferenceHelper.getCallStartBufferValue()) {
                    callTimeLimit += 10000
                }

                if (resolvedNumber.isNotEmpty()) {
                    val contactName = ContactHelper.getContactName(this, resolvedNumber)
                    val newNumber = JSONObject().apply {
                        if (contactName.isNotEmpty()) {
                            put("name", contactName)
                        }
                        put("remaining_time", remainingTime)
                        put("limit", remainingTime)
                        put("last_updated", getTodayDate())
                    }
                    PreferenceHelper.saveContact(resolvedNumber, newNumber.toString())
                }

                startCallTimer(hasHardLimit = true)
            } else if (isWarningReminderEnabled) {
                isReminderOnlyMode = true
                callTimeLimit = 0
                startCallTimer(hasHardLimit = false)
            }
        } else if (state == TelephonyManager.CALL_STATE_IDLE) {
            if (wasInCall) {
                wasInCall = false
                if (isTimerRunning) {
                    stopCallTimer()
                }

                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator?.hasVibrator() == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(200)
                    }
                }

                val savedNumber = currentCallNumber
                if (!savedNumber.isNullOrEmpty() && !isReminderOnlyMode) {
                    val phoneNumberData = PreferenceHelper.getContact(savedNumber)
                    if (phoneNumberData != null) {
                        val jsonObject = JSONObject(phoneNumberData)
                        val isLimitResetEachCall = PreferenceHelper.getLimitForEachCallValue()
                        if (!isLimitResetEachCall) {
                            var remainingTime = (callTimeLimit / 1000) - elapsedTime
                            if (remainingTime < 0) {
                                remainingTime = 0
                            }
                            jsonObject.put("remaining_time", remainingTime)
                        }
                        PreferenceHelper.saveContact(savedNumber, jsonObject.toString())
                    }
                }

                currentCallNumber = null
                lastIncomingNumber = null
            }
        }
    }

    private fun cleanPhoneNumber(raw: String): String {
        return raw.replace("[^\\d]".toRegex(), "")
    }

    private fun startCallTimer(hasHardLimit: Boolean) {
        triggeredWarnings.clear()
        if (isTimerRunning) return
        isTimerRunning = true

        if (hasHardLimit) {
            endCallRunnable = Runnable { endCall() }
            endCallRunnable?.let { handler.postDelayed(it, callTimeLimit.toLong()) }
        } else {
            endCallRunnable = null
        }
        handler.post(updateRunnable)
    }

    fun stopCallTimer() {
        isTimerRunning = false
        triggeredWarnings.clear()
        endCallRunnable?.let { handler.removeCallbacks(it) }
        handler.removeCallbacks(updateRunnable)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(1)
    }

    private val updateRunnable: Runnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                elapsedTime++

                val isWarningReminderEnabled = PreferenceHelper.getWarningReminderEnabled()
                val warningThresholds = PreferenceHelper.getWarningReminderThresholds()

                if (isWarningReminderEnabled) {
                    if (isReminderOnlyMode) {
                        if (warningThresholds.contains(elapsedTime) && !triggeredWarnings.contains(elapsedTime)) {
                            triggeredWarnings.add(elapsedTime)
                            triggerWarningAlert()
                        }
                    } else {
                        val remainingSeconds = (callTimeLimit / 1000) - elapsedTime
                        if (warningThresholds.contains(remainingSeconds) && !triggeredWarnings.contains(remainingSeconds)) {
                            triggeredWarnings.add(remainingSeconds)
                            triggerWarningAlert()
                        }
                    }
                }

                updateTimerNotification()
                handler.postDelayed(this, 1000)
            }
        }
    }

    private fun triggerWarningAlert() {
        val soundEnabled = PreferenceHelper.getWarningSoundEnabled()
        val vibrationEnabled = PreferenceHelper.getWarningVibrationEnabled()
        if (!soundEnabled && !vibrationEnabled) return

        alertExecutor.execute {
            if (soundEnabled) {
                val primary = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 90)
                val played = primary.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
                if (played) {
                    Thread.sleep(400)
                    primary.release()
                } else {
                    primary.release()
                    val fallback = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
                    fallback.startTone(ToneGenerator.TONE_PROP_BEEP2, 350)
                    Thread.sleep(400)
                    fallback.release()
                }
            }

            if (vibrationEnabled) {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (vibrator?.hasVibrator() == true) {
                    val timings = longArrayOf(0, 200, 100, 200)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(timings, -1)
                    }
                }
            }
        }
    }

    private fun endCall() {
        var callEnded = false
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        if (telecomManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val hasPermission = ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ANSWER_PHONE_CALLS
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                @Suppress("DEPRECATION")
                callEnded = telecomManager.endCall()
                Log.d("CallMonitorService", "Call ended: $callEnded")
            }
        }

        if (!callEnded) {
            triggerUrgentLimitReachedAlert()
        }
    }

    private fun triggerUrgentLimitReachedAlert() {
        alertExecutor.execute {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator?.hasVibrator() == true) {
                val timings = longArrayOf(0, 400, 200, 400, 200, 600)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(timings, -1)
                }
            }

            val toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 100)
            toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            val idleChannel = NotificationChannel(
                channelIdIdle,
                getString(R.string.notification_channel_idle_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                description = getString(R.string.notification_channel_idle_desc)
            }

            val activeChannel = NotificationChannel(
                channelIdActive,
                getString(R.string.notification_channel_active_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                description = getString(R.string.notification_channel_active_desc)
            }

            manager.createNotificationChannel(idleChannel)
            manager.createNotificationChannel(activeChannel)
        }
    }

    private fun getIdleNotification(): Notification {
        val appOpenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val intent = PendingIntent.getActivity(
            this,
            0,
            appOpenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelIdIdle)
            .setContentTitle(getString(R.string.notification_idle_title))
            .setContentText(getString(R.string.notification_idle_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(intent)
            .build()
    }

    private fun updateTimerNotification() {
        val remainingSeconds = (callTimeLimit / 1000) - elapsedTime
        val contentText = if (isReminderOnlyMode) {
            "Call Elapsed Time: " + formatTime(elapsedTime)
        } else {
            "Time Left: " + formatTime(remainingSeconds)
        }

        val resolvedNumber = currentCallNumber ?: ""
        val contactName = if (resolvedNumber.isNotEmpty()) {
            val data = PreferenceHelper.getContact(resolvedNumber)
            val storedName = data?.let {
                JSONObject(it).optString("name", "")
            } ?: ""
            storedName.ifEmpty {
                ContactHelper.getContactName(this, resolvedNumber)
            }
        } else {
            ""
        }

        val title = when {
            contactName.isNotEmpty() && resolvedNumber.isNotEmpty() -> "$contactName ($resolvedNumber)"
            contactName.isNotEmpty() -> contactName
            resolvedNumber.isNotEmpty() -> resolvedNumber
            else -> "Tap here to stop call timer"
        }

        val notification = NotificationCompat.Builder(this, channelIdActive)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_v2))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, FOREGROUND_SERVICE_TYPE_PHONE_CALL)
        } else {
            startForeground(1, notification)
        }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(seconds: Int): String {
        val safeSeconds = if (seconds < 0) 0 else seconds
        val h = safeSeconds / 3600
        val m = (safeSeconds % 3600) / 60
        val s = safeSeconds % 60

        return if (h > 0) {
            String.format("%02d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        alertExecutor.shutdown()
        unregisterCallListener()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(1)
    }
}
