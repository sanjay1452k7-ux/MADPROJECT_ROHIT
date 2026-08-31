package com.cit.attendance.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The brand gradients, struck once.
 *
 * Every accented fill in the app comes from here. Building brushes at the point
 * of use is how a palette drifts: one screen ends up coral-to-magenta and
 * another magenta-to-violet, and the product stops looking like one piece of
 * software. Components ask for the direction they need instead.
 */
object AppGradients {

    /** Buttons, chips, progress fills - anything read left to right. */
    @Composable
    fun horizontal(): Brush = Brush.horizontalGradient(AppTheme.colors.accentGradient)

    /** Rings, vertical bars, and marks that read top to bottom. */
    @Composable
    fun vertical(): Brush = Brush.verticalGradient(AppTheme.colors.accentGradient)

    /** Corner-to-corner, for large plates where a flat axis looks mechanical. */
    @Composable
    fun diagonal(): Brush = Brush.linearGradient(AppTheme.colors.accentGradient)

    /**
     * A soft radial wash. Used behind the QR plate and the brand mark, where
     * the intent is a halo rather than a filled shape.
     */
    @Composable
    fun glow(alpha: Float = 1f): Brush {
        val colors = AppTheme.colors
        return Brush.radialGradient(
            listOf(
                colors.accentGlow.copy(alpha = colors.accentGlow.alpha * alpha),
                Color.Transparent,
            ),
        )
    }

    /**
     * The chart area fill: the accent fading to nothing, so the line stays the
     * thing being read and the fill only gives it weight.
     */
    @Composable
    fun areaFill(): Brush {
        val colors = AppTheme.colors
        return Brush.verticalGradient(
            listOf(colors.accent.copy(alpha = 0.30f), colors.accent.copy(alpha = 0f)),
        )
    }

    /**
     * The page canvas: charcoal lifting very slightly towards the bottom. Too
     * subtle to notice directly, enough to stop a full-screen black reading as
     * flat.
     */
    @Composable
    fun canvas(): Brush {
        val colors = AppTheme.colors
        return Brush.verticalGradient(listOf(colors.background, colors.backgroundElevated))
    }
}
