package com.cit.attendance.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The threshold rules decide what a student is told about their standing, so the
 * boundaries are pinned down explicitly rather than assumed.
 */
class AttendancePolicyTest {

    private val policy = AttendancePolicy(thresholdPercent = 75)

    @Test
    fun `exactly at the threshold counts as good`() {
        assertEquals(AttendanceStanding.GOOD, policy.standingFor(75f))
        assertFalse(policy.isBelowThreshold(75f))
    }

    @Test
    fun `just under the threshold is a warning, not high risk`() {
        assertEquals(AttendanceStanding.WARNING, policy.standingFor(74.9f))
        assertTrue(policy.isBelowThreshold(74.9f))
    }

    @Test
    fun `ten points under the threshold is still a warning`() {
        assertEquals(AttendanceStanding.WARNING, policy.standingFor(65f))
    }

    @Test
    fun `more than ten points under the threshold is high risk`() {
        assertEquals(AttendanceStanding.HIGH_RISK, policy.standingFor(64.9f))
        assertEquals(AttendanceStanding.HIGH_RISK, policy.standingFor(0f))
    }

    @Test
    fun `a student already above the threshold needs no further classes`() {
        assertEquals(0, policy.classesNeededToReachThreshold(attended = 80, held = 100))
    }

    @Test
    fun `recovery count is the smallest number that actually reaches the threshold`() {
        val attended = 70
        val held = 100
        val needed = policy.classesNeededToReachThreshold(attended, held)

        // 70/100 = 70%. Attending 20 more gives 90/120 = 75% exactly.
        assertEquals(20, needed)

        val after = ((attended + needed) * 100f) / (held + needed)
        assertTrue("expected to reach the threshold, got $after", after >= 75f)

        val oneFewer = ((attended + needed - 1) * 100f) / (held + needed - 1)
        assertTrue("one fewer class should fall short, got $oneFewer", oneFewer < 75f)
    }

    @Test
    fun `a student with no classes held is not asked to attend any`() {
        assertEquals(0, policy.classesNeededToReachThreshold(attended = 0, held = 0))
    }

    @Test
    fun `the threshold is configuration, not a constant`() {
        val strict = AttendancePolicy(thresholdPercent = 85)
        assertEquals(AttendanceStanding.WARNING, strict.standingFor(80f))
        assertEquals(AttendanceStanding.GOOD, policy.standingFor(80f))
    }
}
