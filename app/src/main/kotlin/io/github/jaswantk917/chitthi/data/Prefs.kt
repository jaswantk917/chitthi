package io.github.jaswantk917.chitthi.data

import android.content.Context
import android.os.Build
import io.github.jaswantk917.chitthi.ui.theme.Palette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Settings(
    val otpAutoCopy: Boolean = true,
    /** 0 = never */
    val otpDeleteAfterMinutes: Int = 0,
    val promoNotifications: Boolean = false,
    val useNano: Boolean = true,
    /** progressive blur under the bars; heavy for Mali GPUs, so on by default only on Pixels */
    val blur: Boolean = Build.MANUFACTURER.equals("Google", ignoreCase = true),
    val palette: Palette = Palette.Chitthi,
    val amoled: Boolean = false,
)

class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())
    val settings: StateFlow<Settings> = state

    private fun read() = Settings(
        otpAutoCopy = prefs.getBoolean("otp_auto_copy", true),
        otpDeleteAfterMinutes = prefs.getInt("otp_delete_after", 0),
        promoNotifications = prefs.getBoolean("promo_notifications", false),
        useNano = prefs.getBoolean("use_nano", true),
        blur = prefs.getBoolean("blur", Settings().blur),
        palette = Palette.entries.firstOrNull { it.name == prefs.getString("palette", null) } ?: Palette.Chitthi,
        amoled = prefs.getBoolean("amoled", false),
    )

    fun update(change: (Settings) -> Settings) {
        val next = change(state.value)
        prefs.edit()
            .putBoolean("otp_auto_copy", next.otpAutoCopy)
            .putInt("otp_delete_after", next.otpDeleteAfterMinutes)
            .putBoolean("promo_notifications", next.promoNotifications)
            .putBoolean("use_nano", next.useNano)
            .putBoolean("blur", next.blur)
            .putString("palette", next.palette.name)
            .putBoolean("amoled", next.amoled)
            .apply()
        state.value = next
    }
}
