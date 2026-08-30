package com.cit.attendance.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for colour (Section 4).
 *
 * The foundation is near-black; purple is an accent reserved for action,
 * selection, identity and focus. Nothing else in the codebase may declare a
 * literal [Color] - always read from [AppTheme.colors].
 */
@Immutable
data class CitColors(
    // Foundation
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceVariant: Color,
    val scrim: Color,

    // Borders / dividers
    val outline: Color,
    val outlineStrong: Color,

    // Accent
    val accent: Color,
    val accentBright: Color,
    val accentMuted: Color,
    val onAccent: Color,

    // Text
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,

    // Semantic status
    val success: Color,
    val successMuted: Color,
    val warning: Color,
    val warningMuted: Color,
    val error: Color,
    val errorMuted: Color,
)

/**
 * The product palette. Deep purple carries identity, electric purple carries
 * interaction; every status colour is desaturated enough to sit calmly on black.
 */
val CitDarkColors = CitColors(
    background = Color(0xFF07070A),
    surface = Color(0xFF0E0E14),
    surfaceElevated = Color(0xFF15151E),
    surfaceVariant = Color(0xFF1C1C27),
    scrim = Color(0xCC040406),

    outline = Color(0xFF262633),
    outlineStrong = Color(0xFF3A3A4D),

    accent = Color(0xFF7C3AED),
    accentBright = Color(0xFFB57BFF),
    accentMuted = Color(0x297C3AED),
    onAccent = Color(0xFFFFFFFF),

    textPrimary = Color(0xFFF4F4F7),
    textSecondary = Color(0xFFA0A0B4),
    textTertiary = Color(0xFF6C6C84),

    success = Color(0xFF3DD68C),
    successMuted = Color(0x243DD68C),
    warning = Color(0xFFF5A524),
    warningMuted = Color(0x24F5A524),
    error = Color(0xFFF04438),
    errorMuted = Color(0x24F04438),
)

val LocalCitColors = staticCompositionLocalOf { CitDarkColors }
