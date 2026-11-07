package io.github.jaswantk917.chitthi.send

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony.Sms
import android.telephony.SmsManager
import io.github.jaswantk917.chitthi.data.SmsStore
import io.github.jaswantk917.chitthi.notify.Notifier
import java.util.concurrent.atomic.AtomicInteger

object SmsSender {
    private val requestCodes = AtomicInteger((System.currentTimeMillis() % Int.MAX_VALUE).toInt())

    /**
     * As the default app Chitthi writes the outbox row itself and flips it to sent or failed;
     * otherwise the system records what it sends.
     */
    fun send(context: Context, addresses: List<String>, text: String) {
        val sms = context.getSystemService(SmsManager::class.java)
        val isDefault = SmsStore.isDefault(context)
        addresses.forEach { address ->
            val parts = sms.divideMessage(text)
            val row = if (isDefault) SmsStore.insertOutgoing(context, address, text) else null
            val sent = ArrayList(parts.indices.map { i ->
                PendingIntent.getBroadcast(
                    context, requestCodes.incrementAndGet(),
                    Intent(context, SmsSentReceiver::class.java)
                        .putExtra(SmsSentReceiver.EXTRA_ROW, row?.toString())
                        .putExtra(SmsSentReceiver.EXTRA_LAST, i == parts.lastIndex)
                        .putExtra(SmsSentReceiver.EXTRA_ADDRESS, address),
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_ONE_SHOT,
                )
            })
            sms.sendMultipartTextMessage(address, null, parts, sent, null)
        }
    }
}

class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val row = intent.getStringExtra(EXTRA_ROW)?.let(Uri::parse)
        if (resultCode == Activity.RESULT_OK) {
            if (row != null && intent.getBooleanExtra(EXTRA_LAST, true)) {
                SmsStore.setType(context, row, Sms.MESSAGE_TYPE_SENT)
            }
        } else {
            row?.let { SmsStore.setType(context, it, Sms.MESSAGE_TYPE_FAILED) }
            Notifier.notSent(context, intent.getStringExtra(EXTRA_ADDRESS).orEmpty())
        }
    }

    companion object {
        const val EXTRA_ROW = "row"
        const val EXTRA_LAST = "last"
        const val EXTRA_ADDRESS = "address"
    }
}
