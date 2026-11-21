package com.duchamp.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 8 dp grid. Radii: 24 (large surfaces) / 16 (cards) / 10 (chips). Sharp, Rams-like surfaces
 * with hairline borders instead of shadows.
 */
val CardShape = RoundedCornerShape(16.dp)
val SurfaceShape = RoundedCornerShape(24.dp)
val ChipShape = RoundedCornerShape(10.dp)

val DuchampShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = ChipShape,
    medium = CardShape,
    large = SurfaceShape,
    extraLarge = RoundedCornerShape(32.dp),
    largeIncreased = RoundedCornerShape(28.dp),
    extraLargeIncreased = RoundedCornerShape(36.dp),
    extraExtraLarge = RoundedCornerShape(44.dp),
)
