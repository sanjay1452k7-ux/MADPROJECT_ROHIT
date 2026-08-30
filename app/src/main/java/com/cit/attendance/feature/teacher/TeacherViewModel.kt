package com.cit.attendance.feature.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cit.attendance.core.common.Outcome
import com.cit.attendance.core.ui.UiState
import com.cit.attendance.core.ui.present
import com.cit.attendance.domain.model.AttendancePolicy
import com.cit.attendance.domain.model.TeacherDashboard
import com.cit.attendance.domain.usecase.GetTeacherDashboardUseCase
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
 * Backs every teacher tab.
 *
 * All four tabs read the same dashboard, so they share one view model scoped to
 * the teacher shell rather than each refetching the same data.
 */
@HiltViewModel
class TeacherViewModel @Inject constructor(
    private val observeSession: ObserveSessionUseCase,
    private val getDashboard: GetTeacherDashboardUseCase,
    val policy: AttendancePolicy,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<TeacherDashboard>>(UiState.Loading)
    val state: StateFlow<UiState<TeacherDashboard>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val teacherId = observeSession().filterNotNull().first().id
            _state.value = when (val result = getDashboard(teacherId)) {
                is Outcome.Success -> UiState.Content(result.data)
                is Outcome.Failure -> UiState.Failed(result.error.present())
            }
        }
    }
}
