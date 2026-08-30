package com.cit.attendance.domain.model

import java.time.LocalTime

/** A taught subject. [code] is what appears on reports and timetables. */
data class Subject(
    val id: String,
    val code: String,
    val name: String,
)

/** A class-section cohort, e.g. CSE / III Year / Section A. */
data class ClassSection(
    val id: String,
    val department: String,
    val year: Int,
    val section: String,
) {
    /** "CSE - III Year - A", the form used across dashboards and reports. */
    val displayName: String get() = "$department - ${romanYear(year)} Year - $section"

    /** "III YEAR - SECTION A", used on the student identity block. */
    val longForm: String get() = "${romanYear(year)} YEAR - SECTION $section"

    private fun romanYear(value: Int): String = when (value) {
        1 -> "I"
        2 -> "II"
        3 -> "III"
        4 -> "IV"
        else -> value.toString()
    }
}

/** One timetabled slot on a given day. */
data class ScheduledClass(
    val id: String,
    val subject: Subject,
    val classSection: ClassSection,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val room: String,
    val teacherName: String,
    /**
     * True when a teacher has an attendance session open for this slot.
     * Milestone 1 seeds this from demo data; Milestone 2 derives it from the
     * backend session state.
     */
    val hasActiveSession: Boolean = false,
)
