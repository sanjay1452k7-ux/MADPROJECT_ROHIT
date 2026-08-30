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

    /** Too many attempts in a short window (Section 34). */
    data object RateLimited : AppError

    // ---------------------------------------------------------- attendance

    /** The QR code passed its validity window before it was scanned. */
    data object QrExpired : AppError

    /** The scanned code is not one this service issued. */
    data object QrInvalid : AppError

    /** A code may be scanned once; this one already was (Section 34). */
    data object QrAlreadyUsed : AppError

    /** The teacher has ended the session. */
    data object AttendanceSessionClosed : AppError

    /** The student is not enrolled in the subject being marked. */
    data object StudentNotEnrolled : AppError

    /**
     * Attendance already exists for this student in this session (Section 24).
     * The detail is carried so the screen can show who, when and for what.
     */
    data class AlreadyMarked(
        val studentName: String?,
        val registerNumber: String?,
        val subjectName: String?,
        val markedAt: String?,
    ) : AppError

    /** Anything genuinely unexpected. Detail is for logs, never for the user. */
    data class Unexpected(val detail: String) : AppError
}
