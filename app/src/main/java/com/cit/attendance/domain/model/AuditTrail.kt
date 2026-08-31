package com.cit.attendance.domain.model

/**
 * One recorded action from the audit trail (Section 35).
 *
 * The trail exists so that a disputed attendance record can be traced back to
 * the account that created it. That is only useful if somebody can read it, so
 * it is surfaced to administrators rather than left in the database.
 */
data class AuditEntry(
    val id: String,
    val action: AuditActionKind,
    val actor: String,
    val actorRole: UserRole?,
    val summary: String,
    val occurredAt: String,
)

/**
 * The actions worth recording.
 *
 * [Other] keeps an unfamiliar action readable rather than dropping the row: the
 * server may record something a released app does not know about yet, and a
 * gap in an audit trail is worse than an unstyled entry.
 */
enum class AuditActionKind {
    SignIn,
    SignOut,
    SessionStarted,
    SessionClosed,
    QrMinted,
    QrScanned,
    QrRejected,
    AttendanceMarked,
    AttendanceManual,
    ReportGenerated,
    Other,
}
