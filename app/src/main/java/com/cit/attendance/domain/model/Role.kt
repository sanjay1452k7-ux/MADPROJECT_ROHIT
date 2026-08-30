package com.cit.attendance.domain.model

/**
 * Roles the product recognises (Section 13).
 *
 * PARENT is deliberately declared but not selectable: the architecture must not
 * prevent adding it later, while Milestone 1 ships no parent behaviour. Any
 * screen that lists roles uses [selectableRoles] rather than `entries`, so
 * enabling it later is a one-line change.
 */
enum class UserRole {
    STUDENT,
    TEACHER,
    ADMIN,
    PARENT,
    ;

    val isSelectableAtSignIn: Boolean
        get() = this != PARENT

    companion object {
        val selectableRoles: List<UserRole> get() = entries.filter { it.isSelectableAtSignIn }

        fun fromStorageKey(key: String): UserRole? = entries.firstOrNull { it.name == key }
    }
}
