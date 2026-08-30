package com.cit.attendance.feature.admin

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cit.attendance.core.designsystem.component.AppCard
import com.cit.attendance.core.designsystem.component.AppTopBar
import com.cit.attendance.core.designsystem.component.ErrorState
import com.cit.attendance.core.designsystem.component.LoadingCard
import com.cit.attendance.core.designsystem.component.ProgressBar
import com.cit.attendance.core.designsystem.component.SecondaryButton
import com.cit.attendance.core.designsystem.component.SectionHeader
import com.cit.attendance.core.designsystem.component.StatCard
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.core.ui.UiState
import com.cit.attendance.core.ui.formatPercentage
import com.cit.attendance.domain.model.AdminDashboard

/** Institution overview (Section 16). */
@Composable
fun AdminOverviewScreen(
    viewModel: AdminViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = "Institution", subtitle = "Attendance overview")

        when (val current = state) {
            UiState.Loading -> LoadingBody()

            is UiState.Failed -> ErrorState(
                icon = Icons.Outlined.CloudOff,
                title = current.error.title,
                message = current.error.message,
                onRetry = if (current.error.isRetryable) viewModel::load else null,
            )

            is UiState.Content -> OverviewContent(
                dashboard = current.data,
                threshold = viewModel.policy.threshold,
            )
        }
    }
}

@Composable
private fun OverviewContent(dashboard: AdminDashboard, threshold: Int) {
    val spacing = AppTheme.spacing

    LazyColumn(
        contentPadding = PaddingValues(
            start = spacing.screenGutter,
            end = spacing.screenGutter,
            bottom = spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "totals") {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                StatCard(
                    label = "Students",
                    value = dashboard.totalStudents.toString(),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Faculty",
                    value = dashboard.totalFaculty.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "today") {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                StatCard(
                    label = "Present today",
                    value = "${dashboard.presentToday}/${dashboard.expectedToday}",
                    caption = formatPercentage(dashboard.todaysAttendancePercentage) + " turnout",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "At risk",
                    value = dashboard.studentsAtRisk.toString(),
                    caption = "below $threshold%",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "average") {
            AppCard {
                Text(
                    text = "AVERAGE ATTENDANCE",
                    style = AppTheme.typography.overline,
                    color = AppTheme.colors.textTertiary,
                )
                Text(
                    text = formatPercentage(dashboard.averageAttendance),
                    style = AppTheme.typography.metricSmall,
                    color = AppTheme.colors.textPrimary,
                    modifier = Modifier.padding(top = spacing.sm),
                )
                ProgressBar(
                    fraction = dashboard.averageAttendance / 100f,
                    belowThreshold = dashboard.averageAttendance < threshold,
                    modifier = Modifier.padding(top = spacing.md),
                )
            }
        }

        item(key = "dept-header") {
            SectionHeader(title = "Departments", modifier = Modifier.padding(top = spacing.sm))
        }

        items(dashboard.departmentStats, key = { it.department }) { stat ->
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = stat.department,
                            style = AppTheme.typography.titleMedium,
                            color = AppTheme.colors.textPrimary,
                        )
                        Text(
                            text = "${stat.studentCount} students",
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.textSecondary,
                        )
                    }
                    Text(
                        text = formatPercentage(stat.averageAttendance),
                        style = AppTheme.typography.metricSmall,
                        color = AppTheme.colors.textPrimary,
                    )
                }
            }
        }
    }
}

/** Directory of the seeded cohort and faculty (Section 16). */
@Composable
fun AdminPeopleScreen(
    viewModel: AdminViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = "People", subtitle = "Institution directory")

        when (val current = state) {
            UiState.Loading -> LoadingBody()

            is UiState.Failed -> ErrorState(
                icon = Icons.Outlined.CloudOff,
                title = current.error.title,
                message = current.error.message,
                onRetry = if (current.error.isRetryable) viewModel::load else null,
            )

            is UiState.Content -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppTheme.spacing.screenGutter),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                StatCard(
                    label = "Enrolled students",
                    value = current.data.totalStudents.toString(),
                    caption = "CSE - III Year - A",
                )
                StatCard(
                    label = "Teaching staff",
                    value = current.data.totalFaculty.toString(),
                    caption = "Computer Science and Engineering",
                )
                Text(
                    text = "Roster management, enrolment and timetable editing arrive " +
                        "with the backend milestone.",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textTertiary,
                )
            }
        }
    }
}

/** Administrator profile. */
@Composable
fun AdminProfileScreen(
    viewModel: AdminViewModel,
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

            is UiState.Content -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = AppTheme.spacing.screenGutter),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                AppCard {
                    Text(
                        text = current.data.administrator.name,
                        style = AppTheme.typography.titleLarge,
                        color = AppTheme.colors.textPrimary,
                    )
                    Text(
                        text = current.data.administrator.designation,
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary,
                    )
                    Text(
                        text = current.data.administrator.email,
                        style = AppTheme.typography.bodyMedium,
                        color = AppTheme.colors.textTertiary,
                        modifier = Modifier.padding(top = AppTheme.spacing.sm),
                    )
                }
                SecondaryButton(text = "Sign out", onClick = onSignOut)
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
