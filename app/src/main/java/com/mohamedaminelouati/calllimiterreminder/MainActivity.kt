package com.mohamedaminelouati.calllimiterreminder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.mohamedaminelouati.calllimiterreminder.BottomSheets.TimerBottomSheet
import com.mohamedaminelouati.calllimiterreminder.Data.PreferenceHelper
import com.mohamedaminelouati.calllimiterreminder.Service.CallMonitorService
import com.mohamedaminelouati.calllimiterreminder.UI.FilterMode
import com.mohamedaminelouati.calllimiterreminder.UI.OnboardingActivity
import com.mohamedaminelouati.calllimiterreminder.UI.SavedLimitItem
import com.mohamedaminelouati.calllimiterreminder.UI.SavedLimitsAdapter
import com.mohamedaminelouati.calllimiterreminder.UI.Settings
import com.mohamedaminelouati.calllimiterreminder.Utils.ContactHelper
import com.mohamedaminelouati.calllimiterreminder.Utils.SystemBarHelper
import com.mohamedaminelouati.calllimiterreminder.Utils.ThemeUtils
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var setLimit: Button
    private lateinit var selectFromContacts: Button
    private var selectedHour = -1
    private var selectedMinute = -1
    private var selectedSecond = -1
    private lateinit var phoneNumberField: TextInputEditText
    private lateinit var contactNameField: TextInputEditText
    private var isPhoneAvailable = false
    private var isTimeAvailable = false
    private lateinit var settings: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SavedLimitsAdapter
    private lateinit var searchInput: TextInputEditText
    private lateinit var filterChipGroup: ChipGroup
    private lateinit var emptyStateText: MaterialTextView

    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 1001
        private const val PICK_CONTACT_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        PreferenceHelper.init(this)
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)

        val isFirstTimeLogin = PreferenceHelper.isFirstTimeLogin()
        if (isFirstTimeLogin) {
            PreferenceHelper.saveBufferTime(10)
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        val rootView = findViewById<View>(android.R.id.content)
        SystemBarHelper.setupStatusBarAppearance(window, resources, rootView)

        val timeLimitButton: Button = findViewById(R.id.set_time_limit_button)
        setLimit = findViewById(R.id.set_limit_button)
        selectFromContacts = findViewById(R.id.select_contact_button)
        phoneNumberField = findViewById(R.id.phone_number_input)
        contactNameField = findViewById(R.id.contact_name)
        settings = findViewById(R.id.settings_button)

        checkAndSetInitialDate()

        requestNecessaryPermissions()

        recyclerView = findViewById(R.id.saved_limits_recycler_view)
        searchInput = findViewById(R.id.search_contacts_input)
        filterChipGroup = findViewById(R.id.filter_chip_group)
        emptyStateText = findViewById(R.id.empty_state_text)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.itemAnimator = null

        adapter = SavedLimitsAdapter(
            onItemClick = { item -> showTimerBottomSheet(item) },
            onEditClick = { item -> showEditContactNameDialog(item) },
            onDeleteClick = { item -> deleteTimeLimitWithUndo(item) }
        )
        recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.getItem(position)
                if (item != null) {
                    deleteTimeLimitWithUndo(item)
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.setSearchQuery(s?.toString() ?: "")
                updateEmptyState()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val mode = when (checkedIds.firstOrNull()) {
                R.id.chip_filter_low_time -> FilterMode.LOW_TIME
                R.id.chip_filter_unlimited -> FilterMode.WHITELIST
                R.id.chip_filter_alphabetical -> FilterMode.ALPHABETICAL
                else -> FilterMode.ALL
            }
            adapter.setFilterMode(mode)
            updateEmptyState()
        }

        updateSavedLimitsUI("LoadOnCreate")

        phoneNumberField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                isPhoneAvailable = count != 0
            }
            override fun afterTextChanged(editable: Editable?) {}
        })

        timeLimitButton.setOnClickListener {
            val bottomSheet = TimerBottomSheet(object : TimerBottomSheet.OnTimeSelectedListener {
                override fun onTimeSelected(hours: Int, minutes: Int, seconds: Int) {
                    selectedHour = hours
                    selectedMinute = minutes
                    selectedSecond = seconds
                    setLimit.text = "SET LIMIT - $hours hrs $minutes mins $seconds secs"
                }

                override fun onTimerReset() {
                    isTimeAvailable = false
                }
            })
            bottomSheet.show(supportFragmentManager, "TimerBottomSheet")
        }

        setLimit.setOnClickListener {
            val phoneNumber = phoneNumberField.text?.toString()?.trim() ?: ""
            var contactName = contactNameField.text?.toString()?.trim() ?: ""
            if (phoneNumber.isEmpty() || selectedHour == -1 || selectedMinute == -1 || selectedSecond == -1) {
                Toast.makeText(this@MainActivity, R.string.error_set_number_and_limit, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (contactName.isEmpty()) {
                contactName = ContactHelper.getContactName(this, phoneNumber)
            }
            val totalSeconds = (selectedHour * 3600) + (selectedMinute * 60) + selectedSecond

            val jsonObject = JSONObject().apply {
                if (contactName.isNotEmpty()) {
                    put("name", contactName)
                }
                put("limit", totalSeconds)
                put("remaining_time", totalSeconds)
                put("last_updated", getTodayDate())
            }

            PreferenceHelper.saveContact(phoneNumber, jsonObject.toString())

            isPhoneAvailable = false
            phoneNumberField.text = null
            contactNameField.text = null
            setLimit.setText(R.string.set_limit)

            clearInputFocus()

            selectedHour = -1
            selectedMinute = -1
            selectedSecond = -1

            updateSavedLimitsUI("SetLimit")
        }

        selectFromContacts.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, PICK_CONTACT_REQUEST)
        }

        settings.setOnClickListener {
            val intent = Intent(this@MainActivity, Settings::class.java)
            startActivity(intent)
        }
    }

    private fun requestNecessaryPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CALL_LOG)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.READ_CONTACTS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ANSWER_PHONE_CALLS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), 1)
        }
    }

    fun updateSavedLimitsUI(action: String) {
        val allEntries = PreferenceHelper.getAllContact() ?: emptyMap<String, Any>()
        CallMonitorService.syncServiceState(this)

        val items = mutableListOf<SavedLimitItem>()
        for (entry in allEntries.entries) {
            val phoneNumber = entry.key
            val phoneNumberData = entry.value as? String ?: continue
            val jsonObject = JSONObject(phoneNumberData)
            val remainingTime = jsonObject.optInt("remaining_time", 0)
            val limit = jsonObject.optInt("limit", 0)
            var contactName = jsonObject.optString("name", "")

            if (contactName.isEmpty()) {
                val resolved = ContactHelper.getContactName(this, phoneNumber)
                if (resolved.isNotEmpty()) {
                    contactName = resolved
                    jsonObject.put("name", resolved)
                    PreferenceHelper.saveContact(phoneNumber, jsonObject.toString())
                }
            }

            val isWhitelisted = PreferenceHelper.isWhitelisted(phoneNumber)
            items.add(SavedLimitItem(phoneNumber, contactName, remainingTime, limit, isWhitelisted))
        }

        adapter.submitList(items)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (adapter.displayedItems.isEmpty()) {
            emptyStateText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showTimerBottomSheet(item: SavedLimitItem) {
        val bottomSheet = TimerBottomSheet(object : TimerBottomSheet.OnTimeSelectedListener {
            override fun onTimeSelected(hours: Int, minutes: Int, seconds: Int) {
                selectedHour = hours
                selectedMinute = minutes
                selectedSecond = seconds

                val phoneNumberData = PreferenceHelper.getContact(item.phoneNumber)
                if (phoneNumberData != null) {
                    val jsonObject = updateLimit(phoneNumberData)
                    PreferenceHelper.saveContact(item.phoneNumber, jsonObject.toString())
                    updateSavedLimitsUI("Refresh")

                    selectedHour = -1
                    selectedMinute = -1
                    selectedSecond = -1
                }
            }

            private fun updateLimit(phoneNumberData: String): JSONObject {
                val jsonObject = JSONObject(phoneNumberData)
                val remainingTime = jsonObject.optInt("remaining_time", 0)
                val limit = jsonObject.optInt("limit", 0)
                val updatedLimit = (selectedHour * 3600) + (selectedMinute * 60) + selectedSecond
                if (limit == remainingTime || updatedLimit == remainingTime || remainingTime >= updatedLimit) {
                    jsonObject.put("limit", updatedLimit)
                    jsonObject.put("remaining_time", updatedLimit)
                } else if (limit < updatedLimit) {
                    val newTime = updatedLimit - limit + remainingTime
                    jsonObject.put("limit", updatedLimit)
                    jsonObject.put("remaining_time", newTime)
                }
                return jsonObject
            }

            override fun onTimerReset() {
                isTimeAvailable = false
            }
        }, item.phoneNumber, item.contactName)
        bottomSheet.show(supportFragmentManager, "TimerBottomSheet")
    }

    private fun showEditContactNameDialog(item: SavedLimitItem) {
        val input = TextInputEditText(this).apply {
            setText(item.contactName)
            setSelection(text?.length ?: 0)
        }
        val layout = com.google.android.material.textfield.TextInputLayout(this).apply {
            hint = getString(R.string.contact_name)
            setPadding(48, 16, 48, 0)
            addView(input)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.edit_contact_name)
            .setView(layout)
            .setPositiveButton(R.string.save) { _, _ ->
                val newName = input.text?.toString()?.trim() ?: ""
                val phoneNumberData = PreferenceHelper.getContact(item.phoneNumber)
                if (phoneNumberData != null) {
                    val jsonObject = JSONObject(phoneNumberData)
                    jsonObject.put("name", newName)
                    PreferenceHelper.saveContact(item.phoneNumber, jsonObject.toString())
                    updateSavedLimitsUI("EditName")
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun deleteTimeLimitWithUndo(item: SavedLimitItem) {
        val originalData = PreferenceHelper.getContact(item.phoneNumber)
        PreferenceHelper.removeContact(item.phoneNumber)
        updateSavedLimitsUI("DeleteTimeLimit")

        Snackbar.make(recyclerView, R.string.contact_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) {
                if (originalData != null) {
                    PreferenceHelper.saveContact(item.phoneNumber, originalData)
                    updateSavedLimitsUI("UndoDelete")
                }
            }.show()
    }

    override fun onResume() {
        super.onResume()
        updateSavedLimitsUI("Refresh")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val rootView = findViewById<View>(android.R.id.content)
        SystemBarHelper.setupStatusBarAppearance(window, resources, rootView)
    }

    private fun checkAndSetInitialDate() {
        val currentDate = getTodayDate()
        val lastSavedDate = PreferenceHelper.getLastUpdatedDate()
        if (lastSavedDate.isEmpty()) {
            PreferenceHelper.saveLastUpdatedDate(currentDate)
        }
    }

    private fun getTodayDate(): String {
        return SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK && data != null) {
            val contactUri = data.data ?: return
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )

            contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)

                    val phoneNumber = cursor.getString(numberIndex) ?: ""
                    val name = cursor.getString(nameIndex) ?: ""

                    val numberWithoutCountryCode = cleanLocalPhoneNumber(phoneNumber)

                    phoneNumberField.setText(numberWithoutCountryCode)
                    contactNameField.setText(name)
                }
            }
        }
    }

    private fun cleanLocalPhoneNumber(rawNumber: String?): String {
        if (rawNumber == null) return ""
        return rawNumber.replace("[^\\d]".toRegex(), "")
    }



    private fun clearInputFocus() {
        phoneNumberField.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(phoneNumberField.windowToken, 0)

        contactNameField.clearFocus()
        imm?.hideSoftInputFromWindow(contactNameField.windowToken, 0)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is TextInputEditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                    clearInputFocus()
                }
            }
        }
        return super.dispatchTouchEvent(event)
    }
}
