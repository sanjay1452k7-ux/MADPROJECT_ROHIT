package com.cit.attendance.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for colour (Section 4).
 *
 * The foundation is near-black; the accent runs coral through magenta into
 * violet and is reserved for action, selection, identity and focus. Nothing
 * else in the codebase may declare a literal [Color] - always read from
 * [AppTheme.colors].
 */
@Immutable
data class CitColors(
    // Foundation
    val background: Color,
    val backgroundElevated: Color,
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
    val accentDeep: Color,
    val accentMuted: Color,
    val onAccent: Color,
    /**
     * The brand gradient, coral to violet. Held as an ordered list so every
     * gradient in the app is struck from the same three stops rather than each
     * component inventing its own pair.
     */
    val accentGradient: List<Color>,
    /** Laid under accented surfaces to suggest a glow without a real shadow. */
    val accentGlow: Color,

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
 * The product palette.
 *
 * The canvas is a near-black charcoal that lifts very slightly towards the
 * bottom of a screen; cards sit a step above it. Coral carries urgency,
 * magenta carries identity and violet carries depth - together they are the
 * brand, and no single one of them is used alone for a large fill.
 */
val CitDarkColors = CitColors(
    background = Color(0xFF0D0F14),
    backgroundElevated = Color(0xFF1A1C23),
    surface = Color(0xFF151821),
    surfaceElevated = Color(0xFF1C1F2A),
    surfaceVariant = Color(0xFF232733),
    scrim = Color(0xCC07080B),

    outline = Color(0xFF262A36),
    outlineStrong = Color(0xFF393F51),

    accent = Color(0xFFFF2E8E),
    accentBright = Color(0xFFFF4D70),
    accentDeep = Color(0xFF9B5CFF),
    accentMuted = Color(0x29FF2E8E),
    onAccent = Color(0xFFFFFFFF),
    accentGradient = listOf(
        Color(0xFFFF4D70),
        Color(0xFFFF2E8E),
        Color(0xFF9B5CFF),
    ),
    accentGlow = Color(0x33FF2E8E),

    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA1A1AA),
    textTertiary = Color(0xFF6C7080),

    success = Color(0xFF3DD68C),
    successMuted = Color(0x243DD68C),
    warning = Color(0xFFF5A524),
    warningMuted = Color(0x24F5A524),
    error = Color(0xFFFF4D70),
    errorMuted = Color(0x24FF4D70),
)

val LocalCitColors = staticCompositionLocalOf { CitDarkColors }
