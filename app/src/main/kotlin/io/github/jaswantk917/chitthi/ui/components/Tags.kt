package io.github.jaswantk917.chitthi.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.jaswantk917.chitthi.model.Tag
import io.github.jaswantk917.chitthi.notify.Clipboard
import io.github.jaswantk917.chitthi.ui.util.Haptic
import io.github.jaswantk917.chitthi.ui.util.pressScale
import io.github.jaswantk917.chitthi.ui.util.rememberHaptics
import kotlinx.coroutines.delay

/** Money in and out reads at a glance in green and red, in both themes. */
@Composable
fun moneyColors(debit: Boolean): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    return when {
        debit -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        dark -> Color(0xFF1D4B2A) to Color(0xFFB4F1C1)
        else -> Color(0xFFCDF3D6) to Color(0xFF0E3B1C)
    }
}

@Composable
fun TagRow(tags: List<Tag>, modifier: Modifier = Modifier) {
    if (tags.isEmpty()) return
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        tags.take(3).forEach { tag ->
            when (tag) {
                is Tag.Otp -> OtpChip(tag.code)
                is Tag.Debit -> moneyColors(true).let { (bg, fg) -> Pill("−₹${tag.amount}", null, bg, fg) }
                is Tag.Credit -> moneyColors(false).let { (bg, fg) -> Pill("+₹${tag.amount}", null, bg, fg) }
                Tag.Bill -> Pill("Bill", Icons.Rounded.ReceiptLong)
                Tag.Delivery -> Pill("Delivery", Icons.Rounded.LocalShipping)
                Tag.Travel -> Pill("Travel", Icons.Rounded.Flight)
                Tag.Alert -> Pill("Alert", Icons.Rounded.Shield, MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
fun Pill(
    text: String,
    icon: ImageVector?,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        Modifier.clip(CircleShape).background(container).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        icon?.let { Icon(it, null, tint = content, modifier = Modifier.size(13.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
    }
}

/** The code itself, one tap from the clipboard, without opening the message. */
@Composable
fun OtpChip(code: String) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    var copied by remember(code) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) { delay(1600); copied = false }
    }
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .pressScale(interaction, 0.9f)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .clickable(interaction, indication = null) {
                Clipboard.copyOtp(context, code)
                haptics.play(Haptic.Copy)
                copied = true
            }
            .padding(start = 8.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        val tint = MaterialTheme.colorScheme.onTertiaryContainer
        Icon(Icons.Rounded.Password, null, tint = tint, modifier = Modifier.size(13.dp))
        Text(code, style = MaterialTheme.typography.labelLargeEmphasized, color = tint)
        val pop = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
        AnimatedContent(
            copied,
            transitionSpec = { (scaleIn(pop) + fadeIn()) togetherWith (scaleOut() + fadeOut()) },
            label = "copied",
        ) { done ->
            Icon(if (done) Icons.Rounded.Check else Icons.Rounded.ContentCopy, "Copy", tint = tint, modifier = Modifier.size(14.dp))
        }
    }
}
