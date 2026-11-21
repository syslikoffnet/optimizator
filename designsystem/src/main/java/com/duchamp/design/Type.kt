package com.duchamp.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Onest (UI, Cyrillic-capable) and JetBrains Mono (telemetry, tabular figures).
val Onest = FontFamily(
    Font(R.font.onest, FontWeight.Normal),
    Font(R.font.onest, FontWeight.Medium),
    Font(R.font.onest, FontWeight.SemiBold),
    Font(R.font.onest, FontWeight.Bold),
)

val Mono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold),
)

/** Monospace label used for section headers — small caps feel via letter spacing. */
val MonoLabel = TextStyle(
    fontFamily = Mono,
    fontSize = 11.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.06.sp,
    color = DuchampColor.TextTertiary,
)

/** Large telemetry value (main figure on a chart). */
val DisplayValue = TextStyle(
    fontFamily = Mono,
    fontSize = 48.sp,
    fontWeight = FontWeight.Medium,
    fontFeatureSettings = "tnum",
    color = DuchampColor.TextPrimary,
)

val DuchampTypography = Typography(
    displayLarge = TextStyle(fontFamily = Onest, fontWeight = FontWeight.SemiBold, fontSize = 56.sp),
    displayMedium = TextStyle(fontFamily = Onest, fontWeight = FontWeight.SemiBold, fontSize = 40.sp),
    headlineLarge = TextStyle(fontFamily = Onest, fontWeight = FontWeight.SemiBold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = Onest, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    titleLarge = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Medium, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Medium, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    bodySmall = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Normal, fontSize = 13.sp),
    labelLarge = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Onest, fontWeight = FontWeight.Medium, fontSize = 11.sp),
)

/** Monospace text with tabular figures so numbers do not jitter. */
fun monoStyle(
    size: Float = 15f,
    weight: FontWeight = FontWeight.Normal,
    color: Color = DuchampColor.TextPrimary,
) = TextStyle(
    fontFamily = Mono,
    fontSize = size.sp,
    fontWeight = weight,
    fontFeatureSettings = "tnum",
    color = color,
)
