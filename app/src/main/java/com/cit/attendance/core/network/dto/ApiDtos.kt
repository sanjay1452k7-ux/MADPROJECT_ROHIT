package com.cit.attendance.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire types for the attendance API.
 *
 * These mirror the server's JSON exactly and are deliberately separate from the
 * domain models: the API may change shape without that leaking into the rest of
 * the application. Mappers in the data layer translate between the two.
 */

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val role: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class AuthUserDto(
    val id: String,
    val displayName: String,
    val email: String,
    val role: String,
)

@Serializable
data class AuthTokensDto(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthUserDto,
)

/** The server's failure envelope: a stable code plus wording (Section 37). */
@Serializable
data class ApiErrorDto(
    val code: String,
    val message: String,
    val actualRole: String? = null,
    val markedAt: String? = null,
    val studentName: String? = null,
    val registerNumber: String? = null,
    val subjectName: String? = null,
)

@Serializable
data class StudentDto(
    val id: String,
    val name: String,
    val registerNumber: String,
    val email: String,
    val department: String,
    val year: Int,
    val section: String,
    val avatarSeed: String,
)

@Serializable
data class SubjectAttendanceDto(
    val subjectId: String,
    val subjectCode: String,
    val subjectName: String,
    val attended: Int,
    val held: Int,
    val percentage: Float,
)

@Serializable
data class AttendanceSummaryDto(
    val overallPercentage: Float,
    val standing: String,
    val subjectBreakdown: List<SubjectAttendanceDto>,
)

@Serializable
data class ScheduledClassDto(
    val id: String,
    val subjectId: String,
    val subjectCode: String,
    val subjectName: String,
    val startTime: String,
    val endTime: String,
    val room: String,
    val teacherName: String,
    val hasActiveSession: Boolean,
)

@Serializable
data class AttendanceRecordDto(
    val id: String,
    val date: String,
    val subjectCode: String,
    val subjectName: String,
    val status: String,
    val markedAt: String? = null,
)

@Serializable
data class StudentDashboardDto(
    val student: StudentDto,
    val summary: AttendanceSummaryDto,
    val todaysClasses: List<ScheduledClassDto>,
    val recentRecords: List<AttendanceRecordDto>,
    val unreadNotifications: Int,
)

@Serializable
data class ActiveSessionDto(
    val id: String,
    val subjectName: String,
    val subjectCode: String,
    val teacherName: String,
    val startedAt: String,
    val alreadyMarked: Boolean,
    val markedAt: String? = null,
)

@Serializable
data class MintedTokenDto(
    val token: String,
    val expiresAt: String,
    val validForSeconds: Int,
)

@Serializable
data class VerifiedStudentDto(
    val verificationId: String,
    val student: VerifiedStudentDetailsDto,
    val session: VerifiedSessionDto,
)

@Serializable
data class VerifiedStudentDetailsDto(
    val id: String,
    val name: String,
    val registerNumber: String,
    val department: String,
    val year: Int,
    val section: String,
    val avatarSeed: String,
    val attendancePercentage: Float,
)

@Serializable
data class VerifiedSessionDto(
    val id: String,
    val subjectName: String,
    val subjectCode: String,
)

@Serializable
data class VerifyTokenRequest(val token: String)

@Serializable
data class MarkAttendanceRequest(val verificationId: String)

@Serializable
data class ManualAttendanceRequest(
    val studentId: String,
    val status: String,
    val reason: String? = null,
)

@Serializable
data class MarkResultDto(
    val recordId: String,
    val studentName: String,
    val registerNumber: String,
    val subjectName: String,
    val markedAt: String,
)

@Serializable
data class StartSessionRequest(
    val subjectId: String,
    val classSectionId: String,
)

@Serializable
data class SessionStudentDto(
    val id: String,
    val name: String,
    val registerNumber: String,
    val avatarSeed: String,
    val status: String? = null,
    val markedAt: String? = null,
)

@Serializable
data class SessionSummaryDto(
    val id: String,
    val status: String,
    val subjectName: String,
    val subjectCode: String,
    val className: String,
    val teacherName: String,
    val startedAt: String,
    val closedAt: String? = null,
    val presentCount: Int,
    val totalStudents: Int,
    val students: List<SessionStudentDto>,
)

@Serializable
data class TeacherSubjectDto(
    val id: String,
    val code: String,
    val name: String,
)

@Serializable
data class TeacherDto(
    val id: String,
    val name: String,
    val employeeCode: String,
    val email: String,
    val department: String,
    val avatarSeed: String,
    val subjects: List<TeacherSubjectDto>,
)

@Serializable
data class TeacherClassDto(
    val id: String,
    val subjectId: String,
    val subjectCode: String,
    val subjectName: String,
    val classSectionId: String,
    val className: String,
    val startTime: String,
    val endTime: String,
    val room: String,
    val hasActiveSession: Boolean,
)

@Serializable
data class ClassSectionDto(
    val id: String,
    val department: String,
    val year: Int,
    val section: String,
    val name: String,
)

@Serializable
data class StudentAtRiskDto(
    val studentId: String,
    val name: String,
    val registerNumber: String,
    val avatarSeed: String,
    val percentage: Float,
    val standing: String,
)

@Serializable
data class ActiveSessionRefDto(
    val id: String,
    val subjectId: String,
    val subjectName: String,
)

@Serializable
data class TeacherDashboardDto(
    val teacher: TeacherDto,
    val todaysClasses: List<TeacherClassDto>,
    val activeSessions: List<ActiveSessionRefDto>,
    val classSections: List<ClassSectionDto>,
    val averageAttendance: Float,
    val studentsAtRisk: List<StudentAtRiskDto>,
)

@Serializable
data class DepartmentStatDto(
    val department: String,
    val studentCount: Int,
    val averageAttendance: Float,
)

@Serializable
data class AdministratorDto(
    val id: String,
    val name: String,
    val email: String,
    val designation: String,
)

@Serializable
data class AdminDashboardDto(
    val administrator: AdministratorDto,
    val totalStudents: Int,
    val totalFaculty: Int,
    val presentToday: Int,
    val sessionsToday: Int,
    val averageAttendance: Float,
    val studentsAtRisk: Int,
    val departmentStats: List<DepartmentStatDto>,
)

@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val timestamp: String,
    @SerialName("isRead") val isRead: Boolean,
)
