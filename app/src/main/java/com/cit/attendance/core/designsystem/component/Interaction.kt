package com.cit.attendance.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.cit.attendance.core.designsystem.theme.AppTheme

/**
 * The single clickable used across the product (Section 6).
 *
 * Centralising it keeps the ripple tinted with the brand accent everywhere and
 * gives every tappable surface an accessibility label slot, rather than each
 * component inventing its own feedback.
 */
@Composable
fun Modifier.appClickable(
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    onClickLabel: String? = null,
    onClick: () -> Unit,
): Modifier {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val accent = AppTheme.colors.accentBright
    return this.clickable(
        interactionSource = source,
        indication = ripple(color = accent),
        enabled = enabled,
        onClickLabel = onClickLabel,
        onClick = onClick,
    )
}
