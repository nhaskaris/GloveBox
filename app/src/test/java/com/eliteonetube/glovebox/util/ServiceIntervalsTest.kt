package com.eliteonetube.glovebox.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceIntervalsTest {
    @Test
    fun everyServiceInTheFormListGetsTheIntervalWeExpect() {
        assertEquals("oil", ServiceIntervals.forType("Oil Change")!!.keyword)
        assertEquals("tire", ServiceIntervals.forType("Tire Replacement")!!.keyword)
        assertEquals("brake", ServiceIntervals.forType("Brake Pads")!!.keyword)
        assertEquals("brake", ServiceIntervals.forType("Brake Rotors")!!.keyword)
        assertEquals("belt", ServiceIntervals.forType("Timing Belt")!!.keyword)
        assertEquals("belt", ServiceIntervals.forType("Drive Belt")!!.keyword)
        assertEquals("spark", ServiceIntervals.forType("Spark Plugs")!!.keyword)
        assertNull(ServiceIntervals.forType("Water Pump"))
    }

    @Test
    fun specificFilterKeywordBeatsTheGenericOne() {
        assertEquals("air filter", ServiceIntervals.forType("Cabin Air Filter")!!.keyword)
        assertEquals("air filter", ServiceIntervals.forType("Engine Air Filter")!!.keyword)
        assertEquals("filter", ServiceIntervals.forType("Fuel Filter")!!.keyword)
    }

    @Test
    fun distanceFollowsTheOdometerUnit() {
        val brake = ServiceIntervals.forType("Brake Service")!!
        assertEquals(40_000, brake.distance("mi"))
        assertEquals(65_000, brake.distance("km"))
        assertNull(ServiceIntervals.forType("Wipers")!!.distance("km"))
    }

    @Test
    fun predictedServicesAreCompleteAndConsistent() {
        assertEquals(4, ServiceIntervals.predicted.size)
        ServiceIntervals.predicted.forEach {
            assertNotNull(it.nameRes)
            assertNotNull(it.miles)
            assertNotNull(it.kilometers)
            assertTrue(it in ServiceIntervals.all)
        }
    }
}
