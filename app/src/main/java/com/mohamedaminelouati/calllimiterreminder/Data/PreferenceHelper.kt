package com.mohamedaminelouati.calllimiterreminder.Data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.util.TreeSet

object PreferenceHelper {
    private const val CONTACT_DATA_PREF = "contact_data_store"
    private const val LAST_UPDATED_PREF = "last_updated_store"
    private const val FIRST_TIME_PREF = "first_time_store"
    private const val READ_TERMS_CONDITIONS_PREF = "read_terms_conditions"
    private const val LAST_UPDATED_KEY = "last_updated_key"
    private const val FIRST_TIME_KEY = "first_time_key"
    private const val REMAINING_TIME = "remaining_time"
    private const val LIMIT = "limit"
    private const val SETTINGS_PREF = "settings_store"
    private const val THEME_KEY = "theme_key"
    private const val ACCENT_COLOR_KEY = "accent_color_key"
    private const val BUFFER_TIME = "buffer_key"
    private const val CALL_START_BUFFER_KEY = "call_start_buffer_key"
    private const val TERMS_CONDITIONS = "terms_conditions_key"
    private const val LIMIT_FOR_ALL_NUMBERS = "limit_for_all_numbers_key"
    private const val TIME_LIMIT_FOR_ALL_NUMBERS = "time_limit_for_all_numbers_key"
    private const val RESET_TIME_LIMIT_EACH_CALL = "reset_time_limit_each_call"
    private const val WARNING_REMINDER_KEY = "warning_reminder_key"
    private const val WARNING_REMINDER_TIME_KEY = "warning_reminder_time_key"
    private const val WARNING_REMINDER_THRESHOLDS_KEY = "warning_reminder_thresholds_key"
    private const val WARNING_SOUND_KEY = "warning_sound_key"
    private const val WARNING_VIBRATION_KEY = "warning_vibration_key"
    private const val WHITELIST_PREF = "whitelist_store"

    private var contactDataStore: SharedPreferences? = null
    private var lastUpdatedStore: SharedPreferences? = null
    private var firstTimeStore: SharedPreferences? = null
    private var settingsStore: SharedPreferences? = null
    private var readTermsAndConditionsStore: SharedPreferences? = null
    private var whitelistStore: SharedPreferences? = null

    private var contactDataEditor: SharedPreferences.Editor? = null
    private var lastUpdatedEditor: SharedPreferences.Editor? = null
    private var firstTimeEditor: SharedPreferences.Editor? = null
    private var settingsEditor: SharedPreferences.Editor? = null
    private var readTermsAndConditionsEditor: SharedPreferences.Editor? = null
    private var whitelistEditor: SharedPreferences.Editor? = null

    @JvmStatic
    fun init(context: Context) {
        val appContext = context.applicationContext
        if (contactDataStore == null) {
            contactDataStore = appContext.getSharedPreferences(CONTACT_DATA_PREF, Context.MODE_PRIVATE)
            contactDataEditor = contactDataStore?.edit()
        }
        if (lastUpdatedStore == null) {
            lastUpdatedStore = appContext.getSharedPreferences(LAST_UPDATED_PREF, Context.MODE_PRIVATE)
            lastUpdatedEditor = lastUpdatedStore?.edit()
        }
        if (firstTimeStore == null) {
            firstTimeStore = appContext.getSharedPreferences(FIRST_TIME_PREF, Context.MODE_PRIVATE)
            firstTimeEditor = firstTimeStore?.edit()
        }
        if (settingsStore == null) {
            settingsStore = appContext.getSharedPreferences(SETTINGS_PREF, Context.MODE_PRIVATE)
            settingsEditor = settingsStore?.edit()
        }
        if (readTermsAndConditionsStore == null) {
            readTermsAndConditionsStore = appContext.getSharedPreferences(READ_TERMS_CONDITIONS_PREF, Context.MODE_PRIVATE)
            readTermsAndConditionsEditor = readTermsAndConditionsStore?.edit()
        }
        if (whitelistStore == null) {
            whitelistStore = appContext.getSharedPreferences(WHITELIST_PREF, Context.MODE_PRIVATE)
            whitelistEditor = whitelistStore?.edit()
        }
    }

