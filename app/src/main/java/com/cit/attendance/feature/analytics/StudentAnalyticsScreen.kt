package com.cit.attendance.feature.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cit.attendance.core.designsystem.component.AppCard
import com.cit.attendance.core.designsystem.component.AppTopBar
import com.cit.attendance.core.designsystem.component.AttendanceCalendar
import com.cit.attendance.core.designsystem.component.ChartEntry
import com.cit.attendance.core.designsystem.component.ComparisonBars
import com.cit.attendance.core.designsystem.component.ErrorState
import com.cit.attendance.core.designsystem.component.LoadingCard
import com.cit.attendance.core.designsystem.component.SectionHeader
import com.cit.attendance.core.designsystem.component.StandingBadge
import com.cit.attendance.core.designsystem.component.StatCard
import com.cit.attendance.core.designsystem.component.TrendChart
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.core.ui.UiState
import com.cit.attendance.core.ui.formatPercentage
import com.cit.attendance.domain.model.RiskAssessment
import com.cit.attendance.domain.model.RiskBand
import com.cit.attendance.domain.model.StudentAnalytics

/** Student analytics (Section 28). */
@Composable
fun StudentAnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: StudentAnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = "Analytics", subtitle = "Your attendance")

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

            is UiState.Content -> AnalyticsContent(current.data)
        }
    }
}

@Composable
private fun AnalyticsContent(analytics: StudentAnalytics) {
    val spacing = AppTheme.spacing

    LazyColumn(
        contentPadding = PaddingValues(
            start = spacing.screenGutter,
            end = spacing.screenGutter,
            bottom = spacing.xxl,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "headline") {
            AppCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "OVERALL",
                            style = AppTheme.typography.overline,
                            color = AppTheme.colors.textTertiary,
                        )
                        Text(
                            text = formatPercentage(analytics.overallPercentage),
                            style = AppTheme.typography.metricSmall,
                            color = AppTheme.colors.textPrimary,
                        )
                    }
                    StandingBadge(standing = analytics.standing)
                }
                Text(
                    text = "${analytics.totals.present} present, " +
                        "${analytics.totals.late} late, " +
                        "${analytics.totals.absent} absent of ${analytics.totals.held} sessions",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = spacing.sm),
                )
            }
        }

        item(key = "risk") { RiskCard(risk = analytics.risk, threshold = analytics.threshold) }

        item(key = "trend-header") {
            SectionHeader(title = "Monthly trend", modifier = Modifier.padding(top = spacing.sm))
        }
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
            SectionHeader(
                title = "Subject attendance",
                modifier = Modifier.padding(top = spacing.sm),
            )
        }
        item(key = "subjects") {
            AppCard {
                ComparisonBars(
                    entries = analytics.subjects.map {
                        ChartEntry(
                            label = it.subjectName,
                            value = it.percentage,
                            detail = "${it.attended} of ${it.held} classes",
                        )
                    },
                    thresholdPercent = analytics.threshold,
                )
            }
        }

        item(key = "calendar-header") {
            SectionHeader(title = "Calendar", modifier = Modifier.padding(top = spacing.sm))
        }
        item(key = "calendar") {
            AppCard { AttendanceCalendar(entries = analytics.calendar) }
        }
    }
}

/**
 * The risk assessment (Section 31).
 *
 * The card names the method that produced the projection, so the figure is
 * never presented as more than the rule-based calculation it is.
 */
@Composable
private fun RiskCard(risk: RiskAssessment, threshold: Int) {
    val colors = AppTheme.colors
    val tint = when (risk.band) {
        RiskBand.LOW -> colors.success
        RiskBand.MODERATE -> colors.warning
        RiskBand.HIGH -> colors.error
    }

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Insights,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "ATTENDANCE RISK",
                style = AppTheme.typography.overline,
                color = colors.textTertiary,
                modifier = Modifier.padding(start = AppTheme.spacing.sm),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.spacing.md),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
        ) {
            StatCard(
                label = "Current",
                value = formatPercentage(risk.currentPercentage),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Projected",
                value = formatPercentage(risk.projectedPercentage),
                caption = if (risk.isDeclining) "if the pattern holds" else "on current form",
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = risk.reason,
            style = AppTheme.typography.bodyLarge,
            color = colors.textPrimary,
            modifier = Modifier.padding(top = AppTheme.spacing.md),
        )
        Text(
            text = "Requirement ${threshold}%. ${risk.method}.",
            style = AppTheme.typography.bodyMedium,
            color = colors.textTertiary,
            modifier = Modifier.padding(top = AppTheme.spacing.xxs),
        )
    }
}
