package io.github.jaswantk917.chitthi.work

import android.content.Context
import android.net.Uri
import android.provider.Telephony.Sms
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.jaswantk917.chitthi.ChitthiApp
import io.github.jaswantk917.chitthi.classify.Otp
import io.github.jaswantk917.chitthi.classify.SenderId
import io.github.jaswantk917.chitthi.data.SmsStore
import java.util.concurrent.TimeUnit

/** Deletes one-time-code messages from businesses once they're older than the chosen age. */
class OtpSweepWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val minutes = ChitthiApp.instance.prefs.settings.value.otpDeleteAfterMinutes
        if (minutes > 0 && SmsStore.isDefault(applicationContext)) {
            expired(applicationContext, minutes).forEach {
                applicationContext.contentResolver.delete(Uri.withAppendedPath(Sms.CONTENT_URI, it.toString()), null, null)
            }
        }
        return Result.success()
    }

    companion object {
        private const val NAME = "otp_sweep"
        private const val EVERY_MINUTES = 15L

        /** ids of received code messages older than [minutes], from senders that aren't people */
        fun expired(context: Context, minutes: Int): List<Long> {
            val cutoff = System.currentTimeMillis() - minutes * 60_000L
            return context.contentResolver.query(
                Sms.CONTENT_URI,
                arrayOf(Sms._ID, Sms.ADDRESS, Sms.BODY),
                "${Sms.TYPE} = ${Sms.MESSAGE_TYPE_INBOX} AND ${Sms.DATE} < ?",
                arrayOf(cutoff.toString()), null,
            )?.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        val sender = SenderId.parse(c.getString(1).orEmpty())
                        if (sender is SenderId.Phone) continue
                        if (Otp.extract(c.getString(2).orEmpty()) != null) add(c.getLong(0))
                    }
                }
            }.orEmpty()
        }

        fun schedule(context: Context, minutes: Int) {
            val work = WorkManager.getInstance(context)
            if (minutes <= 0) {
                work.cancelUniqueWork(NAME)
                return
            }
            work.enqueueUniquePeriodicWork(
                NAME, ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<OtpSweepWorker>(EVERY_MINUTES, TimeUnit.MINUTES).build(),
            )
        }
    }
}
