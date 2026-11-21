package com.duchamp.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Top-level theme. Material 3 with the app's color/typography/shape tokens.
 * (The Material 3 Expressive API is internal in material3 1.5.0, so we use the
 * standard MaterialTheme here; the visual tokens stay the same.)
 */
@Composable
fun DuchampTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DuchampDarkColors else DuchampLightColors
    MaterialTheme(
        colorScheme = colors,
        shapes = DuchampShapes,
        typography = DuchampTypography,
        content = content,
    )
}
