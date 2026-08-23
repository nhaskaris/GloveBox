package com.eliteonetube.glovebox.util

object CurrencyUtility {
    // Base rates relative to USD (approximate/fixed as requested)
    private val usdRates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.92,
        "GBP" to 0.79,
        "JPY" to 150.0,
        "AUD" to 1.52,
        "CAD" to 1.35,
        "CHF" to 0.88,
        "CNY" to 7.19,
        "SEK" to 10.45,
        "NZD" to 1.62
    )

    val supportedCurrencies = usdRates.keys.toList().sorted()

    fun convert(amount: Double, from: String, to: String): Double {
        if (from == to) return amount
        
        val fromRate = usdRates[from] ?: 1.0
        val toRate = usdRates[to] ?: 1.0
        
        // Convert from 'from' to USD, then from USD to 'to'
        val inUsd = amount / fromRate
        return inUsd * toRate
    }

    fun getCurrencySymbol(currencyCode: String): String {
        return when (currencyCode) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "CNY" -> "¥"
            "SEK" -> "kr"
            else -> "$currencyCode "
        }
    }
}