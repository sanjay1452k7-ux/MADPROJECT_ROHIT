package com.cit.attendance.feature.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cit.attendance.core.common.Outcome
import com.cit.attendance.core.navigation.Routes
import com.cit.attendance.core.ui.ErrorPresentation
import com.cit.attendance.core.ui.present
import com.cit.attendance.domain.config.InstitutionConfig
import com.cit.attendance.domain.model.AuthenticatedUser
import com.cit.attendance.domain.model.UserRole
import com.cit.attendance.domain.usecase.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Everything the login screen renders. */
data class LoginUiState(
    val role: UserRole,
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: ErrorPresentation? = null,
    val signedInUser: AuthenticatedUser? = null,
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() && password.isNotBlank() && !isSubmitting
}

/**
 * Drives the credential form.
 *
 * The role arrives as a navigation argument rather than as mutable state, so
 * the screen cannot drift out of sync with the role the user picked.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val signIn: SignInUseCase,
    val institutionConfig: InstitutionConfig,
) : ViewModel() {

    private val role: UserRole = savedStateHandle
        .get<String>(Routes.LOGIN_ARG_ROLE)
        ?.let(UserRole::fromStorageKey)
        ?: UserRole.STUDENT

    private val _state = MutableStateFlow(LoginUiState(role = role))
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onEmailChange(value: String) {
        // Clear the previous failure as soon as the user starts correcting it.
        _state.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _state.update { it.copy(password = value, error = null) }
    }

    fun onSubmit() {
        val current = _state.value
        if (!current.canSubmit) return

        _state.update { it.copy(isSubmitting = true, error = null) }

        viewModelScope.launch {
            when (val result = signIn(current.email, current.password, current.role)) {
                is Outcome.Success -> _state.update {
                    it.copy(isSubmitting = false, signedInUser = result.data)
                }

                is Outcome.Failure -> _state.update {
                    it.copy(isSubmitting = false, error = result.error.present())
                }
            }
        }
    }
}
