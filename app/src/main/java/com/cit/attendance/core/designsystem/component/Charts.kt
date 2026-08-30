package com.cit.attendance.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.core.ui.formatPercentage
import kotlin.math.abs
import kotlin.math.roundToInt

/** One value on a chart. */
data class ChartEntry(
    val label: String,
    val value: Float,
    /** Optional detail shown when the point is selected (Section 29). */
    val detail: String? = null,
)

/**
 * A line chart for attendance over time (Section 28).
 *
 * Tapping a point selects it and shows its exact value, which is the whole
 * reason the chart is interactive: a trend line that cannot be interrogated is
 * decoration, not information (Section 29).
 */
@Composable
fun TrendChart(
    entries: List<ChartEntry>,
    modifier: Modifier = Modifier,
    thresholdPercent: Int? = null,
) {
    val colors = AppTheme.colors
    val motion = AppTheme.motion

    if (entries.isEmpty()) {
        ChartPlaceholder(message = "Not enough data to plot a trend yet.", modifier = modifier)
        return
    }

    var selectedIndex by remember(entries) { mutableIntStateOf(entries.lastIndex) }
    val reveal by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(motion.duration(motion.deliberate)),
        label = "trendReveal",
    )

    // A fixed 0-100 scale: attendance is a percentage, and rescaling to the
    // data's own range would make a drop from 95 to 90 look catastrophic.
    val minValue = 0f
    val maxValue = 100f

    val selected = entries[selectedIndex]

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text(
                    text = selected.label,
                    style = AppTheme.typography.overline,
                    color = colors.textTertiary,
                )
                Text(
                    text = formatPercentage(selected.value),
                    style = AppTheme.typography.metricSmall,
                    color = colors.textPrimary,
                )
            }
            if (selected.detail != null) {
                Text(
                    text = selected.detail,
                    style = AppTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(top = AppTheme.spacing.md)
                .semantics {
                    contentDescription = "Attendance trend chart with ${entries.size} points"
                }
                .pointerInput(entries) {
                    detectTapGestures { tap ->
                        // Snap to the nearest point rather than requiring a
                        // precise hit on a 4dp dot.
                        val step = if (entries.size <= 1) size.width.toFloat()
                        else size.width / (entries.size - 1f)
                        selectedIndex = (tap.x / step).roundToInt()
                            .coerceIn(0, entries.lastIndex)
                    }
                },
        ) {
            val step = if (entries.size <= 1) size.width else size.width / (entries.size - 1)
            fun yFor(value: Float): Float {
                val fraction = ((value - minValue) / (maxValue - minValue)).coerceIn(0f, 1f)
                return size.height - fraction * size.height
            }

            if (thresholdPercent != null) {
                val y = yFor(thresholdPercent.toFloat())
                drawLine(
                    color = colors.outlineStrong,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(8f, 8f),
                    ),
                )
            }

            val points = entries.mapIndexed { index, entry ->
                Offset(index * step, yFor(entry.value))
            }

            val visible = (points.size * reveal).roundToInt().coerceAtLeast(1)
            val shown = points.take(visible)

            if (shown.size > 1) {
                val line = Path().apply {
                    moveTo(shown.first().x, shown.first().y)
                    shown.drop(1).forEach { lineTo(it.x, it.y) }
                }
                // Fill under the line, fading out, so the shape reads at a glance.
                val fill = Path().apply {
                    addPath(line)
                    lineTo(shown.last().x, size.height)
                    lineTo(shown.first().x, size.height)
                    close()
                }
                drawPath(
                    path = fill,
                    brush = Brush.verticalGradient(
                        listOf(colors.accent.copy(alpha = 0.28f), colors.accent.copy(alpha = 0f)),
                    ),
                )
                drawPath(
                    path = line,
                    color = colors.accentBright,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round),
                )
            }

            shown.forEachIndexed { index, point ->
                val isSelected = index == selectedIndex
                drawCircle(
                    color = if (isSelected) colors.accentBright else colors.surfaceVariant,
                    radius = if (isSelected) 6f else 3.5f,
                    center = point,
                )
                if (isSelected) {
                    drawLine(
                        color = colors.accentBright.copy(alpha = 0.35f),
                        start = Offset(point.x, 0f),
                        end = Offset(point.x, size.height),
                        strokeWidth = 1f,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppTheme.spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = entries.first().label,
                style = AppTheme.typography.overline,
                color = colors.textTertiary,
            )
            Text(
                text = entries.last().label,
                style = AppTheme.typography.overline,
                color = colors.textTertiary,
            )
        }
    }
}

