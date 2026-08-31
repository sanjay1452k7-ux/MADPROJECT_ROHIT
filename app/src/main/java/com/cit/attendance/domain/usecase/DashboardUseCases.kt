package com.cit.attendance.domain.usecase

import com.cit.attendance.core.common.Outcome
import com.cit.attendance.domain.model.AdminDashboard
import com.cit.attendance.domain.model.AttendanceRecord
import com.cit.attendance.domain.model.AuditEntry
import com.cit.attendance.domain.model.NotificationItem
import com.cit.attendance.domain.model.Student
import com.cit.attendance.domain.model.StudentDashboard
import com.cit.attendance.domain.model.TeacherDashboard
import com.cit.attendance.domain.repository.AdminRepository
import com.cit.attendance.domain.repository.NotificationRepository
import com.cit.attendance.domain.repository.StudentRepository
import com.cit.attendance.domain.repository.TeacherRepository
import javax.inject.Inject

class GetStudentDashboardUseCase @Inject constructor(
    private val repository: StudentRepository,
) {
    suspend operator fun invoke(studentId: String): Outcome<StudentDashboard> =
        repository.getDashboard(studentId)
}

class GetStudentProfileUseCase @Inject constructor(
    private val repository: StudentRepository,
) {
    suspend operator fun invoke(studentId: String): Outcome<Student> =
        repository.getProfile(studentId)
}

/** History, newest first - the order every history surface expects. */
class GetAttendanceHistoryUseCase @Inject constructor(
    private val repository: StudentRepository,
) {
    suspend operator fun invoke(studentId: String): Outcome<List<AttendanceRecord>> =
        repository.getAttendanceHistory(studentId)
}

class GetTeacherDashboardUseCase @Inject constructor(
    private val repository: TeacherRepository,
) {
    suspend operator fun invoke(teacherId: String): Outcome<TeacherDashboard> =
        repository.getDashboard(teacherId)
}

class GetAdminDashboardUseCase @Inject constructor(
    private val repository: AdminRepository,
) {
    suspend operator fun invoke(adminId: String): Outcome<AdminDashboard> =
        repository.getDashboard(adminId)
}

/**
 * Reads the audit trail. The page size is fixed here rather than exposed to the
 * screen: the trail is for review, not for bulk export.
 */
class GetAuditTrailUseCase @Inject constructor(
    private val repository: AdminRepository,
) {
    suspend operator fun invoke(): Outcome<List<AuditEntry>> = repository.getAuditTrail(PAGE_SIZE)

    private companion object {
        const val PAGE_SIZE = 100
    }
}

class GetNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository,
) {
    suspend operator fun invoke(userId: String): Outcome<List<NotificationItem>> =
        repository.getNotifications(userId)
}

class MarkNotificationReadUseCase @Inject constructor(
    private val repository: NotificationRepository,
) {
    suspend operator fun invoke(userId: String, notificationId: String): Outcome<Unit> =
        repository.markAsRead(userId, notificationId)
}
