package com.cit.attendance.core.network.dto

import kotlinx.serialization.Serializable

/** Wire types for analytics and reports (Sections 28, 31 and 33). */

@Serializable
data class TrendPointDto(
    val period: String,
    val label: String,
    val held: Int,
    val attended: Int,
    val percentage: Float,
)

@Serializable
data class CalendarDayDto(
    val date: String,
    val present: Int,
    val absent: Int,
    val late: Int,
    val status: String,
)

@Serializable
data class AttendanceTotalsDto(
    val held: Int,
    val present: Int,
    val late: Int,
    val absent: Int,
)

/**
 * The risk assessment. [method] states how the figure was produced, and is
 * shown in the interface so the projection is never mistaken for a prediction
 * from a learned model (Section 31).
 */
@Serializable
data class RiskAssessmentDto(
    val currentPercentage: Float,
    val projectedPercentage: Float,
    val band: String,
    val reason: String,
    val method: String,
)

@Serializable
data class AnalyticsSubjectDto(
    val subjectId: String,
    val subjectCode: String,
    val subjectName: String,
    val held: Int,
    val attended: Int,
    val percentage: Float,
    val belowThreshold: Boolean = false,
)

@Serializable
data class StudentAnalyticsDto(
    val overallPercentage: Float,
    val threshold: Int,
    val standing: String,
    val totals: AttendanceTotalsDto,
    val monthlyTrend: List<TrendPointDto>,
    val subjectBreakdown: List<AnalyticsSubjectDto>,
    val calendar: List<CalendarDayDto>,
    val risk: RiskAssessmentDto,
)

@Serializable
data class AbsenteePointDto(
    val date: String,
    val absent: Int,
    val held: Int,
)

@Serializable
data class RiskStudentDto(
    val studentId: String,
    val name: String,
    val registerNumber: String,
    val avatarSeed: String,
    val percentage: Float,
    val inYourSubjects: Float,
    val projectedPercentage: Float,
    val band: String,
    val reason: String,
)

@Serializable
data class TeacherAnalyticsDto(
    val classAverage: Float,
    val threshold: Int,
    val monthlyTrend: List<TrendPointDto>,
    val subjectAverages: List<AnalyticsSubjectDto>,
    val absenteeTrend: List<AbsenteePointDto>,
    val studentsAtRisk: List<RiskStudentDto>,
)

@Serializable
data class AdminClassSectionDto(
    val id: String,
    val name: String,
    val studentCount: Int,
    val averageAttendance: Float,
)

@Serializable
data class AdminDepartmentDto(
    val department: String,
    val studentCount: Int,
    val averageAttendance: Float,
    val studentsBelowThreshold: Int,
    val classSections: List<AdminClassSectionDto>,
)

@Serializable
data class AdminAnalyticsDto(
    val institutionAverage: Float,
    val threshold: Int,
    val totalStudents: Int,
    val studentsBelowThreshold: Int,
    val monthlyTrend: List<TrendPointDto>,
    val departmentStats: List<AdminDepartmentDto>,
)

@Serializable
data class GenerateReportRequest(
    val type: String,
    val format: String,
    val from: String? = null,
    val to: String? = null,
    val subjectId: String? = null,
    val classSectionId: String? = null,
    val studentId: String? = null,
)

@Serializable
data class RegisterDeviceRequest(
    val token: String,
    val platform: String,
)
