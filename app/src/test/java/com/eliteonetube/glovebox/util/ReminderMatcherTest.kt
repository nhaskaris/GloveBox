package com.eliteonetube.glovebox.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderMatcherTest {
    @Test
    fun matchesIgnoringCaseAndContainment() {
        assertTrue(ReminderMatcher.matches("Oil change", "Oil Change"))
        assertTrue(ReminderMatcher.matches("Oil", "Oil Change"))
        assertTrue(ReminderMatcher.matches("Engine oil change due", "Oil Change"))
        assertTrue(ReminderMatcher.matches("  brake service ", "Brake Service"))
    }

    @Test
    fun unrelatedOrTooShortDoNotMatch() {
        assertFalse(ReminderMatcher.matches("Check oil level", "Oil Change"))
        assertFalse(ReminderMatcher.matches("Tire Rotation", "Oil Change"))
        assertFalse(ReminderMatcher.matches("ab", "Oil Change"))
        assertFalse(ReminderMatcher.matches("", ""))
    }
}
