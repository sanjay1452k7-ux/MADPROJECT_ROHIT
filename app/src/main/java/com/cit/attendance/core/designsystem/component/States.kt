package com.cit.attendance.core.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cit.attendance.core.designsystem.theme.AppTheme

/**
 * A single shimmering placeholder block.
 *
 * Skeletons are preferred over spinners because they preserve layout and tell
 * the user what is arriving (Section 38). The shimmer stops entirely when the
 * user has asked for reduced motion.
 */
@Composable
fun SkeletonBlock(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
) {
    val colors = AppTheme.colors
    val motion = AppTheme.motion

    val highlight = if (motion.reduceMotion) {
        0.5f
    } else {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val animated by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "skeletonPhase",
        )
        animated
    }

    Box(
        modifier = modifier
            .height(height)
            .clip(AppTheme.shapes.small)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        colors.surfaceVariant,
                        colors.surfaceElevated.copy(alpha = 0.4f + 0.6f * highlight),
                        colors.surfaceVariant,
                    ),
                ),
            ),
    )
}

/** A card-shaped skeleton used while a dashboard section loads. */
@Composable
fun LoadingCard(modifier: Modifier = Modifier, lines: Int = 3) {
    AppCard(modifier = modifier.semantics { contentDescription = "Loading" }) {
        Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md)) {
            SkeletonBlock(modifier = Modifier.fillMaxWidth(0.45f), height = 12.dp)
            repeat(lines) { index ->
                SkeletonBlock(
                    modifier = Modifier.fillMaxWidth(if (index == lines - 1) 0.7f else 1f),
                    height = 14.dp,
                )
            }
        }
    }
}

/** Shown when a request succeeded but there is genuinely nothing to display. */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    StateBlock(
        icon = icon,
        iconTint = AppTheme.colors.textTertiary,
        title = title,
        message = message,
        modifier = modifier,
    )
}

/**
 * Shown when a request failed.
 *
 * [message] must already be human wording - the error mapper is responsible for
 * that, so no technical detail can reach this component (Section 37).
 */
@Composable
fun ErrorState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    StateBlock(
        icon = icon,
        iconTint = AppTheme.colors.error,
        title = title,
        message = message,
        modifier = modifier,
    ) {
        if (onRetry != null) {
            SecondaryButton(text = "Try again", onClick = onRetry)
        }
    }
}

@Composable
private fun StateBlock(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AppTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.md),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = title,
            style = AppTheme.typography.titleMedium,
            color = AppTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}
