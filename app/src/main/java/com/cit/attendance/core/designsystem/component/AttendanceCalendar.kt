package com.cit.attendance.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.core.ui.currentLocale
import com.cit.attendance.domain.model.CalendarEntry
import com.cit.attendance.domain.model.DayStatus
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle

/**
 * A month of attendance (Section 28).
 *
 * Every day carries a status *and*, when selected, a written breakdown, so the
 * calendar is never read by colour alone (Section 39). Days with no session are
 * left plain rather than coloured, because "no class" is not "absent".
 */
@Composable
fun AttendanceCalendar(
    entries: List<CalendarEntry>,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing

    val byDate = remember(entries) { entries.associateBy { it.date } }
    val months = remember(entries) {
        entries.map { YearMonth.from(it.date) }.distinct().sorted()
    }

    if (months.isEmpty()) {
        Text(
            text = "No attendance has been recorded yet.",
            style = AppTheme.typography.bodyMedium,
            color = colors.textTertiary,
            modifier = modifier,
        )
        return
    }

    var monthIndex by remember(months) { mutableIntStateOf(months.lastIndex) }
    var selected by remember(entries) { mutableStateOf<CalendarEntry?>(null) }
    val month = months[monthIndex]

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalendarStep(
                label = "Previous month",
                symbol = "<",
                enabled = monthIndex > 0,
                onClick = { monthIndex -= 1 },
            )
            // The locale is read from the composition so the month name
            // re-renders when the device language changes.
            val locale = currentLocale()
            Text(
                text = "${month.month.getDisplayName(TextStyle.FULL, locale)} ${month.year}",
                style = AppTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            CalendarStep(
                label = "Next month",
                symbol = ">",
                enabled = monthIndex < months.lastIndex,
                onClick = { monthIndex += 1 },
            )
        }

        Row(modifier = Modifier.padding(top = spacing.md)) {
            // Monday-first, matching the timetable's day numbering.
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    text = day,
                    style = AppTheme.typography.overline,
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        val firstOfMonth = month.atDay(1)
        // DayOfWeek is 1..7 from Monday, so this is the count of blanks needed.
        val leadingBlanks = firstOfMonth.dayOfWeek.value - 1
        val cells = leadingBlanks + month.lengthOfMonth()
        val weeks = (cells + 6) / 7

        Column(modifier = Modifier.padding(top = spacing.xs)) {
            repeat(weeks) { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { dayOfWeek ->
                        val cellIndex = week * 7 + dayOfWeek
                        val dayNumber = cellIndex - leadingBlanks + 1
                        Box(modifier = Modifier.weight(1f)) {
                            if (dayNumber in 1..month.lengthOfMonth()) {
                                val date = month.atDay(dayNumber)
                                CalendarCell(
                                    date = date,
                                    entry = byDate[date],
                                    isSelected = selected?.date == date,
                                    onClick = { byDate[date]?.let { selected = it } },
                                )
                            }
                        }
                    }
                }
            }
        }

        val current = selected
        if (current != null) {
            AppCard(modifier = Modifier.padding(top = spacing.md)) {
                Text(
                    text = current.date.toString(),
                    style = AppTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = buildString {
                        append("${current.present} present")
                        if (current.late > 0) append(", ${current.late} late")
                        append(", ${current.absent} absent")
                    },
                    style = AppTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = spacing.xxs),
                )
            }
        } else {
            Text(
                text = "Select a day to see its sessions.",
                style = AppTheme.typography.bodyMedium,
                color = colors.textTertiary,
                modifier = Modifier.padding(top = spacing.md),
            )
        }

        CalendarLegend(modifier = Modifier.padding(top = spacing.md))
    }
}

@Composable
private fun CalendarCell(
    date: LocalDate,
    entry: CalendarEntry?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors

    val fill: Color = when (entry?.status) {
        DayStatus.PRESENT -> colors.successMuted
        DayStatus.PARTIAL -> colors.warningMuted
        DayStatus.ABSENT -> colors.errorMuted
        null -> Color.Transparent
    }
    val textColor = when (entry?.status) {
        DayStatus.PRESENT -> colors.success
        DayStatus.PARTIAL -> colors.warning
        DayStatus.ABSENT -> colors.error
        null -> colors.textTertiary
    }

    Box(
        modifier = Modifier
            .padding(2.dp)
            // Square by width, but never below the minimum touch target: on a
            // narrow phone a seventh of the width is smaller than a fingertip.
            .sizeIn(minHeight = AppTheme.spacing.minTouchTarget)
            .aspectRatio(1f)
            .clip(AppTheme.shapes.small)
            .background(fill)
            .then(
                if (isSelected) {
                    Modifier.border(1.5.dp, colors.accentBright, AppTheme.shapes.small)
                } else {
                    Modifier
                },
            )
            .then(
                if (entry != null) {
                    Modifier.appClickable(onClickLabel = date.toString(), onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = AppTheme.typography.bodyMedium,
            color = textColor,
        )
    }
}

@Composable
private fun CalendarLegend(modifier: Modifier = Modifier) {
    val colors = AppTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.lg),
    ) {
        LegendDot("All present", colors.success)
        LegendDot("Partial", colors.warning)
        LegendDot("Absent", colors.error)
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label,
            style = AppTheme.typography.overline,
            color = AppTheme.colors.textTertiary,
            modifier = Modifier.padding(start = AppTheme.spacing.xs),
        )
    }
}

@Composable
private fun CalendarStep(
    label: String,
    symbol: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .size(AppTheme.spacing.minTouchTarget)
            .clip(AppTheme.shapes.small)
            .then(
                if (enabled) {
                    Modifier.appClickable(onClickLabel = label, onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style = AppTheme.typography.titleMedium,
            color = if (enabled) colors.textSecondary else colors.outline,
        )
    }
}
