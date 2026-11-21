package com.duchamp.design

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Instrument-panel palette. Dark-first: the device has an AMOLED display. One signal accent
 * ([Accent]) is used only for the active mode and the primary CTA; status colors are for data
 * only, never decoration.
 */
object DuchampColor {
    val Background = Color(0xFF0A0A0B)
    val Surface = Color(0xFF131315)
    val SurfaceRaised = Color(0xFF1B1B1E)
    val Hairline = Color(0xFF2A2A2F)
    val TextPrimary = Color(0xFFEDEDEF)
    val TextSecondary = Color(0xFF8B8B93)
    val TextTertiary = Color(0xFF5A5A62)
    val Accent = Color(0xFFFF5B1F)
    val Ok = Color(0xFF7BD88F)
    val Warn = Color(0xFFF2C14E)
    val Hot = Color(0xFFFF4D3D)
}

val DuchampDarkColors = darkColorScheme(
    primary = DuchampColor.Accent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1505),
    onPrimaryContainer = Color(0xFFFFD2BF),
    secondary = Color(0xFF8B8B93),
    onSecondary = Color.White,
    tertiary = DuchampColor.Ok,
    onTertiary = Color(0xFF0A1A0E),
    background = DuchampColor.Background,
    onBackground = DuchampColor.TextPrimary,
    surface = DuchampColor.Surface,
    onSurface = DuchampColor.TextPrimary,
    surfaceVariant = DuchampColor.SurfaceRaised,
    onSurfaceVariant = DuchampColor.TextSecondary,
    outline = DuchampColor.Hairline,
    outlineVariant = DuchampColor.Hairline,
    error = DuchampColor.Hot,
    onError = Color.White,
    errorContainer = Color(0xFF3A150F),
    onErrorContainer = Color(0xFFFFD0C9),
)

val DuchampLightColors = lightColorScheme(
    primary = Color(0xFFB33A12),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9C8),
    onPrimaryContainer = Color(0xFF3A0D00),
    secondary = Color(0xFF56565F),
    onSecondary = Color.White,
    tertiary = Color(0xFF3A6B45),
    onTertiary = Color.White,
    background = Color(0xFFF7F7F8),
    onBackground = Color(0xFF17171A),
    surface = Color(0xFFFBFBFC),
    onSurface = Color(0xFF17171A),
    surfaceVariant = Color(0xFFE8E8EC),
    onSurfaceVariant = Color(0xFF46464E),
    outline = Color(0xFFC9C9CF),
    outlineVariant = Color(0xFFC9C9CF),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)
