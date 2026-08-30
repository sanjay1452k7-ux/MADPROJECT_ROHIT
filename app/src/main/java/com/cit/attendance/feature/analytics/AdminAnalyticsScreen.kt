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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cit.attendance.core.designsystem.component.AppCard
import com.cit.attendance.core.designsystem.component.AppTopBar
import com.cit.attendance.core.designsystem.component.ChartEntry
import com.cit.attendance.core.designsystem.component.ComparisonBars
import com.cit.attendance.core.designsystem.component.ErrorState
import com.cit.attendance.core.designsystem.component.LoadingCard
import com.cit.attendance.core.designsystem.component.SectionHeader
import com.cit.attendance.core.designsystem.component.StatCard
import com.cit.attendance.core.designsystem.component.TrendChart
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.core.ui.UiState
import com.cit.attendance.core.ui.formatPercentage
import com.cit.attendance.domain.model.AdminAnalytics

/** Institution-wide analytics (Section 28). */
@Composable
fun AdminAnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminAnalyticsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        AppTopBar(title = "Analytics", subtitle = "Institution-wide")

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

            is UiState.Content -> AdminAnalyticsContent(current.data)
        }
    }
}

@Composable
private fun AdminAnalyticsContent(analytics: AdminAnalytics) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
                StatCard(
                    label = "Institution average",
                    value = formatPercentage(analytics.institutionAverage),
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    label = "Below threshold",
                    value = "${analytics.studentsBelowThreshold}/${analytics.totalStudents}",
                    caption = "under ${analytics.threshold}%",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "trend-header") { SectionHeader(title = "Institution trend") }
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

        item(key = "dept-header") {
            SectionHeader(
                title = "Department comparison",
                modifier = Modifier.padding(top = spacing.sm),
            )
        }
        item(key = "dept-chart") {
            AppCard {
                ComparisonBars(
                    entries = analytics.departments.map {
                        ChartEntry(
                            label = it.department,
                            value = it.averageAttendance,
                            detail = "${it.studentCount} students, " +
                                "${it.studentsBelowThreshold} below threshold",
                        )
                    },
                    thresholdPercent = analytics.threshold,
                )
            }
        }

        item(key = "class-header") {
            SectionHeader(
                title = "Class comparison",
                modifier = Modifier.padding(top = spacing.sm),
            )
        }

        items(analytics.departments, key = { it.department }) { department ->
            AppCard {
                Text(
                    text = department.department,
                    style = AppTheme.typography.titleMedium,
                    color = AppTheme.colors.textPrimary,
                )
                ComparisonBars(
                    entries = department.classSections.map {
                        ChartEntry(
                            label = it.name,
                            value = it.averageAttendance,
                            detail = "${it.studentCount} students",
                        )
                    },
                    thresholdPercent = analytics.threshold,
                    modifier = Modifier.padding(top = AppTheme.spacing.md),
                )
            }
        }
    }
}
