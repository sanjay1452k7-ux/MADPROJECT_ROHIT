package com.cit.attendance.data.mapper

import com.cit.attendance.core.network.dto.AbsenteePointDto
import com.cit.attendance.core.network.dto.AdminAnalyticsDto
import com.cit.attendance.core.network.dto.AnalyticsSubjectDto
import com.cit.attendance.core.network.dto.CalendarDayDto
import com.cit.attendance.core.network.dto.RiskAssessmentDto
import com.cit.attendance.core.network.dto.RiskStudentDto
import com.cit.attendance.core.network.dto.StudentAnalyticsDto
import com.cit.attendance.core.network.dto.TeacherAnalyticsDto
import com.cit.attendance.core.network.dto.TrendPointDto
import com.cit.attendance.domain.model.AbsenteePoint
import com.cit.attendance.domain.model.AdminAnalytics
import com.cit.attendance.domain.model.AdminClassSection
import com.cit.attendance.domain.model.AdminDepartment
import com.cit.attendance.domain.model.AttendanceTotals
import com.cit.attendance.domain.model.CalendarEntry
import com.cit.attendance.domain.model.DayStatus
import com.cit.attendance.domain.model.RiskAssessment
import com.cit.attendance.domain.model.RiskBand
import com.cit.attendance.domain.model.RiskStudent
import com.cit.attendance.domain.model.StudentAnalytics
import com.cit.attendance.domain.model.SubjectAnalytics
import com.cit.attendance.domain.model.TeacherAnalytics
import com.cit.attendance.domain.model.TrendPoint
import java.time.LocalDate

/**
 * Analytics wire-to-domain translation.
 *
 * As elsewhere, parsing is forgiving: an unparseable date drops one point off a
 * chart rather than failing the whole screen.
 */

internal fun String.toRiskBand(): RiskBand = when (this) {
    "LOW" -> RiskBand.LOW
    "MODERATE" -> RiskBand.MODERATE
    else -> RiskBand.HIGH
}

internal fun String.toDayStatus(): DayStatus = when (this) {
    "PRESENT" -> DayStatus.PRESENT
    "PARTIAL" -> DayStatus.PARTIAL
    else -> DayStatus.ABSENT
}

private fun parseIsoDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value) }.getOrNull()

internal fun TrendPointDto.toDomain(): TrendPoint = TrendPoint(
    label = label,
    held = held,
    attended = attended,
    percentage = percentage,
)

internal fun AnalyticsSubjectDto.toDomain(): SubjectAnalytics = SubjectAnalytics(
    subjectId = subjectId,
    subjectCode = subjectCode,
    subjectName = subjectName,
    held = held,
    attended = attended,
    percentage = percentage,
    belowThreshold = belowThreshold,
)

internal fun CalendarDayDto.toDomain(): CalendarEntry? {
    val parsed = parseIsoDate(date) ?: return null
    return CalendarEntry(
        date = parsed,
        present = present,
        absent = absent,
        late = late,
        status = status.toDayStatus(),
    )
}

internal fun RiskAssessmentDto.toDomain(): RiskAssessment = RiskAssessment(
    currentPercentage = currentPercentage,
    projectedPercentage = projectedPercentage,
    band = band.toRiskBand(),
    reason = reason,
    method = method,
)

internal fun StudentAnalyticsDto.toDomain(): StudentAnalytics = StudentAnalytics(
    overallPercentage = overallPercentage,
    threshold = threshold,
    standing = standing.toStanding(),
    totals = AttendanceTotals(
        held = totals.held,
        present = totals.present,
        late = totals.late,
        absent = totals.absent,
    ),
    monthlyTrend = monthlyTrend.map { it.toDomain() },
    subjects = subjectBreakdown.map { it.toDomain() },
    calendar = calendar.mapNotNull { it.toDomain() },
    risk = risk.toDomain(),
)

internal fun AbsenteePointDto.toDomain(): AbsenteePoint? {
    val parsed = parseIsoDate(date) ?: return null
    return AbsenteePoint(date = parsed, absent = absent, held = held)
}

internal fun RiskStudentDto.toDomain(): RiskStudent = RiskStudent(
    studentId = studentId,
    name = name,
    registerNumber = registerNumber,
    avatarSeed = avatarSeed,
    percentage = percentage,
    inYourSubjects = inYourSubjects,
    projectedPercentage = projectedPercentage,
    band = band.toRiskBand(),
    reason = reason,
)

internal fun TeacherAnalyticsDto.toDomain(): TeacherAnalytics = TeacherAnalytics(
    classAverage = classAverage,
    threshold = threshold,
    monthlyTrend = monthlyTrend.map { it.toDomain() },
    subjectAverages = subjectAverages.map { it.toDomain() },
    absenteeTrend = absenteeTrend.mapNotNull { it.toDomain() },
    studentsAtRisk = studentsAtRisk.map { it.toDomain() },
)

internal fun AdminAnalyticsDto.toDomain(): AdminAnalytics = AdminAnalytics(
    institutionAverage = institutionAverage,
    threshold = threshold,
    totalStudents = totalStudents,
    studentsBelowThreshold = studentsBelowThreshold,
    monthlyTrend = monthlyTrend.map { it.toDomain() },
    departments = departmentStats.map { department ->
        AdminDepartment(
            department = department.department,
            studentCount = department.studentCount,
            averageAttendance = department.averageAttendance,
            studentsBelowThreshold = department.studentsBelowThreshold,
            classSections = department.classSections.map {
                AdminClassSection(
                    id = it.id,
                    name = it.name,
                    studentCount = it.studentCount,
                    averageAttendance = it.averageAttendance,
                )
            },
        )
    },
)
