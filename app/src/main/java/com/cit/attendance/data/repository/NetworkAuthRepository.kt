package com.cit.attendance.data.repository

import com.cit.attendance.core.common.AppError
import com.cit.attendance.core.common.Outcome
import com.cit.attendance.core.network.ApiErrorMapper
import com.cit.attendance.core.network.AttendanceApi
import com.cit.attendance.core.network.AuthTokenStore
import com.cit.attendance.core.network.dto.LoginRequest
import com.cit.attendance.data.local.PersistedSession
import com.cit.attendance.domain.model.AuthenticatedUser
import com.cit.attendance.domain.model.UserRole
import com.cit.attendance.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication against the real service (Section 12).
 *
 * This satisfies exactly the interface the Milestone 1 in-memory implementation
 * did, so swapping it in was a change to one Hilt binding and nothing else.
 */
@Singleton
class NetworkAuthRepository @Inject constructor(
    private val api: AttendanceApi,
    private val tokens: AuthTokenStore,
    private val errors: ApiErrorMapper,
) : AuthRepository {

    private val sessionState = MutableStateFlow<AuthenticatedUser?>(null)

    override val session: Flow<AuthenticatedUser?> = sessionState.asStateFlow()

    override suspend fun signIn(
        email: String,
        password: String,
        expectedRole: UserRole,
    ): Outcome<AuthenticatedUser> {
        val result = errors.execute {
            api.login(LoginRequest(email = email, password = password, role = expectedRole.name))
        }

        return when (result) {
            is Outcome.Failure -> result
            is Outcome.Success -> {
                val dto = result.data
                val role = UserRole.fromStorageKey(dto.user.role)
                    ?: return Outcome.Failure(AppError.Unexpected("unknown-role"))

                tokens.save(
                    PersistedSession(
                        userId = dto.user.id,
                        role = role.name,
                        token = dto.accessToken,
                        refreshToken = dto.refreshToken,
                    ),
                )

                val user = AuthenticatedUser(
                    id = dto.user.id,
                    displayName = dto.user.displayName,
                    email = dto.user.email,
                    role = role,
                )
                sessionState.value = user
                Outcome.Success(user)
            }
        }
    }

    /**
     * Restores a stored session on cold start.
     *
     * The stored profile is trusted only far enough to get the app moving; the
     * first authenticated request re-validates the token against the server,
     * and a rejected token clears the session.
     */
    override suspend fun restoreSession(): Outcome<AuthenticatedUser?> {
        val persisted = tokens.restore() ?: return Outcome.Success(null)
        val role = UserRole.fromStorageKey(persisted.role)
        if (role == null) {
            tokens.clear()
            return Outcome.Success(null)
        }

        return when (val me = errors.execute { api.me() }) {
            is Outcome.Success -> {
                val user = AuthenticatedUser(
                    id = me.data.id,
                    displayName = me.data.displayName,
                    email = me.data.email,
                    role = UserRole.fromStorageKey(me.data.role) ?: role,
                )
                sessionState.value = user
                Outcome.Success(user)
            }

            is Outcome.Failure -> {
                if (me.error is AppError.SessionExpired || me.error is AppError.NotAuthorised) {
                    tokens.clear()
                    Outcome.Success(null)
                } else {
                    // A transient network problem must not sign the user out.
                    Outcome.Failure(me.error)
                }
            }
        }
    }

    override suspend fun signOut() {
        // Best effort: even if the revoke call cannot reach the server, the
        // local credentials must go.
        errors.execute { api.logout() }
        tokens.clear()
        sessionState.value = null
    }
}
