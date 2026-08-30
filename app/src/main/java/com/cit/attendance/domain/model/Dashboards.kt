package com.cit.attendance.domain.model

/** Everything the student home screen renders in one read (Section 14). */
data class StudentDashboard(
    val student: Student,
    val summary: AttendanceSummary,
    val todaysClasses: List<ScheduledClass>,
    val recentRecords: List<AttendanceRecord>,
    val unreadNotifications: Int,
)

/** A student flagged as needing attention, used on teacher and admin surfaces. */
data class StudentAtRisk(
    val student: Student,
    val percentage: Float,
    val standing: AttendanceStanding,
)

/** Everything the teacher home screen renders in one read (Section 15). */
data class TeacherDashboard(
    val teacher: Teacher,
    val todaysClasses: List<ScheduledClass>,
    val activeSessionCount: Int,
    val studentsAtRisk: List<StudentAtRisk>,
    val averageAttendance: Float,
)

/** A department roll-up shown on the admin dashboard (Section 16). */
data class DepartmentStat(
    val department: String,
    val studentCount: Int,
    val averageAttendance: Float,
)

/** Everything the admin home screen renders in one read (Section 16). */
data class AdminDashboard(
    val administrator: Administrator,
    val totalStudents: Int,
    val totalFaculty: Int,
    val presentToday: Int,
    val expectedToday: Int,
    val averageAttendance: Float,
    val studentsAtRisk: Int,
    val departmentStats: List<DepartmentStat>,
) {
    /** Share of expected students accounted for today, as a percentage. */
    val todaysAttendancePercentage: Float
        get() = if (expectedToday == 0) 0f else (presentToday * 100f) / expectedToday
}
