package io.github.jaswantk917.chitthi.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// generated from the icon's violet (0xFF6D4AFF) with Material's Vibrant scheme, 2025 spec
private val chitthiLight = lightColorScheme(
    primary = Color(0xFF5B3ADE),
    onPrimary = Color(0xFFF6F0FF),
    primaryContainer = Color(0xFFA292FF),
    onPrimaryContainer = Color(0xFF220076),
    inversePrimary = Color(0xFF927DFF),
    secondary = Color(0xFF6B46AE),
    onSecondary = Color(0xFFF9EFFF),
    secondaryContainer = Color(0xFFDDC8FF),
    onSecondaryContainer = Color(0xFF563098),
    tertiary = Color(0xFF9B3667),
    onTertiary = Color(0xFFFFEFF2),
    tertiaryContainer = Color(0xFFFF8CBE),
    onTertiaryContainer = Color(0xFF63033B),
    error = Color(0xFFB41340),
    onError = Color(0xFFFFEFEF),
    errorContainer = Color(0xFFF74B6D),
    onErrorContainer = Color(0xFF510017),
    background = Color(0xFFFCF4FF),
    onBackground = Color(0xFF35284E),
    surface = Color(0xFFFCF4FF),
    onSurface = Color(0xFF35284E),
    surfaceVariant = Color(0xFFE6D6FF),
    onSurfaceVariant = Color(0xFF63547E),
    outline = Color(0xFF7F6F9B),
    outlineVariant = Color(0xFFB7A5D4),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF14062C),
    inverseOnSurface = Color(0xFFA695C3),
    surfaceDim = Color(0xFFDFCCFF),
    surfaceBright = Color(0xFFFCF4FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7EDFF),
    surfaceContainer = Color(0xFFF0E3FF),
    surfaceContainerHigh = Color(0xFFEBDCFF),
    surfaceContainerHighest = Color(0xFFE6D6FF),
    surfaceTint = Color(0xFF5B3ADE),
)

private val chitthiDark = darkColorScheme(
    primary = Color(0xFFB0A2FF),
    onPrimary = Color(0xFF2D0096),
    primaryContainer = Color(0xFFA292FF),
    onPrimaryContainer = Color(0xFF220076),
    inversePrimary = Color(0xFF5F3FE2),
    secondary = Color(0xFFB38CF9),
    onSecondary = Color(0xFF2F006A),
    secondaryContainer = Color(0xFF562F97),
    onSecondaryContainer = Color(0xFFDCC7FF),
    tertiary = Color(0xFFFF9AC4),
    onTertiary = Color(0xFF6B0C41),
    tertiaryContainer = Color(0xFFFC84B9),
    onTertiaryContainer = Color(0xFF5C0036),
    error = Color(0xFFFF6E84),
    onError = Color(0xFF490013),
    errorContainer = Color(0xFFA70138),
    onErrorContainer = Color(0xFFFFB2B9),
    background = Color(0xFF14062C),
    onBackground = Color(0xFFEEE0FF),
    surface = Color(0xFF14062C),
    onSurface = Color(0xFFEEE0FF),
    surfaceVariant = Color(0xFF2E1A50),
    onSurfaceVariant = Color(0xFFB5A3D2),
    outline = Color(0xFF7E6E99),
    outlineVariant = Color(0xFF4F4169),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFFEF7FF),
    inverseOnSurface = Color(0xFF5D4E77),
    surfaceDim = Color(0xFF14062C),
    surfaceBright = Color(0xFF35205A),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF190934),
    surfaceContainer = Color(0xFF200F3E),
    surfaceContainerHigh = Color(0xFF271547),
    surfaceContainerHighest = Color(0xFF2E1A50),
    surfaceTint = Color(0xFFB0A2FF),
)

// Expressive leans on big, soft corners; the "increased" steps are what cards and sheets use
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
    largeIncreased = RoundedCornerShape(28.dp),
    extraLargeIncreased = RoundedCornerShape(36.dp),
    extraExtraLarge = RoundedCornerShape(48.dp),
)

enum class Palette { Chitthi, Wallpaper }

private fun ColorScheme.toAmoled() = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0B0D),
    surfaceContainer = Color(0xFF121215),
    surfaceContainerHigh = Color(0xFF1A1A1E),
    surfaceContainerHighest = Color(0xFF232328),
)

@Composable
fun ChitthiTheme(palette: Palette = Palette.Chitthi, amoled: Boolean = false, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val base = when {
        palette == Palette.Chitthi || Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> if (dark) chitthiDark else chitthiLight
        dark -> dynamicDarkColorScheme(context)
        else -> dynamicLightColorScheme(context)
    }
    // pure black only means something in the dark
    val colorScheme = if (dark && amoled) base.toAmoled() else base
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val controller = WindowCompat.getInsetsController((view.context as Activity).window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = AppShapes,
        typography = AppTypography,
        content = content,
    )
}
