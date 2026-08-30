package com.cit.attendance.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cit.attendance.core.designsystem.theme.AppTheme

/**
 * The product top bar.
 *
 * Deliberately plain: a title, an optional subtitle and at most one action.
 * Screens carry their own hierarchy, so the bar stays out of the way.
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = AppTheme.spacing.screenGutter,
                vertical = AppTheme.spacing.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (subtitle != null) {
                Text(
                    text = subtitle.uppercase(),
                    style = AppTheme.typography.overline,
                    color = AppTheme.colors.textTertiary,
                )
            }
            Text(
                text = title,
                style = AppTheme.typography.titleLarge,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        action?.invoke()
    }
}

/** One destination in the bottom bar. */
data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0,
)

/**
 * Bottom navigation (Sections 14 to 16).
 *
 * The selected item is marked by an accent icon, an accent label and a small
 * indicator - never by colour on its own (Section 39). Each item is a selectable
 * for screen readers so its selected state is announced.
 */
@Composable
fun AppBottomBar(
    items: List<BottomNavItem>,
    selectedRoute: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.outline),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = AppTheme.spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                BottomNavCell(
                    item = item,
                    selected = item.route == selectedRoute,
                    onClick = { onSelect(item.route) },
                )
            }
        }
    }
}

@Composable
private fun BottomNavCell(
    item: BottomNavItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val motion = AppTheme.motion

    val tint by animateColorAsState(
        targetValue = if (selected) colors.accentBright else colors.textTertiary,
        animationSpec = tween(motion.duration(motion.quick)),
        label = "navTint",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = tween(motion.duration(motion.quick)),
        label = "navScale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xxs),
        modifier = Modifier
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .clip(AppTheme.shapes.medium)
            .appClickable(onClickLabel = item.label, onClick = onClick)
            .padding(horizontal = AppTheme.spacing.md, vertical = AppTheme.spacing.sm),
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(22.dp)
                    .scale(iconScale),
            )
            if (item.badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(colors.accentBright),
                )
            }
        }
        Text(
            text = item.label,
            style = AppTheme.typography.overline,
            color = tint,
        )
        Box(
            modifier = Modifier
                .width(if (selected) 14.dp else 0.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(if (selected) colors.accentBright else colors.surface),
        )
    }
}
