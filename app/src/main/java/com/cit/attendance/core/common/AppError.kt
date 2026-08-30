package com.cit.attendance.core.common

/**
 * The closed set of failures the product knows how to talk about (Section 37).
 *
 * Raw exceptions are mapped into one of these at the data-layer edge, so a
 * stack trace or an HTTP status can never reach a user-facing string.
 */
sealed interface AppError {

    /** No usable connection. Attendance verification requires one (Section 36). */
    data object NoConnection : AppError

    /** Reached the server but it could not answer. */
    data object ServerUnavailable : AppError

    /** Email or password did not match. */
    data object InvalidCredentials : AppError

    /** Credentials were valid but the account cannot sign in. */
    data class AccountInactive(val reason: String) : AppError

    /**
     * The address is not a recognised institutional address.
     * Carries the configured domain so the message can name it.
     */
    data class UnrecognisedInstitutionEmail(val expectedDomain: String) : AppError

    /** Signed in, but not with the role the user picked on the role screen. */
    data class RoleMismatch(val actualRoleLabel: String) : AppError

    /** Session expired or was revoked; the user must sign in again. */
    data object SessionExpired : AppError

    /** The signed-in role may not perform this action (Section 34). */
    data object NotAuthorised : AppError

    /** A well-formed request for something that does not exist. */
    data object NotFound : AppError

    /** Client-side validation failed before anything was sent. */
    data class Validation(val message: String) : AppError

    /** Anything genuinely unexpected. Detail is for logs, never for the user. */
    data class Unexpected(val detail: String) : AppError
}
