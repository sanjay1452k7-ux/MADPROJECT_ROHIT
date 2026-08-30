package com.cit.attendance.feature.analytics

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
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cit.attendance.core.designsystem.component.AppCard
import com.cit.attendance.core.designsystem.component.AppTopBar
import com.cit.attendance.core.designsystem.component.ChartEntry
import com.cit.attendance.core.designsystem.component.ComparisonBars
import com.cit.attendance.core.designsystem.component.CountBars
import com.cit.attendance.core.designsystem.component.EmptyState
import com.cit.attendance.core.designsystem.component.ErrorState
import com.cit.attendance.core.designsystem.component.LoadingCard
import com.cit.attendance.core.designsystem.component.SectionHeader
import com.cit.attendance.core.designsystem.component.SeededAvatar
import com.cit.attendance.core.designsystem.component.StatCard
import com.cit.attendance.core.designsystem.component.StatusBadge
import com.cit.attendance.core.designsystem.component.BadgeTone
import com.cit.attendance.core.designsystem.component.TrendChart
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.core.ui.UiState
import com.cit.attendance.core.ui.formatPercentage
import com.cit.attendance.domain.model.RiskBand
import com.cit.attendance.domain.model.RiskStudent
import com.cit.attendance.domain.model.TeacherAnalytics
import java.time.format.DateTimeFormatter

/** Teacher analytics (Section 28). */
@Composable
fun TeacherAnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: TeacherAnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = "Analytics", subtitle = "Cohort performance")

        when (val current = state) {
            UiState.Loading -> Column(
                modifier = Modifier.padding(horizontal = AppTheme.spacing.screenGutter),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
            ) {
                LoadingCard(lines = 3)
                LoadingCard(lines = 4)
            }

            is UiState.Failed -> ErrorState(
                icon = Icons.Outlined.CloudOff,
                title = current.error.title,
                message = current.error.message,
                onRetry = if (current.error.isRetryable) viewModel::load else null,
            )

            is UiState.Content -> TeacherAnalyticsContent(current.data)
        }
    }
}

@Composable
private fun TeacherAnalyticsContent(analytics: TeacherAnalytics) {
    val spacing = AppTheme.spacing
    val dayFormat = remember { DateTimeFormatter.ofPattern("d MMM") }

    LazyColumn(
        contentPadding = PaddingValues(
            start = spacing.screenGutter,
            end = spacing.screenGutter,
            bottom = spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "headline") {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                StatCard(
                    label = "Class average",
                    value = formatPercentage(analytics.classAverage),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "At risk",
                    value = analytics.studentsAtRisk.size.toString(),
                    caption = "below ${analytics.threshold}%",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "trend-header") { SectionHeader(title = "Monthly trend") }
        item(key = "trend") {
            AppCard {
                TrendChart(
                    entries = analytics.monthlyTrend.map {
                        ChartEntry(
                            label = it.label,
                            value = it.percentage,
                            detail = "${it.attended} of ${it.held}",
                        )
                    },
                    thresholdPercent = analytics.threshold,
                )
            }
        }

        item(key = "subjects-header") {
            SectionHeader(title = "Subject averages", modifier = Modifier.padding(top = spacing.sm))
        }
        item(key = "subjects") {
            AppCard {
                ComparisonBars(
                    entries = analytics.subjectAverages.map {
                        ChartEntry(
                            label = it.subjectName,
                            value = it.percentage,
                            detail = "${it.attended} of ${it.held} recorded",
                        )
                    },
                    thresholdPercent = analytics.threshold,
                )
            }
        }

        item(key = "absentee-header") {
            SectionHeader(title = "Absences per day", modifier = Modifier.padding(top = spacing.sm))
        }
        item(key = "absentee") {
            AppCard {
                CountBars(
                    entries = analytics.absenteeTrend.map {
                        ChartEntry(
                            label = it.date.format(dayFormat),
                            value = it.absent.toFloat(),
                        )
                    },
                )
            }
        }

        item(key = "risk-header") {
            SectionHeader(title = "Students at risk", modifier = Modifier.padding(top = spacing.sm))
        }

        if (analytics.studentsAtRisk.isEmpty()) {
            item(key = "risk-empty") {
                EmptyState(
                    icon = Icons.Outlined.TaskAlt,
                    title = "Everyone is above the requirement",
                    message = "No student you teach is currently below ${analytics.threshold}%.",
                )
            }
        } else {
            items(analytics.studentsAtRisk, key = { it.studentId }) { student ->
                RiskStudentCard(student = student)
            }
        }
    }
}

/**
 * One at-risk student with the reason spelled out.
 *
 * Both percentages are labelled, because the overall figure and the figure in
 * this teacher's subjects are different numbers and showing one unlabelled
 * would invite the wrong conclusion.
 */
@Composable
private fun RiskStudentCard(student: RiskStudent) {
    val colors = AppTheme.colors

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SeededAvatar(
                seed = student.avatarSeed,
                initials = student.initials,
                size = 40.dp,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = AppTheme.spacing.md),
            ) {
                Text(
                    text = student.name,
                    style = AppTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = student.registerNumber,
                    style = AppTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
            StatusBadge(
                text = student.band.name,
                tone = when (student.band) {
                    RiskBand.LOW -> BadgeTone.POSITIVE
                    RiskBand.MODERATE -> BadgeTone.CAUTION
                    RiskBand.HIGH -> BadgeTone.CRITICAL
                },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            StatCard(
                label = "Overall",
                value = formatPercentage(student.percentage),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Your subjects",
                value = formatPercentage(student.inYourSubjects),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Projected",
                value = formatPercentage(student.projectedPercentage),
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = student.reason,
            style = AppTheme.typography.bodyMedium,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = AppTheme.spacing.md),
        )
    }
}
