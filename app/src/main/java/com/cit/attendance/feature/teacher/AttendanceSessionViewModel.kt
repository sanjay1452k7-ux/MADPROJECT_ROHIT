package com.cit.attendance.feature.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cit.attendance.core.common.Outcome
import com.cit.attendance.core.ui.ErrorPresentation
import com.cit.attendance.core.ui.present
import com.cit.attendance.domain.model.AttendanceMarkResult
import com.cit.attendance.domain.model.AttendanceStatus
import com.cit.attendance.domain.model.LiveAttendanceSession
import com.cit.attendance.domain.model.VerifiedStudent
import com.cit.attendance.domain.repository.AttendanceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Where the teacher is in the scan-verify-confirm workflow (Section 17). */
sealed interface ScanStage {
    /** Camera is live, waiting for a code. */
    data object Scanning : ScanStage

    /** A code was read and is being checked by the server. */
    data object Validating : ScanStage

    /** The student is on screen awaiting the teacher's visual check. */
    data class AwaitingConfirmation(val student: VerifiedStudent) : ScanStage

    /** Attendance was recorded (Section 23). */
    data class Recorded(val result: AttendanceMarkResult) : ScanStage

    /** The code was refused; the reason is already human wording. */
    data class Rejected(val error: ErrorPresentation) : ScanStage
}

data class AttendanceSessionUiState(
    val isStarting: Boolean = false,
    val session: LiveAttendanceSession? = null,
    val stage: ScanStage = ScanStage.Scanning,
    val isSubmitting: Boolean = false,
    val error: ErrorPresentation? = null,
)

/**
 * Runs a live attendance session.
 *
 * Scanning and confirming are deliberately separate steps: a scan only
 * identifies the student, and nothing is recorded until the teacher, having
 * looked at the person, confirms (Sections 17 and 22).
 */
@HiltViewModel
class AttendanceSessionViewModel @Inject constructor(
    private val attendance: AttendanceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AttendanceSessionUiState())
    val state: StateFlow<AttendanceSessionUiState> = _state.asStateFlow()

    /** True while a scan is being processed, so the camera stops feeding codes. */
    private var scanLocked = false

    fun startSession(subjectId: String, classSectionId: String) {
        _state.update { it.copy(isStarting = true, error = null) }
        viewModelScope.launch {
            when (val result = attendance.startSession(subjectId, classSectionId)) {
                is Outcome.Success -> _state.update {
                    it.copy(isStarting = false, session = result.data, stage = ScanStage.Scanning)
                }

                is Outcome.Failure -> _state.update {
                    it.copy(isStarting = false, error = result.error.present())
                }
            }
        }
    }

    fun refreshSession() {
        val sessionId = _state.value.session?.id ?: return
        viewModelScope.launch {
            val result = attendance.sessionDetail(sessionId)
            if (result is Outcome.Success) {
                _state.update { it.copy(session = result.data) }
            }
        }
    }

    /**
     * Called by the scanner for every decoded code.
     *
     * Guarded so a camera producing thirty frames a second cannot fire thirty
     * verification requests for the same code.
     */
    fun onCodeScanned(rawValue: String) {
        val sessionId = _state.value.session?.id ?: return
        if (scanLocked) return
        scanLocked = true

        _state.update { it.copy(stage = ScanStage.Validating) }

        viewModelScope.launch {
            when (val result = attendance.verifyToken(sessionId, rawValue)) {
                is Outcome.Success -> _state.update {
                    it.copy(stage = ScanStage.AwaitingConfirmation(result.data))
                }

                is Outcome.Failure -> _state.update {
                    it.copy(stage = ScanStage.Rejected(result.error.present()))
                }
            }
        }
    }

    /** The teacher has looked at the student and confirmed (Section 22). */
    fun confirmPresent() {
        val sessionId = _state.value.session?.id ?: return
        val stage = _state.value.stage
        if (stage !is ScanStage.AwaitingConfirmation) return

        _state.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            when (val result = attendance.markPresent(sessionId, stage.student.verificationId)) {
                is Outcome.Success -> {
                    _state.update {
                        it.copy(isSubmitting = false, stage = ScanStage.Recorded(result.data))
                    }
                    refreshSession()
                }

                is Outcome.Failure -> _state.update {
                    it.copy(isSubmitting = false, stage = ScanStage.Rejected(result.error.present()))
                }
            }
        }
    }

    /** Returns to the camera, whether the teacher confirmed or cancelled. */
    fun resumeScanning() {
        scanLocked = false
        _state.update { it.copy(stage = ScanStage.Scanning, error = null) }
    }

    /** Manual fallback for a student whose device or camera will not cooperate. */
    fun markManually(studentId: String, status: AttendanceStatus, reason: String?) {
        val sessionId = _state.value.session?.id ?: return
        _state.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            when (val result = attendance.markManually(sessionId, studentId, status, reason)) {
                is Outcome.Success -> {
                    _state.update { it.copy(isSubmitting = false) }
                    refreshSession()
                }

                is Outcome.Failure -> _state.update {
                    it.copy(isSubmitting = false, error = result.error.present())
                }
            }
        }
    }

    fun closeSession(onClosed: () -> Unit) {
        val sessionId = _state.value.session?.id ?: return
        _state.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            when (val result = attendance.closeSession(sessionId)) {
                is Outcome.Success -> {
                    _state.update { it.copy(isSubmitting = false, session = result.data) }
                    onClosed()
                }

                is Outcome.Failure -> _state.update {
                    it.copy(isSubmitting = false, error = result.error.present())
                }
            }
        }
    }
}
