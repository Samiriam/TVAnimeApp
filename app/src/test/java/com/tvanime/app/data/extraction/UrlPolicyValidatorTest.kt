package com.tvanime.app.data.extraction

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlPolicyValidatorTest {

    private val validator = UrlPolicyValidator()

    @Test
    fun acceptsPublicHttpsUrl() {
        val uri = validator.validate("https://example.com/page")
        assertEquals("example.com", uri.host)
    }

    @Test
    fun acceptsPublicHttpUrl() {
        val uri = validator.validate("http://example.com/page")
        assertEquals("example.com", uri.host)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsLocalhost() {
        validator.validate("https://localhost/page")
    }
}
