package com.cit.attendance.feature.student

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
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cit.attendance.core.designsystem.component.AppCard
import com.cit.attendance.core.designsystem.component.AppTopBar
import com.cit.attendance.core.designsystem.component.AttendanceRecordRow
import com.cit.attendance.core.designsystem.component.EmptyState
import com.cit.attendance.core.designsystem.component.ErrorState
import com.cit.attendance.core.designsystem.component.LoadingCard
import com.cit.attendance.core.designsystem.component.NotificationCard
import com.cit.attendance.core.designsystem.component.SecondaryButton
import com.cit.attendance.core.designsystem.component.SeededAvatar
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.core.ui.UiState
import com.cit.attendance.core.ui.formatPercentage
import com.cit.attendance.domain.model.NotificationType

/** Full attendance history (Section 14). */
@Composable
fun StudentHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: StudentHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = "Attendance history", subtitle = "Recent sessions")

        when (val current = state) {
            UiState.Loading -> LoadingList()

            is UiState.Failed -> ErrorState(
                icon = Icons.Outlined.CloudOff,
                title = current.error.title,
                message = current.error.message,
                onRetry = if (current.error.isRetryable) viewModel::load else null,
            )

            is UiState.Content -> if (current.data.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.EventBusy,
                    title = "No attendance yet",
                    message = "Records appear here once your teachers begin marking attendance.",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = AppTheme.spacing.screenGutter,
                        end = AppTheme.spacing.screenGutter,
                        bottom = AppTheme.spacing.xxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                ) {
                    items(current.data, key = { it.id }) { record ->
                        AttendanceRecordRow(record = record)
                    }
                }
            }
        }
    }
}

/** In-app notification list (Section 32). */
@Composable
fun StudentNotificationsScreen(
    modifier: Modifier = Modifier,
    viewModel: StudentNotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = "Notifications", subtitle = "Updates")

        when (val current = state) {
            UiState.Loading -> LoadingList()

            is UiState.Failed -> ErrorState(
                icon = Icons.Outlined.CloudOff,
                title = current.error.title,
                message = current.error.message,
                onRetry = if (current.error.isRetryable) viewModel::load else null,
            )

            is UiState.Content -> if (current.data.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "You are all caught up",
                    message = "Attendance updates and announcements will appear here.",
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = AppTheme.spacing.screenGutter,
                        end = AppTheme.spacing.screenGutter,
                        bottom = AppTheme.spacing.xxl,
                    ),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
                ) {
                    items(current.data, key = { it.id }) { notification ->
                        NotificationCard(
                            notification = notification,
                            icon = iconFor(notification.type),
                            onClick = { viewModel.onNotificationOpened(notification.id) },
                        )
                    }
                }
            }
        }
    }
}

/** Student profile (Section 27). */
@Composable
fun StudentProfileScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StudentProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = "Profile", subtitle = "Your account")

        when (val current = state) {
            UiState.Loading -> LoadingList()

            is UiState.Failed -> ErrorState(
                icon = Icons.Outlined.CloudOff,
                title = current.error.title,
                message = current.error.message,
                onRetry = if (current.error.isRetryable) viewModel::load else null,
            )

            is UiState.Content -> {
                val student = current.data.student
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = AppTheme.spacing.screenGutter),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                ) {
                    AppCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SeededAvatar(
                                seed = student.avatarSeed,
                                initials = student.initials,
                                size = 64.dp,
                                highlighted = true,
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = AppTheme.spacing.lg),
                            ) {
                                Text(
                                    text = student.name,
                                    style = AppTheme.typography.titleLarge,
                                    color = AppTheme.colors.textPrimary,
                                )
                                Text(
                                    text = student.registerNumber,
                                    style = AppTheme.typography.bodyMedium,
                                    color = AppTheme.colors.textSecondary,
                                )
                            }
                        }
                    }

                    AppCard {
                        ProfileRow("College email", student.email)
                        ProfileRow("Department", student.department)
                        ProfileRow("Class", student.classSection.longForm)
                        ProfileRow(
                            label = "Overall attendance",
                            value = formatPercentage(current.data.overallPercentage),
                        )
                    }

                    SecondaryButton(text = "Sign out", onClick = onSignOut)
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppTheme.spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.textTertiary,
        )
        Text(
            text = value,
            style = AppTheme.typography.bodyLarge,
            color = AppTheme.colors.textPrimary,
        )
    }
}

@Composable
private fun LoadingList() {
    Column(
        modifier = Modifier.padding(horizontal = AppTheme.spacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        repeat(4) { LoadingCard(lines = 2) }
    }
}

private fun iconFor(type: NotificationType): ImageVector = when (type) {
    NotificationType.ATTENDANCE_MARKED -> Icons.Outlined.CheckCircle
    NotificationType.ABSENCE_RECORDED -> Icons.Outlined.EventBusy
    NotificationType.LOW_ATTENDANCE_WARNING -> Icons.Outlined.WarningAmber
    NotificationType.ANNOUNCEMENT -> Icons.Outlined.Campaign
    NotificationType.SYSTEM -> Icons.Outlined.Info
}
