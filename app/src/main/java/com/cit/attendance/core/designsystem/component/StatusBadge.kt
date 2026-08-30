package com.cit.attendance.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.domain.model.AttendanceStanding
import com.cit.attendance.domain.model.AttendanceStatus

/** The visual tone a badge carries. */
enum class BadgeTone { NEUTRAL, POSITIVE, CAUTION, CRITICAL, ACCENT }

/**
 * A compact status chip.
 *
 * Carries a dot *and* a text label so status is never communicated by colour
 * alone (Section 39).
 */
@Composable
fun StatusBadge(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val (foreground: Color, background: Color) = when (tone) {
        BadgeTone.NEUTRAL -> colors.textSecondary to colors.surfaceVariant
        BadgeTone.POSITIVE -> colors.success to colors.successMuted
        BadgeTone.CAUTION -> colors.warning to colors.warningMuted
        BadgeTone.CRITICAL -> colors.error to colors.errorMuted
        BadgeTone.ACCENT -> colors.accentBright to colors.accentMuted
    }

    Row(
        modifier = modifier
            .clip(AppTheme.shapes.small)
            .background(background)
            .padding(horizontal = AppTheme.spacing.sm, vertical = AppTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(foreground),
        )
        Text(text = text, style = AppTheme.typography.overline, color = foreground)
    }
}

/** Maps an attendance standing onto the badge vocabulary (Section 30). */
@Composable
fun StandingBadge(standing: AttendanceStanding, modifier: Modifier = Modifier) {
    StatusBadge(
        text = standing.label,
        tone = when (standing) {
            AttendanceStanding.GOOD -> BadgeTone.POSITIVE
            AttendanceStanding.WARNING -> BadgeTone.CAUTION
            AttendanceStanding.HIGH_RISK -> BadgeTone.CRITICAL
        },
        modifier = modifier,
    )
}

/** Maps a single record's status onto the badge vocabulary. */
@Composable
fun AttendanceStatusBadge(status: AttendanceStatus, modifier: Modifier = Modifier) {
    StatusBadge(
        text = when (status) {
            AttendanceStatus.PRESENT -> "PRESENT"
            AttendanceStatus.ABSENT -> "ABSENT"
            AttendanceStatus.LATE -> "LATE"
        },
        tone = when (status) {
            AttendanceStatus.PRESENT -> BadgeTone.POSITIVE
            AttendanceStatus.ABSENT -> BadgeTone.CRITICAL
            AttendanceStatus.LATE -> BadgeTone.CAUTION
        },
        modifier = modifier,
    )
}
