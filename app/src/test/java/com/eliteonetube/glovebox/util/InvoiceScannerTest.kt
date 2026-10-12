package com.eliteonetube.glovebox.util

import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoiceScannerTest {
    @Test
    fun extractsDateCostMileageAndType() {
        val text = """
            Joe's Garage
            Oil change and filter
            Date: 2026-03-05
            Mileage 45,000 km
            Total: 120.50
        """.trimIndent()

        val invoice = InvoiceScanner.extractMultipleServices(text).single()

        val expectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-03-05")!!.time
        assertEquals(expectedDate, invoice.date)
        assertEquals(120.5, invoice.totalCost!!, 0.0)
        assertEquals("Oil Change", invoice.detectedServiceType)
    }

    @Test
    fun splitsMultipleServiceBlocksOnHeaders() {
        val text = """
            Minor Service 2026-01-10 total 80.00 and some filler text here
            Major Service 2026-02-20 total 300.00 and some filler text here
        """.trimIndent()

        val invoices = InvoiceScanner.extractMultipleServices(text)

        assertEquals(2, invoices.size)
        assertEquals(80.0, invoices[0].totalCost!!, 0.0)
        assertEquals(300.0, invoices[1].totalCost!!, 0.0)
    }

    @Test
    fun textWithoutUsefulDataYieldsNoInvoices() {
        assertTrue(InvoiceScanner.extractMultipleServices("hello world").isEmpty())
        assertNotNull(InvoiceScanner.extractMultipleServices(""))
    }
}
