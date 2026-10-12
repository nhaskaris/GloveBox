package com.eliteonetube.glovebox.ui.viewmodels

import com.eliteonetube.glovebox.data.entity.FuelLog
import com.eliteonetube.glovebox.data.entity.ServiceRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistorySearchTest {
    private val service = HistoryItem.Service(
        ServiceRecord(
            vehicleId = 1, date = 0, mileage = 1000, serviceType = "Oil Change, Tire Rotation",
            serviceLocation = "Joe's Garage", mechanicName = "Nikos", partsUsed = "Mann filter",
            notes = "Used synthetic 5W-30"
        )
    )
    private val fuel = HistoryItem.Fuel(
        FuelLog(vehicleId = 1, date = 0, odometer = 1000, amount = 40.0, totalCost = 60.0, location = "Shell Athens")
    )

    @Test
    fun blankQueryMatchesEverything() {
        assertTrue(service.matchesQuery(""))
        assertTrue(fuel.matchesQuery("   "))
    }

    @Test
    fun searchesAcrossServiceFields_caseInsensitively() {
        assertTrue(service.matchesQuery("TIRE"))
        assertTrue(service.matchesQuery("joe"))
        assertTrue(service.matchesQuery("nikos"))
        assertTrue(service.matchesQuery("mann"))
        assertTrue(service.matchesQuery("synthetic"))
        assertFalse(service.matchesQuery("brake"))
    }

    @Test
    fun everyWordMustMatch_inAnyField() {
        assertTrue(service.matchesQuery("oil nikos"))
        assertFalse(service.matchesQuery("oil brake"))
    }

    @Test
    fun fuelEntriesMatchOnStation() {
        assertTrue(fuel.matchesQuery("shell"))
        assertFalse(fuel.matchesQuery("bp"))
    }
}
