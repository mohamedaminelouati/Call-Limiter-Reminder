package com.mohamedaminelouati.calllimiterreminder.UI

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.mohamedaminelouati.calllimiterreminder.BottomSheets.TimerBottomSheet
import com.mohamedaminelouati.calllimiterreminder.Data.PreferenceHelper
import com.mohamedaminelouati.calllimiterreminder.R
import com.mohamedaminelouati.calllimiterreminder.Service.CallMonitorService
import com.mohamedaminelouati.calllimiterreminder.Utils.SystemBarHelper
import com.mohamedaminelouati.calllimiterreminder.Utils.ThemeUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.TreeSet

class Settings : AppCompatActivity() {
    private lateinit var layoutTheme: LinearLayout
    private lateinit var permissions: LinearLayout
    private lateinit var about: LinearLayout
    private lateinit var timeLimitForAllNumbers: LinearLayout
    private lateinit var whitelistLayout: LinearLayout
    private lateinit var warningReminderTimeLayout: LinearLayout
    private lateinit var exportBackupRow: LinearLayout
    private lateinit var importBackupRow: LinearLayout

    private val exportBackupLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            exportBackupToUri(uri)
        }
    }

    private val importBackupLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importBackupFromUri(uri)
        }
    }

    private lateinit var selectedThemeText: TextView
    private lateinit var bufferValueText: TextView
    private lateinit var timeLimit: TextView
    private lateinit var whitelistSummaryText: TextView
    private lateinit var warningReminderTimeText: TextView

    private lateinit var backBtn: ImageView
    private lateinit var bufferBar: SeekBar
    private lateinit var switchBtn: MaterialSwitch
    private lateinit var callStartBufferSwitchBtn: MaterialSwitch
    private lateinit var limitResetForEachCallSwitchBtn: MaterialSwitch
    private lateinit var warningReminderSwitchBtn: MaterialSwitch
    private lateinit var warningSoundSwitchBtn: MaterialSwitch
    private lateinit var warningVibrationSwitchBtn: MaterialSwitch
    private lateinit var warningReminderOptionsLayout: LinearLayout

    private var isChecked = false
    private var isCallStartBufferEnabled = true
    private var islimitRestForEachCallEnabled = false
    private var isWarningReminderEnabled = true
    private val bufferValues = intArrayOf(10, 20, 30, 60, 120, 180, 240, 300)

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val rootView = findViewById<View>(android.R.id.content)
        SystemBarHelper.setupStatusBarAppearance(window, resources, rootView)

        PreferenceHelper.init(this)

        selectedThemeText = findViewById(R.id.selected_theme_text)
        backBtn = findViewById(R.id.back_btn)
        layoutTheme = findViewById(R.id.theme)
        permissions = findViewById(R.id.permissions)
        about = findViewById(R.id.about_settings)
        bufferBar = findViewById(R.id.buffer_time_seek_bar)
        bufferValueText = findViewById(R.id.buffer_time_value)
        switchBtn = findViewById(R.id.limit_all_numbers_switch)
        timeLimitForAllNumbers = findViewById(R.id.time_limit_all_numbers)
        timeLimit = findViewById(R.id.time_limit_all_numbers_text)
        whitelistLayout = findViewById(R.id.whitelist_layout)
        whitelistSummaryText = findViewById(R.id.whitelist_summary_text)
        callStartBufferSwitchBtn = findViewById(R.id.call_start_buffer_time)
        limitResetForEachCallSwitchBtn = findViewById(R.id.limit_reset_each_call)
        warningReminderSwitchBtn = findViewById(R.id.warning_reminder_switch)
        warningReminderOptionsLayout = findViewById(R.id.warning_reminder_options_layout)
        warningReminderTimeLayout = findViewById(R.id.warning_reminder_time_layout)
        warningReminderTimeText = findViewById(R.id.warning_reminder_time_text)
        warningSoundSwitchBtn = findViewById(R.id.warning_sound_switch)
        warningVibrationSwitchBtn = findViewById(R.id.warning_vibration_switch)
        exportBackupRow = findViewById(R.id.export_backup_row)
        importBackupRow = findViewById(R.id.import_backup_row)

        isChecked = PreferenceHelper.getLimitForAllNumbersEnabled()
        isCallStartBufferEnabled = PreferenceHelper.getCallStartBufferValue()
        islimitRestForEachCallEnabled = PreferenceHelper.getLimitForEachCallValue()
        isWarningReminderEnabled = PreferenceHelper.getWarningReminderEnabled()
        val timeLimit1 = PreferenceHelper.getTimeLimitForAllNumbers()

        val hours = timeLimit1 / 3600
        val minutes = (timeLimit1 % 3600) / 60
        val seconds = timeLimit1 % 60
        val hoursStr = if (hours < 10) "0$hours" else hours.toString()
        val minutesStr = if (minutes < 10) "0$minutes" else minutes.toString()
        val secondsStr = if (seconds < 10) "0$seconds" else seconds.toString()
        timeLimit.text = "$hoursStr:$minutesStr:$secondsStr"

        callStartBufferSwitchBtn.isChecked = isCallStartBufferEnabled

        switchBtn.isChecked = isChecked
        timeLimitForAllNumbers.visibility = if (isChecked) View.VISIBLE else View.GONE
        whitelistLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        updateWhitelistSummary()

        whitelistLayout.setOnClickListener {
            val intent = Intent(this@Settings, Whitelist::class.java)
            startActivity(intent)
        }

        val bufferTime = PreferenceHelper.getBufferTime()
        bufferBar.max = bufferValues.size - 1
        var index = 0
        for (i in bufferValues.indices) {
            if (bufferValues[i] == bufferTime) {
                index = i
            }
        }

        val currentTheme = PreferenceHelper.getTheme()
        if ("OLED" == currentTheme) {
            selectedThemeText.text = getString(R.string.dark_oled)
        } else {
            selectedThemeText.text = currentTheme
        }
        bufferBar.progress = index
        bufferValueText.text = formatBufferTime(bufferTime)

        limitResetForEachCallSwitchBtn.isChecked = islimitRestForEachCallEnabled

        layoutTheme.setOnClickListener { showThemeBottomSheet() }

        backBtn.setOnClickListener { finish() }

        bufferBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val selectedValue = bufferValues[progress]
                bufferValueText.text = formatBufferTime(selectedValue)
                PreferenceHelper.saveBufferTime(selectedValue)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        about.setOnClickListener {
            val intent = Intent(this@Settings, About::class.java)
            startActivity(intent)
        }

        permissions.setOnClickListener {
            val intent = Intent(this@Settings, Permissions::class.java)
            startActivity(intent)
        }

        exportBackupRow.setOnClickListener {
            exportBackupLauncher.launch("call_limiter_backup.json")
        }

        importBackupRow.setOnClickListener {
            importBackupLauncher.launch(arrayOf("application/json", "*/*"))
        }

        callStartBufferSwitchBtn.setOnCheckedChangeListener { _, b ->
            PreferenceHelper.updateCallStartBufferValue(b)
        }

        switchBtn.setOnCheckedChangeListener { _, b ->
            PreferenceHelper.updateLimitForAllNumbersEnabled(b)
            isChecked = b
            timeLimitForAllNumbers.visibility = if (b) View.VISIBLE else View.GONE
            whitelistLayout.visibility = if (b) View.VISIBLE else View.GONE
            CallMonitorService.syncServiceState(this@Settings)
        }

        timeLimit.setOnClickListener {
            val currentLimit = PreferenceHelper.getTimeLimitForAllNumbers()
            val bottomSheet = TimerBottomSheet(object : TimerBottomSheet.OnTimeSelectedListener {
                override fun onTimeSelected(hours1: Int, minutes1: Int, seconds1: Int) {
                    val hStr = if (hours1 < 10) "0$hours1" else hours1.toString()
                    val mStr = if (minutes1 < 10) "0$minutes1" else minutes1.toString()
                    val sStr = if (seconds1 < 10) "0$seconds1" else seconds1.toString()
                    timeLimit.text = "$hStr:$mStr:$sStr"

                    val timeLimitInSeconds = (hours1 * 3600) + (minutes1 * 60) + seconds1
                    PreferenceHelper.updateTimeLimitForAllNumbers(timeLimitInSeconds)
                    CallMonitorService.syncServiceState(this@Settings)
                }

                override fun onTimerReset() {}
            }, currentLimit)
            bottomSheet.show(supportFragmentManager, "TimerBottomSheet")
        }

        limitResetForEachCallSwitchBtn.setOnCheckedChangeListener { _, b ->
            PreferenceHelper.updateLimitForEachCallValue(b)
            if (b) {
                PreferenceHelper.resetAllContactsRemainingTime()
            }
        }

        warningReminderSwitchBtn.isChecked = isWarningReminderEnabled
        warningReminderOptionsLayout.visibility = if (isWarningReminderEnabled) View.VISIBLE else View.GONE
        warningReminderTimeText.text = formatThresholds(PreferenceHelper.getWarningReminderThresholds())
        warningSoundSwitchBtn.isChecked = PreferenceHelper.getWarningSoundEnabled()
        warningVibrationSwitchBtn.isChecked = PreferenceHelper.getWarningVibrationEnabled()

        warningReminderSwitchBtn.setOnCheckedChangeListener { _, enabled ->
            PreferenceHelper.updateWarningReminderEnabled(enabled)
            warningReminderOptionsLayout.visibility = if (enabled) View.VISIBLE else View.GONE
            CallMonitorService.syncServiceState(this@Settings)
        }

        warningSoundSwitchBtn.setOnCheckedChangeListener { _, enabled ->
            PreferenceHelper.updateWarningSoundEnabled(enabled)
        }

        warningVibrationSwitchBtn.setOnCheckedChangeListener { _, enabled ->
            PreferenceHelper.updateWarningVibrationEnabled(enabled)
        }

        warningReminderTimeLayout.setOnClickListener { showWarningThresholdsDialog() }
    }

    private fun showWarningThresholdsDialog() {
        val labels = arrayOf("5 s", "10 s", "15 s", "20 s", "30 s", "45 s", "60 s", "120 s")
        val values = intArrayOf(5, 10, 15, 20, 30, 45, 60, 120)
        val checkedItems = BooleanArray(values.size)
        val currentThresholds = PreferenceHelper.getWarningReminderThresholds()

        for (i in values.indices) {
            checkedItems[i] = currentThresholds.contains(values[i])
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.warning_reminder_thresholds)
            .setMultiChoiceItems(labels, checkedItems) { _, which, isChecked1 ->
                checkedItems[which] = isChecked1
            }
            .setPositiveButton(R.string.ok) { _, _ ->
                val newThresholds = TreeSet<Int>(Comparator.reverseOrder())
                for (i in values.indices) {
                    if (checkedItems[i]) {
                        newThresholds.add(values[i])
                    }
                }
                if (newThresholds.isEmpty()) {
                    newThresholds.add(15)
                }
                PreferenceHelper.updateWarningReminderThresholds(newThresholds)
                warningReminderTimeText.text = formatThresholds(newThresholds)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun formatThresholds(thresholds: Set<Int>): String {
        val sb = StringBuilder()
        for (t in thresholds) {
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append(formatBufferTime(t))
        }
        return if (sb.isNotEmpty()) sb.toString() else "15 s"
    }

    private fun showThemeBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.theme_bottom_sheet, null)
        bottomSheetDialog.setContentView(sheetView)

        val selectedTheme = PreferenceHelper.getTheme()

        val optionSystem: LinearLayout = sheetView.findViewById(R.id.option_system)
        val optionLight: LinearLayout = sheetView.findViewById(R.id.option_light)
        val optionDark: LinearLayout = sheetView.findViewById(R.id.option_dark)
        val optionOled: LinearLayout? = sheetView.findViewById(R.id.option_oled)

        val systemRadioBtn: RadioButton = sheetView.findViewById(R.id.system_radio_btn)
        val lightRadioBtn: RadioButton = sheetView.findViewById(R.id.light_radio_btn)
        val darkRadioBtn: RadioButton = sheetView.findViewById(R.id.dark_radio_btn)
        val oledRadioBtn: RadioButton? = sheetView.findViewById(R.id.oled_radio_btn)

        when (selectedTheme) {
            "Light" -> lightRadioBtn.isChecked = true
            "Dark" -> darkRadioBtn.isChecked = true
            "OLED" -> oledRadioBtn?.isChecked = true
            else -> systemRadioBtn.isChecked = true
        }

        val applyAndDismiss = { themeName: String, displayName: String ->
            PreferenceHelper.saveTheme(themeName)
            selectedThemeText.text = displayName
            ThemeUtils.applyTheme(this@Settings)
            recreate()
            bottomSheetDialog.dismiss()
        }

        optionSystem.setOnClickListener { applyAndDismiss("System", "System") }
        optionLight.setOnClickListener { applyAndDismiss("Light", "Light") }
        optionDark.setOnClickListener { applyAndDismiss("Dark", "Dark") }
        optionOled?.setOnClickListener { applyAndDismiss("OLED", getString(R.string.dark_oled)) }

        systemRadioBtn.setOnClickListener { applyAndDismiss("System", "System") }
        lightRadioBtn.setOnClickListener { applyAndDismiss("Light", "Light") }
        darkRadioBtn.setOnClickListener { applyAndDismiss("Dark", "Dark") }
        oledRadioBtn?.setOnClickListener { applyAndDismiss("OLED", getString(R.string.dark_oled)) }

        bottomSheetDialog.show()
    }

    private fun formatBufferTime(seconds: Int): String {
        return if (seconds < 60) {
            "$seconds s"
        } else {
            val minutes = seconds / 60
            "$minutes min"
        }
    }

    override fun onResume() {
        super.onResume()
        updateWhitelistSummary()
    }

    private fun updateWhitelistSummary() {
        val count = PreferenceHelper.getWhitelistCount()
        if (count == 0) {
            whitelistSummaryText.setText(R.string.whitelist_description)
        } else {
            whitelistSummaryText.text = String.format(getString(R.string.whitelisted_count), count)
        }
    }

    private fun exportBackupToUri(uri: Uri) {
        try {
            val rootJson = JSONObject()
            rootJson.put("version", 1)
            rootJson.put("exported_at", System.currentTimeMillis())

            val contactsMap = PreferenceHelper.getAllContact() ?: emptyMap<String, Any>()
            val contactsJson = JSONObject()
            for ((key, value) in contactsMap) {
                if (value is String) {
                    try {
                        contactsJson.put(key, JSONObject(value))
                    } catch (e: JSONException) {
                        contactsJson.put(key, value)
                    }
                }
            }
            rootJson.put("contacts", contactsJson)

            val whitelistMap = PreferenceHelper.getAllWhitelisted()
            val whitelistJson = JSONObject()
            for ((key, value) in whitelistMap) {
                whitelistJson.put(key, value)
            }
            rootJson.put("whitelist", whitelistJson)

            val settingsJson = JSONObject().apply {
                put("limit_for_all_numbers_enabled", PreferenceHelper.getLimitForAllNumbersEnabled())
                put("limit_for_all_numbers_time", PreferenceHelper.getTimeLimitForAllNumbers())
                put("warning_reminder_enabled", PreferenceHelper.getWarningReminderEnabled())
                put("warning_reminder_thresholds", PreferenceHelper.getWarningReminderThresholds().joinToString(","))
                put("warning_sound_enabled", PreferenceHelper.getWarningSoundEnabled())
                put("warning_vibration_enabled", PreferenceHelper.getWarningVibrationEnabled())
                put("buffer_time", PreferenceHelper.getBufferTime())
            }
            rootJson.put("settings", settingsJson)

            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
                outputStream.flush()
            }

            Toast.makeText(this, R.string.backup_exported_success, Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, R.string.backup_error_io, Toast.LENGTH_SHORT).show()
        } catch (e: JSONException) {
            Toast.makeText(this, R.string.backup_error_io, Toast.LENGTH_SHORT).show()
        }
    }

    private fun importBackupFromUri(uri: Uri) {
        try {
            val content = contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader(Charsets.UTF_8).readText()
            } ?: return

            val rootJson = JSONObject(content)
            val contactsJson = rootJson.optJSONObject("contacts")
            if (contactsJson != null) {
                val keys = contactsJson.keys()
                while (keys.hasNext()) {
                    val phone = keys.next()
                    val contactObj = contactsJson.optJSONObject(phone)
                    if (contactObj != null) {
                        PreferenceHelper.saveContact(phone, contactObj.toString())
                    }
                }
            }

            val whitelistJson = rootJson.optJSONObject("whitelist")
            if (whitelistJson != null) {
                val keys = whitelistJson.keys()
                while (keys.hasNext()) {
                    val phone = keys.next()
                    val name = whitelistJson.optString(phone, "")
                    PreferenceHelper.addToWhitelist(phone, name)
                }
            }

            val settingsJson = rootJson.optJSONObject("settings")
            if (settingsJson != null) {
                if (settingsJson.has("limit_for_all_numbers_enabled")) {
                    PreferenceHelper.updateLimitForAllNumbersEnabled(settingsJson.optBoolean("limit_for_all_numbers_enabled"))
                }
                if (settingsJson.has("limit_for_all_numbers_time")) {
                    PreferenceHelper.updateTimeLimitForAllNumbers(settingsJson.optInt("limit_for_all_numbers_time"))
                }
                if (settingsJson.has("warning_reminder_enabled")) {
                    PreferenceHelper.updateWarningReminderEnabled(settingsJson.optBoolean("warning_reminder_enabled"))
                }
                if (settingsJson.has("warning_reminder_thresholds")) {
                    val threshStr = settingsJson.optString("warning_reminder_thresholds", "")
                    val set = TreeSet<Int>(Comparator.reverseOrder())
                    for (p in threshStr.split(",")) {
                        val n = p.trim().toIntOrNull()
                        if (n != null) set.add(n)
                    }
                    if (set.isNotEmpty()) {
                        PreferenceHelper.updateWarningReminderThresholds(set)
                    }
                }
                if (settingsJson.has("warning_sound_enabled")) {
                    PreferenceHelper.updateWarningSoundEnabled(settingsJson.optBoolean("warning_sound_enabled"))
                }
                if (settingsJson.has("warning_vibration_enabled")) {
                    PreferenceHelper.updateWarningVibrationEnabled(settingsJson.optBoolean("warning_vibration_enabled"))
                }
                if (settingsJson.has("buffer_time")) {
                    PreferenceHelper.saveBufferTime(settingsJson.optInt("buffer_time"))
                }
            }

            CallMonitorService.syncServiceState(this)
            updateWhitelistSummary()
            Toast.makeText(this, R.string.backup_imported_success, Toast.LENGTH_SHORT).show()
        } catch (e: JSONException) {
            Toast.makeText(this, R.string.backup_error_invalid, Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, R.string.backup_error_io, Toast.LENGTH_SHORT).show()
        }
    }
}
