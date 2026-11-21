package com.duchamp.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable

/**
 * Top-level theme. Material 3 Expressive with the expressive motion scheme for the "hero"
 * moments (the main button morph). Falls back to the standard M3 color/typography tokens.
 */
@Composable
fun DuchampTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DuchampDarkColors else DuchampLightColors
    MaterialExpressiveTheme(
        colorScheme = colors,
        motionScheme = MotionScheme.expressive(),
        shapes = DuchampShapes,
        typography = DuchampTypography,
        content = content,
    )
}

/** Reference to the expressive motion scheme, for use in custom animations. */
@Composable
fun expressiveMotion(): MotionScheme = MotionScheme.expressive()
