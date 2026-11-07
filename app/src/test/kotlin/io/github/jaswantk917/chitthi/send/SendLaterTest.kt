package io.github.jaswantk917.chitthi.send

import io.github.jaswantk917.chitthi.ui.util.laterLabel
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class SendLaterTest {
    private val day = LocalDate.of(2026, 11, 7)
    private fun at(hour: Int, minute: Int = 0) = day.atTime(hour, minute)
    private fun times(now: LocalDateTime) = sendLaterPresets(now).map { it.label to it.at }

    @Test
    fun `mid morning offers this evening, tonight and tomorrow morning`() {
        assertEquals(
            listOf("This evening" to at(18), "Tonight" to at(21), "Tomorrow morning" to day.plusDays(1).atTime(8, 0)),
            times(at(10)),
        )
    }

    @Test
    fun `a preset under half an hour away is skipped`() {
        assertEquals(
            listOf("Tonight" to at(21), "Tomorrow morning" to day.plusDays(1).atTime(8, 0), "Tomorrow evening" to day.plusDays(1).atTime(18, 0)),
            times(at(17, 40)),
        )
    }

    @Test
    fun `late at night only tomorrow is left`() {
        assertEquals(
            listOf("Tomorrow morning" to day.plusDays(1).atTime(8, 0), "Tomorrow evening" to day.plusDays(1).atTime(18, 0)),
            times(at(22, 30)),
        )
    }

    @Test
    fun `after midnight the coming morning is this morning, not tomorrow`() {
        assertEquals(
            listOf("This morning" to at(8), "This evening" to at(18), "Tonight" to at(21)),
            times(at(1, 15)),
        )
    }

    @Test
    fun `once awake, morning means tomorrow`() {
        assertEquals("Tomorrow morning", sendLaterPresets(at(7, 45)).last().label)
    }

    @Test
    fun `labels say today, tomorrow, then the date`() {
        val zone = ZoneId.systemDefault()
        fun millis(t: LocalDateTime) = t.atZone(zone).toInstant().toEpochMilli()
        val now = millis(at(10))
        assertEquals("today, 6:00 pm", laterLabel(millis(at(18)), now))
        assertEquals("tomorrow, 8:00 am", laterLabel(millis(day.plusDays(1).atTime(8, 0)), now))
        assertEquals("Tue 10 Nov, 9:30 am", laterLabel(millis(day.plusDays(3).atTime(9, 30)), now))
    }
}
