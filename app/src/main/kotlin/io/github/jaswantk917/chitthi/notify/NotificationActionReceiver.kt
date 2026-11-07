package io.github.jaswantk917.chitthi.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import io.github.jaswantk917.chitthi.ChitthiApp
import io.github.jaswantk917.chitthi.send.SmsSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val senderKey = intent.getStringExtra(Notifier.EXTRA_SENDER) ?: return
        val inbox = ChitthiApp.instance.inbox
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (inbox.sender(senderKey) == null) inbox.reloadNow()
                when (intent.action) {
                    COPY -> intent.getStringExtra(EXTRA_CODE)?.let { Clipboard.copyOtp(context, it) }
                    REPLY -> {
                        val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(Notifier.REPLY_KEY)?.toString()
                        val to = inbox.sender(senderKey)?.replyAddresses.orEmpty()
                        if (!text.isNullOrBlank() && to.isNotEmpty()) SmsSender.send(context, to, text)
                    }
                }
                Notifier.cancel(context, senderKey)
                inbox.markRead(senderKey)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val COPY = "io.github.jaswantk917.chitthi.COPY"
        const val MARK_READ = "io.github.jaswantk917.chitthi.MARK_READ"
        const val REPLY = "io.github.jaswantk917.chitthi.REPLY"
        const val EXTRA_CODE = "code"
    }
}