    @JvmStatic
    fun saveLastUpdatedDate(date: String) {
        lastUpdatedEditor?.putString(LAST_UPDATED_KEY, date)?.apply()
    }

    @JvmStatic
    fun getLastUpdatedDate(): String {
        return lastUpdatedStore?.getString(LAST_UPDATED_KEY, "") ?: ""
    }

    @JvmStatic
    fun saveContact(phoneNumber: String, data: String) {
        contactDataEditor?.putString(phoneNumber, data)?.apply()
    }

    @JvmStatic
    fun removeContact(phoneNumber: String) {
        contactDataEditor?.remove(phoneNumber)?.apply()
    }

    @JvmStatic
    fun getContact(number: String): String? {
        return contactDataStore?.getString(number, null)
    }

    @JvmStatic
    fun getAllContact(): Map<String, *>? {
        return contactDataStore?.all
    }

    @JvmStatic
    fun getAllContactSize(): Int {
        return contactDataStore?.all?.size ?: 0
    }

    @JvmStatic
    fun isFirstTimeLogin(): Boolean {
        return firstTimeStore?.getBoolean(FIRST_TIME_KEY, true) ?: true
    }

    @JvmStatic
    fun setOnboardingCompleted() {
        firstTimeEditor?.putBoolean(FIRST_TIME_KEY, false)?.apply()
    }

    @JvmStatic
    fun saveTheme(theme: String) {
        settingsEditor?.putString(THEME_KEY, theme)?.apply()
    }

    @JvmStatic
    fun getTheme(): String {
        return settingsStore?.getString(THEME_KEY, "System") ?: "System"
    }

    @JvmStatic
    fun saveAccentColor(accent: String) {
        settingsEditor?.putString(ACCENT_COLOR_KEY, accent)?.apply()
    }

    @JvmStatic
    fun getAccentColor(): String {
        return settingsStore?.getString(ACCENT_COLOR_KEY, "green") ?: "green"
    }

    @JvmStatic
    fun saveBufferTime(time: Int) {
        settingsEditor?.putInt(BUFFER_TIME, time)?.apply()
    }

    @JvmStatic
    fun getBufferTime(): Int {
        return settingsStore?.getInt(BUFFER_TIME, 10) ?: 10
    }

    @JvmStatic
    fun updateCallStartBufferValue(enabled: Boolean) {
        settingsEditor?.putBoolean(CALL_START_BUFFER_KEY, enabled)?.apply()
    }

    @JvmStatic
    fun getCallStartBufferValue(): Boolean {
        return settingsStore?.getBoolean(CALL_START_BUFFER_KEY, true) ?: true
    }

    @JvmStatic
    fun readTermsAndConditions(read: Boolean) {
        readTermsAndConditionsEditor?.putBoolean(TERMS_CONDITIONS, read)?.apply()
    }

    @JvmStatic
    fun getStatusTermsAndConditions(): Boolean {
        return readTermsAndConditionsStore?.getBoolean(TERMS_CONDITIONS, false) ?: false
    }

    @JvmStatic
    fun updateLimitForAllNumbersEnabled(enabled: Boolean) {
        settingsEditor?.putBoolean(LIMIT_FOR_ALL_NUMBERS, enabled)?.apply()
    }

    @JvmStatic
    fun getLimitForAllNumbersEnabled(): Boolean {
        return settingsStore?.getBoolean(LIMIT_FOR_ALL_NUMBERS, false) ?: false
    }

    @JvmStatic
    fun updateTimeLimitForAllNumbers(time: Int) {
        settingsEditor?.putInt(TIME_LIMIT_FOR_ALL_NUMBERS, time)?.apply()
    }

    @JvmStatic
    fun getTimeLimitForAllNumbers(): Int {
        return settingsStore?.getInt(TIME_LIMIT_FOR_ALL_NUMBERS, 0) ?: 0
    }

    @JvmStatic
    fun updateLimitForEachCallValue(enabled: Boolean) {
        settingsEditor?.putBoolean(RESET_TIME_LIMIT_EACH_CALL, enabled)?.apply()
    }

