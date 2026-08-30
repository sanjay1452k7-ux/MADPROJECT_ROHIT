package com.cit.attendance.feature.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cit.attendance.core.designsystem.component.AppCard
import com.cit.attendance.core.designsystem.component.AppTopBar
import com.cit.attendance.core.designsystem.component.AttendanceRecordRow
import com.cit.attendance.core.designsystem.component.AttendanceRing
import com.cit.attendance.core.designsystem.component.ErrorState
import com.cit.attendance.core.designsystem.component.LoadingCard
import com.cit.attendance.core.designsystem.component.ScheduleRow
import com.cit.attendance.core.designsystem.component.SectionHeader
import com.cit.attendance.core.designsystem.component.StandingBadge
import com.cit.attendance.core.designsystem.component.SubjectAttendanceCard
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.core.ui.UiState
import com.cit.attendance.domain.model.AttendancePolicy
import com.cit.attendance.domain.model.AttendanceStanding
import com.cit.attendance.domain.model.StudentDashboard
import com.cit.attendance.domain.model.SubjectAttendance
import java.time.LocalTime

/** Student home (Section 14). */
@Composable
fun StudentHomeScreen(
    modifier: Modifier = Modifier,
    viewModel: StudentHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = greeting(), subtitle = "Attendance overview")

        when (val current = state) {
            UiState.Loading -> Column(
                modifier = Modifier.padding(horizontal = AppTheme.spacing.screenGutter),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                LoadingCard(lines = 4)
                LoadingCard(lines = 2)
            }

            is UiState.Failed -> ErrorState(
                icon = Icons.Outlined.CloudOff,
                title = current.error.title,
                message = current.error.message,
                onRetry = if (current.error.isRetryable) viewModel::load else null,
            )

            is UiState.Content -> DashboardContent(
                dashboard = current.data,
                policy = viewModel.policy,
            )
        }
    }
}

@Composable
private fun DashboardContent(
    dashboard: StudentDashboard,
    policy: AttendancePolicy,
) {
    val spacing = AppTheme.spacing

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.screenGutter,
            end = spacing.screenGutter,
            bottom = spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "identity") {
            Text(
                text = dashboard.student.name,
                style = AppTheme.typography.titleLarge,
                color = AppTheme.colors.textPrimary,
            )
            Text(
                text = "${dashboard.student.registerNumber} - " +
                    dashboard.student.classSection.displayName,
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
            )
        }

        item(key = "ring") {
            AppCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "OVERALL ATTENDANCE",
                        style = AppTheme.typography.overline,
                        color = AppTheme.colors.textTertiary,
                    )
                    AttendanceRing(
                        percentage = dashboard.summary.overallPercentage,
                        standing = dashboard.summary.standing,
                        modifier = Modifier.padding(vertical = spacing.md),
                    )
                    StandingBadge(standing = dashboard.summary.standing)
                }
            }
        }

        if (dashboard.summary.standing != AttendanceStanding.GOOD) {
            item(key = "warning") {
                LowAttendanceNotice(
                    standing = dashboard.summary.standing,
                    threshold = policy.threshold,
                )
            }
        }

        item(key = "today-header") {
            SectionHeader(
                title = "Today's classes",
                modifier = Modifier.padding(top = spacing.sm),
            )
        }

        if (dashboard.todaysClasses.isEmpty()) {
            item(key = "today-empty") {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.EventBusy,
                            contentDescription = null,
                            tint = AppTheme.colors.textTertiary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "No classes scheduled today.",
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary,
                            modifier = Modifier.padding(start = spacing.md),
                        )
                    }
                }
            }
        } else {
            items(dashboard.todaysClasses, key = { it.id }) { scheduled ->
                ScheduleRow(scheduledClass = scheduled)
            }
        }

        item(key = "subjects-header") {
            SectionHeader(
                title = "Subject attendance",
                modifier = Modifier.padding(top = spacing.sm),
            )
        }

        items(dashboard.summary.subjectBreakdown, key = { it.subject.id }) { subject ->
            ExpandableSubjectCard(subjectAttendance = subject, policy = policy)
        }

        item(key = "recent-header") {
            SectionHeader(
                title = "Recent attendance",
                modifier = Modifier.padding(top = spacing.sm),
            )
        }

        items(dashboard.recentRecords, key = { it.id }) { record ->
            AttendanceRecordRow(record = record)
        }
    }
}

/**
 * A subject card that opens to answer the obvious follow-up question: how many
 * classes does it take to get back above the threshold (Section 29).
 */
@Composable
private fun ExpandableSubjectCard(
    subjectAttendance: SubjectAttendance,
    policy: AttendancePolicy,
) {
    var expanded by remember { mutableStateOf(false) }
    val belowThreshold = policy.isBelowThreshold(subjectAttendance.percentage)

    Column {
        SubjectAttendanceCard(
            subjectAttendance = subjectAttendance,
            belowThreshold = belowThreshold,
            onClick = { expanded = !expanded },
        )

        if (expanded) {
            val needed = policy.classesNeededToReachThreshold(
                attended = subjectAttendance.attended,
                held = subjectAttendance.held,
            )
            Text(
                text = if (needed == 0) {
                    "Above the ${policy.threshold}% threshold. " +
                        "${subjectAttendance.held - subjectAttendance.attended} classes missed."
                } else {
                    "Attend the next $needed classes to reach ${policy.threshold}%."
                },
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
                modifier = Modifier.padding(
                    start = AppTheme.spacing.lg,
                    top = AppTheme.spacing.sm,
                    end = AppTheme.spacing.lg,
                ),
            )
        }
    }
}

/** Factual, non-alarmist wording (Section 30). */
@Composable
private fun LowAttendanceNotice(
    standing: AttendanceStanding,
    threshold: Int,
) {
    val colors = AppTheme.colors
    val tint = if (standing == AttendanceStanding.HIGH_RISK) colors.error else colors.warning

    AppCard {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.padding(start = AppTheme.spacing.md)) {
                Text(
                    text = standing.label,
                    style = AppTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = "Your current attendance is below the recommended " +
                        "threshold of $threshold%.",
                    style = AppTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = AppTheme.spacing.xxs),
                )
            }
        }
    }
}

private fun greeting(now: LocalTime = LocalTime.now()): String = when (now.hour) {
    in 0..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}
