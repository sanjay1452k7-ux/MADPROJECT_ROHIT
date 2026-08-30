package com.cit.attendance.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cit.attendance.core.common.dataOrNull
import com.cit.attendance.domain.usecase.GetNotificationsUseCase
import com.cit.attendance.domain.usecase.ObserveSessionUseCase
import com.cit.attendance.domain.usecase.SignOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shell-level concerns shared by every tab: ending the session, and the unread
 * badge on the notifications tab.
 */
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val observeSession: ObserveSessionUseCase,
    private val getNotifications: GetNotificationsUseCase,
    private val signOutUseCase: SignOutUseCase,
) : ViewModel() {

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        refreshUnreadCount()
    }

    fun refreshUnreadCount() {
        viewModelScope.launch {
            val userId = observeSession().filterNotNull().first().id
            _unreadCount.value = getNotifications(userId)
                .dataOrNull()
                ?.count { !it.isRead }
                ?: 0
        }
    }

    /**
     * Ends the session. Navigation is driven by the session flow going null, so
     * the caller does not need to navigate manually.
     */
    fun signOut() {
        viewModelScope.launch { signOutUseCase() }
    }
}
