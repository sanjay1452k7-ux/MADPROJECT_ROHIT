package com.cit.attendance.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Locale-aware formatting for composables.
 *
 * Reading `Locale.getDefault()` inside composition is a correctness bug: the
 * value is captured once and the interface keeps showing the old locale after
 * the user changes it. Everything here reads the locale from the composition
 * instead, so a locale change recomposes and reformats.
 */
@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** The product's single percentage format, e.g. "87.4%". */
@Composable
fun formatPercentage(value: Float): String {
    val locale = currentLocale()
    return remember(locale, value) { String.format(locale, "%.1f%%", value) }
}

/** Wall-clock time, e.g. "10:17 AM". */
@Composable
fun rememberTimeFormatter(): DateTimeFormatter {
    val locale = currentLocale()
    return remember(locale) { DateTimeFormatter.ofPattern("h:mm a", locale) }
}

/** Short date, e.g. "30 Aug". */
@Composable
fun rememberDateFormatter(): DateTimeFormatter {
    val locale = currentLocale()
    return remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }
}
