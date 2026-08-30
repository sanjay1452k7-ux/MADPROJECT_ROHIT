package com.cit.attendance.feature.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cit.attendance.core.designsystem.component.AppCard
import com.cit.attendance.core.designsystem.component.AppTopBar
import com.cit.attendance.core.designsystem.component.EmptyState
import com.cit.attendance.core.designsystem.component.ErrorState
import com.cit.attendance.core.designsystem.component.LoadingCard
import com.cit.attendance.core.designsystem.component.PrimaryButton
import com.cit.attendance.core.designsystem.component.ProgressBar
import com.cit.attendance.core.designsystem.component.ScheduleRow
import com.cit.attendance.core.designsystem.component.SecondaryButton
import com.cit.attendance.core.designsystem.component.SectionHeader
import com.cit.attendance.core.designsystem.component.SeededAvatar
import com.cit.attendance.core.designsystem.component.StatCard
import com.cit.attendance.core.designsystem.component.StatusBadge
import com.cit.attendance.core.designsystem.component.BadgeTone
import com.cit.attendance.core.designsystem.component.StudentRiskRow
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.core.ui.UiState
import com.cit.attendance.core.ui.formatPercentage
import com.cit.attendance.domain.model.TeacherDashboard

/**
 * Teacher home (Section 15).
 *
 * "Start attendance" is the primary action: it opens the attendance tab, where
 * the session is created and codes are scanned.
 */
@Composable
fun TeacherHomeScreen(
    viewModel: TeacherViewModel,
    onStartAttendance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = "Today", subtitle = "Teaching overview")

        when (val current = state) {
            UiState.Loading -> LoadingBody()

            is UiState.Failed -> ErrorState(
                icon = Icons.Outlined.CloudOff,
                title = current.error.title,
                message = current.error.message,
                onRetry = if (current.error.isRetryable) viewModel::load else null,
            )

            is UiState.Content -> TeacherHomeContent(
                dashboard = current.data,
                onStartAttendance = onStartAttendance,
            )
        }
    }
}

@Composable
private fun TeacherHomeContent(
    dashboard: TeacherDashboard,
    onStartAttendance: () -> Unit,
) {
    val spacing = AppTheme.spacing

    LazyColumn(
        contentPadding = PaddingValues(
            start = spacing.screenGutter,
            end = spacing.screenGutter,
            bottom = spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "identity") {
            Text(
                text = dashboard.teacher.name,
                style = AppTheme.typography.titleLarge,
                color = AppTheme.colors.textPrimary,
            )
            Text(
                text = "${dashboard.teacher.employeeCode} - ${dashboard.teacher.department}",
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colors.textSecondary,
            )
        }

        item(key = "cta") {
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Attendance session",
                            style = AppTheme.typography.titleMedium,
                            color = AppTheme.colors.textPrimary,
                        )
                        Text(
                            text = "${dashboard.activeSessionCount} active right now",
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary,
                        )
                    }
                    StatusBadge(
                        text = if (dashboard.activeSessionCount > 0) "ACTIVE" else "IDLE",
                        tone = if (dashboard.activeSessionCount > 0) {
                            BadgeTone.ACCENT
                        } else {
                            BadgeTone.NEUTRAL
                        },
                    )
                }

                PrimaryButton(
                    text = if (dashboard.activeSessionCount > 0) {
                        "Continue attendance"
                    } else {
                        "Start attendance"
                    },
                    onClick = onStartAttendance,
                    modifier = Modifier.padding(top = spacing.lg),
                )
            }
        }

        item(key = "stats") {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                StatCard(
                    label = "Class average",
                    value = formatPercentage(dashboard.averageAttendance),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "At risk",
                    value = dashboard.studentsAtRisk.size.toString(),
                    caption = "below threshold",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "classes-header") {
            SectionHeader(title = "Your classes today", modifier = Modifier.padding(top = spacing.sm))
        }

        if (dashboard.todaysClasses.isEmpty()) {
            item(key = "classes-empty") {
                EmptyState(
                    icon = Icons.Outlined.EventAvailable,
                    title = "No classes today",
                    message = "Your timetable has no sessions scheduled for today.",
                )
            }
        } else {
            items(dashboard.todaysClasses, key = { it.id }) { scheduled ->
                ScheduleRow(scheduledClass = scheduled)
            }
        }

        item(key = "risk-header") {
            SectionHeader(title = "Students at risk", modifier = Modifier.padding(top = spacing.sm))
        }

        if (dashboard.studentsAtRisk.isEmpty()) {
            item(key = "risk-empty") {
                EmptyState(
                    icon = Icons.Outlined.TaskAlt,
                    title = "Everyone is above the threshold",
                    message = "No student in this cohort is currently below the requirement.",
                )
            }
        } else {
            items(dashboard.studentsAtRisk, key = { it.student.id }) { risk ->
                StudentRiskRow(studentAtRisk = risk)
            }
        }
    }
}

/** The cohort the teacher is responsible for (Section 15). */
@Composable
fun TeacherClassesScreen(
    viewModel: TeacherViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = "Classes", subtitle = "Assigned subjects")

        when (val current = state) {
            UiState.Loading -> LoadingBody()

            is UiState.Failed -> ErrorState(
                icon = Icons.Outlined.CloudOff,
                title = current.error.title,
                message = current.error.message,
                onRetry = if (current.error.isRetryable) viewModel::load else null,
            )

            is UiState.Content -> LazyColumn(
                contentPadding = PaddingValues(
                    start = AppTheme.spacing.screenGutter,
                    end = AppTheme.spacing.screenGutter,
                    bottom = AppTheme.spacing.xxl,
                ),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                items(current.data.teacher.subjects, key = { it.id }) { subject ->
                    AppCard {
                        Text(
                            text = subject.name,
                            style = AppTheme.typography.titleMedium,
                            color = AppTheme.colors.textPrimary,
                        )
                        Text(
                            text = "${subject.code} - CSE III Year Section A",
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

/** Teacher profile. */
@Composable
fun TeacherProfileScreen(
    viewModel: TeacherViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = "Profile", subtitle = "Your account")

        when (val current = state) {
            UiState.Loading -> LoadingBody()

            is UiState.Failed -> ErrorState(
                icon = Icons.Outlined.CloudOff,
                title = current.error.title,
                message = current.error.message,
                onRetry = if (current.error.isRetryable) viewModel::load else null,
            )

            is UiState.Content -> {
                val teacher = current.data.teacher
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppTheme.spacing.screenGutter),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                ) {
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SeededAvatar(
                                seed = teacher.avatarSeed,
                                initials = teacher.name.take(2).uppercase(),
                                size = 64.dp,
                                highlighted = true,
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = AppTheme.spacing.lg),
                            ) {
                                Text(
                                    text = teacher.name,
                                    style = AppTheme.typography.titleLarge,
                                    color = AppTheme.colors.textPrimary,
                                )
                                Text(
                                    text = teacher.email,
                                    style = AppTheme.typography.bodyMedium,
                                    color = AppTheme.colors.textSecondary,
                                )
                            }
                        }
                    }
                    SecondaryButton(text = "Sign out", onClick = onSignOut)
                }
            }
        }
    }
}

@Composable
private fun LoadingBody() {
    Column(
        modifier = Modifier.padding(horizontal = AppTheme.spacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        repeat(3) { LoadingCard(lines = 3) }
    }
}
