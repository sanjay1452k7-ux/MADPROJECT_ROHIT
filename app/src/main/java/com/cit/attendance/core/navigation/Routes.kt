package com.cit.attendance.core.navigation

import com.cit.attendance.domain.model.UserRole

/**
 * Every destination in the application, declared once.
 *
 * Routes are built through these helpers rather than composed as strings at the
 * call site, so a renamed destination cannot silently break navigation.
 */
object Routes {
    const val SPLASH = "splash"
    const val ROLE_SELECTION = "role_selection"

    const val LOGIN_ARG_ROLE = "role"
    const val LOGIN_PATTERN = "login/{$LOGIN_ARG_ROLE}"
    fun login(role: UserRole): String = "login/${role.name}"

    const val STUDENT_ROOT = "student_root"
    const val TEACHER_ROOT = "teacher_root"
    const val ADMIN_ROOT = "admin_root"

    /** The home destination for a signed-in role. */
    fun rootFor(role: UserRole): String = when (role) {
        UserRole.STUDENT -> STUDENT_ROOT
        UserRole.TEACHER -> TEACHER_ROOT
        UserRole.ADMIN -> ADMIN_ROOT
        // Parent has no surfaces yet (Section 13); it can never reach here
        // because it is not offered at sign-in.
        UserRole.PARENT -> ROLE_SELECTION
    }
}

/** Tabs inside the student shell (Section 14). */
object StudentRoutes {
    const val HOME = "student/home"
    const val ATTENDANCE = "student/attendance"
    const val ANALYTICS = "student/analytics"
    const val HISTORY = "student/history"
    const val NOTIFICATIONS = "student/notifications"
    const val PROFILE = "student/profile"
}

/** Tabs inside the teacher shell (Section 15). */
object TeacherRoutes {
    const val HOME = "teacher/home"
    const val ATTENDANCE = "teacher/attendance"
    const val CLASSES = "teacher/classes"
    const val ANALYTICS = "teacher/analytics"
    const val REPORTS = "teacher/reports"
    const val PROFILE = "teacher/profile"
}

/** Tabs inside the admin shell (Section 16). */
object AdminRoutes {
    const val OVERVIEW = "admin/overview"
    const val PEOPLE = "admin/people"
    const val ANALYTICS = "admin/analytics"
    const val REPORTS = "admin/reports"
    const val PROFILE = "admin/profile"
}
