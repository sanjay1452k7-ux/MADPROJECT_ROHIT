package com.cit.attendance.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.cit.attendance.core.designsystem.theme.AppGradients
import com.cit.attendance.core.designsystem.theme.AppTheme

/**
 * A segmented control for switching between related views within one tab.
 *
 * Used so that adding analytics and reports did not push the bottom bar past
 * five destinations: closely related screens live together behind a segment
 * rather than each claiming a tab of its own (Section 5).
 */
@Composable
fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.pill)
            .background(colors.surface)
            .padding(AppTheme.spacing.xxs),
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex

            val background by animateColorAsState(
                targetValue = if (isSelected) Color.Transparent else colors.surface,
                animationSpec = tween(AppTheme.motion.duration(AppTheme.motion.quick)),
                label = "segmentBackground",
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) colors.onAccent else colors.textSecondary,
                animationSpec = tween(AppTheme.motion.duration(AppTheme.motion.quick)),
                label = "segmentText",
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        role = Role.Tab
                        selected = isSelected
                    }
                    .clip(AppTheme.shapes.pill)
                    .background(if (isSelected) AppGradients.horizontal() else SolidColor(background))
                    .appClickable(onClickLabel = label) { onSelect(index) }
                    .padding(vertical = AppTheme.spacing.sm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = AppTheme.typography.label,
                    color = textColor,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
