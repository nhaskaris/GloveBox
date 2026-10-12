package com.eliteonetube.glovebox.util

/**
 * User-entered exchange rates; nothing is ever fetched.
 *
 * [rates] maps a currency code to how many units of [base] one unit of that currency is worth
 * (e.g. base "USD", rates {"EUR" to 1.08} means 1 EUR = 1.08 USD).
 */
data class ExchangeRates(val base: String, val rates: Map<String, Double> = emptyMap()) {

    /** Converts [amount], or returns null when a needed rate hasn't been entered. */
    fun convert(amount: Double, from: String, to: String): Double? {
        if (from == to) return amount
        val inBase = if (from == base) amount else rates[from]?.let { amount * it } ?: return null
        return if (to == base) inBase else rates[to]?.let { inBase / it }
    }

    /** The same rates expressed against [newBase]; unknown if no rate for it was entered. */
    fun rebased(newBase: String): ExchangeRates {
        if (newBase == base) return this
        val newBaseRate = rates[newBase] ?: return ExchangeRates(newBase)
        val rebased = rates.filterKeys { it != newBase }.mapValues { it.value / newBaseRate } +
            (base to 1 / newBaseRate)
        return ExchangeRates(newBase, rebased)
    }

    fun with(code: String, rate: Double?): ExchangeRates =
        if (code == base) this
        else copy(rates = if (rate != null && rate > 0) rates + (code to rate) else rates - code)

    fun serialize(): String =
        base + "|" + rates.entries.sortedBy { it.key }.joinToString(",") { "${it.key}=${it.value}" }

    companion object {
        fun parse(text: String?, fallbackBase: String): ExchangeRates {
            if (text.isNullOrBlank()) return ExchangeRates(fallbackBase)
            val base = text.substringBefore('|').ifBlank { fallbackBase }
            val rates = text.substringAfter('|', "").split(',').mapNotNull { entry ->
                val code = entry.substringBefore('=', "")
                val rate = entry.substringAfter('=', "").toDoubleOrNull()
                if (code.isNotBlank() && rate != null && rate > 0) code to rate else null
            }.toMap()
            return ExchangeRates(base, rates)
        }
    }
}

/** Sum of amounts converted into one currency, plus how many couldn't be converted. */
data class ConvertedTotal(val total: Double, val unconverted: Int)

fun ExchangeRates.sum(items: List<Pair<Double, String>>, target: String): ConvertedTotal {
    var total = 0.0
    var unconverted = 0
    items.forEach { (amount, currency) ->
        val converted = convert(amount, currency, target)
        if (converted == null) unconverted++ else total += converted
    }
    return ConvertedTotal(total, unconverted)
}
