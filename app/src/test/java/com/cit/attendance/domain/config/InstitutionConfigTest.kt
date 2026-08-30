package com.cit.attendance.domain.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstitutionConfigTest {

    private val config = InstitutionConfig(
        name = "Chennai Institute of Technology",
        emailDomain = "citchennai.net",
        attendanceThresholdPercent = 75,
    )

    @Test
    fun `accepts an address on the configured domain`() {
        assertTrue(config.isInstitutionEmail("24cs0845@citchennai.net"))
    }

    @Test
    fun `is case and whitespace insensitive`() {
        assertTrue(config.isInstitutionEmail("  24CS0845@CITChennai.NET "))
    }

    @Test
    fun `rejects a different domain`() {
        assertFalse(config.isInstitutionEmail("24cs0845@gmail.com"))
    }

    @Test
    fun `rejects a subdomain that merely ends with the domain`() {
        assertFalse(config.isInstitutionEmail("attacker@evil-citchennai.net"))
    }

    @Test
    fun `rejects malformed addresses`() {
        assertFalse(config.isInstitutionEmail("citchennai.net"))
        assertFalse(config.isInstitutionEmail("@citchennai.net"))
        assertFalse(config.isInstitutionEmail(""))
    }

    @Test
    fun `a differently configured institution accepts its own domain only`() {
        val other = config.copy(emailDomain = "example.edu")

        assertTrue(other.isInstitutionEmail("student@example.edu"))
        assertFalse(other.isInstitutionEmail("student@citchennai.net"))
    }
}
