package com.cit.attendance.core.designsystem.theme

import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * The application theme (Section 42).
 *
 * The product is deliberately dark-only: the black foundation is part of the
 * brand, so there is no light scheme to fall back to. Material 3 is still fed a
 * matching scheme so that any stock M3 component we use inherits the palette
 * instead of reverting to Material purple defaults.
 */
@Composable
fun CitTheme(content: @Composable () -> Unit) {
    val colors = CitDarkColors
    val context = LocalContext.current

    // Respect the system "remove animations" accessibility setting.
    val reduceMotion = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }

    val materialScheme = darkColorScheme(
        primary = colors.accentBright,
        onPrimary = colors.onAccent,
        primaryContainer = colors.accent,
        onPrimaryContainer = colors.onAccent,
        background = colors.background,
        onBackground = colors.textPrimary,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        surfaceVariant = colors.surfaceVariant,
        onSurfaceVariant = colors.textSecondary,
        outline = colors.outline,
        error = colors.error,
        onError = colors.onAccent,
        scrim = colors.scrim,
    )

    CompositionLocalProvider(
        LocalCitColors provides colors,
        LocalCitTypography provides CitTypographyDefaults,
        LocalCitSpacing provides CitSpacing(),
        LocalCitShapes provides CitShapes(),
        LocalCitElevation provides CitElevation(),
        LocalCitMotion provides CitMotion(reduceMotion = reduceMotion),
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = MaterialTheme.typography.copy(
                bodyLarge = CitTypographyDefaults.bodyLarge,
                bodyMedium = CitTypographyDefaults.bodyMedium,
                labelLarge = CitTypographyDefaults.label,
            ),
            content = content,
        )
    }
}

/**
 * Token accessor. Feature code reads `AppTheme.colors` / `AppTheme.typography`
 * and never hardcodes a value.
 */
object AppTheme {
    val colors: CitColors
        @Composable get() = LocalCitColors.current

    val typography: CitTypography
        @Composable get() = LocalCitTypography.current

    val spacing: CitSpacing
        @Composable get() = LocalCitSpacing.current

    val shapes: CitShapes
        @Composable get() = LocalCitShapes.current

    val elevation: CitElevation
        @Composable get() = LocalCitElevation.current

    val motion: CitMotion
        @Composable get() = LocalCitMotion.current
}
