package com.cit.attendance.feature.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cit.attendance.core.common.Outcome
import com.cit.attendance.core.ui.ErrorPresentation
import com.cit.attendance.core.ui.present
import com.cit.attendance.domain.model.ActiveAttendanceSession
import com.cit.attendance.domain.model.AttendanceQrToken
import com.cit.attendance.domain.repository.AttendanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil

/** Everything the QR screen renders (Section 19). */
data class StudentQrUiState(
    val isLoadingSession: Boolean = true,
    val session: ActiveAttendanceSession? = null,
    val token: AttendanceQrToken? = null,
    val secondsRemaining: Int = 0,
    val isMinting: Boolean = false,
    val error: ErrorPresentation? = null,
) {
    /** True once the teacher has recorded this student for the session. */
    val isMarked: Boolean get() = session?.alreadyMarked == true

    val progress: Float
        get() {
            val validFor = token?.validForSeconds ?: return 0f
            if (validFor <= 0) return 0f
            return (secondsRemaining.toFloat() / validFor).coerceIn(0f, 1f)
        }
}

/**
 * Drives the student's QR code.
 *
 * The app never generates a token: each refresh is a request to the server,
 * which is the only party able to mint one (Section 19). The countdown exists
 * so the student can see the code is short-lived; expiry is enforced by the
 * server regardless of what this timer shows.
 */
@HiltViewModel
class StudentQrViewModel @Inject constructor(
    private val attendance: AttendanceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StudentQrUiState())
    val state: StateFlow<StudentQrUiState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    init {
        loadSession()
    }

    fun loadSession() {
        _state.update { it.copy(isLoadingSession = true, error = null) }
        viewModelScope.launch {
            when (val result = attendance.activeSessionForStudent()) {
                is Outcome.Success -> _state.update {
                    it.copy(isLoadingSession = false, session = result.data)
                }

                is Outcome.Failure -> _state.update {
                    it.copy(isLoadingSession = false, error = result.error.present())
                }
            }
        }
    }

    /** Requests a token and starts the countdown that will refresh it. */
    fun generateToken() {
        val sessionId = _state.value.session?.id ?: return
        if (_state.value.isMinting) return

        _state.update { it.copy(isMinting = true, error = null) }

        viewModelScope.launch {
            when (val result = attendance.mintQrToken(sessionId)) {
                is Outcome.Success -> {
                    _state.update {
                        it.copy(isMinting = false, token = result.data, error = null)
                    }
                    startCountdown()
                }

                is Outcome.Failure -> {
                    stopCountdown()
                    _state.update {
                        it.copy(
                            isMinting = false,
                            token = null,
                            error = result.error.present(),
                        )
                    }
                    // The most likely reason a mint is refused is that the
                    // teacher has just recorded this student, so re-read the
                    // session to show that instead of a bare error.
                    refreshSessionQuietly()
                }
            }
        }
    }

    /** Stops refreshing when the screen is not visible, to save battery. */
    fun onScreenHidden() {
        stopCountdown()
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (true) {
                val token = _state.value.token ?: break
                val remainingMillis = token.expiresAtEpochMillis - System.currentTimeMillis()
                val remaining = ceil(remainingMillis / 1000.0).toInt().coerceAtLeast(0)

                _state.update { it.copy(secondsRemaining = remaining) }

                if (remaining <= 0) {
                    // Expired: ask for the next one automatically so the student
                    // never has to tap again mid-queue.
                    generateToken()
                    break
                }
                delay(COUNTDOWN_TICK_MS)
            }
        }
    }

    private fun stopCountdown() {
        countdownJob?.cancel()
        countdownJob = null
    }

    private suspend fun refreshSessionQuietly() {
        val result = attendance.activeSessionForStudent()
        if (result is Outcome.Success) {
            _state.update { it.copy(session = result.data) }
        }
    }

    override fun onCleared() {
        stopCountdown()
    }

    private companion object {
        const val COUNTDOWN_TICK_MS = 250L
    }
}
