package com.cit.attendance.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Centralised motion language (Section 41).
 *
 * Every animation in the product picks a duration and easing from here so that
 * timing feels consistent. [CitMotion.reduceMotion] is honoured by the shared
 * components and by the butterfly, satisfying the reduced-motion requirement in
 * Section 39.
 */
@Immutable
data class CitMotion(
    /** Press / selection feedback - must feel immediate. */
    val instant: Int = 90,
    /** Standard state change: colour, border, elevation. */
    val quick: Int = 180,
    /** Screen-level enter/exit. */
    val standard: Int = 300,
    /** Deliberate, attention-carrying transitions such as success confirmation. */
    val deliberate: Int = 480,
    val standardEasing: Easing = FastOutSlowInEasing,
    /** Slight overshoot used only for selection emphasis. */
    val emphasisEasing: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    val reduceMotion: Boolean = false,
) {
    /** Returns 0 when the user has asked the system to minimise animation. */
    fun duration(value: Int): Int = if (reduceMotion) 0 else value
}

val LocalCitMotion = staticCompositionLocalOf { CitMotion() }
