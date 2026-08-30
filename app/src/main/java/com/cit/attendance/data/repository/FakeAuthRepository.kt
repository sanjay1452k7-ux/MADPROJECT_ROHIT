package com.cit.attendance.data.repository

import com.cit.attendance.core.common.AppError
import com.cit.attendance.core.common.Outcome
import com.cit.attendance.data.demo.DemoDirectory
import com.cit.attendance.data.demo.DemoPeople
import com.cit.attendance.data.local.PersistedSession
import com.cit.attendance.data.local.SessionStore
import com.cit.attendance.domain.model.AuthenticatedUser
import com.cit.attendance.domain.model.UserRole
import com.cit.attendance.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory authentication for Milestone 1 (Section 12).
 *
 * It satisfies [AuthRepository] exactly as the Retrofit implementation will, so
 * Milestone 2 swaps a Hilt binding rather than rewriting callers. The role
 * check below mirrors what the server will enforce - it is here so the contract
 * behaves correctly during the demo, not because the client is trusted with it.
 */
@Singleton
class FakeAuthRepository @Inject constructor(
    private val directory: DemoDirectory,
    private val sessionStore: SessionStore,
) : AuthRepository {

    private val sessionState = MutableStateFlow<AuthenticatedUser?>(null)

    override val session: Flow<AuthenticatedUser?> = sessionState.asStateFlow()

    override suspend fun signIn(
        email: String,
        password: String,
        expectedRole: UserRole,
    ): Outcome<AuthenticatedUser> {
        // A short delay so loading states are exercised honestly during the demo
        // instead of resolving instantly and hiding them.
        delay(SIMULATED_LATENCY_MS)

        val account = directory.accountFor(email)
            ?: return Outcome.Failure(AppError.InvalidCredentials)

        if (password != DemoPeople.DEMO_PASSWORD) {
            return Outcome.Failure(AppError.InvalidCredentials)
        }

        if (account.role != expectedRole) {
            return Outcome.Failure(AppError.RoleMismatch(account.role.name.lowercase()))
        }

        val user = AuthenticatedUser(
            id = account.entityId,
            displayName = account.displayName,
            email = account.email,
            role = account.role,
        )

        sessionStore.save(
            PersistedSession(
                userId = user.id,
                role = user.role.name,
                // Milestone 1 has no issuer, so this is an opaque local marker,
                // never presented as a real credential. Milestone 2 stores the
                // JWT returned by the backend in exactly this slot.
                token = "local-session-${user.id}",
            ),
        )
        sessionState.value = user
        return Outcome.Success(user)
    }

    override suspend fun restoreSession(): Outcome<AuthenticatedUser?> {
        val persisted = sessionStore.read() ?: return Outcome.Success(null)
        val role = UserRole.fromStorageKey(persisted.role)
            ?: return Outcome.Success(null)

        val user = when (role) {
            UserRole.STUDENT -> directory.studentById(persisted.userId)
                ?.let { AuthenticatedUser(it.id, it.name, it.email, role) }

            UserRole.TEACHER -> directory.teacherById(persisted.userId)
                ?.let { AuthenticatedUser(it.id, it.name, it.email, role) }

            UserRole.ADMIN -> directory.administrator
                .takeIf { it.id == persisted.userId }
                ?.let { AuthenticatedUser(it.id, it.name, it.email, role) }

            // No parent behaviour ships in Milestone 1 (Section 13).
            UserRole.PARENT -> null
        }

        if (user == null) {
            // The stored id no longer resolves - treat it as a stale session.
            sessionStore.clear()
            return Outcome.Success(null)
        }

        sessionState.value = user
        return Outcome.Success(user)
    }

    override suspend fun signOut() {
        sessionStore.clear()
        sessionState.value = null
    }

    private companion object {
        const val SIMULATED_LATENCY_MS = 550L
    }
}
