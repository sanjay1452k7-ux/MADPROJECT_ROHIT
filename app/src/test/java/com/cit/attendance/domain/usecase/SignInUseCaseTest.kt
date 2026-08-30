package com.cit.attendance.domain.usecase

import com.cit.attendance.core.common.AppError
import com.cit.attendance.core.common.Outcome
import com.cit.attendance.data.demo.DemoDirectory
import com.cit.attendance.data.demo.DemoPeople
import com.cit.attendance.data.repository.FakeAuthRepository
import com.cit.attendance.data.repository.InMemorySessionStorage
import com.cit.attendance.domain.config.InstitutionConfig
import com.cit.attendance.domain.model.UserRole
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The use case does the fast local checks. It must never be the thing that
 * decides a credential is valid - that stays with the repository, and in
 * Milestone 2 with the server.
 */
class SignInUseCaseTest {

    private val config = InstitutionConfig(
        name = "Chennai Institute of Technology",
        emailDomain = "citchennai.net",
        attendanceThresholdPercent = 75,
    )
    private val directory = DemoDirectory(config)
    private val repository = FakeAuthRepository(directory, InMemorySessionStorage())
    private val signIn = SignInUseCase(repository, config)

    @Test
    fun `an empty email is reported before anything is sent`() = runTest {
        val result = signIn("   ", "password", UserRole.STUDENT)

        assertTrue(result is Outcome.Failure)
        assertTrue((result as Outcome.Failure).error is AppError.Validation)
    }

    @Test
    fun `an empty password is reported before anything is sent`() = runTest {
        val result = signIn("24cs0845@citchennai.net", "", UserRole.STUDENT)

        assertTrue(result is Outcome.Failure)
        assertTrue((result as Outcome.Failure).error is AppError.Validation)
    }

    @Test
    fun `an address outside the institution domain is refused`() = runTest {
        val result = signIn("someone@gmail.com", "password", UserRole.STUDENT)

        assertEquals(
            Outcome.Failure(AppError.UnrecognisedInstitutionEmail("citchennai.net")),
            result,
        )
    }

    @Test
    fun `email is normalised before it reaches the repository`() = runTest {
        val result = signIn(
            email = "  24CS0845@CitChennai.net  ",
            password = DemoPeople.DEMO_PASSWORD,
            role = UserRole.STUDENT,
        )

        assertTrue("mixed case and padding should still sign in", result is Outcome.Success)
        assertEquals("Sanjayan A", (result as Outcome.Success).data.displayName)
    }

    @Test
    fun `a teacher account signs in under the teacher role`() = runTest {
        val teacher = directory.teachers.first()

        val result = signIn(teacher.email, DemoPeople.DEMO_PASSWORD, UserRole.TEACHER)

        assertTrue(result is Outcome.Success)
        assertEquals(UserRole.TEACHER, (result as Outcome.Success).data.role)
    }

    @Test
    fun `an administrator account signs in under the admin role`() = runTest {
        val result = signIn(
            email = directory.administrator.email,
            password = DemoPeople.DEMO_PASSWORD,
            role = UserRole.ADMIN,
        )

        assertTrue(result is Outcome.Success)
        assertEquals(UserRole.ADMIN, (result as Outcome.Success).data.role)
    }
}
