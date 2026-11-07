package io.github.jaswantk917.chitthi.notify

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle

object Clipboard {
    fun copyOtp(context: Context, code: String) {
        val clip = ClipData.newPlainText("OTP", code)
        // keeps the code out of the clipboard preview and keyboard history
        clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
    }
}
