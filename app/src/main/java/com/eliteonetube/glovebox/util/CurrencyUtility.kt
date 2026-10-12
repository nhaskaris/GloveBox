package com.eliteonetube.glovebox.util

object CurrencyUtility {
    /** Currencies offered in the pickers. Conversion between them uses user-entered [ExchangeRates]. */
    val supportedCurrencies = listOf("AUD", "CAD", "CHF", "CNY", "EUR", "GBP", "JPY", "NZD", "SEK", "USD")

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