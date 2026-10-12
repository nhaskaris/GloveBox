package com.eliteonetube.glovebox.util

import com.eliteonetube.glovebox.data.entity.FuelLog

/**
 * Fuel efficiency from fill-up logs: L/100km for "km" vehicles, MPG for "mi" vehicles.
 *
 * A single fill-up only tells us how much was added, not how empty the tank was, so a pair of
 * consecutive fills is noisy whenever someone tops up. The litres added in a fill replace the fuel
 * burned since the previous fill; over a longer stretch of driving the tank level at both ends
 * cancels out. We therefore measure each point over a short window of fills, and the average over
 * the whole history (which is off by at most one tank's worth of fuel).
 */
object FuelEfficiency {
    data class Point(val date: Long, val value: Double)

    private const val WINDOW = 3

    fun value(fuel: Double, distance: Int, unit: String): Double =
        if (unit == "mi") distance / fuel else fuel / distance * 100

    /** One point per fill-up (from the second onwards), each smoothed over up to [WINDOW] fills. */
    fun points(logs: List<FuelLog>, unit: String, window: Int = WINDOW): List<Point> {
        val fills = usable(logs)
        val points = mutableListOf<Point>()
        for (k in 1 until fills.size) {
            val start = maxOf(0, k - window)
            val distance = fills[k].odometer - fills[start].odometer
            val fuel = (start + 1..k).sumOf { fills[it].amount }
            if (distance > 0 && fuel > 0) {
                points.add(Point(fills[k].date, value(fuel, distance, unit)))
            }
        }
        return points
    }

    /** Fuel burned between the first and last fill-up divided by the distance driven, or null. */
    fun average(logs: List<FuelLog>, unit: String): Double? {
        val fills = usable(logs)
        if (fills.size < 2) return null
        val distance = fills.last().odometer - fills.first().odometer
        val fuel = fills.drop(1).sumOf { it.amount }
        return if (distance > 0 && fuel > 0) value(fuel, distance, unit) else null
    }

    private fun usable(logs: List<FuelLog>) =
        logs.filter { it.amount > 0 }.sortedWith(compareBy({ it.date }, { it.odometer }))
}
