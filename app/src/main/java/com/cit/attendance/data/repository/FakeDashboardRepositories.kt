package com.cit.attendance.data.repository

import com.cit.attendance.core.common.AppError
import com.cit.attendance.core.common.Outcome
import com.cit.attendance.data.demo.DemoAcademics
import com.cit.attendance.data.demo.DemoAttendance
import com.cit.attendance.data.demo.DemoDirectory
import com.cit.attendance.domain.model.AdminDashboard
import com.cit.attendance.domain.model.AttendancePolicy
import com.cit.attendance.domain.model.AttendanceRecord
import com.cit.attendance.domain.model.AttendanceSummary
import com.cit.attendance.domain.model.DepartmentStat
import com.cit.attendance.domain.model.NotificationItem
import com.cit.attendance.domain.model.Student
import com.cit.attendance.domain.model.StudentAtRisk
import com.cit.attendance.domain.model.StudentDashboard
import com.cit.attendance.domain.model.TeacherDashboard
import com.cit.attendance.domain.repository.AdminRepository
import com.cit.attendance.domain.repository.NotificationRepository
import com.cit.attendance.domain.repository.StudentRepository
import com.cit.attendance.domain.repository.TeacherRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/** Shared pacing so every mocked read exercises the loading states. */
private const val SIMULATED_LATENCY_MS = 400L

@Singleton
class FakeStudentRepository @Inject constructor(
    private val directory: DemoDirectory,
    private val policy: AttendancePolicy,
    private val notifications: FakeNotificationRepository,
) : StudentRepository {

    override suspend fun getDashboard(studentId: String): Outcome<StudentDashboard> {
        delay(SIMULATED_LATENCY_MS)
        val student = directory.studentById(studentId)
            ?: return Outcome.Failure(AppError.NotFound)

        return Outcome.Success(
            StudentDashboard(
                student = student,
                summary = summaryFor(student),
                todaysClasses = DemoAcademics.todaysTimetable(),
                recentRecords = DemoAttendance.history(student, days = 6),
                unreadNotifications = notifications.unreadCount(student.id),
            ),
        )
    }

    override suspend fun getProfile(studentId: String): Outcome<Student> {
        delay(SIMULATED_LATENCY_MS)
        return directory.studentById(studentId)
            ?.let { Outcome.Success(it) }
            ?: Outcome.Failure(AppError.NotFound)
    }

    override suspend fun getAttendanceHistory(studentId: String): Outcome<List<AttendanceRecord>> {
        delay(SIMULATED_LATENCY_MS)
        val student = directory.studentById(studentId)
            ?: return Outcome.Failure(AppError.NotFound)
        return Outcome.Success(DemoAttendance.history(student, days = 24))
    }

    private fun summaryFor(student: Student): AttendanceSummary {
        val percentage = DemoAttendance.overallPercentage(student)
        return AttendanceSummary(
            overallPercentage = percentage,
            standing = policy.standingFor(percentage),
            subjectBreakdown = DemoAttendance.subjectBreakdown(student),
        )
    }
}

@Singleton
class FakeTeacherRepository @Inject constructor(
    private val directory: DemoDirectory,
    private val policy: AttendancePolicy,
) : TeacherRepository {

    override suspend fun getDashboard(teacherId: String): Outcome<TeacherDashboard> {
        delay(SIMULATED_LATENCY_MS)
        val teacher = directory.teacherById(teacherId)
            ?: return Outcome.Failure(AppError.NotFound)

        val timetable = DemoAcademics.todaysTimetable()
        val teacherSubjectIds = teacher.subjects.map { it.id }.toSet()
        val ownClasses = timetable.filter { it.subject.id in teacherSubjectIds }

        val atRisk = directory.studentsBelow(policy.threshold).map { (student, percentage) ->
            StudentAtRisk(
                student = student,
                percentage = percentage,
                standing = policy.standingFor(percentage),
            )
        }

        return Outcome.Success(
            TeacherDashboard(
                teacher = teacher,
                todaysClasses = ownClasses,
                activeSessionCount = ownClasses.count { it.hasActiveSession },
                studentsAtRisk = atRisk,
                averageAttendance = directory.cohortAverage(),
            ),
        )
    }
}

@Singleton
class FakeAdminRepository @Inject constructor(
    private val directory: DemoDirectory,
    private val policy: AttendancePolicy,
) : AdminRepository {

    override suspend fun getDashboard(adminId: String): Outcome<AdminDashboard> {
        delay(SIMULATED_LATENCY_MS)
        val admin = directory.administrator.takeIf { it.id == adminId }
            ?: return Outcome.Failure(AppError.NotFound)

        val average = directory.cohortAverage()
        val expectedToday = directory.students.size
        // Today's turnout tracks the cohort average, which keeps the headline
        // figures on this screen consistent with each other.
        val presentToday = (expectedToday * average / 100f).roundToInt()

        return Outcome.Success(
            AdminDashboard(
                administrator = admin,
                totalStudents = directory.students.size,
                totalFaculty = directory.teachers.size,
                presentToday = presentToday,
                expectedToday = expectedToday,
                averageAttendance = average,
                studentsAtRisk = directory.studentsBelow(policy.threshold).size,
                departmentStats = listOf(
                    DepartmentStat(
                        department = "CSE",
                        studentCount = directory.students.size,
                        averageAttendance = average,
                    ),
                ),
            ),
        )
    }
}

/**
 * Notifications with mutable read state (Section 32).
 *
 * Read state is kept per user and guarded by a mutex because the dashboard and
 * the notification list can both touch it concurrently.
 */
@Singleton
class FakeNotificationRepository @Inject constructor(
    private val directory: DemoDirectory,
) : NotificationRepository {

    private val mutex = Mutex()
    private val readIds = mutableMapOf<String, MutableSet<String>>()

    override suspend fun getNotifications(userId: String): Outcome<List<NotificationItem>> {
        delay(SIMULATED_LATENCY_MS)
        val student = directory.studentById(userId)
            ?: return Outcome.Success(emptyList())

        val read = mutex.withLock { readIds[userId]?.toSet() ?: emptySet() }
        return Outcome.Success(
            DemoAttendance.notifications(student)
                .map { item -> if (item.id in read) item.copy(isRead = true) else item }
                .sortedByDescending { it.timestamp },
        )
    }

    override suspend fun markAsRead(userId: String, notificationId: String): Outcome<Unit> {
        mutex.withLock {
            readIds.getOrPut(userId) { mutableSetOf() }.add(notificationId)
        }
        return Outcome.Success(Unit)
    }

    /** Unread count without the artificial latency, for dashboard assembly. */
    suspend fun unreadCount(userId: String): Int {
        val student = directory.studentById(userId) ?: return 0
        val read = mutex.withLock { readIds[userId]?.toSet() ?: emptySet() }
        return DemoAttendance.notifications(student).count { it.id !in read && !it.isRead }
    }
}
