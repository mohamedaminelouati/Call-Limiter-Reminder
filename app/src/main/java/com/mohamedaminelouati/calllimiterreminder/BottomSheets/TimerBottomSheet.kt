package com.mohamedaminelouati.calllimiterreminder.BottomSheets

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textview.MaterialTextView
import com.mohamedaminelouati.calllimiterreminder.R

class TimerBottomSheet : BottomSheetDialogFragment {
    private var hourPicker: NumberPicker? = null
    private var minutePicker: NumberPicker? = null
    private var secondPicker: NumberPicker? = null
    private var btnDeleteTimer: Button? = null
    private var btnOk: Button? = null
    private var btnCancel: Button? = null
    private var displayPhoneNumberAndName: MaterialTextView? = null
    private var phoneNumber: String = ""
    private var name: String = ""
    private var timeSelectedListener: OnTimeSelectedListener? = null

    interface OnTimeSelectedListener {
        fun onTimeSelected(hours: Int, minutes: Int, seconds: Int)
        fun onTimerReset()
    }

    private var initialHours = 0
    private var initialMinutes = 0
    private var initialSeconds = 0

    constructor() : super()

    constructor(listener: OnTimeSelectedListener) : super() {
        this.timeSelectedListener = listener
    }

    constructor(listener: OnTimeSelectedListener, initialTotalSeconds: Int) : super() {
        this.timeSelectedListener = listener
        if (initialTotalSeconds > 0) {
            this.initialHours = initialTotalSeconds / 3600
            this.initialMinutes = (initialTotalSeconds % 3600) / 60
            this.initialSeconds = initialTotalSeconds % 60
        }
    }

    constructor(listener: OnTimeSelectedListener, phoneNumber: String, name: String) : super() {
        this.timeSelectedListener = listener
        this.phoneNumber = phoneNumber
        this.name = name
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_timer_bottom_sheet, container, false)

        hourPicker = view.findViewById(R.id.hourPicker)
        minutePicker = view.findViewById(R.id.minutePicker)
        secondPicker = view.findViewById(R.id.secondPicker)
        btnDeleteTimer = view.findViewById(R.id.btnDeleteTimer)
        btnOk = view.findViewById(R.id.btnOk)
        btnCancel = view.findViewById(R.id.btnCancel)
        displayPhoneNumberAndName = view.findViewById(R.id.phone_number_name)

        if (phoneNumber.isNotEmpty()) {
            if (name.isNotEmpty()) {
                displayPhoneNumberAndName?.text = "$phoneNumber  $name"
            } else {
                displayPhoneNumberAndName?.text = phoneNumber
            }
        } else {
            displayPhoneNumberAndName?.visibility = View.GONE
        }

        hourPicker?.minValue = 0
        hourPicker?.maxValue = 24

        minutePicker?.minValue = 0
        minutePicker?.maxValue = 59

        secondPicker?.minValue = 0
        secondPicker?.maxValue = 59

        hourPicker?.value = initialHours
        minutePicker?.value = initialMinutes
        secondPicker?.value = initialSeconds

        btnDeleteTimer?.setOnClickListener {
            hourPicker?.value = 0
            minutePicker?.value = 0
            secondPicker?.value = 0
            timeSelectedListener?.onTimerReset()
        }

        btnOk?.setOnClickListener {
            val selectedHours = hourPicker?.value ?: 0
            val selectedMinutes = minutePicker?.value ?: 0
            val selectedSeconds = secondPicker?.value ?: 0
            if (selectedHours == 0 && selectedMinutes == 0 && selectedSeconds == 0) {
                Toast.makeText(activity, R.string.error_proper_limit, Toast.LENGTH_SHORT).show()
            } else {
                timeSelectedListener?.onTimeSelected(selectedHours, selectedMinutes, selectedSeconds)
                dismiss()
            }
        }

        btnCancel?.setOnClickListener { dismiss() }

        return view
    }
}
