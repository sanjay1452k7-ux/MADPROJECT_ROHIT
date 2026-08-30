package com.cit.attendance.feature.student

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cit.attendance.core.designsystem.component.AppCard
import com.cit.attendance.core.designsystem.component.AppTopBar
import com.cit.attendance.core.designsystem.component.BadgeTone
import com.cit.attendance.core.designsystem.component.EmptyState
import com.cit.attendance.core.designsystem.component.ErrorState
import com.cit.attendance.core.designsystem.component.LoadingCard
import com.cit.attendance.core.designsystem.component.PrimaryButton
import com.cit.attendance.core.designsystem.component.ProgressBar
import com.cit.attendance.core.designsystem.component.QrDisplay
import com.cit.attendance.core.designsystem.component.StatusBadge
import com.cit.attendance.core.designsystem.theme.AppTheme

/**
 * The student's half of the attendance workflow (Section 19).
 *
 * There is no "I am present" button anywhere on this screen: the student can
 * only present a code, and a teacher decides whether attendance is recorded
 * (Section 17).
 */
@Composable
fun StudentQrScreen(
    modifier: Modifier = Modifier,
    viewModel: StudentQrViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val spacing = AppTheme.spacing

    DisposableEffect(Unit) {
        onDispose { viewModel.onScreenHidden() }
    }

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = "Attendance", subtitle = "Present your code")

        when {
            state.isLoadingSession -> Column(
                modifier = Modifier.padding(horizontal = spacing.screenGutter),
            ) {
                LoadingCard(lines = 3)
            }

            state.error != null && state.session == null -> {
                val error = state.error
                if (error != null) {
                    ErrorState(
                        icon = Icons.Outlined.CloudOff,
                        title = error.title,
                        message = error.message,
                        onRetry = if (error.isRetryable) viewModel::loadSession else null,
                    )
                }
            }

            state.session == null -> EmptyState(
                icon = Icons.Outlined.EventBusy,
                title = "No attendance session",
                message = "Your code appears here once a teacher starts attendance.",
            )

            state.isMarked -> AttendanceRecordedCard(
                subjectName = state.session?.subjectName.orEmpty(),
                markedAtLabel = state.session?.markedAtLabel,
            )

            else -> ActiveSessionContent(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun ActiveSessionContent(
    state: StudentQrUiState,
    viewModel: StudentQrViewModel,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val session = state.session ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.screenGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Attendance session active",
                        style = AppTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = "${session.subjectName} - ${session.teacherName}",
                        style = AppTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
                StatusBadge(text = "ACTIVE", tone = BadgeTone.ACCENT)
            }
        }

        val token = state.token
        if (token == null) {
            Column(
                modifier = Modifier.padding(top = spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Icon(
                    imageVector = Icons.Outlined.QrCode2,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = "Generate your attendance code, then join the queue for your teacher to scan it.",
                    style = AppTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                PrimaryButton(
                    text = "Generate QR",
                    onClick = viewModel::generateToken,
                    loading = state.isMinting,
                )
            }
        } else {
            AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn(initialScale = 0.94f)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                ) {
                    QrDisplay(token = token.token, freshness = state.progress)

                    Text(
                        text = "Refreshes in ${state.secondsRemaining} seconds",
                        style = AppTheme.typography.label,
                        color = colors.textSecondary,
                    )

                    ProgressBar(
                        fraction = state.progress,
                        belowThreshold = state.progress < 0.3f,
                        modifier = Modifier.fillMaxWidth(0.7f),
                    )

                    Text(
                        text = "Show this to your teacher. It refreshes automatically.",
                        style = AppTheme.typography.bodyMedium,
                        color = colors.textTertiary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        val error = state.error
        AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
            if (error != null) {
                AppCard {
                    Text(
                        text = error.title,
                        style = AppTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = error.message,
                        style = AppTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = spacing.xxs),
                    )
                }
            }
        }
    }
}

/** The state after a teacher records the student (Sections 23 and 24). */
@Composable
private fun AttendanceRecordedCard(subjectName: String, markedAtLabel: String?) {
    val colors = AppTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppTheme.spacing.screenGutter),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.height(AppTheme.spacing.xxxl))
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(colors.successMuted, AppTheme.shapes.extraLarge),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            text = "ATTENDANCE MARKED",
            style = AppTheme.typography.overline,
            color = colors.success,
            modifier = Modifier.padding(top = AppTheme.spacing.lg),
        )
        Text(
            text = subjectName,
            style = AppTheme.typography.titleLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = AppTheme.spacing.sm),
        )
        if (markedAtLabel != null) {
            Text(
                text = markedAtLabel,
                style = AppTheme.typography.bodyLarge,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = AppTheme.spacing.xs),
            )
        }
    }
}
