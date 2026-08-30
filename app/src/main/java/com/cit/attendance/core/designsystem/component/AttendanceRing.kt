package com.cit.attendance.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.domain.model.AttendanceStanding
import java.util.Locale

/**
 * The headline attendance figure, drawn as a progress ring.
 *
 * The sweep animates from zero on first composition so the number arrives with
 * a sense of being measured rather than simply appearing (Section 6). The ring
 * colour follows the standing, and the percentage is always written out, so the
 * reading never depends on colour alone (Section 39).
 */
@Composable
fun AttendanceRing(
    percentage: Float,
    standing: AttendanceStanding,
    modifier: Modifier = Modifier,
    diameter: Dp = 168.dp,
) {
    val colors = AppTheme.colors
    val motion = AppTheme.motion

    val ringColor = when (standing) {
        AttendanceStanding.GOOD -> colors.accentBright
        AttendanceStanding.WARNING -> colors.warning
        AttendanceStanding.HIGH_RISK -> colors.error
    }

    val sweep by animateFloatAsState(
        targetValue = percentage.coerceIn(0f, 100f) / 100f,
        animationSpec = tween(
            durationMillis = motion.duration(motion.deliberate),
            easing = motion.standardEasing,
        ),
        label = "attendanceSweep",
    )

    val formatted = String.format(Locale.getDefault(), "%.1f%%", percentage)
    val description = "Overall attendance $formatted, status ${standing.label}"

    Box(
        modifier = modifier
            .size(diameter)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val strokeWidth = size.minDimension * 0.075f
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)

            // Track
            drawArc(
                color = colors.surfaceVariant,
                startAngle = START_ANGLE,
                sweepAngle = FULL_SWEEP,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // Progress
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(colors.accent, ringColor, colors.accent),
                ),
                startAngle = START_ANGLE,
                sweepAngle = FULL_SWEEP * sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }

        Box(contentAlignment = Alignment.Center) {
            Text(
                text = formatted,
                style = AppTheme.typography.metricLarge,
                color = colors.textPrimary,
            )
        }
    }
}

private const val START_ANGLE = 135f
private const val FULL_SWEEP = 270f
