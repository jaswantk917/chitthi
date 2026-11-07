package io.github.jaswantk917.chitthi.send

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.jaswantk917.chitthi.ChitthiApp
import io.github.jaswantk917.chitthi.model.Scheduled
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Messages written now and sent later, by an exact alarm per message. */
object Scheduler {
    internal const val EXTRA_ID = "scheduled_id"
    private val _changes = MutableStateFlow(0)
    /** ticks whenever the pending list changes, for screens showing it */
    val changes: StateFlow<Int> = _changes

    private val store get() = ChitthiApp.instance.store

    fun schedule(context: Context, senderKey: String, addresses: List<String>, body: String, at: Long) {
        val id = store.addScheduled(senderKey, addresses, body, at)
        arm(context, id, at)
        _changes.update { it + 1 }
    }

    fun reschedule(context: Context, item: Scheduled, at: Long) {
        store.setScheduledTime(item.id, at)
        arm(context, item.id, at)
        _changes.update { it + 1 }
    }

    fun cancel(context: Context, item: Scheduled) {
        store.takeScheduled(item.id)
        alarms(context).cancel(alarm(context, item.id))
        _changes.update { it + 1 }
    }

    fun sendNow(context: Context, item: Scheduled) {
        alarms(context).cancel(alarm(context, item.id))
        fire(context, item.id)
    }

    fun pending(senderKey: String? = null): List<Scheduled> = store.scheduled(senderKey)

    internal fun fire(context: Context, id: Long) {
        val item = store.takeScheduled(id) ?: return
        SmsSender.send(context, item.addresses, item.body)
        _changes.update { it + 1 }
    }

    /** alarms don't survive a reboot; anything whose time passed while the phone was off goes now */
    internal fun rearmAll(context: Context) {
        val now = System.currentTimeMillis()
        store.scheduled().forEach { if (it.at <= now) fire(context, it.id) else arm(context, it.id, it.at) }
    }

    private fun arm(context: Context, id: Long, at: Long) {
        val alarms = alarms(context)
        val intent = alarm(context, id)
        if (alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    private fun alarms(context: Context) = context.getSystemService(AlarmManager::class.java)

    private fun alarm(context: Context, id: Long) = PendingIntent.getBroadcast(
        context, id.toInt(),
        Intent(context, ScheduledSendReceiver::class.java).putExtra(EXTRA_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )
}

class ScheduledSendReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Scheduler.fire(context, intent.getLongExtra(Scheduler.EXTRA_ID, -1))
    }
}

/** boot and app updates */
class ScheduleRearmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Scheduler.rearmAll(context)
}
