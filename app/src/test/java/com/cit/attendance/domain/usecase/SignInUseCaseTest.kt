package com.cit.attendance.domain.usecase

import com.cit.attendance.core.common.AppError
import com.cit.attendance.core.common.Outcome
import com.cit.attendance.domain.config.InstitutionConfig
import com.cit.attendance.domain.model.AuthenticatedUser
import com.cit.attendance.domain.model.UserRole
import com.cit.attendance.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The use case performs the fast local checks only. It must never be the thing
 * that decides a credential is valid - that belongs to the server.
 */
class SignInUseCaseTest {

    private val config = InstitutionConfig(
        name = "Chennai Institute of Technology",
        emailDomain = "citchennai.net",
        attendanceThresholdPercent = 75,
    )

    /** Records what reached the repository so the test can assert on it. */
    private class RecordingAuthRepository : AuthRepository {
        var lastEmail: String? = null
        var lastRole: UserRole? = null
        var callCount = 0

        override val session: Flow<AuthenticatedUser?> = flowOf(null)

        override suspend fun signIn(
            email: String,
            password: String,
            expectedRole: UserRole,
        ): Outcome<AuthenticatedUser> {
            callCount++
            lastEmail = email
            lastRole = expectedRole
            return Outcome.Success(
                AuthenticatedUser(
                    id = "stu-1",
                    displayName = "Sanjayan A",
                    email = email,
                    role = expectedRole,
                ),
            )
        }

        override suspend fun restoreSession(): Outcome<AuthenticatedUser?> = Outcome.Success(null)

        override suspend fun signOut() = Unit
    }

    private val repository = RecordingAuthRepository()
    private val signIn = SignInUseCase(repository, config)

    @Test
    fun `an empty email is reported before anything is sent`() = runTest {
        val result = signIn("   ", "password", UserRole.STUDENT)

        assertTrue(result is Outcome.Failure)
        assertTrue((result as Outcome.Failure).error is AppError.Validation)
        assertEquals("nothing should reach the network", 0, repository.callCount)
    }

    @Test
    fun `an empty password is reported before anything is sent`() = runTest {
        val result = signIn("24cs0845@citchennai.net", "", UserRole.STUDENT)

        assertTrue(result is Outcome.Failure)
        assertTrue((result as Outcome.Failure).error is AppError.Validation)
        assertEquals(0, repository.callCount)
    }

    @Test
    fun `an address outside the institution domain is refused locally`() = runTest {
        val result = signIn("someone@gmail.com", "password", UserRole.STUDENT)

        assertEquals(
            Outcome.Failure(AppError.UnrecognisedInstitutionEmail("citchennai.net")),
            result,
        )
        assertEquals(0, repository.callCount)
    }

    @Test
    fun `email is trimmed and lower-cased before it reaches the repository`() = runTest {
        val result = signIn("  24CS0845@CitChennai.net  ", "Cit@2026", UserRole.STUDENT)

        assertTrue(result is Outcome.Success)
        assertEquals("24cs0845@citchennai.net", repository.lastEmail)
    }

    @Test
    fun `the chosen role is passed through for the server to check`() = runTest {
        signIn("meenakshi.s@citchennai.net", "Cit@2026", UserRole.TEACHER)

        assertEquals(UserRole.TEACHER, repository.lastRole)
    }

    @Test
    fun `a valid request reaches the repository exactly once`() = runTest {
        val result = signIn("24cs0845@citchennai.net", "Cit@2026", UserRole.STUDENT)

        assertTrue(result is Outcome.Success)
        assertEquals(1, repository.callCount)
    }
}
