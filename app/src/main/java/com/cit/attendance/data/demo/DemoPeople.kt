package com.cit.attendance.data.demo

import com.cit.attendance.domain.model.Administrator
import com.cit.attendance.domain.model.Student
import com.cit.attendance.domain.model.Teacher
import com.cit.attendance.domain.model.UserRole

/**
 * Demonstration people (Section 26).
 *
 * Register numbers, names and attendance targets are hand-authored rather than
 * randomly generated so the cohort reads like a real class list: a realistic
 * spread of strong, average and at-risk students.
 */
internal object DemoPeople {

    /**
     * The shared password for every demo account. Milestone 1 has no real
     * credential store; Milestone 2 replaces this with server-side Argon2/bcrypt
     * hashes and this constant disappears entirely.
     */
    const val DEMO_PASSWORD = "Cit@2026"

    /** Register number of the primary demonstration student (Section 26). */
    const val PRIMARY_STUDENT_REGISTER = "24CS0845"

    /** Second named student required by Section 26. */
    const val SECOND_STUDENT_REGISTER = "24CS0812"

    /**
     * (register number, name, classes attended out of
     * [DemoAcademics.totalClassesHeld]).
     */
    private val roster: List<Triple<String, String, Int>> = listOf(
        Triple("24CS0801", "Aarthi Balasubramanian", 158),
        Triple("24CS0803", "Abdul Rahman S", 149),
        Triple("24CS0805", "Aishwarya Venkatesh", 161),
        Triple("24CS0807", "Akash Nair", 128),
        Triple("24CS0809", "Anitha Krishnan", 154),
        Triple(SECOND_STUDENT_REGISTER, "Rohit", 133),
        Triple("24CS0814", "Bhavana Reddy", 145),
        Triple("24CS0816", "Charan Kumar M", 118),
        Triple("24CS0818", "Deepak Raj", 139),
        Triple("24CS0820", "Divya Lakshmi", 165),
        Triple("24CS0822", "Faizal Ahmed", 126),
        Triple("24CS0824", "Gokul Prasad", 143),
        Triple("24CS0826", "Harini Subramani", 157),
        Triple("24CS0828", "Ishaan Mehta", 112),
        Triple("24CS0830", "Janani Murugan", 151),
        Triple("24CS0832", "Karthik Raja", 136),
        Triple("24CS0834", "Keerthana S", 163),
        Triple("24CS0836", "Lakshmi Priya", 147),
        Triple("24CS0838", "Manoj Kumar", 122),
        Triple("24CS0840", "Naveen Chandra", 141),
        Triple("24CS0842", "Nithya Sree", 159),
        Triple(PRIMARY_STUDENT_REGISTER, "Sanjayan A", 152),
        Triple("24CS0847", "Praveen Kumar", 130),
        Triple("24CS0849", "Priya Darshini", 156),
        Triple("24CS0851", "Rahul Menon", 138),
        Triple("24CS0853", "Ramya Devi", 160),
        Triple("24CS0855", "Sandhya Rajan", 148),
        Triple("24CS0857", "Sathish Kumar", 108),
        Triple("24CS0859", "Swetha Narayanan", 155),
        Triple("24CS0861", "Vignesh Anand", 134),
        Triple("24CS0863", "Yamini Raghavan", 150),
    )

    /** Classes attended, keyed by register number. */
    val attendedByRegister: Map<String, Int> =
        roster.associate { (register, _, attended) -> register to attended }

    fun students(emailDomain: String): List<Student> = roster.map { (register, name, _) ->
        Student(
            id = "stu-${register.lowercase()}",
            name = name,
            registerNumber = register,
            email = "${register.lowercase()}@$emailDomain",
            classSection = DemoAcademics.cseThirdYearA,
            // Seeded by register number so a student always renders the same
            // generated avatar (Section 26) - never a photo of a real person.
            avatarSeed = register,
        )
    }

    val meenakshi = Teacher(
        id = "tea-001",
        name = "Dr. Meenakshi Sundaram",
        employeeCode = "CIT-CSE-114",
        email = "meenakshi.s@%s",
        department = "CSE",
        subjects = listOf(
            DemoAcademics.mobileApplicationDevelopment,
            DemoAcademics.theoryOfComputation,
        ),
        avatarSeed = "CIT-CSE-114",
    )

    val anitha = Teacher(
        id = "tea-002",
        name = "Prof. Anitha Ramesh",
        employeeCode = "CIT-CSE-127",
        email = "anitha.r@%s",
        department = "CSE",
        subjects = listOf(
            DemoAcademics.databaseManagementSystems,
            DemoAcademics.softwareEngineering,
        ),
        avatarSeed = "CIT-CSE-127",
    )

    val karthikeyan = Teacher(
        id = "tea-003",
        name = "Dr. Karthikeyan V",
        employeeCode = "CIT-CSE-138",
        email = "karthikeyan.v@%s",
        department = "CSE",
        subjects = listOf(
            DemoAcademics.computerNetworks,
            DemoAcademics.operatingSystems,
        ),
        avatarSeed = "CIT-CSE-138",
    )

    /** The email templates above carry a placeholder for the configured domain. */
    fun teachers(emailDomain: String): List<Teacher> =
        listOf(meenakshi, anitha, karthikeyan).map { it.copy(email = it.email.format(emailDomain)) }

    fun administrator(emailDomain: String) = Administrator(
        id = "adm-001",
        name = "Dr. Rajalakshmi Narayanan",
        email = "academics.office@$emailDomain",
        designation = "Director of Academics",
    )

    /** A demo sign-in credential mapped to the entity it unlocks. */
    data class DemoAccount(
        val email: String,
        val role: UserRole,
        val entityId: String,
        val displayName: String,
    )

    fun accounts(emailDomain: String): List<DemoAccount> {
        val studentAccounts = students(emailDomain).map { student ->
            DemoAccount(
                email = student.email,
                role = UserRole.STUDENT,
                entityId = student.id,
                displayName = student.name,
            )
        }
        val teacherAccounts = teachers(emailDomain).map { teacher ->
            DemoAccount(
                email = teacher.email,
                role = UserRole.TEACHER,
                entityId = teacher.id,
                displayName = teacher.name,
            )
        }
        val admin = administrator(emailDomain)
        return studentAccounts + teacherAccounts + DemoAccount(
            email = admin.email,
            role = UserRole.ADMIN,
            entityId = admin.id,
            displayName = admin.name,
        )
    }
}