    @JvmStatic
    fun getLimitForEachCallValue(): Boolean {
        return settingsStore?.getBoolean(RESET_TIME_LIMIT_EACH_CALL, false) ?: false
    }

    @JvmStatic
    fun updateWarningReminderEnabled(enabled: Boolean) {
        settingsEditor?.putBoolean(WARNING_REMINDER_KEY, enabled)?.apply()
    }

    @JvmStatic
    fun getWarningReminderEnabled(): Boolean {
        return settingsStore?.getBoolean(WARNING_REMINDER_KEY, true) ?: true
    }

    @JvmStatic
    fun updateWarningReminderTime(time: Int) {
        settingsEditor?.putInt(WARNING_REMINDER_TIME_KEY, time)?.apply()
    }

    @JvmStatic
    fun getWarningReminderTime(): Int {
        return settingsStore?.getInt(WARNING_REMINDER_TIME_KEY, 15) ?: 15
    }

    @JvmStatic
    fun updateWarningReminderThresholds(thresholds: Set<Int>) {
        val joined = thresholds.joinToString(",")
        settingsEditor?.putString(WARNING_REMINDER_THRESHOLDS_KEY, joined)?.apply()
    }

    @JvmStatic
    fun getWarningReminderThresholds(): Set<Int> {
        val saved = settingsStore?.getString(WARNING_REMINDER_THRESHOLDS_KEY, null)
        val set = TreeSet<Int>(Comparator.reverseOrder())
        if (saved.isNullOrBlank()) {
            val singleTime = settingsStore?.getInt(WARNING_REMINDER_TIME_KEY, 15) ?: 15
            set.add(singleTime)
            return set
        }
        val parts = saved.split(",")
        for (p in parts) {
            val num = p.trim().toIntOrNull()
            if (num != null) {
                set.add(num)
            }
        }
        if (set.isEmpty()) {
            set.add(15)
        }
        return set
    }

    @JvmStatic
    fun updateWarningSoundEnabled(enabled: Boolean) {
        settingsEditor?.putBoolean(WARNING_SOUND_KEY, enabled)?.apply()
    }

    @JvmStatic
    fun getWarningSoundEnabled(): Boolean {
        return settingsStore?.getBoolean(WARNING_SOUND_KEY, true) ?: true
    }

    @JvmStatic
    fun updateWarningVibrationEnabled(enabled: Boolean) {
        settingsEditor?.putBoolean(WARNING_VIBRATION_KEY, enabled)?.apply()
    }

    @JvmStatic
    fun getWarningVibrationEnabled(): Boolean {
        return settingsStore?.getBoolean(WARNING_VIBRATION_KEY, true) ?: true
    }

    @JvmStatic
    fun resetAllContactsRemainingTime() {
        val all = getAllContact() ?: return
        for (phoneNumber in all.keys) {
            val dataStr = all[phoneNumber] as? String ?: continue
            val jsonObject = JSONObject(dataStr)
            val limit = jsonObject.optInt(LIMIT, 0)
            jsonObject.put(REMAINING_TIME, limit)
            saveContact(phoneNumber, jsonObject.toString())
        }
    }

    @JvmStatic
    fun addToWhitelist(phoneNumber: String, name: String) {
        whitelistEditor?.putString(phoneNumber, name)?.apply()
    }

    @JvmStatic
    fun removeFromWhitelist(phoneNumber: String) {
        whitelistEditor?.remove(phoneNumber)?.apply()
    }

    @JvmStatic
    fun isWhitelisted(phoneNumber: String): Boolean {
        return whitelistStore?.contains(phoneNumber) ?: false
    }

    @JvmStatic
    fun getAllWhitelisted(): Map<String, String> {
        val all = whitelistStore?.all ?: return emptyMap()
        val result = mutableMapOf<String, String>()
        for ((key, value) in all) {
            result[key] = (value as? String) ?: ""
        }
        return result
    }

    @JvmStatic
    fun getWhitelistCount(): Int {
        return whitelistStore?.all?.size ?: 0
    }
}
