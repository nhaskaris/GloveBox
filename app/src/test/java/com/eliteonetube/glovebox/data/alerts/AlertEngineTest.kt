package com.eliteonetube.glovebox.data.alerts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertEngineTest {
    @Test
    fun documentSeverity_thresholds() {
        assertEquals(AlertSeverity.CRITICAL, AlertEngine.documentSeverity(-5))
        assertEquals(AlertSeverity.CRITICAL, AlertEngine.documentSeverity(7))
        assertEquals(AlertSeverity.WARNING, AlertEngine.documentSeverity(8))
        assertEquals(AlertSeverity.WARNING, AlertEngine.documentSeverity(30))
        assertNull(AlertEngine.documentSeverity(31))
    }

    @Test
    fun reminderSeverity_usesKilometerThresholds() {
        assertEquals(AlertSeverity.CRITICAL, AlertEngine.reminderSeverity(-10, null, isMiles = false))
        assertEquals(AlertSeverity.CRITICAL, AlertEngine.reminderSeverity(100, null, isMiles = false))
        assertEquals(AlertSeverity.WARNING, AlertEngine.reminderSeverity(101, null, isMiles = false))
        assertEquals(AlertSeverity.WARNING, AlertEngine.reminderSeverity(500, null, isMiles = false))
        assertNull(AlertEngine.reminderSeverity(501, null, isMiles = false))
    }

    @Test
    fun reminderSeverity_scalesThresholdsForMiles() {
        assertEquals(AlertSeverity.CRITICAL, AlertEngine.reminderSeverity(60, null, isMiles = true))
        assertEquals(AlertSeverity.WARNING, AlertEngine.reminderSeverity(61, null, isMiles = true))
        assertEquals(AlertSeverity.WARNING, AlertEngine.reminderSeverity(300, null, isMiles = true))
        assertNull(AlertEngine.reminderSeverity(301, null, isMiles = true))
    }

    @Test
    fun reminderSeverity_usesDays_andTheWorseOfBothInputsWins() {
        assertEquals(AlertSeverity.CRITICAL, AlertEngine.reminderSeverity(null, 3, isMiles = false))
        assertEquals(AlertSeverity.WARNING, AlertEngine.reminderSeverity(null, 14, isMiles = false))
        assertNull(AlertEngine.reminderSeverity(null, 15, isMiles = false))
        assertEquals(AlertSeverity.CRITICAL, AlertEngine.reminderSeverity(2000, 2, isMiles = false))
        assertNull(AlertEngine.reminderSeverity(null, null, isMiles = false))
    }
}
