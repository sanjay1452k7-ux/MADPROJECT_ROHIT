package com.cit.attendance.feature.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.cit.attendance.core.designsystem.component.AttendanceStatusBadge
import com.cit.attendance.core.designsystem.component.AppTopBar
import com.cit.attendance.core.designsystem.component.ErrorState
import com.cit.attendance.core.designsystem.component.LoadingCard
import com.cit.attendance.core.designsystem.component.PrimaryButton
import com.cit.attendance.core.designsystem.component.RoleCard
import com.cit.attendance.core.designsystem.component.SecondaryButton
import com.cit.attendance.core.designsystem.component.SectionHeader
import com.cit.attendance.core.designsystem.component.SeededAvatar
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.core.ui.UiState
import com.cit.attendance.domain.model.AttendanceStatus
import com.cit.attendance.domain.model.SessionRosterEntry
import androidx.compose.material.icons.outlined.MenuBook

/**
 * Chooses which class to open attendance for, then hands over to the scanner
 * (Section 18).
 *
 * A session is always tied to one subject and one cohort, so the teacher picks
 * both before any code can be scanned.
 */
@Composable
fun TeacherSessionLauncher(
    teacherViewModel: TeacherViewModel,
    sessionViewModel: AttendanceSessionViewModel,
    onSessionStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dashboardState by teacherViewModel.state.collectAsStateWithLifecycle()
    val sessionState by sessionViewModel.state.collectAsStateWithLifecycle()

    var selectedSubjectId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(sessionState.session?.id) {
        if (sessionState.session != null) {
            onSessionStarted()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = "Start attendance", subtitle = "Choose a class")

        when (val current = dashboardState) {
            UiState.Loading -> Column(
                modifier = Modifier.padding(horizontal = AppTheme.spacing.screenGutter),
            ) {
                LoadingCard(lines = 3)
            }

            is UiState.Failed -> ErrorState(
                icon = Icons.Outlined.CloudOff,
                title = current.error.title,
                message = current.error.message,
                onRetry = if (current.error.isRetryable) teacherViewModel::load else null,
            )

            is UiState.Content -> {
                val cohortId = teacherViewModel.primaryCohortId
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = AppTheme.spacing.screenGutter),
                    verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
                ) {
                    SectionHeader(title = "Your subjects")

                    current.data.teacher.subjects.forEach { subject ->
                        RoleCard(
                            title = subject.name,
                            description = subject.code,
                            icon = Icons.Outlined.MenuBook,
                            selected = subject.id == selectedSubjectId,
                            onClick = { selectedSubjectId = subject.id },
                        )
                    }

                    val error = sessionState.error
                    if (error != null) {
                        AppCard {
                            Text(
                                text = error.title,
                                style = AppTheme.typography.titleMedium,
                                color = AppTheme.colors.textPrimary,
                            )
                            Text(
                                text = error.message,
                                style = AppTheme.typography.bodyMedium,
                                color = AppTheme.colors.textSecondary,
                            )
                        }
                    }

                    PrimaryButton(
                        text = "Start attendance",
                        onClick = {
                            val subjectId = selectedSubjectId
                            if (subjectId != null && cohortId != null) {
                                sessionViewModel.startSession(subjectId, cohortId)
                            }
                        },
                        enabled = selectedSubjectId != null && cohortId != null,
                        loading = sessionState.isStarting,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (cohortId == null) {
                        Text(
                            text = "No class is assigned to you in the timetable yet.",
                            style = AppTheme.typography.bodyMedium,
                            color = AppTheme.colors.textTertiary,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The live session: roster, running count and the manual fallback
 * (Sections 18 and 37).
 */
@Composable
fun TeacherLiveSessionScreen(
    sessionViewModel: AttendanceSessionViewModel,
    onScan: () -> Unit,
    onClosed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by sessionViewModel.state.collectAsStateWithLifecycle()
    val session = state.session

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(
            title = session?.subjectName ?: "Attendance session",
            subtitle = session?.className,
        )

        if (session == null) {
            LoadingCard(
                modifier = Modifier.padding(horizontal = AppTheme.spacing.screenGutter),
            )
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppTheme.spacing.screenGutter),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            AppCard {
                Text(
                    text = "${session.presentCount} of ${session.totalStudents} marked",
                    style = AppTheme.typography.metricSmall,
                    color = AppTheme.colors.textPrimary,
                )
                Text(
                    text = if (session.isActive) "Session active" else "Session closed",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                )
            }

            if (session.isActive) {
                PrimaryButton(text = "Scan QR", onClick = onScan)
            }

            SectionHeader(title = "Roster")

            session.roster.forEach { entry ->
                RosterRow(
                    entry = entry,
                    canMarkManually = session.isActive && entry.status == null,
                    onMarkManually = {
                        sessionViewModel.markManually(
                            studentId = entry.studentId,
                            status = AttendanceStatus.PRESENT,
                            reason = "Marked manually by the teacher",
                        )
                    },
                )
            }

            if (session.isActive) {
                SecondaryButton(
                    text = "Close session",
                    onClick = { sessionViewModel.closeSession(onClosed) },
                    enabled = !state.isSubmitting,
                )
            }
        }
    }
}

/**
 * One student on the live roster.
 *
 * The manual action is offered only for students nobody has accounted for yet,
 * which keeps the fallback from becoming a way to overwrite a scan.
 */
@Composable
private fun RosterRow(
    entry: SessionRosterEntry,
    canMarkManually: Boolean,
    onMarkManually: () -> Unit,
) {
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SeededAvatar(
                seed = entry.avatarSeed,
                initials = entry.name.trim().split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() },
                size = 40.dp,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppTheme.spacing.md),
            ) {
                Text(
                    text = entry.name,
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.textPrimary,
                )
                Text(
                    text = entry.registerNumber,
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                )
            }

            val status = entry.status
            if (status != null) {
                AttendanceStatusBadge(status = status)
            } else if (canMarkManually) {
                SecondaryButton(
                    text = "Mark",
                    onClick = onMarkManually,
                    modifier = Modifier
                        .fillMaxWidth(0.32f)
                        .defaultMinSize(minHeight = AppTheme.spacing.minTouchTarget),
                )
            }
        }
    }
}
