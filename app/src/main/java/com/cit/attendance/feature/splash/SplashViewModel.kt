package com.cit.attendance.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cit.attendance.core.common.dataOrNull
import com.cit.attendance.domain.model.AuthenticatedUser
import com.cit.attendance.domain.usecase.RestoreSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What the splash screen is waiting on. */
sealed interface SplashState {
    data object Restoring : SplashState

    /** [user] is null when there is no valid session to resume. */
    data class Ready(val user: AuthenticatedUser?) : SplashState
}

/**
 * Restores any persisted session while the brand animation plays.
 *
 * The restore and the minimum on-screen time run concurrently, so the splash
 * lasts exactly as long as the brand moment needs and never adds waiting on top
 * of it.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val restoreSession: RestoreSessionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<SplashState>(SplashState.Restoring)
    val state: StateFlow<SplashState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val restored = async { restoreSession().dataOrNull() }
            val minimumDwell = async { delay(BRAND_DWELL_MS) }

            val user = restored.await()
            minimumDwell.await()
            _state.value = SplashState.Ready(user)
        }
    }

    private companion object {
        /** Long enough for the mark to register, short enough not to annoy. */
        const val BRAND_DWELL_MS = 1400L
    }
}
