package com.cit.attendance.core.network

import com.cit.attendance.core.common.AppError
import com.cit.attendance.core.common.Outcome
import com.cit.attendance.core.network.dto.ApiErrorDto
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single point where anything from the network becomes an [AppError].
 *
 * Nothing above the data layer ever sees an [HttpException], an [IOException]
 * or a status code (Sections 37 and 46).
 */
@Singleton
class ApiErrorMapper @Inject constructor(
    private val json: Json,
) {

    /**
     * Runs a network call and converts any failure into an [Outcome.Failure].
     *
     * Cancellation is deliberately not caught: a cancelled coroutine must stay
     * cancelled rather than be reported as an error.
     */
    suspend fun <T> execute(block: suspend () -> T): Outcome<T> = try {
        Outcome.Success(block())
    } catch (cancellation: kotlinx.coroutines.CancellationException) {
        throw cancellation
    } catch (http: HttpException) {
        Outcome.Failure(fromHttp(http))
    } catch (io: IOException) {
        // No route to the server: DNS, timeout, aeroplane mode, server down.
        Outcome.Failure(AppError.NoConnection)
    } catch (other: Exception) {
        Outcome.Failure(AppError.Unexpected(other::class.simpleName ?: "unknown"))
    }

    private fun fromHttp(http: HttpException): AppError {
        val body = runCatching { http.response()?.errorBody()?.string() }.getOrNull()
        val parsed = body
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { json.decodeFromString<ApiErrorDto>(it) }.getOrNull() }

        if (parsed == null) {
            // The server answered but not in a shape we recognise; fall back to
            // the status so the user still gets sensible wording.
            return when (http.code()) {
                401 -> AppError.SessionExpired
                403 -> AppError.NotAuthorised
                404 -> AppError.NotFound
                in 500..599 -> AppError.ServerUnavailable
                else -> AppError.Unexpected("http-${http.code()}")
            }
        }

        return when (parsed.code) {
            "INVALID_CREDENTIALS" -> AppError.InvalidCredentials
            "ACCOUNT_INACTIVE" -> AppError.AccountInactive(parsed.message)
            "ROLE_MISMATCH" -> AppError.RoleMismatch(parsed.actualRole ?: "another role")
            "SESSION_EXPIRED" -> AppError.SessionExpired
            "NOT_AUTHORISED" -> AppError.NotAuthorised
            "NOT_FOUND" -> AppError.NotFound
            "RATE_LIMITED" -> AppError.RateLimited
            "VALIDATION" -> AppError.Validation(parsed.message)

            "QR_EXPIRED" -> AppError.QrExpired
            "QR_INVALID" -> AppError.QrInvalid
            "QR_ALREADY_USED" -> AppError.QrAlreadyUsed
            "SESSION_CLOSED" -> AppError.AttendanceSessionClosed
            "NOT_ENROLLED" -> AppError.StudentNotEnrolled
            "ALREADY_MARKED" -> AppError.AlreadyMarked(
                studentName = parsed.studentName,
                registerNumber = parsed.registerNumber,
                subjectName = parsed.subjectName,
                markedAt = parsed.markedAt,
            )

            else -> AppError.Unexpected(parsed.code)
        }
    }
}
