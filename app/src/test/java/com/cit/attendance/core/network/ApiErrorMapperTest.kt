package com.cit.attendance.core.network

import com.cit.attendance.core.common.AppError
import com.cit.attendance.core.common.Outcome
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * The mapper is what stands between a raw transport failure and the words a
 * user reads, so every code the server can return is pinned down here.
 */
class ApiErrorMapperTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val mapper = ApiErrorMapper(json)

    private fun httpError(status: Int, body: String): HttpException =
        HttpException(
            Response.error<Any>(status, body.toResponseBody("application/json".toMediaType())),
        )

    private suspend fun errorFrom(status: Int, body: String): AppError {
        val result = mapper.execute<Unit> { throw httpError(status, body) }
        assertTrue("expected a failure", result is Outcome.Failure)
        return (result as Outcome.Failure).error
    }

    @Test
    fun `a successful call passes the value through`() = runTest {
        val result = mapper.execute { "ok" }

        assertEquals(Outcome.Success("ok"), result)
    }

    @Test
    fun `no connectivity is reported as such, not as a server fault`() = runTest {
        val result = mapper.execute<Unit> { throw IOException("socket closed") }

        assertEquals(Outcome.Failure(AppError.NoConnection), result)
    }

    @Test
    fun `invalid credentials map to the credentials error`() = runTest {
        val error = errorFrom(401, """{"code":"INVALID_CREDENTIALS","message":"nope"}""")

        assertEquals(AppError.InvalidCredentials, error)
    }

    @Test
    fun `a role mismatch carries the role the account actually holds`() = runTest {
        val error = errorFrom(
            403,
            """{"code":"ROLE_MISMATCH","message":"wrong","actualRole":"teacher"}""",
        )

        assertEquals(AppError.RoleMismatch("teacher"), error)
    }

    @Test
    fun `an expired QR maps to the expired error`() = runTest {
        assertEquals(
            AppError.QrExpired,
            errorFrom(410, """{"code":"QR_EXPIRED","message":"expired"}"""),
        )
    }

    @Test
    fun `an invalid QR maps to the invalid error`() = runTest {
        assertEquals(
            AppError.QrInvalid,
            errorFrom(400, """{"code":"QR_INVALID","message":"bad"}"""),
        )
    }

    @Test
    fun `a reused QR is distinguished from an invalid one`() = runTest {
        assertEquals(
            AppError.QrAlreadyUsed,
            errorFrom(409, """{"code":"QR_ALREADY_USED","message":"used"}"""),
        )
    }

    @Test
    fun `a closed session maps to the closed error`() = runTest {
        assertEquals(
            AppError.AttendanceSessionClosed,
            errorFrom(409, """{"code":"SESSION_CLOSED","message":"closed"}"""),
        )
    }

    @Test
    fun `an unenrolled student maps to the enrolment error`() = runTest {
        assertEquals(
            AppError.StudentNotEnrolled,
            errorFrom(403, """{"code":"NOT_ENROLLED","message":"no"}"""),
        )
    }

    @Test
    fun `a duplicate carries the detail needed to explain it`() = runTest {
        val error = errorFrom(
            409,
            """{"code":"ALREADY_MARKED","message":"dup","studentName":"Sanjayan A",
               "registerNumber":"24CS0845","subjectName":"Mobile Application Development",
               "markedAt":"2026-08-30T10:17:00.000Z"}""",
        )

        assertTrue(error is AppError.AlreadyMarked)
        val marked = error as AppError.AlreadyMarked
        assertEquals("Sanjayan A", marked.studentName)
        assertEquals("24CS0845", marked.registerNumber)
        assertEquals("Mobile Application Development", marked.subjectName)
    }

    @Test
    fun `rate limiting is not mistaken for a validation problem`() = runTest {
        assertEquals(
            AppError.RateLimited,
            errorFrom(429, """{"code":"RATE_LIMITED","message":"slow down"}"""),
        )
    }

    @Test
    fun `an unparseable body still yields sensible wording from the status`() = runTest {
        assertEquals(AppError.SessionExpired, errorFrom(401, "<html>gateway</html>"))
        assertEquals(AppError.NotAuthorised, errorFrom(403, ""))
        assertEquals(AppError.NotFound, errorFrom(404, "not json"))
        assertEquals(AppError.ServerUnavailable, errorFrom(503, "upstream down"))
    }

    @Test
    fun `an unrecognised code does not crash the mapper`() = runTest {
        val error = errorFrom(400, """{"code":"SOMETHING_NEW","message":"?"}""")

        assertTrue(error is AppError.Unexpected)
    }

    @Test
    fun `cancellation propagates rather than being swallowed as an error`() = runTest {
        var threw = false
        try {
            mapper.execute<Unit> { throw kotlinx.coroutines.CancellationException("stop") }
        } catch (_: kotlinx.coroutines.CancellationException) {
            threw = true
        }

        assertTrue("cancellation must not be converted into a failure", threw)
    }
}
