package com.cit.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cit.attendance.domain.config.InstitutionConfig
import com.cit.attendance.domain.usecase.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Application-level state.
 *
 * Watches the session so that a sign-out - from anywhere, including a token
 * being cleared underneath the app - moves the user out of the signed-in area.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    val institutionConfig: InstitutionConfig,
) : ViewModel() {

    /**
     * True once a session has existed and then gone away. It stays false on a
     * cold start so the splash screen, not this flag, decides where to land.
     */
    val isSignedOut: StateFlow<Boolean> = observeSession()
        .map { it == null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = false,
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
