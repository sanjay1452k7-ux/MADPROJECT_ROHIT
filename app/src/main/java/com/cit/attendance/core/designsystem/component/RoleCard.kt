package com.cit.attendance.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cit.attendance.core.designsystem.theme.AppTheme

/**
 * A selectable role on the sign-in screen (Section 8).
 *
 * Selection is deliberately a considered act rather than a dropdown: the card
 * lifts slightly, the border takes the accent, and the icon tile fills. All
 * three change together so selection is never signalled by colour alone
 * (Section 39).
 */
@Composable
fun RoleCard(
    title: String,
    description: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val shapes = AppTheme.shapes
    val motion = AppTheme.motion

    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.outline,
        animationSpec = tween(motion.duration(motion.quick)),
        label = "roleBorder",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.surfaceElevated else colors.surface,
        animationSpec = tween(motion.duration(motion.quick)),
        label = "roleContainer",
    )
    val iconTileColor by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.surfaceVariant,
        animationSpec = tween(motion.duration(motion.quick)),
        label = "roleIconTile",
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) colors.onAccent else colors.textSecondary,
        animationSpec = tween(motion.duration(motion.quick)),
        label = "roleIconTint",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = tween(
            durationMillis = motion.duration(motion.quick),
            easing = motion.emphasisEasing,
        ),
        label = "roleIconScale",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shapes.large)
            .background(containerColor)
            .border(if (selected) 1.5.dp else 1.dp, borderColor, shapes.large)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
            .appClickable(onClickLabel = title, onClick = onClick)
            .padding(AppTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(shapes.medium)
                .background(iconTileColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .size(20.dp)
                    .scale(iconScale),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = AppTheme.spacing.lg),
        ) {
            Text(
                text = title,
                style = AppTheme.typography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = description,
                style = AppTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = AppTheme.spacing.xxs),
            )
        }
    }
}
