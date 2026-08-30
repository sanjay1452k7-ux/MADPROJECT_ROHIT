package com.cit.attendance.data.mapper

import com.cit.attendance.core.network.dto.ActiveSessionDto
import com.cit.attendance.core.network.dto.AdminDashboardDto
import com.cit.attendance.core.network.dto.AttendanceRecordDto
import com.cit.attendance.core.network.dto.MarkResultDto
import com.cit.attendance.core.network.dto.MintedTokenDto
import com.cit.attendance.core.network.dto.NotificationDto
import com.cit.attendance.core.network.dto.ScheduledClassDto
import com.cit.attendance.core.network.dto.SessionSummaryDto
import com.cit.attendance.core.network.dto.StudentDashboardDto
import com.cit.attendance.core.network.dto.StudentDto
import com.cit.attendance.core.network.dto.SubjectAttendanceDto
import com.cit.attendance.core.network.dto.TeacherDashboardDto
import com.cit.attendance.core.network.dto.VerifiedStudentDto
import com.cit.attendance.domain.model.ActiveAttendanceSession
import com.cit.attendance.domain.model.AdminDashboard
import com.cit.attendance.domain.model.Administrator
import com.cit.attendance.domain.model.AttendanceMarkResult
import com.cit.attendance.domain.model.AttendanceQrToken
import com.cit.attendance.domain.model.AttendanceRecord
import com.cit.attendance.domain.model.AttendanceStanding
import com.cit.attendance.domain.model.AttendanceStatus
import com.cit.attendance.domain.model.AttendanceSummary
import com.cit.attendance.domain.model.ClassSection
import com.cit.attendance.domain.model.DepartmentStat
import com.cit.attendance.domain.model.LiveAttendanceSession
import com.cit.attendance.domain.model.NotificationItem
import com.cit.attendance.domain.model.NotificationType
import com.cit.attendance.domain.model.ScheduledClass
import com.cit.attendance.domain.model.SessionRosterEntry
import com.cit.attendance.domain.model.Student
import com.cit.attendance.domain.model.StudentAtRisk
import com.cit.attendance.domain.model.StudentDashboard
import com.cit.attendance.domain.model.Subject
import com.cit.attendance.domain.model.SubjectAttendance
import com.cit.attendance.domain.model.Teacher
import com.cit.attendance.domain.model.TeacherDashboard
import com.cit.attendance.domain.model.TeachingCohort
import com.cit.attendance.domain.model.VerifiedStudent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * Wire-to-domain translation.
 *
 * Parsing is defensive on purpose: a malformed timestamp degrades one field
 * rather than failing a whole screen, which matters because these mappers sit
 * between a server we do not control and UI the user is looking at.
 */

/**
 * Built per call rather than held in a static field: capturing the locale once
 * at class-load time means the app keeps formatting in the old locale after the
 * user changes it.
 */
private fun displayTimeFormat(): DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

/** Parses an ISO-8601 instant into local wall-clock time, or null. */
internal fun parseInstantToLocalTime(value: String?): LocalTime? = value?.let {
    runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalTime() }.getOrNull()
}

internal fun formatInstantAsTime(value: String?): String? =
    parseInstantToLocalTime(value)?.format(displayTimeFormat())

/** Parses "HH:mm" from the timetable. Falls back to midnight if malformed. */
private fun parseClockTime(value: String): LocalTime =
    runCatching { LocalTime.parse(value) }.getOrDefault(LocalTime.MIDNIGHT)

private fun parseDate(value: String): LocalDate =
    try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        LocalDate.now()
    }

internal fun String.toAttendanceStatus(): AttendanceStatus = when (this) {
    "PRESENT" -> AttendanceStatus.PRESENT
    "LATE" -> AttendanceStatus.LATE
    else -> AttendanceStatus.ABSENT
}

internal fun String.toStanding(): AttendanceStanding = when (this) {
    "GOOD" -> AttendanceStanding.GOOD
    "WARNING" -> AttendanceStanding.WARNING
    else -> AttendanceStanding.HIGH_RISK
}

internal fun String.toNotificationType(): NotificationType = when (this) {
    "ATTENDANCE_MARKED" -> NotificationType.ATTENDANCE_MARKED
    "ABSENCE_RECORDED" -> NotificationType.ABSENCE_RECORDED
    "LOW_ATTENDANCE_WARNING" -> NotificationType.LOW_ATTENDANCE_WARNING
    "ANNOUNCEMENT" -> NotificationType.ANNOUNCEMENT
    else -> NotificationType.SYSTEM
}

internal fun StudentDto.toDomain(): Student = Student(
    id = id,
    name = name,
    registerNumber = registerNumber,
    email = email,
    classSection = ClassSection(
        id = "$department-$year-$section",
        department = department,
        year = year,
        section = section,
    ),
    avatarSeed = avatarSeed,
)

internal fun SubjectAttendanceDto.toDomain(): SubjectAttendance = SubjectAttendance(
    subject = Subject(id = subjectId, code = subjectCode, name = subjectName),
    attended = attended,
    held = held,
)

internal fun ScheduledClassDto.toDomain(classSection: ClassSection): ScheduledClass =
    ScheduledClass(
        id = id,
        subject = Subject(id = subjectId, code = subjectCode, name = subjectName),
        classSection = classSection,
        startTime = parseClockTime(startTime),
        endTime = parseClockTime(endTime),
        room = room,
        teacherName = teacherName,
        hasActiveSession = hasActiveSession,
    )

internal fun AttendanceRecordDto.toDomain(): AttendanceRecord = AttendanceRecord(
    id = id,
    date = parseDate(date),
    subject = Subject(id = subjectCode, code = subjectCode, name = subjectName),
    status = status.toAttendanceStatus(),
    markedAt = parseInstantToLocalTime(markedAt),
)

