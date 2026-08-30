package com.cit.attendance.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale (Section 42). A 4dp base keeps rhythm consistent. */
@Immutable
data class CitSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 12.dp,
    val lg: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 48.dp,
    /** Horizontal page gutter, shared by every screen. */
    val screenGutter: Dp = 20.dp,
    /** Minimum accessible touch target (Section 39). */
    val minTouchTarget: Dp = 48.dp,
)

/** Corner radii - controlled, never pill-shaped for containers (Section 5). */
@Immutable
data class CitShapes(
    val small: RoundedCornerShape = RoundedCornerShape(8.dp),
    val medium: RoundedCornerShape = RoundedCornerShape(12.dp),
    val large: RoundedCornerShape = RoundedCornerShape(16.dp),
    val extraLarge: RoundedCornerShape = RoundedCornerShape(22.dp),
)

/**
 * Elevation is expressed with borders and subtle tonal shifts rather than heavy
 * shadows, which read poorly on a near-black canvas.
 */
@Immutable
data class CitElevation(
    val none: Dp = 0.dp,
    val raised: Dp = 2.dp,
    val overlay: Dp = 8.dp,
)

val LocalCitSpacing = staticCompositionLocalOf { CitSpacing() }
val LocalCitShapes = staticCompositionLocalOf { CitShapes() }
val LocalCitElevation = staticCompositionLocalOf { CitElevation() }
