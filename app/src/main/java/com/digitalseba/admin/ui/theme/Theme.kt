package com.digitalseba.admin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Brand {
    val Violet = Color(0xFF6C3BFF)
    val Blue = Color(0xFF2F6BFF)
    val Sky = Color(0xFF19A7FF)
    val Pink = Color(0xFFEC4899)
    val Amber = Color(0xFFF59E0B)
    val Green = Color(0xFF10B981)
    val Rose = Color(0xFFF43F5E)
    val Purple = Color(0xFF8B5CF6)
    val Teal = Color(0xFF14B8A6)
    val Slate = Color(0xFF64748B)
    val WhatsApp = Color(0xFF25D366)
    val WhatsAppDark = Color(0xFF128C7E)
}

val BrandGradient: Brush = Brush.linearGradient(listOf(Color(0xFF7B3BFF), Color(0xFF4F5BFF), Color(0xFF19A7FF)))

/** Colorful gradient pairs used by metric tiles and accents. */
enum class Tone(val start: Color, val end: Color) {
    Blue(Color(0xFF4F8BFF), Color(0xFF2F5BEA)),
    Purple(Color(0xFFA05CFF), Color(0xFF6A3BEA)),
    Amber(Color(0xFFFFB547), Color(0xFFF5821F)),
    Green(Color(0xFF34D399), Color(0xFF0EA371)),
    Pink(Color(0xFFFF6FA8), Color(0xFFE8457E)),
    Teal(Color(0xFF2DD4BF), Color(0xFF0E9F9A)),
    Red(Color(0xFFFF7A7A), Color(0xFFE5484D)),
    Indigo(Color(0xFF7C83FF), Color(0xFF4F46E5));

    val brush: Brush get() = Brush.linearGradient(listOf(start, end))
    val solid: Color get() = end
}

fun statusColor(status: String): Color = when (status.lowercase()) {
    "pending" -> Brand.Amber
    "processing" -> Brand.Blue
    "completed" -> Brand.Green
    "on-hold" -> Brand.Purple
    "cancelled", "failed", "refunded" -> Brand.Rose
    else -> Brand.Slate
}

fun statusTone(status: String): Tone = when (status.lowercase()) {
    "pending" -> Tone.Amber
    "processing" -> Tone.Blue
    "completed" -> Tone.Green
    "on-hold" -> Tone.Purple
    "cancelled", "failed", "refunded" -> Tone.Red
    else -> Tone.Indigo
}

fun statusLabel(status: String): String = when (status.lowercase()) {
    "pending" -> "Pending"
    "processing" -> "Processing"
    "completed" -> "Completed"
    "on-hold" -> "On Hold"
    "cancelled" -> "Cancelled"
    "refunded" -> "Refunded"
    "failed" -> "Failed"
    "all" -> "All"
    "" -> "Unknown"
    else -> status.replaceFirstChar { it.uppercase() }
}

fun statusBangla(status: String): String = when (status.lowercase()) {
    "pending" -> "অপেক্ষমাণ"
    "processing" -> "প্রসেসিং"
    "completed" -> "সম্পন্ন"
    "on-hold" -> "হোল্ডে"
    "cancelled" -> "বাতিল"
    "refunded" -> "রিফান্ড"
    "failed" -> "ব্যর্থ"
    else -> ""
}

fun classColor(cls: String): Color = when (cls) {
    "danger" -> Brand.Rose
    "warning" -> Brand.Amber
    "success" -> Brand.Green
    "info" -> Brand.Blue
    "purple" -> Brand.Purple
    else -> Brand.Slate
}

data class ExtraColors(val cardBorder: Color, val subtle: Color, val muted: Color)

val LocalExtra = staticCompositionLocalOf { ExtraColors(Color(0xFFE6E9F5), Color(0xFFF1F3FB), Color(0xFF6B7290)) }

private val LightColors = lightColorScheme(
    primary = Brand.Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE7FF),
    onPrimaryContainer = Color(0xFF2A0F80),
    secondary = Brand.Blue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3ECFF),
    onSecondaryContainer = Color(0xFF0B2A73),
    tertiary = Brand.Pink,
    background = Color(0xFFF4F6FD),
    onBackground = Color(0xFF141A33),
    surface = Color.White,
    onSurface = Color(0xFF141A33),
    surfaceVariant = Color(0xFFEFF1FA),
    onSurfaceVariant = Color(0xFF5B6280),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF7F8FD),
    surfaceContainerLow = Color.White,
    outline = Color(0xFFD9DDEC),
    outlineVariant = Color(0xFFE6E9F5),
    error = Brand.Rose,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9B7BFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF34237A),
    onPrimaryContainer = Color(0xFFE6DEFF),
    secondary = Color(0xFF6F98FF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1C2D5C),
    onSecondaryContainer = Color(0xFFDCE5FF),
    tertiary = Color(0xFFFF7EB6),
    background = Color(0xFF0D1120),
    onBackground = Color(0xFFE8EAF6),
    surface = Color(0xFF161B2E),
    onSurface = Color(0xFFE8EAF6),
    surfaceVariant = Color(0xFF1E2439),
    onSurfaceVariant = Color(0xFFA3A9C6),
    surfaceContainer = Color(0xFF161B2E),
    surfaceContainerHigh = Color(0xFF1C2236),
    surfaceContainerLow = Color(0xFF141929),
    outline = Color(0xFF2E3550),
    outlineVariant = Color(0xFF262C44),
    error = Color(0xFFFF6B81),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 21.sp),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.Bold),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        bodyMedium = t.bodyMedium.copy(lineHeight = 20.sp),
    )
}

val NumberStyle = TextStyle(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp)

@Composable
fun DSTheme(mode: String, content: @Composable () -> Unit) {
    val dark = when (mode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val extra = if (dark) ExtraColors(Color(0xFF262C44), Color(0xFF1C2236), Color(0xFF8C93B3))
    else ExtraColors(Color(0xFFE6E9F5), Color(0xFFF1F3FB), Color(0xFF6B7290))
    CompositionLocalProvider(LocalExtra provides extra) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = AppTypography, content = content)
    }
}
