package com.mohamedaminelouati.calllimiterreminder.Service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

class CommonService {
    fun copyToClipboard(context: Context, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Copied to clipboard", text)
        clipboard?.setPrimaryClip(clip)
    }
}