/**
 * A horizontal bar chart for comparing subjects or departments.
 *
 * Horizontal because the labels are subject names: vertical bars would force
 * them to be rotated or truncated.
 */
@Composable
fun ComparisonBars(
    entries: List<ChartEntry>,
    modifier: Modifier = Modifier,
    thresholdPercent: Int? = null,
    onEntryClick: ((ChartEntry) -> Unit)? = null,
) {
    val colors = AppTheme.colors

    if (entries.isEmpty()) {
        ChartPlaceholder(message = "Nothing to compare yet.", modifier = modifier)
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        entries.forEach { entry ->
            val belowThreshold = thresholdPercent != null && entry.value < thresholdPercent
            Column(
                modifier = if (onEntryClick != null) {
                    Modifier.appClickable(onClickLabel = entry.label) { onEntryClick(entry) }
                } else {
                    Modifier
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = entry.label,
                        style = AppTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(0.62f),
                    )
                    Text(
                        text = formatPercentage(entry.value),
                        style = AppTheme.typography.label,
                        color = if (belowThreshold) colors.warning else colors.textPrimary,
                    )
                }
                ProgressBar(
                    fraction = entry.value / 100f,
                    belowThreshold = belowThreshold,
                    modifier = Modifier.padding(top = AppTheme.spacing.xs),
                )
                if (entry.detail != null) {
                    Text(
                        text = entry.detail,
                        style = AppTheme.typography.bodyMedium,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(top = AppTheme.spacing.xxs),
                    )
                }
            }
        }
    }
}

/**
 * A vertical bar chart for counts rather than percentages, such as absences per
 * day. Tapping a bar reports its exact value.
 */
@Composable
fun CountBars(
    entries: List<ChartEntry>,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors

    if (entries.isEmpty()) {
        ChartPlaceholder(message = "No sessions recorded in this period.", modifier = modifier)
        return
    }

    var selectedIndex by remember(entries) { mutableIntStateOf(entries.lastIndex) }
    val peak = entries.maxOf { it.value }.coerceAtLeast(1f)
    val selected = entries[selectedIndex]

    Column(modifier = modifier) {
        Text(
            text = "${selected.value.roundToInt()} absent - ${selected.label}",
            style = AppTheme.typography.label,
            color = colors.textPrimary,
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .padding(top = AppTheme.spacing.sm)
                .semantics {
                    contentDescription = "Absences per day, ${entries.size} days"
                }
                .pointerInput(entries) {
                    detectTapGestures { tap ->
                        val slot = size.width / entries.size.toFloat()
                        selectedIndex = (tap.x / slot).toInt().coerceIn(0, entries.lastIndex)
                    }
                },
        ) {
            val slot = size.width / entries.size
            val barWidth = slot * 0.55f

            entries.forEachIndexed { index, entry ->
                val height = (entry.value / peak) * size.height
                val left = index * slot + (slot - barWidth) / 2f
                drawRoundRect(
                    color = if (index == selectedIndex) colors.accentBright else colors.surfaceVariant,
                    topLeft = Offset(left, size.height - height),
                    size = androidx.compose.ui.geometry.Size(barWidth, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f),
                )
            }
        }
    }
}

/** Shown when a chart has nothing worth drawing (Section 37). */
@Composable
private fun ChartPlaceholder(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

/** Rounds a percentage difference for display, keeping the sign. */
internal fun signedDifference(from: Float, to: Float): String {
    val delta = to - from
    val rounded = (abs(delta) * 10).roundToInt() / 10f
    if (rounded == 0f) return "no change"
    return if (delta > 0) "up $rounded points" else "down $rounded points"
}
