package com.cit.attendance.domain.model

import java.time.LocalDate
import java.time.LocalTime

/** How a student was accounted for in one session. */
enum class AttendanceStatus { PRESENT, ABSENT, LATE }

/** A single recorded attendance event. */
data class AttendanceRecord(
    val id: String,
    val date: LocalDate,
    val subject: Subject,
    val status: AttendanceStatus,
    /** Null when the student was absent - nothing was ever scanned. */
    val markedAt: LocalTime?,
)

/** Attendance for one subject, from which the percentage is derived. */
data class SubjectAttendance(
    val subject: Subject,
    val attended: Int,
    val held: Int,
) {
    /** Percentage attended. A subject with no held classes reads as 0. */
    val percentage: Float get() = if (held == 0) 0f else (attended * 100f) / held
}

/**
 * Where a percentage sits against the institutional threshold (Section 30).
 *
 * Wording stays factual - never alarmist.
 */
enum class AttendanceStanding {
    GOOD,
    WARNING,
    HIGH_RISK,
    ;

    val label: String
        get() = when (this) {
            GOOD -> "GOOD"
            WARNING -> "WARNING"
            HIGH_RISK -> "HIGH RISK"
        }
}

/**
 * Classifies a percentage against the configured threshold.
 *
 * The threshold is configuration, not a literal buried in a screen: it is
 * supplied from BuildConfig through DI so an institution can change it without
 * touching feature code.
 */
class AttendancePolicy(private val thresholdPercent: Int) {

    val threshold: Int get() = thresholdPercent

    /** More than ten points under the threshold is treated as high risk. */
    private val highRiskFloor: Float get() = thresholdPercent - 10f

    fun standingFor(percentage: Float): AttendanceStanding = when {
        percentage >= thresholdPercent -> AttendanceStanding.GOOD
        percentage >= highRiskFloor -> AttendanceStanding.WARNING
        else -> AttendanceStanding.HIGH_RISK
    }

    fun isBelowThreshold(percentage: Float): Boolean = percentage < thresholdPercent
}

/** A student's overall attendance position. */
data class AttendanceSummary(
    val overallPercentage: Float,
    val standing: AttendanceStanding,
    val subjectBreakdown: List<SubjectAttendance>,
)
