package com.fixoom.core.model

import org.junit.Assert.*
import org.junit.Test

class ContactPhoneTest {
    @Test
    fun localAndInternationalFormatsHaveOneCanonicalValue() {
        listOf("0771234567", "077 123 4567", "+94 (77) 123-4567", "0094771234567", "94771234567")
            .forEach { assertEquals("+94771234567", ContactPhone.normalize(it)) }
        assertEquals("+442079460000", ContactPhone.normalize("+44 20 7946 0000"))
    }

    @Test
    fun invalidOrAmbiguousNumbersAreRejected() {
        listOf("", "077", "771234567", "call 0771234567", "+0123456789", "++94771234567").forEach {
            assertNull(ContactPhone.normalize(it))
        }
    }
}
