package com.cit.attendance.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cit.attendance.core.common.Outcome
import com.cit.attendance.core.ui.UiState
import com.cit.attendance.core.ui.present
import com.cit.attendance.domain.model.AdminDashboard
import com.cit.attendance.domain.model.AttendancePolicy
import com.cit.attendance.domain.usecase.GetAdminDashboardUseCase
import com.cit.attendance.domain.usecase.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the institution-wide surfaces (Section 16). */
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val observeSession: ObserveSessionUseCase,
    private val getDashboard: GetAdminDashboardUseCase,
    val policy: AttendancePolicy,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<AdminDashboard>>(UiState.Loading)
    val state: StateFlow<UiState<AdminDashboard>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            val adminId = observeSession().filterNotNull().first().id
            _state.value = when (val result = getDashboard(adminId)) {
                is Outcome.Success -> UiState.Content(result.data)
                is Outcome.Failure -> UiState.Failed(result.error.present())
            }
        }
    }
}
