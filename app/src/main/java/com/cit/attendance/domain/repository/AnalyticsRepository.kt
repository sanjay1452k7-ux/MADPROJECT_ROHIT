package com.cit.attendance.domain.repository

import com.cit.attendance.core.common.Outcome
import com.cit.attendance.domain.model.AdminAnalytics
import com.cit.attendance.domain.model.GeneratedReport
import com.cit.attendance.domain.model.ReportFormat
import com.cit.attendance.domain.model.ReportType
import com.cit.attendance.domain.model.StudentAnalytics
import com.cit.attendance.domain.model.TeacherAnalytics

/**
 * Analytics reads (Section 28).
 *
 * Each call is scoped by the server to the signed-in user's role, so there is
 * deliberately no "analytics for student X" variant here for a student to call.
 */
interface AnalyticsRepository {
    suspend fun studentAnalytics(): Outcome<StudentAnalytics>
    suspend fun teacherAnalytics(): Outcome<TeacherAnalytics>
    suspend fun adminAnalytics(): Outcome<AdminAnalytics>
}

/**
 * Report generation (Section 33).
 *
 * The file is rendered by the server and written to app storage here, so the
 * two formats can never disagree about the figures.
 */
interface ReportsRepository {
    suspend fun generate(
        type: ReportType,
        format: ReportFormat,
        subjectId: String? = null,
        classSectionId: String? = null,
    ): Outcome<GeneratedReport>
}
