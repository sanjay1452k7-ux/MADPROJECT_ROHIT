package com.cit.attendance.data.repository

import com.cit.attendance.core.common.Outcome
import com.cit.attendance.core.common.map
import com.cit.attendance.core.network.ApiErrorMapper
import com.cit.attendance.core.network.AttendanceApi
import com.cit.attendance.core.network.dto.ManualAttendanceRequest
import com.cit.attendance.core.network.dto.MarkAttendanceRequest
import com.cit.attendance.core.network.dto.StartSessionRequest
import com.cit.attendance.core.network.dto.VerifyTokenRequest
import com.cit.attendance.data.mapper.cohorts
import com.cit.attendance.data.mapper.toDomain
import com.cit.attendance.domain.model.ActiveAttendanceSession
import com.cit.attendance.domain.model.AdminDashboard
import com.cit.attendance.domain.model.AttendanceMarkResult
import com.cit.attendance.domain.model.AttendanceQrToken
import com.cit.attendance.domain.model.AttendanceRecord
import com.cit.attendance.domain.model.AuditEntry
import com.cit.attendance.domain.model.AttendanceStatus
import com.cit.attendance.domain.model.LiveAttendanceSession
import com.cit.attendance.domain.model.NotificationItem
import com.cit.attendance.domain.model.Student
import com.cit.attendance.domain.model.StudentDashboard
import com.cit.attendance.domain.model.TeacherDashboard
import com.cit.attendance.domain.model.TeachingCohort
import com.cit.attendance.domain.model.VerifiedStudent
import com.cit.attendance.domain.repository.AdminRepository
import com.cit.attendance.domain.repository.AttendanceRepository
import com.cit.attendance.domain.repository.NotificationRepository
import com.cit.attendance.domain.repository.StudentRepository
import com.cit.attendance.domain.repository.TeacherRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The real implementations of the domain contracts (Section 12).
 *
 * The `studentId` and `teacherId` parameters on these interfaces are ignored on
 * the wire: the server identifies the caller from the access token, which is
 * what stops one user requesting another's data. The parameters remain in the
 * contract because an administrator-facing lookup will need them.
 */

@Singleton
class NetworkStudentRepository @Inject constructor(
    private val api: AttendanceApi,
    private val errors: ApiErrorMapper,
) : StudentRepository {

    override suspend fun getDashboard(studentId: String): Outcome<StudentDashboard> =
        errors.execute { api.studentDashboard() }.map { it.toDomain() }

    override suspend fun getProfile(studentId: String): Outcome<Student> =
        errors.execute { api.studentProfile() }.map { it.toDomain() }

    override suspend fun getAttendanceHistory(studentId: String): Outcome<List<AttendanceRecord>> =
        errors.execute { api.studentAttendance() }.map { list -> list.map { it.toDomain() } }
}

@Singleton
class NetworkTeacherRepository @Inject constructor(
    private val api: AttendanceApi,
    private val errors: ApiErrorMapper,
) : TeacherRepository {

    override suspend fun getDashboard(teacherId: String): Outcome<TeacherDashboard> =
        errors.execute { api.teacherDashboard() }.map { it.toDomain() }

    override suspend fun getCohorts(): Outcome<List<TeachingCohort>> =
        errors.execute { api.teacherDashboard() }.map { it.cohorts() }
}

@Singleton
class NetworkAdminRepository @Inject constructor(
    private val api: AttendanceApi,
    private val errors: ApiErrorMapper,
) : AdminRepository {

    // The service scopes the response to the caller's own institution, so the
    // id is not sent: the token decides whose figures come back.
    override suspend fun getDashboard(adminId: String): Outcome<AdminDashboard> =
        errors.execute { api.adminDashboard() }.map { it.toDomain() }

    override suspend fun getAuditTrail(limit: Int): Outcome<List<AuditEntry>> =
        errors.execute { api.auditTrail(limit) }.map { page -> page.entries.map { it.toDomain() } }
}

@Singleton
class NetworkNotificationRepository @Inject constructor(
    private val api: AttendanceApi,
    private val errors: ApiErrorMapper,
) : NotificationRepository {

    override suspend fun getNotifications(userId: String): Outcome<List<NotificationItem>> =
        errors.execute { api.notifications() }.map { list -> list.map { it.toDomain() } }

    override suspend fun markAsRead(userId: String, notificationId: String): Outcome<Unit> =
        errors.execute { api.markNotificationRead(notificationId) }.map { }
}

@Singleton
class NetworkAttendanceRepository @Inject constructor(
    private val api: AttendanceApi,
    private val errors: ApiErrorMapper,
) : AttendanceRepository {

    override suspend fun activeSessionForStudent(): Outcome<ActiveAttendanceSession?> =
        errors.execute { api.activeSession() }.map { it?.toDomain() }

    override suspend fun mintQrToken(sessionId: String): Outcome<AttendanceQrToken> =
        errors.execute { api.mintAttendanceToken(sessionId) }.map { it.toDomain() }

    override suspend fun startSession(
        subjectId: String,
        classSectionId: String,
    ): Outcome<LiveAttendanceSession> =
        errors.execute {
            api.startSession(StartSessionRequest(subjectId, classSectionId))
        }.map { it.toDomain() }

    override suspend fun sessionDetail(sessionId: String): Outcome<LiveAttendanceSession> =
        errors.execute { api.sessionDetail(sessionId) }.map { it.toDomain() }

    override suspend fun closeSession(sessionId: String): Outcome<LiveAttendanceSession> =
        errors.execute { api.closeSession(sessionId) }.map { it.toDomain() }

    override suspend fun verifyToken(
        sessionId: String,
        token: String,
    ): Outcome<VerifiedStudent> =
        errors.execute { api.verifyToken(sessionId, VerifyTokenRequest(token)) }
            .map { it.toDomain() }

    override suspend fun markPresent(
        sessionId: String,
        verificationId: String,
    ): Outcome<AttendanceMarkResult> =
        errors.execute { api.markAttendance(sessionId, MarkAttendanceRequest(verificationId)) }
            .map { it.toDomain() }

    override suspend fun markManually(
        sessionId: String,
        studentId: String,
        status: AttendanceStatus,
        reason: String?,
    ): Outcome<Unit> =
        errors.execute {
            api.markManually(
                sessionId,
                ManualAttendanceRequest(
                    studentId = studentId,
                    status = status.name,
                    reason = reason,
                ),
            )
        }.map { }
}
