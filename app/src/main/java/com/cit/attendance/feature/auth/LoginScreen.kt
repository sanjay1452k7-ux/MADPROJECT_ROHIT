package com.cit.attendance.feature.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cit.attendance.core.designsystem.branding.ButterflyMark
import com.cit.attendance.core.designsystem.component.AppCard
import com.cit.attendance.core.designsystem.component.PrimaryButton
import com.cit.attendance.core.designsystem.theme.AppTheme
import com.cit.attendance.domain.model.AuthenticatedUser
import com.cit.attendance.domain.model.UserRole

/**
 * The credential form for the role chosen on the previous screen (Section 9).
 *
 * The role is restated at the top so the user always knows which door they are
 * walking through, and failures appear inline rather than as a toast that can
 * be missed.
 */
@Composable
fun LoginScreen(
    onSignedIn: (AuthenticatedUser) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = AppTheme.colors
    val spacing = AppTheme.spacing

    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.signedInUser) {
        state.signedInUser?.let(onSignedIn)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = spacing.screenGutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ButterflyMark(
                modifier = Modifier
                    .padding(top = spacing.xxl)
                    .size(96.dp),
                emphasis = 0.6f,
            )

            Text(
                text = roleTitle(state.role),
                style = AppTheme.typography.overline,
                color = colors.accentBright,
                modifier = Modifier.padding(top = spacing.lg),
            )
            Text(
                text = "Sign in",
                style = AppTheme.typography.displayLarge,
                color = colors.textPrimary,
                modifier = Modifier.padding(top = spacing.xs),
            )
            Text(
                text = "Use your @${viewModel.institutionConfig.emailDomain} account.",
                style = AppTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = spacing.xs),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xl),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("Institutional email") },
                    singleLine = true,
                    enabled = !state.isSubmitting,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    colors = fieldColors(),
                    shape = AppTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = state.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !state.isSubmitting,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = if (passwordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                },
                                tint = colors.textSecondary,
                            )
                        }
                    },
                    colors = fieldColors(),
                    shape = AppTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            AnimatedVisibility(
                visible = state.error != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                val error = state.error
                if (error != null) {
                    AppCard(modifier = Modifier.padding(top = spacing.md)) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = colors.error,
                                modifier = Modifier.size(18.dp),
                            )
                            Column(modifier = Modifier.padding(start = spacing.md)) {
                                Text(
                                    text = error.title,
                                    style = AppTheme.typography.titleMedium,
                                    color = colors.textPrimary,
                                )
                                Text(
                                    text = error.message,
                                    style = AppTheme.typography.bodyMedium,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(top = spacing.xxs),
                                )
                            }
                        }
                    }
                }
            }

            PrimaryButton(
                text = "Sign in",
                onClick = viewModel::onSubmit,
                enabled = state.canSubmit,
                loading = state.isSubmitting,
                modifier = Modifier.padding(top = spacing.xl, bottom = spacing.xxl),
            )
        }
    }
}

private fun roleTitle(role: UserRole): String = when (role) {
    UserRole.STUDENT -> "Student access"
    UserRole.TEACHER -> "Teacher access"
    UserRole.ADMIN -> "Administrator access"
    UserRole.PARENT -> "Parent access"
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AppTheme.colors.accent,
    unfocusedBorderColor = AppTheme.colors.outline,
    focusedLabelColor = AppTheme.colors.accentBright,
    unfocusedLabelColor = AppTheme.colors.textTertiary,
    focusedTextColor = AppTheme.colors.textPrimary,
    unfocusedTextColor = AppTheme.colors.textPrimary,
    cursorColor = AppTheme.colors.accentBright,
    focusedContainerColor = AppTheme.colors.surface,
    unfocusedContainerColor = AppTheme.colors.surface,
    disabledContainerColor = AppTheme.colors.surface,
    disabledBorderColor = AppTheme.colors.outline,
    disabledTextColor = AppTheme.colors.textTertiary,
    disabledLabelColor = AppTheme.colors.textTertiary,
)