internal fun StudentDashboardDto.toDomain(): StudentDashboard {
    val domainStudent = student.toDomain()
    return StudentDashboard(
        student = domainStudent,
        summary = AttendanceSummary(
            overallPercentage = summary.overallPercentage,
            standing = summary.standing.toStanding(),
            subjectBreakdown = summary.subjectBreakdown.map { it.toDomain() },
        ),
        todaysClasses = todaysClasses.map { it.toDomain(domainStudent.classSection) },
        recentRecords = recentRecords.map { it.toDomain() },
        unreadNotifications = unreadNotifications,
    )
}

internal fun TeacherDashboardDto.toDomain(): TeacherDashboard {
    val subjects = teacher.subjects.map { Subject(id = it.id, code = it.code, name = it.name) }

    return TeacherDashboard(
        teacher = Teacher(
            id = teacher.id,
            name = teacher.name,
            employeeCode = teacher.employeeCode,
            email = teacher.email,
            department = teacher.department,
            subjects = subjects,
            avatarSeed = teacher.avatarSeed,
        ),
        todaysClasses = todaysClasses.map { slot ->
            ScheduledClass(
                id = slot.id,
                subject = Subject(slot.subjectId, slot.subjectCode, slot.subjectName),
                classSection = ClassSection(
                    id = slot.classSectionId,
                    department = teacher.department,
                    year = 0,
                    section = slot.className,
                ),
                startTime = parseClockTime(slot.startTime),
                endTime = parseClockTime(slot.endTime),
                room = slot.room,
                teacherName = teacher.name,
                hasActiveSession = slot.hasActiveSession,
            )
        },
        activeSessionCount = activeSessions.size,
        studentsAtRisk = studentsAtRisk.map { risk ->
            StudentAtRisk(
                student = Student(
                    id = risk.studentId,
                    name = risk.name,
                    registerNumber = risk.registerNumber,
                    email = "",
                    classSection = ClassSection(
                        id = "",
                        department = teacher.department,
                        year = 0,
                        section = "",
                    ),
                    avatarSeed = risk.avatarSeed,
                ),
                percentage = risk.percentage,
                standing = risk.standing.toStanding(),
            )
        },
        averageAttendance = averageAttendance,
    )
}

internal fun TeacherDashboardDto.cohorts(): List<TeachingCohort> = classSections.map {
    TeachingCohort(classSectionId = it.id, label = it.name)
}

internal fun AdminDashboardDto.toDomain(): AdminDashboard =
    AdminDashboard(
        administrator = Administrator(
            id = administrator.id,
            name = administrator.name,
            email = administrator.email,
            designation = administrator.designation,
        ),
        totalStudents = totalStudents,
        totalFaculty = totalFaculty,
        presentToday = presentToday,
        // Sessions run today times the cohort size is what "expected" means
        // before every timetabled slot has actually been held.
        expectedToday = sessionsToday * totalStudents,
        averageAttendance = averageAttendance,
        studentsAtRisk = studentsAtRisk,
        departmentStats = departmentStats.map {
            DepartmentStat(
                department = it.department,
                studentCount = it.studentCount,
                averageAttendance = it.averageAttendance,
            )
        },
    )

internal fun NotificationDto.toDomain(): NotificationItem = NotificationItem(
    id = id,
    type = type.toNotificationType(),
    title = title,
    message = message,
    timestamp = runCatching {
        Instant.parse(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }.getOrElse { java.time.LocalDateTime.now() },
    isRead = isRead,
)

internal fun ActiveSessionDto.toDomain(): ActiveAttendanceSession = ActiveAttendanceSession(
    id = id,
    subjectName = subjectName,
    subjectCode = subjectCode,
    teacherName = teacherName,
    alreadyMarked = alreadyMarked,
    markedAtLabel = formatInstantAsTime(markedAt),
)

internal fun MintedTokenDto.toDomain(): AttendanceQrToken = AttendanceQrToken(
    token = token,
    validForSeconds = validForSeconds,
    expiresAtEpochMillis = runCatching { Instant.parse(expiresAt).toEpochMilli() }
        .getOrElse { System.currentTimeMillis() + validForSeconds * 1000L },
)

internal fun VerifiedStudentDto.toDomain(): VerifiedStudent = VerifiedStudent(
    verificationId = verificationId,
    studentId = student.id,
    name = student.name,
    registerNumber = student.registerNumber,
    department = student.department,
    year = student.year,
    section = student.section,
    avatarSeed = student.avatarSeed,
    attendancePercentage = student.attendancePercentage,
    sessionId = session.id,
    subjectName = session.subjectName,
    subjectCode = session.subjectCode,
)

internal fun SessionSummaryDto.toDomain(): LiveAttendanceSession = LiveAttendanceSession(
    id = id,
    isActive = status == "ACTIVE",
    subjectName = subjectName,
    subjectCode = subjectCode,
    className = className,
    teacherName = teacherName,
    presentCount = presentCount,
    totalStudents = totalStudents,
    roster = students.map { entry ->
        SessionRosterEntry(
            studentId = entry.id,
            name = entry.name,
            registerNumber = entry.registerNumber,
            avatarSeed = entry.avatarSeed,
            status = entry.status?.toAttendanceStatus(),
            markedAtLabel = formatInstantAsTime(entry.markedAt),
        )
    },
)

internal fun MarkResultDto.toDomain(): AttendanceMarkResult = AttendanceMarkResult(
    studentName = studentName,
    registerNumber = registerNumber,
    subjectName = subjectName,
    markedAtLabel = formatInstantAsTime(markedAt) ?: "",
)
