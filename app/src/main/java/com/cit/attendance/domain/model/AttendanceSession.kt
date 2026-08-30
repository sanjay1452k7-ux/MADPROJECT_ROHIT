package com.cit.attendance.domain.model

/** An open session the student may present a QR code for (Section 19). */
data class ActiveAttendanceSession(
    val id: String,
    val subjectName: String,
    val subjectCode: String,
    val teacherName: String,
    /** True once the teacher has recorded this student, which hides the QR. */
    val alreadyMarked: Boolean,
    val markedAtLabel: String?,
)

/**
 * A server-minted QR credential.
 *
 * The app only renders [token]; it never constructs or signs one (Section 19).
 */
data class AttendanceQrToken(
    val token: String,
    val validForSeconds: Int,
    val expiresAtEpochMillis: Long,
)

/** The identity block the teacher checks before confirming (Section 22). */
data class VerifiedStudent(
    val verificationId: String,
    val studentId: String,
    val name: String,
    val registerNumber: String,
    val department: String,
    val year: Int,
    val section: String,
    val avatarSeed: String,
    val attendancePercentage: Float,
    val sessionId: String,
    val subjectName: String,
    val subjectCode: String,
) {
    /** "III YEAR - SECTION A", as shown on the verification card. */
    val classLabel: String
        get() = ClassSection(
            id = "",
            department = department,
            year = year,
            section = section,
        ).longForm

    val initials: String
        get() = name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
}

/** One row of the live roster on the teacher's session screen. */
data class SessionRosterEntry(
    val studentId: String,
    val name: String,
    val registerNumber: String,
    val avatarSeed: String,
    val status: AttendanceStatus?,
    val markedAtLabel: String?,
)

/** The teacher's view of a running session. */
data class LiveAttendanceSession(
    val id: String,
    val isActive: Boolean,
    val subjectName: String,
    val subjectCode: String,
    val className: String,
    val teacherName: String,
    val presentCount: Int,
    val totalStudents: Int,
    val roster: List<SessionRosterEntry>,
)

/** The confirmation shown after attendance is recorded (Section 23). */
data class AttendanceMarkResult(
    val studentName: String,
    val registerNumber: String,
    val subjectName: String,
    val markedAtLabel: String,
)

/** A cohort a teacher may open a session for. */
data class TeachingCohort(
    val classSectionId: String,
    val label: String,
)
