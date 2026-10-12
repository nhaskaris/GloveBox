package com.eliteonetube.glovebox.util

import com.eliteonetube.glovebox.data.entity.ServiceRecord
import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvUtilityTest {
    private val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun day(s: String) = format.parse(s)!!.time

    private val header = "Date,ServiceType,Mileage,Cost,Currency,Location,Mechanic,Notes,Parts,Labor,DIY\n"

    @Test
    fun exportThenImport_roundTripsAwkwardText() {
        val original = ServiceRecord(
            vehicleId = 1, date = day("2026-03-05"), mileage = 45000, serviceType = "Oil Change",
            cost = 120.5, currency = "EUR", serviceLocation = "Joe's, Garage",
            notes = "Said \"all good\",\nsecond line", isDiy = true
        )

        val csv = CsvUtility.exportServiceRecords(listOf(original))
        val result = CsvUtility.parseServiceRecords(csv, vehicleId = 7)

        assertEquals(0, result.skipped)
        val parsed = result.records.single()
        assertEquals(7L, parsed.vehicleId)
        assertEquals(original.date, parsed.date)
        assertEquals("Oil Change", parsed.serviceType)
        assertEquals(45000, parsed.mileage)
        assertEquals(120.5, parsed.cost!!, 0.0)
        assertEquals("EUR", parsed.currency)
        assertEquals("Joe's, Garage", parsed.serviceLocation)
        assertEquals("Said \"all good\",\nsecond line", parsed.notes)
        assertTrue(parsed.isDiy)
    }

    @Test
    fun import_handlesWindowsLineEndingsAndBom() {
        val csv = "﻿" + header.replace("\n", "\r\n") +
            "2026-01-10,Brake Service,30000,200.00,USD,,,,,,0\r\n" +
            "2026-02-11,Tire Rotation,31000,,USD,,,,,,0\r\n"

        val result = CsvUtility.parseServiceRecords(csv, 1)

        assertEquals(listOf("Brake Service", "Tire Rotation"), result.records.map { it.serviceType })
        assertEquals(0, result.skipped)
    }

    @Test
    fun import_skipsInvalidDatesAndMissingTypes() {
        val csv = header +
            "not-a-date,Oil Change,1000,10.00,USD,,,,,,0\n" +
            "2026-13-45,Oil Change,1000,10.00,USD,,,,,,0\n" +
            "2026-01-10,,1000,10.00,USD,,,,,,0\n" +
            "2026-01-10,Oil Change,1000,10.00,USD,,,,,,0\n"

        val result = CsvUtility.parseServiceRecords(csv, 1)

        assertEquals(1, result.records.size)
        assertEquals(3, result.skipped)
    }

    @Test
    fun import_skipsDuplicatesOfExistingRecordsAndWithinTheFile() {
        val existing = listOf(
            ServiceRecord(vehicleId = 1, date = day("2026-01-10"), mileage = 1000, serviceType = "Oil Change", cost = 10.0)
        )
        val csv = header +
            "2026-01-10,oil change,1000,10.00,USD,,,,,,0\n" +
            "2026-02-10,Brake Service,2000,50.00,USD,,,,,,0\n" +
            "2026-02-10,Brake Service,2000,50.00,USD,,,,,,0\n"

        val result = CsvUtility.parseServiceRecords(csv, 1, existing)

        assertEquals(listOf("Brake Service"), result.records.map { it.serviceType })
        assertEquals(2, result.skipped)
    }

    @Test
    fun import_headerOnlyOrEmptyYieldsNothing() {
        assertTrue(CsvUtility.parseServiceRecords("", 1).records.isEmpty())
        assertTrue(CsvUtility.parseServiceRecords(header, 1).records.isEmpty())
    }
}
