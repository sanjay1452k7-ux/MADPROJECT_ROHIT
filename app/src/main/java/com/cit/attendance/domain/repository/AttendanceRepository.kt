package com.cit.attendance.domain.repository

import com.cit.attendance.core.common.Outcome
import com.cit.attendance.domain.model.ActiveAttendanceSession
import com.cit.attendance.domain.model.AttendanceMarkResult
import com.cit.attendance.domain.model.AttendanceQrToken
import com.cit.attendance.domain.model.AttendanceStatus
import com.cit.attendance.domain.model.LiveAttendanceSession
import com.cit.attendance.domain.model.VerifiedStudent

/**
 * The attendance workflow (Section 17).
 *
 * Note what is absent: there is no "mark present by student id" for the QR
 * path. Recording attendance requires a [verificationId] that only a successful
 * [verifyToken] can produce, so the client cannot skip the verification step.
 */
interface AttendanceRepository {

    /** The open session for the signed-in student, or null if none. */
    suspend fun activeSessionForStudent(): Outcome<ActiveAttendanceSession?>

    /** Asks the server for a fresh QR credential (Section 19). */
    suspend fun mintQrToken(sessionId: String): Outcome<AttendanceQrToken>

    /** Opens a session for a subject and cohort. */
    suspend fun startSession(
        subjectId: String,
        classSectionId: String,
    ): Outcome<LiveAttendanceSession>

    suspend fun sessionDetail(sessionId: String): Outcome<LiveAttendanceSession>

    suspend fun closeSession(sessionId: String): Outcome<LiveAttendanceSession>

    /** Validates a scanned code and identifies the student. Records nothing. */
    suspend fun verifyToken(sessionId: String, token: String): Outcome<VerifiedStudent>

    /** Records attendance after the teacher has visually confirmed. */
    suspend fun markPresent(
        sessionId: String,
        verificationId: String,
    ): Outcome<AttendanceMarkResult>

    /** Manual fallback when a device or camera will not cooperate. */
    suspend fun markManually(
        sessionId: String,
        studentId: String,
        status: AttendanceStatus,
        reason: String?,
    ): Outcome<Unit>
}
