package com.cit.attendance.domain.model

import java.time.LocalDate

/** A point on a trend line (Section 28). */
data class TrendPoint(
    val label: String,
    val held: Int,
    val attended: Int,
    val percentage: Float,
)

/** A day on the attendance calendar. */
data class CalendarEntry(
    val date: LocalDate,
    val present: Int,
    val absent: Int,
    val late: Int,
    val status: DayStatus,
)

enum class DayStatus { PRESENT, PARTIAL, ABSENT }

/** Counts behind the headline percentage. */
data class AttendanceTotals(
    val held: Int,
    val present: Int,
    val late: Int,
    val absent: Int,
)

/** How confident the engine is, and why. */
enum class RiskBand { LOW, MODERATE, HIGH }

/**
 * The output of the risk scoring engine (Section 31).
 *
 * [method] is carried all the way to the interface deliberately: the product
 * shows how the projection was produced rather than implying a learned model.
 */
data class RiskAssessment(
    val currentPercentage: Float,
    val projectedPercentage: Float,
    val band: RiskBand,
    val reason: String,
    val method: String,
) {
    /** True when the projection is worse than where the student stands today. */
    val isDeclining: Boolean get() = projectedPercentage < currentPercentage
}

/** Attendance for one subject, as analytics reports it. */
data class SubjectAnalytics(
    val subjectId: String,
    val subjectCode: String,
    val subjectName: String,
    val held: Int,
    val attended: Int,
    val percentage: Float,
    val belowThreshold: Boolean,
)

data class StudentAnalytics(
    val overallPercentage: Float,
    val threshold: Int,
    val standing: AttendanceStanding,
    val totals: AttendanceTotals,
    val monthlyTrend: List<TrendPoint>,
    val subjects: List<SubjectAnalytics>,
    val calendar: List<CalendarEntry>,
    val risk: RiskAssessment,
)

/** Absences on one teaching day. */
data class AbsenteePoint(
    val date: LocalDate,
    val absent: Int,
    val held: Int,
)

/** A student the teacher should look at, with the reason spelled out. */
data class RiskStudent(
    val studentId: String,
    val name: String,
    val registerNumber: String,
    val avatarSeed: String,
    /** Overall attendance, which is what the requirement measures. */
    val percentage: Float,
    /** The same student counting only this teacher's subjects. */
    val inYourSubjects: Float,
    val projectedPercentage: Float,
    val band: RiskBand,
    val reason: String,
) {
    val initials: String
        get() = name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
}

data class TeacherAnalytics(
    val classAverage: Float,
    val threshold: Int,
    val monthlyTrend: List<TrendPoint>,
    val subjectAverages: List<SubjectAnalytics>,
    val absenteeTrend: List<AbsenteePoint>,
    val studentsAtRisk: List<RiskStudent>,
)

data class AdminClassSection(
    val id: String,
    val name: String,
    val studentCount: Int,
    val averageAttendance: Float,
)

data class AdminDepartment(
    val department: String,
    val studentCount: Int,
    val averageAttendance: Float,
    val studentsBelowThreshold: Int,
    val classSections: List<AdminClassSection>,
)

data class AdminAnalytics(
    val institutionAverage: Float,
    val threshold: Int,
    val totalStudents: Int,
    val studentsBelowThreshold: Int,
    val monthlyTrend: List<TrendPoint>,
    val departments: List<AdminDepartment>,
)

/** The report types a user can ask for (Section 33). */
enum class ReportType(val label: String, val description: String) {
    STUDENT_ATTENDANCE("Student attendance", "Every recorded session for one student"),
    SUBJECT_ATTENDANCE("Subject attendance", "One row per student, for a single subject"),
    CLASS_ATTENDANCE("Class attendance", "The whole cohort with standing"),
    LOW_ATTENDANCE("Low attendance", "Only students under the requirement"),
    ABSENTEE("Absentee", "Every individual absence in the period"),
}

enum class ReportFormat(val label: String, val extension: String) {
    PDF("PDF", "pdf"),
    XLSX("Excel", "xlsx"),
}

/** A generated report, already written to a file the user can open. */
data class GeneratedReport(
    val fileName: String,
    val absolutePath: String,
    val sizeBytes: Long,
    val format: ReportFormat,
)
