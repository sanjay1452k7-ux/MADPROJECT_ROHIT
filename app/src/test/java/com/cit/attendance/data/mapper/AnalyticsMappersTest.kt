package com.cit.attendance.data.mapper

import com.cit.attendance.core.network.dto.AbsenteePointDto
import com.cit.attendance.core.network.dto.AnalyticsSubjectDto
import com.cit.attendance.core.network.dto.AttendanceTotalsDto
import com.cit.attendance.core.network.dto.CalendarDayDto
import com.cit.attendance.core.network.dto.RiskAssessmentDto
import com.cit.attendance.core.network.dto.StudentAnalyticsDto
import com.cit.attendance.core.network.dto.TrendPointDto
import com.cit.attendance.domain.model.AttendanceStanding
import com.cit.attendance.domain.model.DayStatus
import com.cit.attendance.domain.model.RiskBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * These mappers sit between a server we do not control and screens the user is
 * looking at, so malformed input must degrade one field rather than fail a
 * whole screen.
 */
class AnalyticsMappersTest {

    private fun risk(
        current: Float = 80f,
        projected: Float = 78f,
        band: String = "MODERATE",
    ) = RiskAssessmentDto(
        currentPercentage = current,
        projectedPercentage = projected,
        band = band,
        reason = "Attendance has declined.",
        method = "Rule-based projection from recent attendance rate",
    )

    @Test
    fun `risk bands map across the whole set`() {
        assertEquals(RiskBand.LOW, risk(band = "LOW").toDomain().band)
        assertEquals(RiskBand.MODERATE, risk(band = "MODERATE").toDomain().band)
        assertEquals(RiskBand.HIGH, risk(band = "HIGH").toDomain().band)
    }

    @Test
    fun `an unknown risk band is treated as the most serious`() {
        // Failing safe: an unrecognised band must not silently read as "fine".
        assertEquals(RiskBand.HIGH, risk(band = "SOMETHING_NEW").toDomain().band)
    }

    @Test
    fun `the scoring method is carried through to the interface`() {
        val assessment = risk().toDomain()

        assertEquals("Rule-based projection from recent attendance rate", assessment.method)
    }

    @Test
    fun `a falling projection is reported as declining`() {
        assertTrue(risk(current = 80f, projected = 74f).toDomain().isDeclining)
        assertFalse(risk(current = 74f, projected = 80f).toDomain().isDeclining)
    }

    @Test
    fun `calendar days map their status`() {
        val present = CalendarDayDto("2026-08-24", 3, 0, 0, "PRESENT").toDomain()
        val partial = CalendarDayDto("2026-08-25", 2, 1, 0, "PARTIAL").toDomain()
        val absent = CalendarDayDto("2026-08-26", 0, 3, 0, "ABSENT").toDomain()

        assertEquals(DayStatus.PRESENT, present?.status)
        assertEquals(DayStatus.PARTIAL, partial?.status)
        assertEquals(DayStatus.ABSENT, absent?.status)
        assertEquals(LocalDate.of(2026, 8, 24), present?.date)
    }

    @Test
    fun `a malformed calendar date drops that day rather than failing`() {
        assertNull(CalendarDayDto("not-a-date", 1, 0, 0, "PRESENT").toDomain())
    }

    @Test
    fun `a malformed absentee date drops that point rather than failing`() {
        assertNull(AbsenteePointDto("31-08-2026", 4, 30).toDomain())
        assertEquals(4, AbsenteePointDto("2026-08-31", 4, 30).toDomain()?.absent)
    }

    @Test
    fun `a student analytics payload maps whole`() {
        val dto = StudentAnalyticsDto(
            overallPercentage = 87.4f,
            threshold = 75,
            standing = "GOOD",
            totals = AttendanceTotalsDto(held = 174, present = 152, late = 0, absent = 22),
            monthlyTrend = listOf(
                TrendPointDto("2026-07", "Jul 2026", 80, 72, 90f),
                TrendPointDto("2026-08", "Aug 2026", 94, 80, 85.1f),
            ),
            subjectBreakdown = listOf(
                AnalyticsSubjectDto("s1", "CS8501", "Mobile Application Development", 30, 27, 90f),
            ),
            calendar = listOf(
                CalendarDayDto("2026-08-24", 2, 0, 0, "PRESENT"),
                CalendarDayDto("broken", 1, 0, 0, "PRESENT"),
            ),
            risk = risk(),
        )

        val domain = dto.toDomain()

        assertEquals(87.4f, domain.overallPercentage, 0.001f)
        assertEquals(AttendanceStanding.GOOD, domain.standing)
        assertEquals(2, domain.monthlyTrend.size)
        assertEquals("Jul 2026", domain.monthlyTrend.first().label)
        assertEquals(1, domain.subjects.size)
        // The broken calendar entry is dropped; the good one survives.
        assertEquals(1, domain.calendar.size)
        assertEquals(22, domain.totals.absent)
    }

    @Test
    fun `a subject below the requirement keeps that flag`() {
        val dto = AnalyticsSubjectDto(
            subjectId = "s1",
            subjectCode = "CS8505",
            subjectName = "Theory of Computation",
            held = 26,
            attended = 18,
            percentage = 69.2f,
            belowThreshold = true,
        )

        assertTrue(dto.toDomain().belowThreshold)
    }
}
