package io.github.jaswantk917.chitthi.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import io.github.jaswantk917.chitthi.MainActivity
import io.github.jaswantk917.chitthi.R
import io.github.jaswantk917.chitthi.model.Category
import io.github.jaswantk917.chitthi.model.Message
import io.github.jaswantk917.chitthi.model.SenderInfo
import io.github.jaswantk917.chitthi.model.Verdict
import io.github.jaswantk917.chitthi.ui.components.logoUrl

object Notifier {
    const val CH_OTP = "otp"
    private const val CH_PERSONAL = "personal"
    private const val CH_IMPORTANT = "important"
    private const val CH_UNKNOWN = "unknown"
    private const val CH_PROMOS = "promos"
    private const val CH_FAILED = "failed"
    const val EXTRA_SENDER = "sender_key"
    const val REPLY_KEY = "reply"
    private const val OTP_TIMEOUT_MS = 10 * 60_000L
    private const val HISTORY = 5

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CH_OTP, "One-time codes", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "Codes, with a copy button. They vanish after 10 minutes." },
                NotificationChannel(CH_PERSONAL, "Personal", NotificationManager.IMPORTANCE_HIGH),
                NotificationChannel(CH_IMPORTANT, "Important", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Banks, bills, deliveries, bookings" },
                NotificationChannel(CH_UNKNOWN, "Unknown numbers", NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(CH_PROMOS, "Promotions", NotificationManager.IMPORTANCE_MIN),
                NotificationChannel(CH_FAILED, "Not sent", NotificationManager.IMPORTANCE_HIGH),
            )
        )
    }

    private fun id(senderKey: String) = senderKey.hashCode()

    private fun canPost(context: Context) =
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openIntent(context: Context, senderKey: String): PendingIntent = PendingIntent.getActivity(
        context, id(senderKey),
        Intent(context, MainActivity::class.java).putExtra(EXTRA_SENDER, senderKey).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun action(context: Context, action: String, senderKey: String, code: String? = null, mutable: Boolean = false) =
        PendingIntent.getBroadcast(
            context, (action + senderKey).hashCode(),
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(action)
                .putExtra(EXTRA_SENDER, senderKey)
                .putExtra(NotificationActionReceiver.EXTRA_CODE, code),
            (if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE) or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    suspend fun incoming(
        context: Context,
        message: Message,
        verdict: Verdict,
        sender: SenderInfo,
        recent: List<Message>,
        promoNotifications: Boolean,
    ) {
        if (!canPost(context)) return
        if (verdict.category == Category.Promotions && verdict.otp == null && !promoNotifications) return
        val avatar = avatar(context, sender)
        val otp = verdict.otp
        val channel = when {
            otp != null -> CH_OTP
            verdict.category == Category.Personal -> CH_PERSONAL
            verdict.category == Category.Important -> CH_IMPORTANT
            verdict.category == Category.Unknown -> CH_UNKNOWN
            else -> CH_PROMOS
        }
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openIntent(context, sender.key))
            .setAutoCancel(true)
            .setWhen(message.date)
            .setShowWhen(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setLargeIcon(avatar)

        if (otp != null) {
            builder
                .setContentTitle("$otp · ${sender.title}")
                .setContentText(message.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message.body))
                .setTimeoutAfter(OTP_TIMEOUT_MS)
                .addAction(0, "Copy $otp", action(context, NotificationActionReceiver.COPY, sender.key, otp))
                .addAction(0, "Mark read", action(context, NotificationActionReceiver.MARK_READ, sender.key))
        } else {
            val them = Person.Builder()
                .setName(sender.title)
                .setKey(sender.key)
                .apply { avatar?.let { setIcon(IconCompat.createWithBitmap(it)) } }
                .build()
            val me = Person.Builder().setName("You").build()
            val style = NotificationCompat.MessagingStyle(me)
            recent.takeLast(HISTORY).forEach { style.addMessage(it.body, it.date, if (it.incoming) them else me) }
            builder.setStyle(style)
            if (sender.canReply) {
                val input = RemoteInput.Builder(REPLY_KEY).setLabel("Reply").build()
                builder.addAction(
                    NotificationCompat.Action.Builder(0, "Reply", action(context, NotificationActionReceiver.REPLY, sender.key, mutable = true))
                        .addRemoteInput(input)
                        .setAllowGeneratedReplies(true)
                        .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                        .build()
                )
            }
            builder.addAction(
                NotificationCompat.Action.Builder(0, "Mark read", action(context, NotificationActionReceiver.MARK_READ, sender.key))
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                    .setShowsUserInterface(false)
                    .build()
            )
        }
        NotificationManagerCompat.from(context).notify(id(sender.key), builder.build())
    }

    fun notSent(context: Context, address: String) {
        if (!canPost(context)) return
        val notification = NotificationCompat.Builder(context, CH_FAILED)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Message not sent")
            .setContentText("To $address. Open Chitthi to try again.")
            .setContentIntent(openIntent(context, "p:" + address.filter(Char::isDigit).takeLast(10)))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(("failed$address").hashCode(), notification)
    }

    fun cancel(context: Context, senderKey: String) {
        NotificationManagerCompat.from(context).cancel(id(senderKey))
    }

    private suspend fun avatar(context: Context, sender: SenderInfo): Bitmap? {
        val data: Any = sender.photo ?: sender.logoDomain?.let(::logoUrl) ?: return null
        val result = context.imageLoader.execute(
            ImageRequest.Builder(context)
                .data(data)
                .size(AVATAR_PX)
                .allowHardware(false)
                .transformations(CircleCropTransformation())
                .build()
        )
        return (result.drawable as? BitmapDrawable)?.bitmap
    }

    private const val AVATAR_PX = 192
}
