package io.github.jaswantk917.chitthi.model

import android.net.Uri

enum class Category(val label: String) {
    Important("Important"),
    Personal("Personal"),
    Promotions("Promos"),
    Unknown("Unknown"),
}

sealed interface Tag {
    data class Otp(val code: String) : Tag
    data class Debit(val amount: String) : Tag
    data class Credit(val amount: String) : Tag
    data object Bill : Tag
    data object Delivery : Tag
    data object Travel : Tag
    data object Alert : Tag
}

data class Verdict(
    val category: Category,
    val tags: List<Tag> = emptyList(),
    /** rules that had to guess; Gemini Nano gets a second look at these */
    val unsure: Boolean = false,
) {
    val otp: String? get() = tags.firstNotNullOfOrNull { (it as? Tag.Otp)?.code }
}

enum class Box { Inbox, Sent, Sending, Failed, Draft }

data class Message(
    /** "s<id>" for SMS, "m<id>" for MMS; ids of the two tables overlap */
    val key: String,
    val id: Long,
    val isMms: Boolean,
    val threadId: Long,
    val address: String,
    val body: String,
    /** millis */
    val date: Long,
    val box: Box,
    val read: Boolean,
    val hasImage: Boolean = false,
) {
    val incoming: Boolean get() = box == Box.Inbox
}

data class MmsPart(val uri: Uri, val contentType: String, val text: String?)

/** How a sender shows up: its name, how to draw it, and whether it takes replies. */
data class SenderInfo(
    val key: String,
    val title: String,
    val subtitle: String?,
    val photo: Uri? = null,
    val logoDomain: String? = null,
    val canReply: Boolean,
    /** what to send replies to */
    val replyAddresses: List<String> = emptyList(),
    val isContact: Boolean = false,
)

data class Conversation(
    val senderKey: String,
    val category: Category,
    val sender: SenderInfo,
    val latest: Message,
    val latestVerdict: Verdict,
    val unread: Int,
    val pinned: Boolean,
)
