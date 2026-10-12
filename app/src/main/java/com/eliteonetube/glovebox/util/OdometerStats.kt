package com.eliteonetube.glovebox.util

import java.util.concurrent.TimeUnit

object OdometerStats {
    /**
     * Average distance driven per day, from (timestamp, odometer) readings.
     * Returns null when there isn't enough data (fewer than two readings, no elapsed full day,
     * or an odometer that didn't increase).
     */
    fun averageDailyDistance(readings: List<Pair<Long, Int>>): Double? {
        if (readings.size < 2) return null
        val sorted = readings.sortedBy { it.first }
        val distance = sorted.last().second - sorted.first().second
        val days = TimeUnit.MILLISECONDS.toDays(sorted.last().first - sorted.first().first)
        return if (distance > 0 && days > 0) distance.toDouble() / days else null
    }
}
