package com.eliteonetube.glovebox.util

import com.eliteonetube.glovebox.data.entity.FuelLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FuelEfficiencyTest {
    private fun fill(day: Long, odometer: Int, litres: Double) =
        FuelLog(vehicleId = 1, date = day * 86_400_000L, odometer = odometer, amount = litres, totalCost = litres * 1.8)

    @Test
    fun fullTanks_matchSimpleLitresPerHundred() {
        val logs = listOf(fill(0, 1000, 40.0), fill(5, 1500, 40.0), fill(10, 2000, 40.0))
        assertEquals(8.0, FuelEfficiency.points(logs, "km").last().value, 0.0001)
        assertEquals(8.0, FuelEfficiency.average(logs, "km")!!, 0.0001)
    }

    @Test
    fun topUps_dontSkewTheSmoothedPointOrTheAverage() {
        // Car really burns 8 L/100km. The middle fill-up is only a 10 L top-up.
        val logs = listOf(fill(0, 0, 40.0), fill(2, 200, 10.0), fill(4, 400, 22.0))

        // A naive pair-by-pair figure would say 5.0 then 11.0.
        val points = FuelEfficiency.points(logs, "km")
        assertEquals(5.0, points[0].value, 0.0001)
        assertEquals(8.0, points[1].value, 0.0001)
        assertEquals(8.0, FuelEfficiency.average(logs, "km")!!, 0.0001)
    }

    @Test
    fun milesGiveMpg() {
        val logs = listOf(fill(0, 0, 10.0), fill(5, 300, 10.0))
        assertEquals(30.0, FuelEfficiency.average(logs, "mi")!!, 0.0001)
    }

    @Test
    fun badOrMissingDataIsIgnored() {
        assertNull(FuelEfficiency.average(emptyList(), "km"))
        assertNull(FuelEfficiency.average(listOf(fill(0, 0, 40.0)), "km"))
        // odometer went backwards
        assertNull(FuelEfficiency.average(listOf(fill(0, 500, 40.0), fill(5, 400, 40.0)), "km"))
        // zero-litre rows are dropped
        assertNull(FuelEfficiency.average(listOf(fill(0, 0, 40.0), fill(5, 400, 0.0)), "km"))
    }
}
