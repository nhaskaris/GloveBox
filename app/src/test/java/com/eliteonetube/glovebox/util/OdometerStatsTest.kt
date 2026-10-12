package com.eliteonetube.glovebox.util

import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OdometerStatsTest {
    private val day = TimeUnit.DAYS.toMillis(1)

    @Test
    fun averagesDistanceOverElapsedDays_regardlessOfInputOrder() {
        val readings = listOf(10 * day to 1500, 0L to 1000, 5 * day to 1200)
        assertEquals(50.0, OdometerStats.averageDailyDistance(readings)!!, 0.0001)
    }

    @Test
    fun returnsNullWithoutUsableData() {
        assertNull(OdometerStats.averageDailyDistance(emptyList()))
        assertNull(OdometerStats.averageDailyDistance(listOf(0L to 1000)))
        // same day
        assertNull(OdometerStats.averageDailyDistance(listOf(0L to 1000, day / 2 to 1100)))
        // odometer did not increase
        assertNull(OdometerStats.averageDailyDistance(listOf(0L to 1000, 5 * day to 1000)))
        assertNull(OdometerStats.averageDailyDistance(listOf(0L to 1000, 5 * day to 900)))
    }
}
