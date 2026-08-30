package com.cit.attendance.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cit.attendance.core.common.Outcome
import com.cit.attendance.core.ui.ErrorPresentation
import com.cit.attendance.core.ui.UiState
import com.cit.attendance.core.ui.present
import com.cit.attendance.domain.model.AdminAnalytics
import com.cit.attendance.domain.model.GeneratedReport
import com.cit.attendance.domain.model.ReportFormat
import com.cit.attendance.domain.model.ReportType
import com.cit.attendance.domain.model.StudentAnalytics
import com.cit.attendance.domain.model.TeacherAnalytics
import com.cit.attendance.domain.repository.AnalyticsRepository
import com.cit.attendance.domain.repository.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentAnalyticsViewModel @Inject constructor(
    private val analytics: AnalyticsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<StudentAnalytics>>(UiState.Loading)
    val state: StateFlow<UiState<StudentAnalytics>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = analytics.studentAnalytics()) {
                is Outcome.Success -> UiState.Content(result.data)
                is Outcome.Failure -> UiState.Failed(result.error.present())
            }
        }
    }
}

@HiltViewModel
class TeacherAnalyticsViewModel @Inject constructor(
    private val analytics: AnalyticsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<TeacherAnalytics>>(UiState.Loading)
    val state: StateFlow<UiState<TeacherAnalytics>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = analytics.teacherAnalytics()) {
                is Outcome.Success -> UiState.Content(result.data)
                is Outcome.Failure -> UiState.Failed(result.error.present())
            }
        }
    }
}

@HiltViewModel
class AdminAnalyticsViewModel @Inject constructor(
    private val analytics: AnalyticsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<AdminAnalytics>>(UiState.Loading)
    val state: StateFlow<UiState<AdminAnalytics>> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = analytics.adminAnalytics()) {
                is Outcome.Success -> UiState.Content(result.data)
                is Outcome.Failure -> UiState.Failed(result.error.present())
            }
        }
    }
}

/** What the reports screen is doing (Section 33). */
data class ReportsUiState(
    val availableTypes: List<ReportType>,
    val selectedType: ReportType,
    val selectedFormat: ReportFormat = ReportFormat.PDF,
    val isGenerating: Boolean = false,
    val lastGenerated: GeneratedReport? = null,
    val error: ErrorPresentation? = null,
)

/**
 * Generates reports and hands back a file on disk.
 *
 * The list of offered types is narrowed by role here as a convenience; the
 * server enforces the same rule, so a request the UI did not offer would still
 * be refused (Section 34).
 */
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reports: ReportsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ReportsUiState(
            availableTypes = listOf(ReportType.STUDENT_ATTENDANCE),
            selectedType = ReportType.STUDENT_ATTENDANCE,
        ),
    )
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    /** Called by each role's screen with the types that role may request. */
    fun configureFor(types: List<ReportType>) {
        _state.update {
            if (it.availableTypes == types) {
                it
            } else {
                it.copy(availableTypes = types, selectedType = types.first())
            }
        }
    }

    fun onTypeSelected(type: ReportType) {
        _state.update { it.copy(selectedType = type, error = null) }
    }

    fun onFormatSelected(format: ReportFormat) {
        _state.update { it.copy(selectedFormat = format, error = null) }
    }

    fun generate() {
        val current = _state.value
        if (current.isGenerating) return

        _state.update { it.copy(isGenerating = true, error = null, lastGenerated = null) }

        viewModelScope.launch {
            when (val result = reports.generate(current.selectedType, current.selectedFormat)) {
                is Outcome.Success -> _state.update {
                    it.copy(isGenerating = false, lastGenerated = result.data)
                }

                is Outcome.Failure -> _state.update {
                    it.copy(isGenerating = false, error = result.error.present())
                }
            }
        }
    }
}
