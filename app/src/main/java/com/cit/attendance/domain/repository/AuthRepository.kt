package com.cit.attendance.domain.repository

import com.cit.attendance.core.common.Outcome
import com.cit.attendance.domain.model.AuthenticatedUser
import com.cit.attendance.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Authentication and session lifecycle (Section 9).
 *
 * This interface is written as though a real service already sits behind it.
 * Milestone 1 binds an in-memory implementation; Milestone 2 binds a
 * Retrofit-backed one. Neither this interface nor its callers change - only the
 * Hilt binding does (Section 12).
 */
interface AuthRepository {

    /**
     * The signed-in user, or null when signed out. Emits on every session
     * change so the navigation graph can react without polling.
     */
    val session: Flow<AuthenticatedUser?>

    /**
     * Signs in with institutional credentials.
     *
     * [expectedRole] is the role chosen on the role screen. The check that the
     * account actually holds that role is an authorisation decision and belongs
     * to the server; passing it here keeps the contract honest for Milestone 2,
     * where the backend is the party that enforces it. Hiding UI is never
     * treated as security (Section 34).
     */
    suspend fun signIn(
        email: String,
        password: String,
        expectedRole: UserRole,
    ): Outcome<AuthenticatedUser>

    /** Restores a persisted session on cold start, if one is still valid. */
    suspend fun restoreSession(): Outcome<AuthenticatedUser?>

    /** Clears the session and any stored token. */
    suspend fun signOut()
}
