package com.cit.attendance.feature.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cit.attendance.core.common.AppError
import com.cit.attendance.core.common.Outcome
import com.cit.attendance.core.ui.UiState
import com.cit.attendance.core.ui.present
import com.cit.attendance.domain.model.AttendancePolicy
import com.cit.attendance.domain.model.AttendanceRecord
import com.cit.attendance.domain.model.NotificationItem
import com.cit.attendance.domain.model.Student
import com.cit.attendance.domain.model.StudentDashboard
import com.cit.attendance.domain.usecase.GetAttendanceHistoryUseCase
import com.cit.attendance.domain.usecase.GetNotificationsUseCase
import com.cit.attendance.domain.usecase.GetStudentDashboardUseCase
import com.cit.attendance.domain.usecase.GetStudentProfileUseCase
import com.cit.attendance.domain.usecase.MarkNotificationReadUseCase
import com.cit.attendance.domain.usecase.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Loads whatever the signed-in student's screen needs.
 *
 * Each view model resolves the student id from the session rather than
 * receiving it as an argument, so a screen can never be pointed at another
 * student's data by constructing a route by hand.
 */
@HiltViewModel
class StudentHomeViewModel @Inject constructor(
    private val observeSession: ObserveSessionUseCase,
    private val getDashboard: GetStudentDashboardUseCase,
    val policy: AttendancePolicy,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<StudentDashboard>>(UiState.Loading)
    val state: StateFlow<UiState<StudentDashboard>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val studentId = observeSession().filterNotNull().first().id
            _state.value = when (val result = getDashboard(studentId)) {
                is Outcome.Success -> UiState.Content(result.data)
                is Outcome.Failure -> UiState.Failed(result.error.present())
            }
        }
    }
}

@HiltViewModel
class StudentHistoryViewModel @Inject constructor(
    private val observeSession: ObserveSessionUseCase,
    private val getHistory: GetAttendanceHistoryUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<AttendanceRecord>>>(UiState.Loading)
    val state: StateFlow<UiState<List<AttendanceRecord>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val studentId = observeSession().filterNotNull().first().id
            _state.value = when (val result = getHistory(studentId)) {
                is Outcome.Success -> UiState.Content(result.data)
                is Outcome.Failure -> UiState.Failed(result.error.present())
            }
        }
    }
}

@HiltViewModel
class StudentNotificationsViewModel @Inject constructor(
    private val observeSession: ObserveSessionUseCase,
    private val getNotifications: GetNotificationsUseCase,
    private val markRead: MarkNotificationReadUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<NotificationItem>>>(UiState.Loading)
    val state: StateFlow<UiState<List<NotificationItem>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val userId = observeSession().filterNotNull().first().id
            _state.value = when (val result = getNotifications(userId)) {
                is Outcome.Success -> UiState.Content(result.data)
                is Outcome.Failure -> UiState.Failed(result.error.present())
            }
        }
    }

    /** Marks one item read and refreshes, so the badge and list stay in step. */
    fun onNotificationOpened(notificationId: String) {
        viewModelScope.launch {
            val userId = observeSession().filterNotNull().first().id
            markRead(userId, notificationId)
            when (val result = getNotifications(userId)) {
                is Outcome.Success -> _state.value = UiState.Content(result.data)
                is Outcome.Failure -> _state.value = UiState.Failed(result.error.present())
            }
        }
    }
}

@HiltViewModel
class StudentProfileViewModel @Inject constructor(
    private val observeSession: ObserveSessionUseCase,
    private val getProfile: GetStudentProfileUseCase,
    private val getDashboard: GetStudentDashboardUseCase,
    val policy: AttendancePolicy,
) : ViewModel() {

    /** Profile plus the headline percentage, which the profile screen shows. */
    data class ProfileContent(
        val student: Student,
        val overallPercentage: Float,
    )

    private val _state = MutableStateFlow<UiState<ProfileContent>>(UiState.Loading)
    val state: StateFlow<UiState<ProfileContent>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val studentId = observeSession().filterNotNull().first().id

            val profile = getProfile(studentId)
            if (profile is Outcome.Failure) {
                _state.value = UiState.Failed(profile.error.present())
                return@launch
            }
            val dashboard = getDashboard(studentId)
            if (dashboard is Outcome.Failure) {
                _state.value = UiState.Failed(dashboard.error.present())
                return@launch
            }

            val student = (profile as? Outcome.Success)?.data
            val summary = (dashboard as? Outcome.Success)?.data?.summary
            _state.value = if (student != null && summary != null) {
                UiState.Content(
                    ProfileContent(
                        student = student,
                        overallPercentage = summary.overallPercentage,
                    ),
                )
            } else {
                UiState.Failed(AppError.NotFound.present())
            }
        }
    }
}
