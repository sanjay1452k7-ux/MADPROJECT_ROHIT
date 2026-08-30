package com.cit.attendance.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.cit.attendance.core.designsystem.theme.AppTheme

/**
 * The base container every card in the product is built from.
 *
 * Depth comes from a hairline border and a slight tonal lift rather than a drop
 * shadow, which reads as grey mud on a near-black canvas (Section 5). When
 * [onClick] is supplied the card gains press feedback and behaves as a control.
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    selected: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    val shapes = AppTheme.shapes
    val motion = AppTheme.motion

    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val borderColor by animateColorAsState(
        targetValue = when {
            selected -> colors.accent
            pressed && onClick != null -> colors.outlineStrong
            else -> colors.outline
        },
        animationSpec = tween(motion.duration(motion.quick)),
        label = "cardBorder",
    )

    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.surfaceElevated else colors.surface,
        animationSpec = tween(motion.duration(motion.quick)),
        label = "cardContainer",
    )

    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.995f else 1f,
        animationSpec = tween(motion.duration(motion.instant)),
        label = "cardScale",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(shapes.large)
            .background(containerColor)
            .border(1.dp, borderColor, shapes.large)
            .then(
                if (onClick != null) {
                    Modifier.appClickable(
                        interactionSource = interactionSource,
                        onClickLabel = onClickLabel,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(AppTheme.spacing.lg),
        content = content,
    )
}

/**
 * A titled group of content. The optional trailing slot holds a single action
 * such as "View all", keeping section headers consistent across screens.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title.uppercase(),
            style = AppTheme.typography.overline,
            color = AppTheme.colors.textTertiary,
        )
        trailing?.invoke()
    }
}
