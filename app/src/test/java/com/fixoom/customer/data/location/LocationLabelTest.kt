package com.fixoom.customer.data.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationLabelTest {
    @Test
    fun normalizesWhitespace() {
        assertEquals("Kandy, Sri Lanka", normalizeLocationLabel("  Kandy, \n Sri Lanka  "))
    }

    @Test
    fun rejectsEmptyOrMeaninglessNames() {
        listOf("", "  ", "A", "12345", "---", "a".repeat(81)).forEach {
            assertFalse(isValidLocationLabel(it))
        }
    }

    @Test
    fun acceptsAreaNamesAndLocalLanguages() {
        listOf("කොළඹ", "யாழ்ப்பாணம்", "Galle, Sri Lanka", "Dehiwala").forEach {
            assertTrue(isValidLocationLabel(it))
        }
    }
}
