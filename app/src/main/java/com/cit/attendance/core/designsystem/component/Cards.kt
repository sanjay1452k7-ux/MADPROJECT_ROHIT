package com.cit.attendance.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cit.attendance.core.designsystem.theme.AppGradients
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.core.ui.formatPercentage
import com.cit.attendance.core.ui.rememberDateFormatter
import com.cit.attendance.core.ui.rememberTimeFormatter
import com.cit.attendance.domain.model.AttendanceRecord
import com.cit.attendance.domain.model.NotificationItem
import com.cit.attendance.domain.model.NotificationType
import com.cit.attendance.domain.model.ScheduledClass
import com.cit.attendance.domain.model.StudentAtRisk
import com.cit.attendance.domain.model.SubjectAttendance

/** A single headline number with a label (Section 16). */
@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    onClick: (() -> Unit)? = null,
) {
    AppCard(modifier = modifier, onClick = onClick, onClickLabel = label) {
        Text(
            text = label.uppercase(),
            style = AppTheme.typography.overline,
            color = AppTheme.colors.textTertiary,
        )
        Text(
            text = value,
            style = AppTheme.typography.metricSmall,
            color = AppTheme.colors.textPrimary,
            modifier = Modifier.padding(top = AppTheme.spacing.sm),
        )
        if (caption != null) {
            Text(
                text = caption,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(top = AppTheme.spacing.xxs),
            )
        }
    }
}

/**
 * One timetable slot.
 *
 * An open attendance session is called out explicitly, because that is the one
 * piece of state on this row the student needs to act on (Section 29).
 */
@Composable
fun ScheduleRow(
    scheduledClass: ScheduledClass,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val timeFormat = rememberTimeFormatter()

    AppCard(
        modifier = modifier,
        onClick = onClick,
        onClickLabel = "View ${scheduledClass.subject.name}",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(72.dp)) {
                Text(
                    text = scheduledClass.startTime.format(timeFormat),
                    style = AppTheme.typography.label,
                    color = AppTheme.colors.textPrimary,
                )
                Text(
                    text = scheduledClass.endTime.format(timeFormat),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textTertiary,
                )
            }

            Box(
                modifier = Modifier
                    .padding(end = AppTheme.spacing.md)
                    .width(2.dp)
                    .height(36.dp)
                    .clip(AppTheme.shapes.small)
                    .background(
                        if (scheduledClass.hasActiveSession) {
                            AppTheme.colors.accentBright
                        } else {
                            AppTheme.colors.outline
                        },
                    ),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scheduledClass.subject.name,
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${scheduledClass.room} - ${scheduledClass.teacherName}",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (scheduledClass.hasActiveSession) {
                StatusBadge(text = "ACTIVE", tone = BadgeTone.ACCENT)
            }
        }
    }
}

/** Per-subject attendance with a proportional bar (Section 28). */
@Composable
fun SubjectAttendanceCard(
    subjectAttendance: SubjectAttendance,
    belowThreshold: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val percentage = subjectAttendance.percentage

    AppCard(
        modifier = modifier,
        onClick = onClick,
        onClickLabel = subjectAttendance.subject.name,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subjectAttendance.subject.name,
                    style = AppTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${subjectAttendance.subject.code} - " +
                        "${subjectAttendance.attended} of ${subjectAttendance.held} classes",
                    style = AppTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            Text(
                text = formatPercentage(percentage),
                style = AppTheme.typography.metricSmall,
                color = if (belowThreshold) colors.warning else colors.textPrimary,
            )
        }

        ProgressBar(
            fraction = percentage / 100f,
            belowThreshold = belowThreshold,
            modifier = Modifier.padding(top = AppTheme.spacing.md),
        )
    }
}

/** A thin proportional bar. Used wherever a percentage needs a visual anchor. */
@Composable
fun ProgressBar(
    fraction: Float,
    belowThreshold: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(colors.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(CircleShape)
                .background(
                    if (belowThreshold) {
                        SolidColor(colors.warning)
                    } else {
                        AppGradients.horizontal()
                    },
                ),
        )
    }
}

/** One row of attendance history. */
@Composable
fun AttendanceRecordRow(
    record: AttendanceRecord,
    modifier: Modifier = Modifier,
) {
    val timeFormat = rememberTimeFormatter()
    val dateFormat = rememberDateFormatter()

    AppCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(64.dp)) {
                Text(
                    text = record.date.format(dateFormat),
                    style = AppTheme.typography.label,
                    color = AppTheme.colors.textPrimary,
                )
                Text(
                    text = record.markedAt?.format(timeFormat) ?: "--",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textTertiary,
                )
            }
            Text(
                text = record.subject.name,
                style = AppTheme.typography.bodyLarge,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppTheme.spacing.md),
            )
            AttendanceStatusBadge(status = record.status)
        }
    }
}

/** A student flagged as at risk, for teacher and admin surfaces. */
@Composable
fun StudentRiskRow(
    studentAtRisk: StudentAtRisk,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    AppCard(
        modifier = modifier,
        onClick = onClick,
        onClickLabel = studentAtRisk.student.name,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SeededAvatar(
                seed = studentAtRisk.student.avatarSeed,
                initials = studentAtRisk.student.initials,
                size = 40.dp,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppTheme.spacing.md),
            ) {
                Text(
                    text = studentAtRisk.student.name,
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = studentAtRisk.student.registerNumber,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatPercentage(studentAtRisk.percentage),
                    style = AppTheme.typography.label,
                    color = AppTheme.colors.textPrimary,
                )
                StandingBadge(standing = studentAtRisk.standing)
            }
        }
    }
}

/** One notification (Section 32). Unread items carry an accent marker. */
@Composable
fun NotificationCard(
    notification: NotificationItem,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val timeFormat = rememberTimeFormatter()
    val tint = when (notification.type) {
        NotificationType.LOW_ATTENDANCE_WARNING -> colors.warning
        NotificationType.ABSENCE_RECORDED -> colors.error
        NotificationType.ATTENDANCE_MARKED -> colors.success
        NotificationType.ANNOUNCEMENT, NotificationType.SYSTEM -> colors.accentBright
    }

    AppCard(modifier = modifier, onClick = onClick, onClickLabel = notification.title) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppTheme.spacing.md),
            ) {
                Text(
                    text = notification.title,
                    style = AppTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = notification.message,
                    style = AppTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = AppTheme.spacing.xxs),
                )
                Text(
                    text = notification.timestamp.format(timeFormat),
                    style = AppTheme.typography.overline,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(top = AppTheme.spacing.sm),
                )
            }
            if (!notification.isRead) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.accentBright),
                )
            }
        }
    }
}
