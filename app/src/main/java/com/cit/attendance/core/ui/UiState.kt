package com.cit.attendance.core.ui

/**
 * The three states every data-backed screen can be in.
 *
 * Modelling them as a closed set means a screen cannot forget to render a
 * loading or error case - the compiler asks for all three (Sections 37 and 38).
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Content<T>(val data: T) : UiState<T>
    data class Failed(val error: ErrorPresentation) : UiState<Nothing>
}
