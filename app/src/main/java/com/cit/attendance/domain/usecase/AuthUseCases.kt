package com.cit.attendance.domain.usecase

import com.cit.attendance.core.common.AppError
import com.cit.attendance.core.common.Outcome
import com.cit.attendance.domain.config.InstitutionConfig
import com.cit.attendance.domain.model.AuthenticatedUser
import com.cit.attendance.domain.model.UserRole
import com.cit.attendance.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Signs a user in.
 *
 * Client-side checks here exist to give fast, precise feedback - not to secure
 * anything. The server remains the authority on credentials and on whether the
 * account really holds the requested role (Section 34).
 */
class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val institutionConfig: InstitutionConfig,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        role: UserRole,
    ): Outcome<AuthenticatedUser> {
        val trimmedEmail = email.trim()

        if (trimmedEmail.isEmpty()) {
            return Outcome.Failure(AppError.Validation("Enter your institutional email address."))
        }
        if (password.isEmpty()) {
            return Outcome.Failure(AppError.Validation("Enter your password."))
        }
        if (!institutionConfig.isInstitutionEmail(trimmedEmail)) {
            return Outcome.Failure(
                AppError.UnrecognisedInstitutionEmail(institutionConfig.emailDomain),
            )
        }

        return authRepository.signIn(
            email = trimmedEmail.lowercase(),
            password = password,
            expectedRole = role,
        )
    }
}

/** Emits the current session so navigation can follow it (Section 9). */
class ObserveSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthenticatedUser?> = authRepository.session
}

/** Restores a persisted session during the splash window. */
class RestoreSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): Outcome<AuthenticatedUser?> = authRepository.restoreSession()
}

/** Ends the session and clears stored credentials. */
class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() = authRepository.signOut()
}
