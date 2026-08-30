package com.cit.attendance.domain.config

/**
 * Institution-specific configuration (Section 9).
 *
 * The email domain and attendance threshold are configuration values supplied
 * at build time, never literals inside feature code. Deploying for a different
 * institution is a build-config change.
 */
data class InstitutionConfig(
    val name: String,
    val emailDomain: String,
    val attendanceThresholdPercent: Int,
) {
    /** True when [email] is a well-formed address on the institution domain. */
    fun isInstitutionEmail(email: String): Boolean {
        val normalised = email.trim().lowercase()
        val atIndex = normalised.indexOf('@')
        if (atIndex <= 0) return false
        return normalised.substring(atIndex + 1) == emailDomain.lowercase()
    }
}
