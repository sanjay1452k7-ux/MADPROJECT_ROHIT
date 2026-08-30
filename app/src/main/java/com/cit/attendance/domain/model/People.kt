package com.cit.attendance.domain.model

/**
 * The identity established by signing in.
 *
 * Deliberately small: it carries who the user is and what they may do, and
 * nothing else. Role-specific detail is fetched by the role's own repository.
 */
data class AuthenticatedUser(
    val id: String,
    val displayName: String,
    val email: String,
    val role: UserRole,
)

/**
 * A student record.
 *
 * [avatarSeed] is a stable seed for a generated illustrated avatar (Section 26)
 * rather than a photograph of a real person; the register number is used so the
 * same student always renders the same avatar.
 */
data class Student(
    val id: String,
    val name: String,
    val registerNumber: String,
    val email: String,
    val classSection: ClassSection,
    val avatarSeed: String,
) {
    val department: String get() = classSection.department
    val year: Int get() = classSection.year
    val section: String get() = classSection.section

    /** Initials used by the avatar component when no image is available. */
    val initials: String
        get() = name.trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
}

/** A faculty record. */
data class Teacher(
    val id: String,
    val name: String,
    val employeeCode: String,
    val email: String,
    val department: String,
    val subjects: List<Subject>,
    val avatarSeed: String,
)

/** An administrator record. */
data class Administrator(
    val id: String,
    val name: String,
    val email: String,
    val designation: String,
)
