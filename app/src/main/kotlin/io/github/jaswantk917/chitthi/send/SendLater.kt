package io.github.jaswantk917.chitthi.send

import java.time.LocalDateTime

enum class SendLaterKind { Morning, Evening, Night }

data class SendLaterPreset(val label: String, val kind: SendLaterKind, val at: LocalDateTime)

/** a preset closer than this would barely be "later" */
private const val MIN_LEAD_MINUTES = 30L
/** before this hour, "tomorrow" in people's heads is still today */
private const val NIGHT_OWL_HOUR = 5

/** The quick "send later" choices for [now]: the next morning, evening and night that are far enough away. */
fun sendLaterPresets(now: LocalDateTime): List<SendLaterPreset> {
    val lead = now.plusMinutes(MIN_LEAD_MINUTES)
    val today = now.toLocalDate()
    val morningDay = if (now.hour < NIGHT_OWL_HOUR) today else today.plusDays(1)
    val candidates = listOf(
        SendLaterPreset(if (morningDay == today) "This morning" else "Tomorrow morning", SendLaterKind.Morning, morningDay.atTime(8, 0)),
        SendLaterPreset("This evening", SendLaterKind.Evening, today.atTime(18, 0)),
        SendLaterPreset("Tonight", SendLaterKind.Night, today.atTime(21, 0)),
        SendLaterPreset("Tomorrow evening", SendLaterKind.Evening, today.plusDays(1).atTime(18, 0)),
    )
    return candidates.filter { it.at > lead }.sortedBy { it.at }.take(3)
}
