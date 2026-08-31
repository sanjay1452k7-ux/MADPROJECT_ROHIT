package com.cit.attendance.feature.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cit.attendance.core.common.Outcome
import com.cit.attendance.core.ui.UiState
import com.cit.attendance.core.ui.present
import com.cit.attendance.domain.model.AuditEntry
import com.cit.attendance.domain.usecase.GetAuditTrailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Backs the administrator's activity log (Section 35). */
@HiltViewModel
class AuditTrailViewModel @Inject constructor(
    private val getAuditTrail: GetAuditTrailUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<AuditEntry>>>(UiState.Loading)
    val state: StateFlow<UiState<List<AuditEntry>>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = getAuditTrail()) {
                is Outcome.Success -> UiState.Content(result.data)
                is Outcome.Failure -> UiState.Failed(result.error.present())
            }
        }
    }
}
