package com.cit.attendance.feature.auth

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CoPresent
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.cit.attendance.core.designsystem.branding.ButterflyMark
import com.cit.attendance.core.designsystem.branding.CitBrandLockup
import com.cit.attendance.core.designsystem.component.PrimaryButton
import com.cit.attendance.core.designsystem.component.RoleCard
import com.cit.attendance.core.designsystem.theme.AppGradients
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.domain.model.UserRole

/** Copy for each selectable role (Section 8). */
private data class RolePresentation(
    val role: UserRole,
    val title: String,
    val description: String,
    val icon: ImageVector,
)

private val rolePresentations = listOf(
    RolePresentation(
        role = UserRole.STUDENT,
        title = "Student",
        description = "Access your attendance and academic activity",
        icon = Icons.Outlined.School,
    ),
    RolePresentation(
        role = UserRole.TEACHER,
        title = "Teacher",
        description = "Manage classes and record attendance",
        icon = Icons.Outlined.CoPresent,
    ),
    RolePresentation(
        role = UserRole.ADMIN,
        title = "Admin",
        description = "Manage institution-wide attendance",
        icon = Icons.Outlined.AdminPanelSettings,
    ),
)

/**
 * "Who are you?" - role selection before any credential form (Section 8).
 *
 * The butterfly drifts a few points toward whichever card is chosen and grows
 * slightly brighter. The movement is small on purpose: it acknowledges the
 * choice without turning into a screensaver (Section 7).
 */
@Composable
fun RoleSelectionScreen(
    institutionName: String,
    onContinue: (UserRole) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing
    val motion = AppTheme.motion

    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    val selectedIndex = rolePresentations.indexOfFirst { it.role == selectedRole }

    val butterflyEmphasis by animateFloatAsState(
        targetValue = if (selectedRole == null) 0f else 1f,
        animationSpec = tween(motion.duration(motion.standard)),
        label = "butterflyEmphasis",
    )

    // A few points of travel toward the chosen card - no more.
    val butterflyOffset by animateDpAsState(
        targetValue = when (selectedIndex) {
            0 -> (-10).dp
            1 -> 0.dp
            2 -> 10.dp
            else -> 0.dp
        },
        animationSpec = tween(
            durationMillis = motion.duration(motion.standard),
            easing = motion.emphasisEasing,
        ),
        label = "butterflyDrift",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppGradients.canvas()),
    ) {
        // Ambient purple wash, strongest behind the brand mark and fading out
        // well before the content begins.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            colors.accent.copy(alpha = 0.10f + 0.05f * butterflyEmphasis),
                            colors.background,
                            colors.background,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenGutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ButterflyMark(
                modifier = Modifier
                    .padding(top = spacing.xxxl)
                    .offset { IntOffset(x = butterflyOffset.roundToPx(), y = 0) }
                    .size(132.dp),
                emphasis = butterflyEmphasis,
            )

            CitBrandLockup(
                institutionName = institutionName,
                modifier = Modifier.padding(top = spacing.lg),
                emblemSize = 44.dp,
            )

            Text(
                text = "Welcome",
                style = AppTheme.typography.displayLarge,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = spacing.xxl),
            )
            Text(
                text = "Who are you?",
                style = AppTheme.typography.bodyLarge,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.xs),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xl),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                rolePresentations.forEach { presentation ->
                    RoleCard(
                        title = presentation.title,
                        description = presentation.description,
                        icon = presentation.icon,
                        selected = presentation.role == selectedRole,
                        onClick = { selectedRole = presentation.role },
                    )
                }
            }

            PrimaryButton(
                text = "Continue",
                onClick = { selectedRole?.let(onContinue) },
                enabled = selectedRole != null,
                modifier = Modifier.padding(top = spacing.xl, bottom = spacing.xxl),
            )
        }
    }
}
