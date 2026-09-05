package com.mohamedaminelouati.calllimiterreminder.UI

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.mohamedaminelouati.calllimiterreminder.Data.PreferenceHelper
import com.mohamedaminelouati.calllimiterreminder.R
import com.mohamedaminelouati.calllimiterreminder.Utils.ContactHelper
import com.mohamedaminelouati.calllimiterreminder.Utils.SystemBarHelper
import com.mohamedaminelouati.calllimiterreminder.Utils.ThemeUtils

class Whitelist : AppCompatActivity() {

    private lateinit var backBtn: ImageView
    private lateinit var phoneInput: TextInputEditText
    private lateinit var nameInput: TextInputEditText
    private lateinit var btnAdd: MaterialButton
    private lateinit var btnSelectContact: MaterialButton
    private lateinit var itemsContainer: LinearLayout
    private lateinit var emptyMessage: MaterialTextView

    companion object {
        private const val PICK_CONTACT_REQUEST = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtils.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whitelist)

        val rootView = findViewById<View>(android.R.id.content)
        SystemBarHelper.setupStatusBarAppearance(window, resources, rootView)

        PreferenceHelper.init(this)

        backBtn = findViewById(R.id.back_btn_whitelist)
        phoneInput = findViewById(R.id.whitelist_phone_input)
        nameInput = findViewById(R.id.whitelist_name_input)
        btnAdd = findViewById(R.id.btn_add_whitelist)
        btnSelectContact = findViewById(R.id.btn_select_contact_whitelist)
        itemsContainer = findViewById(R.id.whitelist_items_container)
        emptyMessage = findViewById(R.id.whitelist_empty_message)

        backBtn.setOnClickListener { finish() }

        btnSelectContact.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, PICK_CONTACT_REQUEST)
        }

        btnAdd.setOnClickListener {
            val rawPhone = phoneInput.text?.toString()?.trim() ?: ""
            val cleanedPhone = cleanPhoneNumber(rawPhone)
            var contactName = nameInput.text?.toString()?.trim() ?: ""

            if (cleanedPhone.isEmpty()) {
                Toast.makeText(this, R.string.error_set_number_and_limit, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (contactName.isEmpty()) {
                contactName = ContactHelper.getContactName(this, cleanedPhone)
            }

            PreferenceHelper.addToWhitelist(cleanedPhone, contactName)

            phoneInput.text = null
            nameInput.text = null
            clearFocusAndKeyboard()
            refreshWhitelistUI()
        }

        refreshWhitelistUI()
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

                    phoneInput.setText(cleanPhoneNumber(phoneNumber))
                    nameInput.setText(name)
                }
            }
        }
    }

    private fun refreshWhitelistUI() {
        itemsContainer.removeAllViews()
        val allWhitelisted = PreferenceHelper.getAllWhitelisted()

        if (allWhitelisted.isEmpty()) {
            emptyMessage.visibility = View.VISIBLE
            return
        }

        emptyMessage.visibility = View.GONE

        for ((phoneNumber, storedName) in allWhitelisted) {
            val resolvedName = if (storedName.isNotEmpty()) {
                storedName
            } else {
                val found = ContactHelper.getContactName(this, phoneNumber)
                if (found.isNotEmpty()) {
                    PreferenceHelper.addToWhitelist(phoneNumber, found)
                    found
                } else {
                    phoneNumber
                }
            }

            createItemView(phoneNumber, resolvedName)
        }
    }

    private fun createItemView(phoneNumber: String, displayName: String) {
        val entryLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            }
        }

        val textLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.8f)
        }

        val titleView = TextView(this).apply {
            text = displayName
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
        }

        val subtitleView = TextView(this).apply {
            text = phoneNumber
            textSize = 14f
            alpha = 0.7f
            visibility = if (displayName != phoneNumber) View.VISIBLE else View.GONE
            setPadding(0, 2, 0, 0)
        }

        textLayout.addView(titleView)
        textLayout.addView(subtitleView)

        val deleteButton = MaterialButton(this).apply {
            id = View.generateViewId()
            textSize = 16f
            cornerRadius = 8
            iconPadding = 0
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            icon = ContextCompat.getDrawable(this@Whitelist, R.drawable.delete_24px)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.2f)
            setOnClickListener {
                PreferenceHelper.removeFromWhitelist(phoneNumber)
                refreshWhitelistUI()
            }
        }

        entryLayout.addView(textLayout)
        entryLayout.addView(deleteButton)
        itemsContainer.addView(entryLayout)
    }

    private fun cleanPhoneNumber(raw: String): String {
        return raw.replace("[^\\d]".toRegex(), "")
    }

    private fun clearFocusAndKeyboard() {
        phoneInput.clearFocus()
        nameInput.clearFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(phoneInput.windowToken, 0)
    }
}
