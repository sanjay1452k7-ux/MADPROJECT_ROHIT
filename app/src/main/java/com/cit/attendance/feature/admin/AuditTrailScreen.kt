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
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cit.attendance.core.designsystem.component.AppCard
import com.cit.attendance.core.designsystem.component.EmptyState
import com.cit.attendance.core.designsystem.component.ErrorState
import com.cit.attendance.core.designsystem.component.SkeletonBlock
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.core.ui.UiState
import com.cit.attendance.core.ui.currentLocale
import com.cit.attendance.domain.model.AuditEntry
import com.cit.attendance.domain.model.UserRole
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The institution's activity log (Section 35).
 *
 * An audit trail that nobody can read is not accountability, so the record the
 * service keeps is shown here rather than left in the database. It is
 * deliberately read-only: the value of the trail comes from it being
 * append-only, and an interface that could edit it would defeat the point.
 */
@Composable
fun AuditTrailScreen(modifier: Modifier = Modifier) {
    val viewModel: AuditTrailViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (val current = state) {
        UiState.Loading -> AuditLoading(modifier)

        is UiState.Failed -> ErrorState(
            icon = Icons.Outlined.CloudOff,
            title = current.error.title,
            message = current.error.message,
            onRetry = if (current.error.isRetryable) viewModel::load else null,
            modifier = modifier,
        )

        is UiState.Content -> if (current.data.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.HistoryToggleOff,
                title = "No activity yet",
                message = "Sign-ins, attendance sessions and marked attendance " +
                    "will appear here as they happen.",
                modifier = modifier,
            )
        } else {
            AuditList(entries = current.data, modifier = modifier)
        }
    }
}

@Composable
private fun AuditList(entries: List<AuditEntry>, modifier: Modifier = Modifier) {
    val locale = currentLocale()
    val formatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(locale)
        .withZone(ZoneId.systemDefault())

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = AppTheme.spacing.screenGutter,
            vertical = AppTheme.spacing.sm,
        ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        items(entries, key = { it.id }) { entry ->
            AuditRow(entry = entry, formatted = entry.formattedTime(formatter))
        }
    }
}

@Composable
private fun AuditRow(entry: AuditEntry, formatted: String) {
    val colors = AppTheme.colors

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs)) {
            Text(
                text = entry.summary,
                style = AppTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.actorLabel(),
                    style = AppTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(0.6f),
                )
                Text(
                    text = formatted,
                    style = AppTheme.typography.overline,
                    color = colors.textTertiary,
                )
            }
        }
    }
}

@Composable
private fun AuditLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(AppTheme.spacing.screenGutter),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
    ) {
        repeat(SKELETON_ROWS) {
            SkeletonBlock(
                modifier = Modifier.fillMaxWidth(),
                height = AppTheme.spacing.xxl,
            )
        }
    }
}

/** "Meenakshi S (Teacher)", or just the name when the role is unknown. */
private fun AuditEntry.actorLabel(): String = when (actorRole) {
    UserRole.STUDENT -> "$actor (Student)"
    UserRole.TEACHER -> "$actor (Teacher)"
    UserRole.ADMIN -> "$actor (Administrator)"
    UserRole.PARENT -> "$actor (Parent)"
    null -> actor
}

/**
 * The server sends an ISO instant. A row whose timestamp cannot be parsed is
 * still worth showing - losing the entry entirely would be the worse failure -
 * so the raw value is displayed instead.
 */
private fun AuditEntry.formattedTime(formatter: DateTimeFormatter): String =
    runCatching { formatter.format(Instant.parse(occurredAt)) }.getOrDefault(occurredAt)

/** Enough rows to fill the first screen while the request is in flight. */
private const val SKELETON_ROWS = 6
