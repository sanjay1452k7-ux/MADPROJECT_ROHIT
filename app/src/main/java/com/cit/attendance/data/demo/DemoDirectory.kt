package com.cit.attendance.data.demo

import com.cit.attendance.domain.config.InstitutionConfig
import com.cit.attendance.domain.model.Administrator
import com.cit.attendance.domain.model.Student
import com.cit.attendance.domain.model.Teacher
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Assembles the demo dataset once and answers lookups against it.
 *
 * This is the Milestone 1 stand-in for the database. It exists behind the
 * repository interfaces, so replacing it in Milestone 2 does not reach any
 * screen or use case (Section 12).
 */
@Singleton
class DemoDirectory @Inject constructor(
    institutionConfig: InstitutionConfig,
) {
    private val emailDomain = institutionConfig.emailDomain

    val students: List<Student> = DemoPeople.students(emailDomain)
    val teachers: List<Teacher> = DemoPeople.teachers(emailDomain)
    val administrator: Administrator = DemoPeople.administrator(emailDomain)
    private val accounts: List<DemoPeople.DemoAccount> = DemoPeople.accounts(emailDomain)

    private val studentsById = students.associateBy { it.id }
    private val teachersById = teachers.associateBy { it.id }
    private val accountsByEmail = accounts.associateBy { it.email.lowercase() }

    /** The student the demo script signs in as (Section 26). */
    val primaryStudent: Student = students.first {
        it.registerNumber == DemoPeople.PRIMARY_STUDENT_REGISTER
    }

    internal fun accountFor(email: String): DemoPeople.DemoAccount? =
        accountsByEmail[email.lowercase()]

    fun studentById(id: String): Student? = studentsById[id]

    fun teacherById(id: String): Teacher? = teachersById[id]

    /** Students below the given threshold, worst first. */
    fun studentsBelow(thresholdPercent: Int): List<Pair<Student, Float>> = students
        .map { it to DemoAttendance.overallPercentage(it) }
        .filter { (_, percentage) -> percentage < thresholdPercent }
        .sortedBy { (_, percentage) -> percentage }

    /** Mean attendance across the cohort. */
    fun cohortAverage(): Float =
        if (students.isEmpty()) 0f
        else students.map { DemoAttendance.overallPercentage(it) }.average().toFloat()
}
