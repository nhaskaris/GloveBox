package com.eliteonetube.glovebox.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExchangeRatesTest {
    private val rates = ExchangeRates("USD", mapOf("EUR" to 1.10, "GBP" to 1.25))

    @Test
    fun convertsViaBase() {
        assertEquals(110.0, rates.convert(100.0, "EUR", "USD")!!, 1e-9)
        assertEquals(100.0, rates.convert(110.0, "USD", "EUR")!!, 1e-9)
        assertEquals(88.0, rates.convert(100.0, "EUR", "GBP")!!, 1e-9)
        assertEquals(42.0, rates.convert(42.0, "JPY", "JPY")!!, 1e-9)
    }

    @Test
    fun missingRateIsNullNotGuessed() {
        assertNull(rates.convert(100.0, "JPY", "USD"))
        assertNull(rates.convert(100.0, "USD", "JPY"))
    }

    @Test
    fun rebasingKeepsRatesConsistent() {
        val inEur = rates.rebased("EUR")
        assertEquals("EUR", inEur.base)
        assertEquals(100.0, inEur.convert(110.0, "USD", "EUR")!!, 1e-9)
        assertEquals(88.0, inEur.convert(100.0, "EUR", "GBP")!!, 1e-9)
        // No rate known for the new base => start empty rather than invent one.
        assertEquals(emptyMap<String, Double>(), rates.rebased("JPY").rates)
    }

    @Test
    fun serializeRoundTrips() {
        assertEquals(rates, ExchangeRates.parse(rates.serialize(), "USD"))
        assertEquals(ExchangeRates("EUR"), ExchangeRates.parse(null, "EUR"))
        assertEquals(ExchangeRates("USD", mapOf("EUR" to 1.1)), ExchangeRates.parse("USD|EUR=1.1,BAD=x,=2,GBP=-3", "USD"))
    }

    @Test
    fun withSetsAndClearsRates() {
        assertEquals(1.2, rates.with("EUR", 1.2).rates["EUR"]!!, 0.0)
        assertNull(rates.with("EUR", null).rates["EUR"])
        assertNull(rates.with("EUR", 0.0).rates["EUR"])
        assertEquals(rates, rates.with("USD", 5.0))
    }

    @Test
    fun sumCountsWhatItCouldNotConvert() {
        val result = rates.sum(listOf(10.0 to "USD", 10.0 to "EUR", 10.0 to "JPY"), "USD")
        assertEquals(21.0, result.total, 1e-9)
        assertEquals(1, result.unconverted)
    }
}
