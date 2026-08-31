package com.cit.attendance.domain.repository

import com.cit.attendance.core.common.Outcome
import com.cit.attendance.domain.model.AdminDashboard
import com.cit.attendance.domain.model.AttendanceRecord
import com.cit.attendance.domain.model.AuditEntry
import com.cit.attendance.domain.model.NotificationItem
import com.cit.attendance.domain.model.Student
import com.cit.attendance.domain.model.StudentDashboard
import com.cit.attendance.domain.model.TeacherDashboard
import com.cit.attendance.domain.model.TeachingCohort

/**
 * Student-facing reads (Section 14).
 *
 * Every call is scoped to a student id rather than "the current user" so that
 * the server can authorise each request independently in Milestone 2.
 */
interface StudentRepository {
    suspend fun getDashboard(studentId: String): Outcome<StudentDashboard>
    suspend fun getProfile(studentId: String): Outcome<Student>
    suspend fun getAttendanceHistory(studentId: String): Outcome<List<AttendanceRecord>>
}

/** Teacher-facing reads (Section 15). */
interface TeacherRepository {
    suspend fun getDashboard(teacherId: String): Outcome<TeacherDashboard>

    /** The cohorts this teacher may open an attendance session for. */
    suspend fun getCohorts(): Outcome<List<TeachingCohort>>
}

/** Institution-wide reads (Section 16). */
interface AdminRepository {
    suspend fun getDashboard(adminId: String): Outcome<AdminDashboard>

    /** The audit trail, newest first (Section 35). */
    suspend fun getAuditTrail(limit: Int): Outcome<List<AuditEntry>>
}

/**
 * Notifications (Section 32).
 *
 * The in-app list and its read/unread state are independent of push delivery,
 * so this contract is complete without Firebase being configured.
 */
interface NotificationRepository {
    suspend fun getNotifications(userId: String): Outcome<List<NotificationItem>>
    suspend fun markAsRead(userId: String, notificationId: String): Outcome<Unit>
}
