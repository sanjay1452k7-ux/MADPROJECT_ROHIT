package com.cit.attendance.data.demo

import com.cit.attendance.domain.model.AttendanceRecord
import com.cit.attendance.domain.model.AttendanceStatus
import com.cit.attendance.domain.model.NotificationItem
import com.cit.attendance.domain.model.NotificationType
import com.cit.attendance.domain.model.Student
import com.cit.attendance.domain.model.SubjectAttendance
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.abs

/**
 * Derives per-subject attendance and history from each student's hand-authored
 * total (Section 26).
 *
 * Everything here is deterministic: the same register number always produces
 * the same breakdown and the same history, so screenshots and demo runs are
 * reproducible and the numbers never contradict each other between screens.
 */
internal object DemoAttendance {

    /**
     * Splits a student's attended total across subjects.
     *
     * The split is proportional to how many classes each subject has held, then
     * nudged by a seed derived from the register number so the table does not
     * look uniformly generated. The total is preserved exactly, which is what
     * keeps the per-subject figures consistent with the overall percentage.
     */
    fun subjectBreakdown(student: Student): List<SubjectAttendance> {
        val total = DemoPeople.attendedByRegister[student.registerNumber] ?: 0
        val held = DemoAcademics.classesHeld
        val capacity = DemoAcademics.totalClassesHeld
        val seed = abs(student.registerNumber.hashCode())

        val attended = held.map { h -> (h.toLong() * total / capacity).toInt() }.toMutableList()

        // Hand out the rounding remainder, starting at a seed-dependent subject.
        var remaining = total - attended.sum()
        var step = 0
        while (remaining > 0 && step < held.size * 4) {
            val index = (seed + step) % held.size
            if (attended[index] < held[index]) {
                attended[index]++
                remaining--
            }
            step++
        }

        // Shift a class or two between subjects so strengths and weaknesses vary
        // per student. Total is conserved, so the overall percentage is unchanged.
        for (index in held.indices step 2) {
            val from = index
            val to = (index + 3) % held.size
            val shift = (seed shr (index + 1)) % 3
            if (shift > 0 && attended[from] - shift >= 0 && attended[to] + shift <= held[to]) {
                attended[from] -= shift
                attended[to] += shift
            }
        }

        return DemoAcademics.subjects.mapIndexed { index, subject ->
            SubjectAttendance(
                subject = subject,
                attended = attended[index],
                held = held[index],
            )
        }
    }

    fun overallPercentage(student: Student): Float {
        val total = DemoPeople.attendedByRegister[student.registerNumber] ?: 0
        return (total * 100f) / DemoAcademics.totalClassesHeld
    }

    /**
     * Recent attendance events, newest first.
     *
     * A sample of the most recent weeks rather than the full semester: the
     * authoritative counts remain [subjectBreakdown]. Statuses are drawn from a
     * seeded generator biased by the student's own percentage, so a strong
     * student shows mostly present days and an at-risk student visibly does not.
     */
    fun history(student: Student, days: Int = 20): List<AttendanceRecord> {
        val percentage = overallPercentage(student)
        var random = abs(student.registerNumber.hashCode()).toLong() or 1L
        val records = mutableListOf<AttendanceRecord>()

        var date = LocalDate.now()
        var slotIndex = 0
        var produced = 0

        while (produced < days) {
            date = date.minusDays(1)
            if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
                continue
            }

            // Two recorded sessions per teaching day.
            repeat(2) {
                if (produced >= days) return@repeat
                random = nextSeed(random)
                val roll = (random % 100).toInt()
                val status = when {
                    roll < percentage.toInt() - 6 -> AttendanceStatus.PRESENT
                    roll < percentage.toInt() -> AttendanceStatus.LATE
                    else -> AttendanceStatus.ABSENT
                }
                val subject = DemoAcademics.subjects[slotIndex % DemoAcademics.subjects.size]
                slotIndex++

                records += AttendanceRecord(
                    id = "att-${student.registerNumber}-$produced",
                    date = date,
                    subject = subject,
                    status = status,
                    markedAt = when (status) {
                        AttendanceStatus.PRESENT -> LocalTime.of(10, 12 + (roll % 6))
                        AttendanceStatus.LATE -> LocalTime.of(10, 34 + (roll % 8))
                        AttendanceStatus.ABSENT -> null
                    },
                )
                produced++
            }
        }
        return records
    }

    /** A small, plausible notification inbox (Section 32). */
    fun notifications(student: Student): List<NotificationItem> {
        val percentage = overallPercentage(student)
        val now = LocalDateTime.now()
        val items = mutableListOf(
            NotificationItem(
                id = "ntf-${student.id}-1",
                type = NotificationType.ATTENDANCE_MARKED,
                title = "Attendance marked",
                message = "You were marked present for Mobile Application Development.",
                timestamp = now.minusHours(3),
                isRead = false,
            ),
            NotificationItem(
                id = "ntf-${student.id}-2",
                type = NotificationType.ANNOUNCEMENT,
                title = "Internal assessment schedule",
                message = "The second internal assessment begins on 14 September.",
                timestamp = now.minusDays(1).withHour(9).withMinute(20),
                isRead = false,
            ),
            NotificationItem(
                id = "ntf-${student.id}-3",
                type = NotificationType.ABSENCE_RECORDED,
                title = "Absence recorded",
                message = "You were marked absent for Theory of Computation on Monday.",
                timestamp = now.minusDays(2).withHour(15).withMinute(5),
                isRead = true,
            ),
        )

        if (percentage < 75f) {
            items.add(
                index = 0,
                element = NotificationItem(
                    id = "ntf-${student.id}-risk",
                    type = NotificationType.LOW_ATTENDANCE_WARNING,
                    title = "Attendance below threshold",
                    message = "Your current attendance is below the recommended threshold.",
                    timestamp = now.minusHours(1),
                    isRead = false,
                ),
            )
        }
        return items
    }

    /** A small linear congruential step - deterministic and dependency-free. */
    private fun nextSeed(previous: Long): Long =
        (previous * 6364136223846793005L + 1442695040888963407L) ushr 1
}
