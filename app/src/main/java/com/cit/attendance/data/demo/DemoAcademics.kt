package com.cit.attendance.data.demo

import com.cit.attendance.domain.model.ClassSection
import com.cit.attendance.domain.model.ScheduledClass
import com.cit.attendance.domain.model.Subject
import java.time.LocalTime

/**
 * Academic structure for the demonstration cohort (Section 26).
 *
 * Milestone 1 holds this in memory behind the repository interfaces; Milestone 2
 * replaces it with PostgreSQL seed scripts carrying the same shape.
 */
internal object DemoAcademics {

    val cseThirdYearA = ClassSection(
        id = "cls-cse-3-a",
        department = "CSE",
        year = 3,
        section = "A",
    )

    val mobileApplicationDevelopment = Subject("sub-mad", "CS8501", "Mobile Application Development")
    val databaseManagementSystems = Subject("sub-dbms", "CS8502", "Database Management Systems")
    val computerNetworks = Subject("sub-cn", "CS8503", "Computer Networks")
    val operatingSystems = Subject("sub-os", "CS8504", "Operating Systems")
    val theoryOfComputation = Subject("sub-toc", "CS8505", "Theory of Computation")
    val softwareEngineering = Subject("sub-se", "CS8506", "Software Engineering")

    /** Order is fixed: attendance distribution below depends on it. */
    val subjects: List<Subject> = listOf(
        mobileApplicationDevelopment,
        databaseManagementSystems,
        computerNetworks,
        operatingSystems,
        theoryOfComputation,
        softwareEngineering,
    )

    /** Classes held so far this semester, per subject, in [subjects] order. */
    val classesHeld: List<Int> = listOf(30, 32, 28, 30, 26, 28)

    val totalClassesHeld: Int = classesHeld.sum()

    /**
     * Today's timetable for the cohort.
     *
     * Mobile Application Development is flagged active so the Milestone 4 demo
     * script has a session to walk through; the flag is demo state here and
     * comes from the backend session table in Milestone 2.
     */
    fun todaysTimetable(): List<ScheduledClass> = listOf(
        ScheduledClass(
            id = "slot-1",
            subject = mobileApplicationDevelopment,
            classSection = cseThirdYearA,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(10, 50),
            room = "Block C - 402",
            teacherName = DemoPeople.meenakshi.name,
            hasActiveSession = true,
        ),
        ScheduledClass(
            id = "slot-2",
            subject = databaseManagementSystems,
            classSection = cseThirdYearA,
            startTime = LocalTime.of(11, 0),
            endTime = LocalTime.of(11, 50),
            room = "Block C - 402",
            teacherName = DemoPeople.anitha.name,
        ),
        ScheduledClass(
            id = "slot-3",
            subject = computerNetworks,
            classSection = cseThirdYearA,
            startTime = LocalTime.of(13, 30),
            endTime = LocalTime.of(14, 20),
            room = "Block C - 407",
            teacherName = DemoPeople.karthikeyan.name,
        ),
        ScheduledClass(
            id = "slot-4",
            subject = operatingSystems,
            classSection = cseThirdYearA,
            startTime = LocalTime.of(14, 30),
            endTime = LocalTime.of(15, 20),
            room = "Block C - 407",
            teacherName = DemoPeople.karthikeyan.name,
        ),
    )
}
