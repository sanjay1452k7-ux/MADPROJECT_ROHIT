package com.cit.attendance.core.ui

import com.cit.attendance.core.common.AppError

/** A failure expressed the way a user should read it. */
data class ErrorPresentation(
    val title: String,
    val message: String,
    val isRetryable: Boolean,
)

/**
 * Translates an [AppError] into wording (Section 37).
 *
 * This is the only place failures become English. Because [AppError] is a
 * closed set, adding a failure mode forces a wording decision here rather than
 * letting a raw exception leak into the interface.
 */
fun AppError.present(): ErrorPresentation = when (this) {
    AppError.NoConnection -> ErrorPresentation(
        title = "No internet connection",
        message = "Reconnect and try again. Attendance verification needs a connection.",
        isRetryable = true,
    )

    AppError.ServerUnavailable -> ErrorPresentation(
        title = "Service unavailable",
        message = "The attendance service is not responding right now. Please try again shortly.",
        isRetryable = true,
    )

    AppError.InvalidCredentials -> ErrorPresentation(
        title = "Sign in failed",
        message = "That email and password combination was not recognised.",
        isRetryable = false,
    )

    is AppError.AccountInactive -> ErrorPresentation(
        title = "Account unavailable",
        message = reason,
        isRetryable = false,
    )

    is AppError.UnrecognisedInstitutionEmail -> ErrorPresentation(
        title = "Use your institutional email",
        message = "Sign in with your @$expectedDomain address.",
        isRetryable = false,
    )

    is AppError.RoleMismatch -> ErrorPresentation(
        title = "Wrong role selected",
        message = "This account signs in as $actualRoleLabel. Go back and choose that role.",
        isRetryable = false,
    )

    AppError.SessionExpired -> ErrorPresentation(
        title = "Session expired",
        message = "Please sign in again to continue.",
        isRetryable = false,
    )

    AppError.NotAuthorised -> ErrorPresentation(
        title = "Not permitted",
        message = "Your account does not have access to this.",
        isRetryable = false,
    )

    AppError.NotFound -> ErrorPresentation(
        title = "Nothing to show",
        message = "We could not find that record.",
        isRetryable = true,
    )

    is AppError.Validation -> ErrorPresentation(
        title = "Check your details",
        message = message,
        isRetryable = false,
    )

    // The detail is carried for logging only and is deliberately not shown.
    is AppError.Unexpected -> ErrorPresentation(
        title = "Something went wrong",
        message = "Please try again. If it keeps happening, contact the academics office.",
        isRetryable = true,
    )
}
