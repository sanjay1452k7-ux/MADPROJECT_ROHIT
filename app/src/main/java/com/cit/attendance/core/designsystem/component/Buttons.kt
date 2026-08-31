package com.cit.attendance.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.cit.attendance.core.designsystem.theme.AppGradients
import com.cit.attendance.core.designsystem.theme.AppTheme

/**
 * The primary call to action.
 *
 * Press feedback is a small scale plus a brightness shift (Section 6): enough to
 * confirm the touch landed, not enough to be a performance. Disabled and
 * loading states are visually distinct so a pending action never looks dead.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val shapes = AppTheme.shapes
    val motion = AppTheme.motion

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val active = enabled && !loading

    val scale by animateFloatAsState(
        targetValue = if (pressed && active) 0.98f else 1f,
        animationSpec = tween(motion.duration(motion.instant)),
        label = "buttonScale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .defaultMinSize(minHeight = AppTheme.spacing.minTouchTarget)
            // The glow is cast by the button itself, so it tracks the press
            // scale and disappears with the disabled state.
            .then(
                if (active) {
                    Modifier.shadow(
                        elevation = AppTheme.elevation.overlay,
                        shape = shapes.pill,
                        ambientColor = colors.accent,
                        spotColor = colors.accent,
                    )
                } else {
                    Modifier
                },
            )
            .clip(shapes.pill)
            .background(
                brush = if (active) {
                    AppGradients.horizontal()
                } else {
                    SolidColor(colors.surfaceVariant)
                },
            )
            .appClickable(
                enabled = active,
                interactionSource = interactionSource,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.sm),
            modifier = Modifier.padding(
                horizontal = AppTheme.spacing.xl,
                vertical = AppTheme.spacing.md,
            ),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = colors.onAccent,
                    strokeWidth = 2.dp,
                )
            }
            Text(
                text = text,
                style = typography.label,
                color = if (active) colors.onAccent else colors.textTertiary,
            )
        }
    }
}

/**
 * A quieter action. Carries a border rather than a fill so it never competes
 * with the primary button on the same screen.
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AppTheme.colors
    val shapes = AppTheme.shapes
    val motion = AppTheme.motion

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.outline
            pressed -> colors.accentBright
            else -> colors.outlineStrong
        },
        animationSpec = tween(motion.duration(motion.quick)),
        label = "secondaryBorder",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = AppTheme.spacing.minTouchTarget)
            .clip(shapes.pill)
            .border(1.dp, borderColor, shapes.pill)
            .appClickable(
                enabled = enabled,
                interactionSource = interactionSource,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTheme.typography.label,
            color = if (enabled) colors.textPrimary else colors.textTertiary,
            modifier = Modifier.padding(
                horizontal = AppTheme.spacing.xl,
                vertical = AppTheme.spacing.md,
            ),
        )
    }
}
