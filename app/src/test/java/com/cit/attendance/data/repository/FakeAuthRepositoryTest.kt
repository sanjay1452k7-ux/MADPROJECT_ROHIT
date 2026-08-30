package com.cit.attendance.data.repository

import app.cash.turbine.test
import com.cit.attendance.core.common.AppError
import com.cit.attendance.core.common.Outcome
import com.cit.attendance.data.demo.DemoDirectory
import com.cit.attendance.data.demo.DemoPeople
import com.cit.attendance.data.local.PersistedSession
import com.cit.attendance.domain.config.InstitutionConfig
import com.cit.attendance.domain.model.UserRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FakeAuthRepositoryTest {

    private val config = InstitutionConfig(
        name = "Chennai Institute of Technology",
        emailDomain = "citchennai.net",
        attendanceThresholdPercent = 75,
    )
    private val directory = DemoDirectory(config)
    private val storage = InMemorySessionStorage()
    private val repository = FakeAuthRepository(directory, storage)

    private val sanjayanEmail = "24cs0845@citchennai.net"

    @Test
    fun `valid credentials for the chosen role sign the student in`() = runTest {
        val result = repository.signIn(
            email = sanjayanEmail,
            password = DemoPeople.DEMO_PASSWORD,
            expectedRole = UserRole.STUDENT,
        )

        assertTrue(result is Outcome.Success)
        val user = (result as Outcome.Success).data
        assertEquals("Sanjayan A", user.displayName)
        assertEquals(UserRole.STUDENT, user.role)
    }

    @Test
    fun `a wrong password is rejected as invalid credentials`() = runTest {
        val result = repository.signIn(sanjayanEmail, "not-the-password", UserRole.STUDENT)

        assertEquals(Outcome.Failure(AppError.InvalidCredentials), result)
    }

    @Test
    fun `an unknown address is rejected without revealing whether it exists`() = runTest {
        val result = repository.signIn(
            email = "nobody@citchennai.net",
            password = DemoPeople.DEMO_PASSWORD,
            expectedRole = UserRole.STUDENT,
        )

        // Deliberately the same failure as a wrong password, so the response
        // cannot be used to enumerate valid accounts.
        assertEquals(Outcome.Failure(AppError.InvalidCredentials), result)
    }

    @Test
    fun `signing in as the wrong role is refused`() = runTest {
        val result = repository.signIn(
            email = sanjayanEmail,
            password = DemoPeople.DEMO_PASSWORD,
            expectedRole = UserRole.TEACHER,
        )

        assertTrue(result is Outcome.Failure)
        assertTrue((result as Outcome.Failure).error is AppError.RoleMismatch)
    }

    @Test
    fun `a failed sign-in persists nothing`() = runTest {
        repository.signIn(sanjayanEmail, "wrong", UserRole.STUDENT)

        assertEquals(0, storage.saveCount)
        assertNull(storage.read())
    }

    @Test
    fun `the session flow tracks sign-in and sign-out`() = runTest {
        repository.session.test {
            assertNull(awaitItem())

            repository.signIn(sanjayanEmail, DemoPeople.DEMO_PASSWORD, UserRole.STUDENT)
            assertEquals("Sanjayan A", awaitItem()?.displayName)

            repository.signOut()
            assertNull(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `signing out clears the persisted session`() = runTest {
        repository.signIn(sanjayanEmail, DemoPeople.DEMO_PASSWORD, UserRole.STUDENT)
        assertEquals(1, storage.saveCount)

        repository.signOut()

        assertNull(storage.read())
        assertTrue(storage.clearCount > 0)
    }

    @Test
    fun `a stored session is restored on a cold start`() = runTest {
        val student = directory.primaryStudent
        val warmStorage = InMemorySessionStorage(
            PersistedSession(
                userId = student.id,
                role = UserRole.STUDENT.name,
                token = "local-session-${student.id}",
            ),
        )
        val coldRepository = FakeAuthRepository(directory, warmStorage)

        val restored = coldRepository.restoreSession()

        assertTrue(restored is Outcome.Success)
        assertEquals(student.id, (restored as Outcome.Success).data?.id)
    }

    @Test
    fun `a stored session pointing at an unknown user is discarded`() = runTest {
        val staleStorage = InMemorySessionStorage(
            PersistedSession(
                userId = "stu-does-not-exist",
                role = UserRole.STUDENT.name,
                token = "stale",
            ),
        )
        val coldRepository = FakeAuthRepository(directory, staleStorage)

        val restored = coldRepository.restoreSession()

        assertTrue(restored is Outcome.Success)
        assertNull((restored as Outcome.Success).data)
        assertNull(staleStorage.read())
    }

    @Test
    fun `restoring with no stored session yields no user`() = runTest {
        val restored = repository.restoreSession()

        assertTrue(restored is Outcome.Success)
        assertNull((restored as Outcome.Success).data)
    }
}
