package com.cit.attendance.data.demo

import com.cit.attendance.domain.config.InstitutionConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The demo dataset is what faculty will see, so its internal consistency matters:
 * a per-subject table that does not add up to the headline percentage would be
 * visible on screen.
 */
class DemoDataTest {

    private val config = InstitutionConfig(
        name = "Chennai Institute of Technology",
        emailDomain = "citchennai.net",
        attendanceThresholdPercent = 75,
    )
    private val directory = DemoDirectory(config)

    @Test
    fun `the primary demonstration student is present with the required details`() {
        val sanjayan = directory.primaryStudent

        assertEquals("Sanjayan A", sanjayan.name)
        assertEquals("24CS0845", sanjayan.registerNumber)
        assertEquals("CSE", sanjayan.department)
        assertEquals(3, sanjayan.year)
        assertEquals("A", sanjayan.section)
    }

    @Test
    fun `the second named student is present in the same cohort`() {
        val rohit = directory.students.first { it.name == "Rohit" }

        assertEquals("24CS0812", rohit.registerNumber)
        assertEquals("CSE", rohit.department)
        assertEquals(3, rohit.year)
        assertEquals("A", rohit.section)
    }

    @Test
    fun `register numbers are unique across the cohort`() {
        val registers = directory.students.map { it.registerNumber }
        assertEquals(registers.size, registers.toSet().size)
    }

    @Test
    fun `subject breakdown always sums to the authored attendance total`() {
        directory.students.forEach { student ->
            val expected = DemoPeople.attendedByRegister.getValue(student.registerNumber)
            val actual = DemoAttendance.subjectBreakdown(student).sumOf { it.attended }

            assertEquals(
                "breakdown for ${student.registerNumber} must match the total",
                expected,
                actual,
            )
        }
    }

    @Test
    fun `no subject reports more attended classes than were held`() {
        directory.students.forEach { student ->
            DemoAttendance.subjectBreakdown(student).forEach { subject ->
                assertTrue(
                    "${student.registerNumber} / ${subject.subject.code} exceeds classes held",
                    subject.attended in 0..subject.held,
                )
            }
        }
    }

    @Test
    fun `the demonstration student reads as 87 point 4 percent`() {
        val percentage = DemoAttendance.overallPercentage(directory.primaryStudent)
        assertEquals(87.4f, percentage, 0.05f)
    }

    @Test
    fun `the breakdown is deterministic across repeated reads`() {
        val first = DemoAttendance.subjectBreakdown(directory.primaryStudent)
        val second = DemoAttendance.subjectBreakdown(directory.primaryStudent)
        assertEquals(first, second)
    }

    @Test
    fun `the cohort contains a realistic spread of standings`() {
        val percentages = directory.students.map { DemoAttendance.overallPercentage(it) }

        assertTrue("expected students above 85%", percentages.any { it >= 85f })
        assertTrue("expected students between 75% and 85%", percentages.any { it in 75f..85f })
        assertTrue("expected students below the threshold", percentages.any { it < 75f })
        assertTrue("expected at least one clearly at-risk student", percentages.any { it < 65f })
    }

    @Test
    fun `every account resolves against the configured institution domain`() {
        directory.students.forEach { student ->
            assertTrue(
                "${student.email} is not on the configured domain",
                config.isInstitutionEmail(student.email),
            )
        }
        directory.teachers.forEach { teacher ->
            assertTrue(
                "${teacher.email} is not on the configured domain",
                config.isInstitutionEmail(teacher.email),
            )
        }
        assertTrue(config.isInstitutionEmail(directory.administrator.email))
    }

    @Test
    fun `students below the threshold are reported worst first`() {
        val atRisk = directory.studentsBelow(75)

        assertTrue(atRisk.isNotEmpty())
        atRisk.zipWithNext().forEach { (lower, higher) ->
            assertTrue("at-risk list must be sorted ascending", lower.second <= higher.second)
        }
        atRisk.forEach { (_, percentage) -> assertTrue(percentage < 75f) }
    }

    @Test
    fun `history reflects the student's own attendance level`() {
        val strongest = directory.students.maxBy { DemoAttendance.overallPercentage(it) }
        val weakest = directory.students.minBy { DemoAttendance.overallPercentage(it) }

        val strongPresent = DemoAttendance.history(strongest).count {
            it.status != com.cit.attendance.domain.model.AttendanceStatus.ABSENT
        }
        val weakPresent = DemoAttendance.history(weakest).count {
            it.status != com.cit.attendance.domain.model.AttendanceStatus.ABSENT
        }

        assertTrue(
            "the strongest student should not show fewer attended days than the weakest",
            strongPresent >= weakPresent,
        )
    }

    @Test
    fun `an absent record carries no marked time`() {
        directory.students.take(5).forEach { student ->
            DemoAttendance.history(student).forEach { record ->
                if (record.status == com.cit.attendance.domain.model.AttendanceStatus.ABSENT) {
                    assertEquals(null, record.markedAt)
                } else {
                    assertNotNull(record.markedAt)
                }
            }
        }
    }

    @Test
    fun `the cohort average matches the mean of the individual percentages`() {
        val manual = directory.students
            .map { DemoAttendance.overallPercentage(it) }
            .average()
            .toFloat()

        assertTrue(abs(directory.cohortAverage() - manual) < 0.01f)
    }
}
