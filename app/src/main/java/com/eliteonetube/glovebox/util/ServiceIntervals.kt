package com.eliteonetube.glovebox.util

import com.eliteonetube.glovebox.R

/**
 * Default maintenance intervals, shared by the service form's "schedule next" suggestion, the
 * predictive notifications and the Insights predictions so they never disagree.
 *
 * A service type (stored in English, e.g. "Brake Pads") belongs to the first interval whose
 * [keyword] it contains, so more specific keywords must come before general ones.
 */
data class ServiceInterval(
    val keyword: String,
    val miles: Int?,
    val kilometers: Int?,
    val months: Int?,
    /** Display name for the services we predict ahead of time, null for the rest. */
    val nameRes: Int? = null
) {
    fun distance(unit: String): Int? = if (unit == "mi") miles else kilometers
    fun matches(serviceType: String): Boolean = serviceType.contains(keyword, ignoreCase = true)
}

object ServiceIntervals {
    private val oil = ServiceInterval("oil", 5_000, 8_000, 6, R.string.oil_change)
    private val tire = ServiceInterval("tire", 6_000, 10_000, 6, R.string.tire_rotation)
    private val brake = ServiceInterval("brake", 40_000, 65_000, 12, R.string.brake_service)
    private val airFilter = ServiceInterval("air filter", 20_000, 30_000, 12, R.string.air_filter)

    val all: List<ServiceInterval> = listOf(
        oil, tire, brake, airFilter,
        ServiceInterval("filter", 10_000, 15_000, 12),
        ServiceInterval("spark", 60_000, 100_000, 48),
        ServiceInterval("transmission", 50_000, 80_000, 36),
        ServiceInterval("belt", 80_000, 120_000, 60),
        ServiceInterval("coolant", 30_000, 50_000, 24),
        ServiceInterval("alignment", 10_000, 15_000, 12),
        ServiceInterval("wiper", null, null, 6),
        ServiceInterval("battery", null, null, 24),
        ServiceInterval("inspection", null, null, 12)
    )

    /** Services we predict the next due date for. */
    val predicted: List<ServiceInterval> = listOf(oil, brake, tire, airFilter)

    fun forType(serviceType: String): ServiceInterval? = all.firstOrNull { it.matches(serviceType) }
}
